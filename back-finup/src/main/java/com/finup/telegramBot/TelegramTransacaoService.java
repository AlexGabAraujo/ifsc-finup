package com.finup.telegramBot;

import com.finup.categoria.CategoriaRepository;
import com.finup.classePrincipal.ClassePrincipal;
import com.finup.classePrincipal.ClassePrincipalRepository;
import com.finup.cnpjs.CnpjRepository;
import com.finup.cnpjs.CnpjService;
import com.finup.credencial.Credencial;
import com.finup.credencial.CredencialRepository;
import com.finup.openai.ChatService;
import com.finup.openai.IaResponseParser;
import com.finup.pessoaFisica.PessoaFisica;
import com.finup.transacao.TipoGasto;
import com.finup.transacao.TipoPagamento;
import com.finup.transacao.Transacao;
import com.finup.transacao.TransacaoRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TelegramTransacaoService {

    private static final Logger log = LoggerFactory.getLogger(TelegramTransacaoService.class);

    private final CredencialRepository credencialRepository;
    private final ClassePrincipalRepository classePrincipalRepository;
    private final CategoriaRepository categoriaRepository;
    private final TransacaoRepository transacaoRepository;
    private final CnpjService cnpjService;
    private final CnpjRepository cnpjRepository;
    private final ChatService chatService;

    public TelegramTransacaoService(CredencialRepository credencialRepository,
                                    ClassePrincipalRepository classePrincipalRepository,
                                    CategoriaRepository categoriaRepository,
                                    TransacaoRepository transacaoRepository,
                                    CnpjService cnpjService,
                                    CnpjRepository cnpjRepository,
                                    ChatService chatService) {
        this.credencialRepository = credencialRepository;
        this.classePrincipalRepository = classePrincipalRepository;
        this.categoriaRepository = categoriaRepository;
        this.transacaoRepository = transacaoRepository;
        this.cnpjService = cnpjService;
        this.cnpjRepository = cnpjRepository;
        this.chatService = chatService;
    }

    public boolean isVinculado(Long chatId) {
        return credencialRepository.findByTelegramChatId(chatId).isPresent();
    }

    public Optional<PessoaFisica> buscarPessoa(Long chatId) {
        return credencialRepository.findPessoaFisicaByTelegramChatId(chatId);
    }

    @Transactional
    public boolean vincularConta(Long chatId, String username) {
        Optional<Credencial> credencialOpt = credencialRepository.findByUsername(username.trim());
        if (credencialOpt.isEmpty()) return false;

        Credencial credencial = credencialOpt.get();
        credencial.vincularTelegram(chatId);
        credencialRepository.save(credencial);
        log.info("Telegram chatId {} vinculado ao usuario '{}'.", chatId, username);
        return true;
    }

    public String processarOcr(String textoOcr, BotSessionState sessao) {
        String respostaIa = chatService.extrairDadosOcr(textoOcr);
        sessao.setRespostaIa(respostaIa);

        IaResponseParser.parseValor(respostaIa).ifPresent(sessao::setValor);
        IaResponseParser.parseCnpj(respostaIa).ifPresent(sessao::setCnpjIdentificado);
        IaResponseParser.parseCategoria(respostaIa).ifPresent(sessao::setCategoriaSugerida);

        if (sessao.getCnpjIdentificado() != null) {
            cnpjService.buscarOuCriarCnpj(sessao.getCnpjIdentificado()).ifPresent(cnpj -> {
                sessao.setCnpjId(cnpj.getId());
                sessao.setNomeEmpresa(cnpj.getNomeEmpresa());
                if (sessao.getCategoriaSugerida() == null && cnpj.getClassePrincipal() != null) {
                    sessao.setCategoriaSugerida(cnpj.getClassePrincipal().getNome());
                }
            });
        }

        return respostaIa;
    }

    public String processarTexto(String descricao, BotSessionState sessao) {
        String respostaIa = chatService.extrairDadosTexto(descricao);
        sessao.setRespostaIa(respostaIa);

        IaResponseParser.parseValor(respostaIa).ifPresent(sessao::setValor);
        IaResponseParser.parseCategoria(respostaIa).ifPresent(sessao::setCategoriaSugerida);

        return respostaIa;
    }

    public boolean resolverCategoria(BotSessionState sessao, Long pessoaFisicaId) {
        if (sessao.getCategoriaSugerida() == null) return false;

        Optional<ClassePrincipal> cpOpt = classePrincipalRepository.findByNome(sessao.getCategoriaSugerida());
        if (cpOpt.isEmpty()) return false;

        ClassePrincipal cp = cpOpt.get();
        boolean temCategoria = categoriaRepository
                .findByPessoaFisicaIdAndClassePrincipalId(pessoaFisicaId, cp.getId())
                .isPresent();

        if (temCategoria) {
            sessao.setClassePrincipalId(cp.getId());
            return true;
        }
        return false;
    }

    @Transactional
    public Transacao salvarTransacao(BotSessionState sessao, PessoaFisica pessoa, TipoGasto tipoGasto) {
        if (sessao.getValor() == null || sessao.getValor().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalStateException("Valor inválido para a transação.");

        Transacao transacao = new Transacao();
        transacao.setValor(sessao.getValor());
        transacao.setPessoaFisica(pessoa);
        transacao.setTipoPagamento(TipoPagamento.PIX);
        transacao.setTipoGasto(tipoGasto);
        transacao.setDataInsercao(LocalDateTime.now());

        if (sessao.getCnpjId() != null) {
            transacao.setCnpj(cnpjRepository.getReferenceById(sessao.getCnpjId()));
        }

        Long cpId = sessao.getClassePrincipalId();
        if (cpId == null && sessao.getCategoriaSugerida() != null) {
            cpId = classePrincipalRepository.findByNome(sessao.getCategoriaSugerida())
                    .map(ClassePrincipal::getId)
                    .orElse(null);
        }

        if (cpId != null) {
            ClassePrincipal cp = classePrincipalRepository.findById(cpId).orElse(null);
            if (cp != null) {
                transacao.setClassePrincipal(cp);
                categoriaRepository
                        .findByPessoaFisicaIdAndClassePrincipalId(pessoa.getId(), cp.getId())
                        .ifPresent(transacao::setCategoria);
            }
        }

        log.info("Salvando transacao bot: valor={}, tipoGasto={}, classePrincipal={}, pessoa={}",
                sessao.getValor(), tipoGasto,
                transacao.getClassePrincipal() != null ? transacao.getClassePrincipal().getNome() : "sem categoria",
                pessoa.getId());

        return transacaoRepository.save(transacao);
    }

    public String montarMensagemConfirmacao(BotSessionState sessao) {
        StringBuilder sb = new StringBuilder();
        sb.append("Resumo da transacao identificada:\n\n");

        if (sessao.getValor() != null)
            sb.append("Valor: R$ ").append(sessao.getValor()).append("\n");
        else
            sb.append("Valor: nao identificado\n");

        if (sessao.getNomeEmpresa() != null)
            sb.append("Estabelecimento: ").append(sessao.getNomeEmpresa()).append("\n");

        if (sessao.getCnpjIdentificado() != null)
            sb.append("CNPJ: ").append(sessao.getCnpjIdentificado()).append("\n");

        if (sessao.getCategoriaSugerida() != null)
            sb.append("Categoria: ").append(sessao.getCategoriaSugerida()).append("\n");
        else
            sb.append("Categoria: nao identificada\n");

        return sb.toString();
    }
}
