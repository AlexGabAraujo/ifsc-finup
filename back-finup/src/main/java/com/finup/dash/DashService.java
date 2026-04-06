package com.finup.dash;

import com.finup.dash.dto.DetailGraficoDashResponse;
import com.finup.dash.dto.DetailGraficoPizzaResponse;
import com.finup.dash.dto.DetailInfoDashResponse;
import com.finup.dash.dto.DetailTransacoesResponse;
import com.finup.pessoaFisica.PessoaFisica;
import com.finup.pessoaFisica.PessoaFisicaRepository;
import com.finup.transacao.TipoGasto;
import com.finup.transacao.Transacao;
import com.finup.transacao.TransacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DashService {

    @Autowired
    private PessoaFisicaRepository pessoaFisicaRepository;

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private DashRepository dashRepository;


    //Cards

    @Transactional
    public void setAtualizarMeta(Long id, BigDecimal novaMetaEco) {
        PessoaFisica p = pessoaFisicaRepository.findById(id).orElseThrow();
        p.setMetaEconomia(novaMetaEco);
    }

    @Transactional
    public void setAtualizarMetaEcoAtual(Long id, BigDecimal valorEco
    ) {
        PessoaFisica p = pessoaFisicaRepository.findById(id).orElseThrow();
        BigDecimal valorAntigo = p.getMetaEcoAtual() != null ? p.getMetaEcoAtual() : BigDecimal.ZERO;
        p.setMetaEcoAtual(valorAntigo.add(valorEco));
    }

    @Transactional(readOnly = true)
    public Double calcularPorcentagem(Long id) {
        PessoaFisica p = pessoaFisicaRepository.findById(id).orElseThrow();

        BigDecimal valorAtual = p.getMetaEcoAtual() != null ? p.getMetaEcoAtual() : BigDecimal.ZERO;
        BigDecimal valorMeta = p.getMetaEconomia();

        if (valorMeta == null || valorMeta.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

        Double porcenMetaEco = valorAtual.divide(valorMeta, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).doubleValue();
        return porcenMetaEco;
    }

    @Transactional(readOnly = true)
    public BigDecimal getMetaAtual(Long id) {
        PessoaFisica p = pessoaFisicaRepository.findById(id).orElseThrow();
        BigDecimal valorMeta = p.getMetaEconomia() != null ? p.getMetaEconomia() : BigDecimal.ZERO;
        return valorMeta;
    }

    @Transactional(readOnly = true)
    public BigDecimal getMetaEcoAtual(Long id) {
        PessoaFisica p = pessoaFisicaRepository.findById(id).orElseThrow();
        BigDecimal valorAtual = p.getMetaEcoAtual() != null ? p.getMetaEcoAtual() : BigDecimal.ZERO;
        return valorAtual;
    }

    @Transactional(readOnly = true)
    public BigDecimal getReceitaTotal(Long pessoaFisicaId) {

        LocalDateTime inicio = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime fim = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);

        BigDecimal ReceitaTotal = dashRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.CREDITO, inicio, fim) != null ? dashRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.CREDITO, inicio, fim) : BigDecimal.ZERO;
        return ReceitaTotal;
    }

    @Transactional(readOnly = true)
    public BigDecimal consultarDespesas(Long pessoaFisicaId) {

        LocalDateTime inicio = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime fim = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);

        BigDecimal totalDespesa = dashRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.DEBITO, inicio, fim);
        return totalDespesa != null ? totalDespesa : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal saldoAtual(Long pessoaFisicaId) {

        LocalDateTime inicio = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime fim = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);

        BigDecimal totalReceita = dashRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.CREDITO, inicio, fim) != null ? dashRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.CREDITO, inicio, fim) : BigDecimal.ZERO;
        BigDecimal totalDespesa = dashRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.DEBITO, inicio, fim) != null ? dashRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.DEBITO, inicio, fim) : BigDecimal.ZERO;

        BigDecimal receitaAtual = totalReceita.subtract(totalDespesa);

        return receitaAtual;
    }

    @Transactional(readOnly = true)
    public Double getEstatisticaSaldoAtual(Long pessoaFisicaId) {

        LocalDateTime inicio = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime fim = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);

        BigDecimal totalReceita = dashRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.CREDITO, inicio, fim) != null ? dashRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.CREDITO, inicio, fim) : BigDecimal.ZERO;
        BigDecimal totalDespesa = dashRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.DEBITO, inicio, fim) != null ? dashRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.DEBITO, inicio, fim) : BigDecimal.ZERO;

        LocalDate mesPassado = LocalDate.now().minusMonths(1);
        LocalDateTime inicioMesAnterior = mesPassado.atStartOfDay();
        LocalDateTime fimMesAnterior = mesPassado.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);

        BigDecimal totalReceitaMesAnterior = dashRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.CREDITO, inicioMesAnterior, fimMesAnterior) != null ? dashRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.CREDITO, inicioMesAnterior, fimMesAnterior) : BigDecimal.ZERO;
        BigDecimal totalDespesaMesAnterior = dashRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.DEBITO, inicioMesAnterior, fimMesAnterior) != null ? dashRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.DEBITO, inicioMesAnterior, fimMesAnterior) : BigDecimal.ZERO;

        BigDecimal receitaAtual = totalReceita.subtract(totalDespesa);
        BigDecimal receitaMesAnterior = totalReceitaMesAnterior.subtract(totalDespesaMesAnterior);
        if (receitaMesAnterior.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

        Double estatisticaComparacao = receitaAtual.divide(receitaMesAnterior, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).doubleValue();

        return estatisticaComparacao;
    }

    @Transactional(readOnly = true)
    public Double getEstatisticaDespesas(Long pessoaFisicaId) {
        LocalDateTime inicio = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime fim = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);
        LocalDate mesPassado = LocalDate.now().minusMonths(1);
        LocalDateTime inicioMesAnterior = mesPassado.atStartOfDay();
        LocalDateTime fimMesAnterior = mesPassado.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);


        BigDecimal totalDespesa = dashRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.DEBITO, inicio, fim) != null ? dashRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.DEBITO, inicio, fim) : BigDecimal.ZERO;
        BigDecimal totalDespesaMesPassado = dashRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.DEBITO, inicioMesAnterior, fimMesAnterior) != null ? dashRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.DEBITO, inicioMesAnterior, fimMesAnterior) : BigDecimal.ZERO;
        if (totalDespesaMesPassado.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

        Double estatisticaComparacaoDespesas = totalDespesa.divide(totalDespesaMesPassado, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).doubleValue();

        return estatisticaComparacaoDespesas;
    }

    @Transactional(readOnly = true)
    public Double getEstatisticaReceitaAtual(Long pessoaFisicaId) {
        LocalDateTime inicio = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime fim = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);

        LocalDate mesPassado = LocalDate.now().minusMonths(1);
        LocalDateTime inicioMesAnterior = mesPassado.atStartOfDay();
        LocalDateTime fimMesAnterior = mesPassado.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);

        // Busca e garante que não seja null
        BigDecimal totalReceita = dashRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.CREDITO, inicio, fim);
        totalReceita = (totalReceita != null) ? totalReceita : BigDecimal.ZERO;

        BigDecimal totalReceitaAnterior = dashRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.CREDITO, inicioMesAnterior, fimMesAnterior);
        totalReceitaAnterior = (totalReceitaAnterior != null) ? totalReceitaAnterior : BigDecimal.ZERO;

        if (totalReceitaAnterior.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

        return totalReceita.divide(totalReceitaAnterior, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }

    //Graficos

    @Transactional(readOnly = true)
    public DetailGraficoDashResponse buscarGraficoLinha (Long pessoaFisicaId){
        List<String> meses = new ArrayList<>();
        List<BigDecimal> receitas = new ArrayList<>();
        List<BigDecimal> despesas = new ArrayList<>();

        for(int i = 5; i >= 0; i--){            LocalDate mes = LocalDate.now().minusMonths(i);
            LocalDateTime inicio = mes.withDayOfMonth(1).atStartOfDay();
            LocalDateTime fim = mes.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);

            String mesNome = mes.getMonth().getDisplayName(TextStyle.SHORT, new Locale("pt", "BR")).toUpperCase().replace(".","");
            meses.add(mesNome);

            BigDecimal totalReceita = dashRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.CREDITO, inicio, fim);
            receitas.add(totalReceita != null ? totalReceita : BigDecimal.ZERO);

            BigDecimal totalDespesa = dashRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.DEBITO, inicio, fim);
            despesas.add(totalDespesa != null ? totalDespesa : BigDecimal.ZERO);
        }
        return new DetailGraficoDashResponse(meses, receitas, despesas);
    }

    @Transactional(readOnly = true)
    public List<DetailGraficoPizzaResponse> getGraficoPizza (Long pessoaFisicaId){
        LocalDateTime inicio = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime fim = LocalDateTime.now();
        return dashRepository.buscarGastosPorCategoria(pessoaFisicaId,inicio,fim);
    }

    @Transactional(readOnly = true)
    public List<DetailTransacoesResponse> buscarUltimasTransacoes(Long pessoaFisicaId){
        List<Transacao> transacoes = dashRepository.findTop6ByPessoaFisicaIdOrderByDataInsercaoDesc(pessoaFisicaId);

        return transacoes.stream().map(t -> new DetailTransacoesResponse(
                t.getSubClasse() != null ? t.getSubClasse().getNome() : t.getClassePrincipal().getNome(),
                t.getValor(),
                t.getDataInsercao().toLocalDate(),
                t.getTipoGasto().name()
        )).toList();
    }

    public DetailInfoDashResponse buscarResumoCompleto(Long id) {
        return new DetailInfoDashResponse(
                getReceitaTotal(id),
                getMetaAtual(id),
                saldoAtual(id),
                consultarDespesas(id),
                getMetaEcoAtual(id),
                getEstatisticaSaldoAtual(id),
                getEstatisticaDespesas(id),
                getEstatisticaReceitaAtual(id),
                calcularPorcentagem(id)
        );
    }
}
