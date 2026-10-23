package com.finup.pluggy;

import ai.pluggy.client.PluggyClient;
import ai.pluggy.client.response.DocumentNumber;
import ai.pluggy.client.response.Transaction;
import ai.pluggy.client.response.TransactionPaymentParticipant;
import ai.pluggy.client.response.TransactionType;
import br.com.caelum.stella.validation.CNPJValidator;
import com.finup.categoria.Categoria;
import com.finup.categoria.CategoriaRepository;
import com.finup.classePrincipal.ClassePrincipal;
import com.finup.classePrincipal.ClassePrincipalRepository;
import com.finup.cnpjs.Cnpj;
import com.finup.cnpjs.CnpjService;
import com.finup.pessoaFisica.PessoaFisica;
import com.finup.pessoaFisica.PessoaFisicaRepository;
import com.finup.pluggy.contaBancaria.ContaBancaria;
import com.finup.pluggy.contaBancaria.ContaBancariaRepository;
import com.finup.pluggy.itemBancario.ItemBancario;
import com.finup.pluggy.itemBancario.ItemBancarioRepository;
import com.finup.transacao.Transacao;
import com.finup.transacao.TransacaoRepository;
import com.finup.transacao.TipoGasto;
import com.finup.transacao.TipoPagamento;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class PluggySyncService {

    private static final Logger log = LoggerFactory.getLogger(PluggySyncService.class);
    private static final String CLASSE_INDEFINIDA = "Indefinido";

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Value("${CLIENT_ID_PLUGGY}")
    private String clientId;

    @Value("${CLIENT_SECRET_PLUGGY}")
    private String clientSecret;

    private PluggyClient pluggy;

    private final ItemBancarioRepository itemBancarioRepository;
    private final ContaBancariaRepository contaBancariaRepository;
    private final TransacaoRepository transacaoRepository;
    private final PessoaFisicaRepository pessoaFisicaRepository;
    private final CnpjService cnpjService;
    private final ClassePrincipalRepository classePrincipalRepository;

    public PluggySyncService(
            ItemBancarioRepository itemBancarioRepository,
            ContaBancariaRepository contaBancariaRepository,
            TransacaoRepository transacaoRepository,
            PessoaFisicaRepository pessoaFisicaRepository,
            CnpjService cnpjService,
            ClassePrincipalRepository classePrincipalRepository) {
        this.itemBancarioRepository = itemBancarioRepository;
        this.contaBancariaRepository = contaBancariaRepository;
        this.transacaoRepository = transacaoRepository;
        this.pessoaFisicaRepository = pessoaFisicaRepository;
        this.cnpjService = cnpjService;
        this.classePrincipalRepository = classePrincipalRepository;
    }

    @PostConstruct
    public void init() {
        this.pluggy = PluggyClient.builder()
                .clientIdAndSecret(clientId, clientSecret)
                .build();
    }

    public void registrarItem(String pluggyItemId, Long pessoaFisicaId) throws Exception {
        // Se o item já foi registrado, não precisa fazer nada
        if (itemBancarioRepository.findByPluggyItemId(pluggyItemId).isPresent()) {
            return;
        }

        PessoaFisica pessoa = pessoaFisicaRepository.findById(pessoaFisicaId)
                .orElseThrow(() -> new RuntimeException("Pessoa física não encontrada"));

        var itemResponse = pluggy.service().getItem(pluggyItemId).execute();
        if (!itemResponse.isSuccessful()) {
            throw new RuntimeException("Erro ao buscar item: " + pluggy.parseError(itemResponse));
        }

        String nomeInstituicao = itemResponse.body().getConnector().getName();

        ItemBancario item = ItemBancario.builder()
                .pluggyItemId(pluggyItemId)
                .pessoaFisica(pessoa)
                .nomeInstituicao(nomeInstituicao)
                .build();

        itemBancarioRepository.save(item);
    }

    public void sincronizarTransacoes(String pluggyItemId) throws Exception {
        ItemBancario item = itemBancarioRepository.findByPluggyItemId(pluggyItemId)
                .orElseThrow(() -> new RuntimeException("Item não encontrado. Registre o item antes de sincronizar."));

        var accountsResponse = pluggy.service().getAccounts(pluggyItemId).execute();
        if (!accountsResponse.isSuccessful()) {
            throw new RuntimeException("Erro ao buscar contas: " + pluggy.parseError(accountsResponse));
        }

        ClassePrincipal classeIndefinida = classePrincipalRepository.findFirstByNomeOrderByIdAsc(CLASSE_INDEFINIDA).orElse(null);

        for (var account : accountsResponse.body().getResults()) {
            boolean isCartaoCredito = "CREDIT".equals(account.getType());

            ContaBancaria conta = contaBancariaRepository.findByPluggyAccountId(account.getId())
                    .orElseGet(() -> {
                        ContaBancaria nova = ContaBancaria.builder()
                                .itemBancario(item)
                                .pluggyAccountId(account.getId())
                                .marketingName(account.getMarketingName())
                                .tipo(account.getType())
                                .subtipo(account.getSubtype())
                                .build();
                        return contaBancariaRepository.save(nova);
                    });

            var transactionsResponse = pluggy.service().getTransactions(account.getId()).execute();
            if (!transactionsResponse.isSuccessful()) {
                throw new RuntimeException("Erro ao buscar transações: " + pluggy.parseError(transactionsResponse));
            }

            for (var pluggyTx : transactionsResponse.body().getResults()) {
                TipoGasto tipoGasto = pluggyTx.getType() == TransactionType.CREDIT
                        ? TipoGasto.CREDITO
                        : TipoGasto.DEBITO;

                Optional<Transacao> existente = transacaoRepository.findByExternalId(pluggyTx.getId());
                if (existente.isPresent()) {
                    atualizarTransacaoExistente(existente.get(), pluggyTx, tipoGasto, classeIndefinida);
                    continue;
                }

                TipoPagamento tipoPagamento;
                if (isCartaoCredito) {
                    tipoPagamento = TipoPagamento.CARTAO_DE_CREDITO;
                } else if (pluggyTx.getPaymentData() != null) {
                    String metodo = pluggyTx.getPaymentData().getPaymentMethod();
                    if ("BOLETO".equals(metodo)) {
                        tipoPagamento = TipoPagamento.BOLETO;
                    } else if ("PIX".equals(metodo)) {
                        tipoPagamento = TipoPagamento.PIX;
                    } else {
                        tipoPagamento = TipoPagamento.TRANSFERENCIA;
                    }
                } else {
                    tipoPagamento = TipoPagamento.TRANSFERENCIA;
                }

                Transacao transacao = Transacao.builder()
                        .valor(BigDecimal.valueOf(Math.abs(pluggyTx.getAmount())))
                        .pessoaFisica(item.getPessoaFisica())
                        .tipoPagamento(tipoPagamento)
                        .tipoGasto(tipoGasto)
                        .externalId(pluggyTx.getId())
                        .contaBancaria(conta)
                        .build();

                aplicarClassificacao(transacao, buscarEmpresa(pluggyTx, tipoGasto).orElse(null), classeIndefinida);

                transacaoRepository.save(transacao);
            }
        }
    }

    /**
     * Transação já importada em uma sincronização anterior.
     * - Corrige o tipo de gasto (importações antigas gravavam tudo como DEBITO).
     * - Se ficou "Indefinido" sem CNPJ (ex: CNPJá/cnae-api indisponível na época),
     *   tenta classificar de novo pelo CNPJ.
     * Só salva se algo mudou.
     */
    private void atualizarTransacaoExistente(Transacao transacao, Transaction pluggyTx, TipoGasto tipoGasto,
                                             ClassePrincipal classeIndefinida) {
        boolean alterada = false;

        if (transacao.getTipoGasto() != tipoGasto) {
            transacao.setTipoGasto(tipoGasto);
            alterada = true;
        }

        boolean semClassificacao = transacao.getCnpj() == null
                && (transacao.getClassePrincipal() == null
                    || (classeIndefinida != null && classeIndefinida.getId().equals(transacao.getClassePrincipal().getId())));

        if (semClassificacao) {
            Optional<Cnpj> empresa = buscarEmpresa(pluggyTx, tipoGasto);
            if (empresa.isPresent()) {
                aplicarClassificacao(transacao, empresa.get(), classeIndefinida);
                alterada = true;
            }
        }

        if (alterada) {
            transacaoRepository.save(transacao);
        }
    }

    /**
     * Valida o CNPJ da transação e busca a empresa pelo CnpjService:
     * se o CNPJ já está salvo usa o registro salvo; senão consulta o CNPJá,
     * classifica o CNAE na cnae-api e salva na tabela cnpj para as próximas transações.
     * Vazio se não houver CNPJ válido ou se o CNPJá/cnae-api estiverem indisponíveis.
     */
    private Optional<Cnpj> buscarEmpresa(Transaction pluggyTx, TipoGasto tipoGasto) {
        String cnpj = extrairCnpj(pluggyTx, tipoGasto);
        if (cnpj == null) {
            return Optional.empty();
        }

        try {
            // Banco (cnpj já salvo) -> CNPJá -> cnae-api -> salva na tabela cnpj
            return cnpjService.buscarOuCriarCnpj(cnpj);
        } catch (Exception e) {
            log.error("Falha ao classificar CNPJ {} da transação {}: {}", cnpj, pluggyTx.getId(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Aplica na transação a classificação da empresa (CNPJ, classe principal e subclasse).
     * Sem empresa, a transação fica como "Indefinido".
     * A categoria do usuário é escolhida pela subclasse, depois pela classe principal
     * e, por último, é usada a primeira categoria do usuário.
     */
    private void aplicarClassificacao(Transacao transacao, Cnpj empresa, ClassePrincipal classeIndefinida) {
        if (empresa != null) {
            transacao.setCnpj(empresa);
            transacao.setClassePrincipal(empresa.getClassePrincipal());
            transacao.setSubClasse(empresa.getSubClasse());
        }

        if (transacao.getClassePrincipal() == null) {
            transacao.setClassePrincipal(classeIndefinida);
        }

        Long pessoaFisicaId = transacao.getPessoaFisica().getId();
        Categoria categoria = null;
        if (transacao.getSubClasse() != null) {
            categoria = categoriaRepository
                    .findByPessoaFisicaIdAndSubClasseId(pessoaFisicaId, transacao.getSubClasse().getId())
                    .orElse(null);
        }
        if (categoria == null && transacao.getClassePrincipal() != null) {
            categoria = categoriaRepository
                    .findByPessoaFisicaIdAndClassePrincipalId(pessoaFisicaId, transacao.getClassePrincipal().getId())
                    .orElse(null);
        }
        if (categoria == null) {
            categoria = categoriaRepository.findFirstByPessoaFisicaId(pessoaFisicaId);
        }
        transacao.setCategoria(categoria);
    }

    /**
     * Procura o CNPJ da empresa na transação: primeiro no merchant e, se não houver,
     * no documento da contraparte (recebedor em débitos, pagador em créditos).
     * Retorna somente CNPJs válidos (dígitos verificadores conferidos), ou null.
     */
    private String extrairCnpj(Transaction pluggyTx, TipoGasto tipoGasto) {
        if (pluggyTx.getMerchant() != null) {
            String cnpjMerchant = somenteDigitos(pluggyTx.getMerchant().getCnpj());
            if (cnpjValido(cnpjMerchant)) {
                return cnpjMerchant;
            }
        }

        if (pluggyTx.getPaymentData() != null) {
            TransactionPaymentParticipant contraparte = tipoGasto == TipoGasto.CREDITO
                    ? pluggyTx.getPaymentData().getPayer()
                    : pluggyTx.getPaymentData().getReceiver();
            DocumentNumber documento = contraparte != null ? contraparte.getDocumentNumber() : null;
            if (documento != null) {
                String cnpjContraparte = somenteDigitos(documento.getValue());
                if (cnpjValido(cnpjContraparte)) {
                    return cnpjContraparte;
                }
            }
        }

        return null;
    }

    private static boolean cnpjValido(String cnpj) {
        return cnpj != null && cnpj.length() == 14 && new CNPJValidator().invalidMessagesFor(cnpj).isEmpty();
    }

    private static String somenteDigitos(String valor) {
        return valor == null ? null : valor.replaceAll("[^0-9]", "");
    }
}
