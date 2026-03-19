package com.finup.transacao;

import com.finup.auth.AuthService;
import com.finup.classePrincipal.ClassePrincipalRepository;
import com.finup.cnpjs.CnpjRepository;
import com.finup.infra.exceptions.ValidacaoException;
import com.finup.pessoaFisica.PessoaFisica;
import com.finup.pessoaFisica.PessoaFisicaRepository;
import com.finup.subclasse.SubClasseRepository;
import com.finup.transacao.dtos.CreateTransacaoRequest;
import com.finup.transacao.dtos.DetailTransacaoResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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
    private AuthService  authService;

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

        atribuirCamposOpcionais(transacao, dados);
        transacaoRepository.save(transacao);

        return new DetailTransacaoResponse(transacao);
    }

    private void atribuirCamposOpcionais(Transacao transacao, CreateTransacaoRequest dados) {
        if (dados.cnpjId() != null) {
            transacao.setCnpj(cnpjRepository.findById(dados.cnpjId()).orElse(null));
        }

        if (dados.classePrincipalId() != null) {
            transacao.setClassePrincipal(classePrincipalRepository.findById(dados.classePrincipalId()).orElse(null));
        }

        if (dados.subClasseId() != null) {
            transacao.setSubClasse(subClasseRepository.findById(dados.subClasseId()).orElse(null));
        }
    }
}
