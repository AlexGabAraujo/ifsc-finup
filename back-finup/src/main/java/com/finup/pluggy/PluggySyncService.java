package com.finup.pluggy;

import ai.pluggy.client.PluggyClient;
import ai.pluggy.client.request.TransactionsSearchRequest;
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
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
public class PluggySyncService {

    private static final Logger log = LoggerFactory.getLogger(PluggySyncService.class);
    private static final int PAGE_SIZE = 500;
    private static final String CLASSE_INDEFINIDA = "Indefinido";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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

        String dataInicio = LocalDate.now().minusMonths(12).format(DATE_FMT);
        String dataFim    = LocalDate.now().format(DATE_FMT);

        ClassePrincipal classeIndefinida = classePrincipalRepository
                .findFirstByNomeOrderByIdAsc(CLASSE_INDEFINIDA).orElse(null);

        int totalImportadas = 0;

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

            int paginaAtual = 1;
            int totalPaginas = 1;

            do {
                TransactionsSearchRequest searchRequest = new TransactionsSearchRequest()
                        .from(dataInicio)
                        .to(dataFim)
                        .page(paginaAtual)
                        .pageSize(PAGE_SIZE);

                var transactionsResponse = pluggy.service()
                        .getTransactions(account.getId(), searchRequest)
                        .execute();

                if (!transactionsResponse.isSuccessful()) {
                    log.warn("Erro ao buscar transações da conta {}: {}", account.getId(), pluggy.parseError(transactionsResponse));
                    break;
                }

                var pageResponse = transactionsResponse.body();
                totalPaginas = pageResponse.getTotalPages() != null ? pageResponse.getTotalPages() : 1;

                log.info("Conta {} — página {}/{}, {} transações",
                        account.getId(), paginaAtual, totalPaginas, pageResponse.getResults().size());

                for (var pluggyTx : pageResponse.getResults()) {
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

                    LocalDateTime dataTransacao = parseDate(pluggyTx.getDate());

                    Transacao transacao = Transacao.builder()
                            .valor(BigDecimal.valueOf(Math.abs(pluggyTx.getAmount())))
                            .pessoaFisica(item.getPessoaFisica())
                            .tipoPagamento(tipoPagamento)
                            .tipoGasto(tipoGasto)
                            .externalId(pluggyTx.getId())
                            .contaBancaria(conta)
                            .dataTransacao(dataTransacao)
                            .build();

                    aplicarClassificacao(transacao, buscarEmpresa(pluggyTx, tipoGasto).orElse(null), classeIndefinida);

                    transacaoRepository.save(transacao);
                    totalImportadas++;
                }

                paginaAtual++;
            } while (paginaAtual <= totalPaginas);
        }

        log.info("Sincronização concluída para item {}: {} transações importadas.", pluggyItemId, totalImportadas);
    }

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

    private Optional<Cnpj> buscarEmpresa(Transaction pluggyTx, TipoGasto tipoGasto) {
        String cnpj = extrairCnpj(pluggyTx, tipoGasto);
        if (cnpj == null) return Optional.empty();

        try {
            return cnpjService.buscarOuCriarCnpj(cnpj);
        } catch (Exception e) {
            log.error("Falha ao classificar CNPJ {} da transação {}: {}", cnpj, pluggyTx.getId(), e.getMessage());
            return Optional.empty();
        }
    }

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

    private String extrairCnpj(Transaction pluggyTx, TipoGasto tipoGasto) {
        if (pluggyTx.getMerchant() != null) {
            String cnpjMerchant = somenteDigitos(pluggyTx.getMerchant().getCnpj());
            if (cnpjValido(cnpjMerchant)) return cnpjMerchant;
        }

        if (pluggyTx.getPaymentData() != null) {
            TransactionPaymentParticipant contraparte = tipoGasto == TipoGasto.CREDITO
                    ? pluggyTx.getPaymentData().getPayer()
                    : pluggyTx.getPaymentData().getReceiver();
            DocumentNumber documento = contraparte != null ? contraparte.getDocumentNumber() : null;
            if (documento != null) {
                String cnpjContraparte = somenteDigitos(documento.getValue());
                if (cnpjValido(cnpjContraparte)) return cnpjContraparte;
            }
        }

        return null;
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return LocalDateTime.now();
        try {
            return LocalDate.parse(dateStr.substring(0, 10), DATE_FMT).atStartOfDay();
        } catch (DateTimeParseException e) {
            log.warn("Não foi possível parsear a data '{}', usando data atual.", dateStr);
            return LocalDateTime.now();
        }
    }

    private static boolean cnpjValido(String cnpj) {
        return cnpj != null && cnpj.length() == 14 && new CNPJValidator().invalidMessagesFor(cnpj).isEmpty();
    }

    private static String somenteDigitos(String valor) {
        return valor == null ? null : valor.replaceAll("[^0-9]", "");
    }

    @Transactional
    public void desconectarItem(ItemBancario item) {
        List<ContaBancaria> contas = contaBancariaRepository.findAllByItemBancarioId(item.getId());

        if (!contas.isEmpty()) {
            List<Long> contaIds = contas.stream().map(ContaBancaria::getId).toList();
            transacaoRepository.desvincularContas(contaIds);
            contaBancariaRepository.deleteAll(contas);
        }

        itemBancarioRepository.delete(item);
    }
}
