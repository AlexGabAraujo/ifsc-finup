package com.finup.transacao;

import com.finup.auth.AuthService;
import com.finup.categoria.Categoria;
import com.finup.categoria.CategoriaRepository;
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
import java.util.List;

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

    @Autowired
    private CategoriaRepository categoriaRepository;

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

        if (dados.categoriaId() != null) {
            Categoria categoria = categoriaRepository.findById(dados.categoriaId())
                    .orElseThrow(() -> new ValidacaoException("Categoria não encontrada."));

            if (!categoria.getPessoaFisica().getId().equals(pessoa.getId()))
                throw new ValidacaoException("Categoria não pertence ao usuário.");

            transacao.setCategoria(categoria);
        }

        atribuirCamposOpcionais(transacao, dados);
        transacaoRepository.save(transacao);

        return new DetailTransacaoResponse(transacao);
    }

    private void atribuirCamposOpcionais(Transacao transacao, CreateTransacaoRequest dados) {
        if (dados.cnpjId() != null) {
            transacao.setCnpj(cnpjRepository.findById(dados.cnpjId()).orElse(null));
        }

        if(dados.classePrincipalId() == null && dados.subClasseId() == null){
            throw new ValidacaoException("Pelo menos uma categoria deve ser atribuída a uma transação.");
        }

        if (dados.classePrincipalId() != null) {
            transacao.setClassePrincipal(classePrincipalRepository.findById(dados.classePrincipalId())
                    .orElseThrow(() -> new ValidacaoException("Classe Principal informada não existe.")));
        }

        if (dados.subClasseId() != null) {
            transacao.setSubClasse(subClasseRepository.findById(dados.subClasseId())
                    .orElseThrow(() -> new ValidacaoException("SubClasse informada não existe.")));;
        }
    }


}
