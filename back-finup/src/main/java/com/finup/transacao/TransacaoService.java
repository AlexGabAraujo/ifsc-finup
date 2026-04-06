package com.finup.transacao;

import com.finup.auth.AuthService;
import com.finup.classePrincipal.ClassePrincipalRepository;
import com.finup.cnpjs.CnpjRepository;
import com.finup.infra.exceptions.AutorizacaoException;
import com.finup.infra.exceptions.ValidacaoException;
import com.finup.pessoaFisica.PessoaFisicaRepository;
import com.finup.subclasse.SubClasseRepository;
import com.finup.transacao.dtos.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class TransacaoService {

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private PessoaFisicaRepository pessoaFisicaRepository;

    @Autowired
    private CnpjRepository cnpjRepository;

    @Autowired
    private ClassePrincipalRepository classePrincipalRepository;

    @Autowired
    private SubClasseRepository subClasseRepository;

    @Autowired
    private AuthService authService;

    @Transactional
    public DetailTransacaoResponse createTransacao(CreateTransacaoRequest dados) {
        var pessoa = authService.getUsuarioAutenticado();

        if (dados.valor().compareTo(BigDecimal.ZERO) <= 0)
            throw new ValidacaoException("O valor da transação deve ser maior que R$ 0,00.");

        Transacao transacao = new Transacao();
        transacao.setValor(dados.valor());
        transacao.setPessoaFisica(pessoa);
        transacao.setTipoPagamento(dados.tipoPagamento());
        transacao.setTipoGasto(dados.tipoGasto());
        transacao.setDataTransacao(dados.dataTransacao()); // Task 5.1

        atribuirCamposOpcionais(transacao, dados);
        transacaoRepository.save(transacao);

        return new DetailTransacaoResponse(transacao);
    }

    public InsightsResponse getInsights(Integer mes, Integer ano) {
        boolean mesPresente = mes != null;
        boolean anoPresente = ano != null;

        if (mesPresente != anoPresente)
            throw new ValidacaoException("Os parâmetros mes e ano devem ser informados juntos.");

        var usuario = authService.getUsuarioAutenticado();
        Long userId = usuario.getId();

        BigDecimal totalReceitas = transacaoRepository.sumValorByTipoGastoAndPessoaFisicaId(userId, TipoGasto.CREDITO, mes, ano);
        BigDecimal totalDespesas = transacaoRepository.sumValorByTipoGastoAndPessoaFisicaId(userId, TipoGasto.DEBITO, mes, ano);
        Long totalTransacoes = transacaoRepository.countByPessoaFisicaId(userId, mes, ano);

        Map<TipoGasto, Long> transacoesPorTipo = Map.of(
                TipoGasto.CREDITO, transacaoRepository.countByTipoGastoAndPessoaFisicaId(userId, TipoGasto.CREDITO, mes, ano),
                TipoGasto.DEBITO, transacaoRepository.countByTipoGastoAndPessoaFisicaId(userId, TipoGasto.DEBITO, mes, ano)
        );

        return new InsightsResponse(totalTransacoes, totalReceitas, totalDespesas, transacoesPorTipo);
    }

    public TransacaoPageResponse listarTransacoes(Integer mes, Integer ano, Long categoriaId, String categoriaType, int page) {
        var usuario = authService.getUsuarioAutenticado();

        Page<Transacao> resultado = transacaoRepository.findByFilters(
                usuario.getId(), mes, ano, categoriaId, categoriaType, PageRequest.of(page, 10));

        return new TransacaoPageResponse(
                resultado.getContent().stream().map(DetailTransacaoResponse::new).toList(),
                resultado.getTotalElements(),
                resultado.getTotalPages(),
                page,
                10
        );
    }

    @Transactional
    public DetailTransacaoResponse atualizarTransacao(Long id, UpdateTransacaoRequest dados) {
        Transacao transacao = transacaoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transação não encontrada."));

        var usuarioAutenticado = authService.getUsuarioAutenticado();

        if (!transacao.getPessoaFisica().getId().equals(usuarioAutenticado.getId()))
            throw new AutorizacaoException("Você não tem permissão para editar esta transação.");

        transacao.setValor(dados.valor());
        transacao.setTipoPagamento(dados.tipoPagamento());
        transacao.setTipoGasto(dados.tipoGasto());
        transacao.setDataTransacao(dados.dataTransacao());

        transacao.setSubClasse(dados.subClasseId() != null
                ? subClasseRepository.findById(dados.subClasseId()).orElse(null) : null);

        transacao.setClassePrincipal(dados.classePrincipalId() != null
                ? classePrincipalRepository.findById(dados.classePrincipalId()).orElse(null) : null);

        transacao.setCnpj(dados.cnpjId() != null
                ? cnpjRepository.findById(dados.cnpjId()).orElse(null) : null);

        return new DetailTransacaoResponse(transacao);
    }

    @Transactional
    public void deletarTransacao(Long id) {
        Transacao transacao = transacaoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transação não encontrada."));

        var usuarioAutenticado = authService.getUsuarioAutenticado();

        if (!transacao.getPessoaFisica().getId().equals(usuarioAutenticado.getId()))
            throw new AutorizacaoException("Você não tem permissão para excluir esta transação.");

        transacaoRepository.delete(transacao);
    }

    private void atribuirCamposOpcionais(Transacao transacao, CreateTransacaoRequest dados) {
        if (dados.cnpjId() != null) {
            transacao.setCnpj(cnpjRepository.findById(dados.cnpjId()).orElse(null));
        }

        if (dados.classePrincipalId() == null && dados.subClasseId() == null)
            throw new ValidacaoException("Pelo menos uma categoria deve ser atribuída a uma transação.");

        if (dados.classePrincipalId() != null) {
            transacao.setClassePrincipal(classePrincipalRepository.findById(dados.classePrincipalId())
                    .orElseThrow(() -> new ValidacaoException("Classe Principal informada não existe.")));
        }

        if (dados.subClasseId() != null) {
            transacao.setSubClasse(subClasseRepository.findById(dados.subClasseId())
                    .orElseThrow(() -> new ValidacaoException("SubClasse informada não existe.")));
        }
    }
}
