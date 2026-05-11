package com.finup.categoria;

import com.finup.auth.AuthService;
import com.finup.categoria.UtilCategoria.ComparacaoFiltro;
import com.finup.categoria.dto.*;
import com.finup.classePrincipal.ClassePrincipalRepository;
import com.finup.infra.exceptions.ValidacaoException;
import com.finup.pessoaFisica.PessoaFisicaRepository;
import com.finup.subclasse.SubClasseRepository;
import com.finup.transacao.TransacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CategoriaService {

    private static final DateTimeFormatter LABEL_MES = DateTimeFormatter.ofPattern("yyyy-MM");

    @Autowired
    private ClassePrincipalRepository classePrincipalRepository;

    @Autowired
    private SubClasseRepository subClasseRepository;

    @Autowired
    private AuthService  authService;

    @Autowired
    private PessoaFisicaRepository pessoaFisicaRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private TransacaoRepository transacaoRepository;


    @Transactional
    public DetailCategoriaResponse criarCategoria(CreateCategoriaRequest dados) {
        var pessoa = authService.getUsuarioAutenticado();

        if(dados.orcamento().compareTo(BigDecimal.ZERO)<=0)
            throw new ValidacaoException("O valor do orçamento deve ser maior que R$ 0,00.");

        Categoria c = new Categoria();
        c.setOrcamento(dados.orcamento());
        c.setPessoaFisica(pessoa);

        camposCategoria(c, dados);
        categoriaRepository.save(c);

        return new DetailCategoriaResponse(c);
    }

    private void camposCategoria(Categoria c, CreateCategoriaRequest dados) {

        if(dados.classePrincipalId() == null && dados.subClasseId() == null){
            throw new ValidacaoException("Pelo menos uma categoria deve ser atribuída a uma transação.");
        }

        if (dados.classePrincipalId() != null) {
            c.setClassePrincipal(classePrincipalRepository.findById(dados.classePrincipalId())
                    .orElseThrow(() -> new ValidacaoException("Classe Principal informada não existe.")));
        }

        if (dados.subClasseId() != null) {
            c.setSubClasse(subClasseRepository.findById(dados.subClasseId())
                    .orElseThrow(() -> new ValidacaoException("SubClasse informada não existe.")));;
        }
    }

    @Transactional
    public DetailCategoriaResponse atualizarCategoria(Long id, UpdateCategoriaRequest dados) {

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ValidacaoException("Categoria não encontrada."));

        if (dados.orcamento().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacaoException("O valor do orçamento deve ser maior que R$ 0,00.");
        }

        /*categoria.setIcone(dados.icone());
        categoria.setCor(dados.cor());*/
        categoria.setOrcamento(dados.orcamento());

        return new DetailCategoriaResponse(categoria);
    }

    @Transactional
    public void deletarCategoria(Long id) {

        Categoria categoria = categoriaRepository.findById(id).orElseThrow(() -> new ValidacaoException("Categoria não encontrada."));
        var pessoa = authService.getUsuarioAutenticado();
        if (!categoria.getPessoaFisica().getId().equals(pessoa.getId()))
            throw new ValidacaoException("Categoria não pertence ao usuário.");

        // Deleta primeiro as transações vinculadas (evita erro de FK)
        transacaoRepository.deleteAllByCategoriaId(id);

        categoriaRepository.delete(categoria);
    }

    @Transactional(readOnly = true)
    public DetailCategoriaCardResponse getResumoCategoria(Long categoriaId, PeriodoFiltro periodo) {
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ValidacaoException("Categoria não encontrada."));

        var pessoa = authService.getUsuarioAutenticado();
        if (!categoria.getPessoaFisica().getId().equals(pessoa.getId()))
            throw new ValidacaoException("Categoria não pertence ao usuário.");

        return calcularResumo(categoria, periodo);
    }

    @Transactional(readOnly = true)
    public DetailCategoriaOrcamentoResponse getOrcamentoTotal(PeriodoFiltro periodo) {

        var pessoa = authService.getUsuarioAutenticado();
        List<Categoria> categorias = categoriaRepository.findAllByPessoaFisicaId(pessoa.getId());

        List<DetailCategoriaCardResponse> resumos = categorias.stream()
                .map(c -> calcularResumo(c, periodo))
                .toList();

        BigDecimal orcamentoTotal = resumos.stream()
                .map(DetailCategoriaCardResponse::orcamento)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal gastoTotal = resumos.stream()
                .map(DetailCategoriaCardResponse::gasto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal disponivel = orcamentoTotal.subtract(gastoTotal);
        BigDecimal percentualUso = calcularPercentual(gastoTotal, orcamentoTotal);

        return new DetailCategoriaOrcamentoResponse(
                orcamentoTotal,
                gastoTotal,
                disponivel,
                percentualUso,
                resumos
        );
    }

    private DetailCategoriaCardResponse calcularResumo(Categoria categoria, PeriodoFiltro periodo) {
        var intervaloAtual = ComparacaoFiltro.calcularIntervalo(periodo);
        var intervaloAnterior = ComparacaoFiltro.intervaloPeriodoAnterior(periodo);

        Long categoriaId = categoria.getId();

        BigDecimal gastoAtual = nullToZero(transacaoRepository.somarPorCategoriaEPeriodo(
                categoriaId, intervaloAtual.inicio(), intervaloAtual.fim()
        ));

        BigDecimal gastoAnterior = nullToZero(transacaoRepository.somarPorCategoriaEPeriodo(
                categoriaId, intervaloAnterior.inicio(), intervaloAnterior.fim()
        ));

        Long quantidade = transacaoRepository.contarPorCategoriaEPeriodo(
                categoriaId, intervaloAtual.inicio(), intervaloAtual.fim()
        );
        if (quantidade == null) quantidade = 0L;

        BigDecimal orcamento = nullToZero(categoria.getOrcamento());
        BigDecimal disponivel = orcamento.subtract(gastoAtual);
        BigDecimal percentualGasto = calcularPercentual(gastoAtual, orcamento);
        BigDecimal variacao = calcularVariacao(gastoAtual, gastoAnterior);

        return new DetailCategoriaCardResponse(
                categoria.getId(),
                obterNomeCategoria(categoria),
                obterDescricaoCategoria(categoria),
                orcamento,
                gastoAtual,
                disponivel,
                percentualGasto,
                gastoAnterior,
                variacao,
                quantidade
        );
    }

    private BigDecimal calcularPercentual(BigDecimal valor, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return valor.multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularVariacao(BigDecimal atual, BigDecimal anterior) {
        if (anterior == null || anterior.compareTo(BigDecimal.ZERO) <= 0) {
            // Sem base de comparação. Se gastou algo agora, retornamos 100%; senão 0%.
            return atual.compareTo(BigDecimal.ZERO) > 0
                    ? BigDecimal.valueOf(100)
                    : BigDecimal.ZERO;
        }
        return atual.subtract(anterior)
                .multiply(BigDecimal.valueOf(100))
                .divide(anterior, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal nullToZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String obterNomeCategoria(Categoria c) {
        if (c.getSubClasse() != null) {
            return safeNome(c.getSubClasse().getNome());
        }
        if (c.getClassePrincipal() != null) {
            return safeNome(c.getClassePrincipal().getNome());
        }
        return "Categoria";
    }

    private String obterDescricaoCategoria(Categoria c) {
        if (c.getClassePrincipal() != null && c.getSubClasse() != null) {
            return c.getClassePrincipal().toString() + " - " + c.getSubClasse().toString();
        }
        return null;
    }

    private String safeNome(String s) {
        return s == null ? "Categoria" : s;
    }

    @Transactional(readOnly = true)
    public DetailGraficoCategoriaResponse getDadosGraficoLinha(PeriodoFiltro periodo) {
        var pessoa = authService.getUsuarioAutenticado();

        var intervalo = ComparacaoFiltro.calcularIntervalo(periodo);

        List<Categoria> categorias = categoriaRepository.findAllByPessoaFisicaId(pessoa.getId());

        List<Object[]> linhas = transacaoRepository.somarPorMesECategoria(
                pessoa.getId(), intervalo.inicio(), intervalo.fim()
        );

        Map<YearMonth, Map<Long, BigDecimal>> agrupado = new TreeMap<>();
        for (Object[] linha : linhas) {
            int ano = ((Number) linha[0]).intValue();
            int mes = ((Number) linha[1]).intValue();
            Long catId = ((Number) linha[2]).longValue();
            BigDecimal total = (BigDecimal) linha[3];

            YearMonth ym = YearMonth.of(ano, mes);
            agrupado.computeIfAbsent(ym, k -> new HashMap<>()).put(catId, total);
        }

        List<YearMonth> meses = listarMesesNoIntervalo(intervalo);
        List<String> labels = meses.stream()
                .map(ym -> ym.format(LABEL_MES))
                .toList();

        List<DetailGraficoCategoriaResponse.DatasetCategoria> datasets = categorias.stream()
                .map(c -> {
                    List<BigDecimal> serie = meses.stream()
                            .map(ym -> agrupado
                                    .getOrDefault(ym, Map.of())
                                    .getOrDefault(c.getId(), BigDecimal.ZERO))
                            .toList();
                    return new DetailGraficoCategoriaResponse.DatasetCategoria(
                            c.getId(),
                            obterNomeCategoria(c),
                            serie
                    );
                })
                .toList();

        return new DetailGraficoCategoriaResponse(labels, datasets);
    }

    private List<YearMonth> listarMesesNoIntervalo(ComparacaoFiltro.IntervaloPeriodo intervalo) {
        YearMonth atual = YearMonth.from(intervalo.inicio());
        YearMonth fim = YearMonth.from(intervalo.fim());
        List<YearMonth> meses = new ArrayList<>();
        while (!atual.isAfter(fim)) {
            meses.add(atual);
            atual = atual.plusMonths(1);
        }
        return meses;
    }

    public DetailCategoriaResponse buscarPorId(Long id) {

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ValidacaoException("Categoria não encontrada."));

        return new DetailCategoriaResponse(categoria);
    }


}
