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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    /**
     * Memória de uma única execução do sync: guarda o que já foi consultado para não
     * repetir a mesma busca a cada transação. Vale só durante a chamada, então o que é
     * gravado é o mesmo de antes — só o número de consultas muda.
     */
    private static final class SyncContexto {
        final ClassePrincipal classeIndefinida;
        /** CNPJs já resolvidos nesta execução. Falhas NÃO entram: continuam sendo tentadas de novo. */
        final Map<String, Cnpj> empresasPorCnpj = new HashMap<>();
        final Map<ChaveCategoria, Optional<Categoria>> categoriaPorSubClasse = new HashMap<>();
        final Map<ChaveCategoria, Optional<Categoria>> categoriaPorClasse = new HashMap<>();
        final Map<Long, Optional<Categoria>> categoriaPadrao = new HashMap<>();

        SyncContexto(ClassePrincipal classeIndefinida) {
            this.classeIndefinida = classeIndefinida;
        }
    }

    private record ChaveCategoria(Long pessoaFisicaId, Long id) {
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

        SyncContexto ctx = new SyncContexto(classePrincipalRepository
                .findFirstByNomeOrderByIdAsc(CLASSE_INDEFINIDA).orElse(null));

        long inicioMs = System.currentTimeMillis();
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

                // Uma consulta por página para saber o que já foi importado (antes: uma por transação)
                Map<String, Transacao> jaImportadas = buscarJaImportadas(pageResponse.getResults());
                // Novas e alteradas da página são gravadas juntas no fim (antes: um commit por transação)
                List<Transacao> paraSalvar = new ArrayList<>();

                for (var pluggyTx : pageResponse.getResults()) {
                    TipoGasto tipoGasto = pluggyTx.getType() == TransactionType.CREDIT
                            ? TipoGasto.CREDITO
                            : TipoGasto.DEBITO;

                    Transacao existente = jaImportadas.get(pluggyTx.getId());
                    if (existente != null) {
                        boolean alterada = atualizarTransacaoExistente(existente, pluggyTx, tipoGasto, ctx);
                        // id nulo = criada nesta mesma página e já está na fila para salvar
                        if (alterada && existente.getId() != null) {
                            paraSalvar.add(existente);
                        }
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

                    aplicarClassificacao(transacao, buscarEmpresa(pluggyTx, tipoGasto, ctx).orElse(null), ctx);

                    // Se o mesmo id vier de novo nesta página, cai no caminho de "já importada"
                    jaImportadas.put(pluggyTx.getId(), transacao);
                    paraSalvar.add(transacao);
                    totalImportadas++;
                }

                if (!paraSalvar.isEmpty()) {
                    transacaoRepository.saveAll(paraSalvar);
                }

                paginaAtual++;
            } while (paginaAtual <= totalPaginas);
        }

        log.info("Sincronização concluída para item {}: {} transações importadas em {} ms.",
                pluggyItemId, totalImportadas, System.currentTimeMillis() - inicioMs);
    }

    private Map<String, Transacao> buscarJaImportadas(List<Transaction> pagina) {
        List<String> ids = pagina.stream()
                .map(Transaction::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, Transacao> porExternalId = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Transacao t : transacaoRepository.findAllByExternalIdIn(ids)) {
                porExternalId.put(t.getExternalId(), t);
            }
        }
        return porExternalId;
    }

    /** @return true se a transação foi alterada e precisa ser salva */
    private boolean atualizarTransacaoExistente(Transacao transacao, Transaction pluggyTx, TipoGasto tipoGasto,
                                                SyncContexto ctx) {
        ClassePrincipal classeIndefinida = ctx.classeIndefinida;
        boolean alterada = false;

        if (transacao.getTipoGasto() != tipoGasto) {
            transacao.setTipoGasto(tipoGasto);
            alterada = true;
        }

        boolean semClassificacao = transacao.getCnpj() == null
                && (transacao.getClassePrincipal() == null
                    || (classeIndefinida != null && classeIndefinida.getId().equals(transacao.getClassePrincipal().getId())));

        if (semClassificacao) {
            Optional<Cnpj> empresa = buscarEmpresa(pluggyTx, tipoGasto, ctx);
            if (empresa.isPresent()) {
                aplicarClassificacao(transacao, empresa.get(), ctx);
                alterada = true;
            }
        }

        return alterada;
    }

    private Optional<Cnpj> buscarEmpresa(Transaction pluggyTx, TipoGasto tipoGasto, SyncContexto ctx) {
        String cnpj = extrairCnpj(pluggyTx, tipoGasto);
        if (cnpj == null) return Optional.empty();

        Cnpj jaResolvido = ctx.empresasPorCnpj.get(cnpj);
        if (jaResolvido != null) return Optional.of(jaResolvido);

        try {
            Optional<Cnpj> empresa = cnpjService.buscarOuCriarCnpj(cnpj);
            empresa.ifPresent(e -> ctx.empresasPorCnpj.put(cnpj, e));
            return empresa;
        } catch (Exception e) {
            log.error("Falha ao classificar CNPJ {} da transação {}: {}", cnpj, pluggyTx.getId(), e.getMessage());
            return Optional.empty();
        }
    }

    private void aplicarClassificacao(Transacao transacao, Cnpj empresa, SyncContexto ctx) {
        if (empresa != null) {
            transacao.setCnpj(empresa);
            transacao.setClassePrincipal(empresa.getClassePrincipal());
            transacao.setSubClasse(empresa.getSubClasse());
        }

        if (transacao.getClassePrincipal() == null) {
            transacao.setClassePrincipal(ctx.classeIndefinida);
        }

        // Mesma ordem de sempre (subclasse → classe → primeira categoria do usuário);
        // cada combinação só vai ao banco uma vez por execução.
        Long pessoaFisicaId = transacao.getPessoaFisica().getId();
        Categoria categoria = null;
        if (transacao.getSubClasse() != null) {
            categoria = ctx.categoriaPorSubClasse
                    .computeIfAbsent(new ChaveCategoria(pessoaFisicaId, transacao.getSubClasse().getId()),
                            chave -> categoriaRepository.findByPessoaFisicaIdAndSubClasseId(chave.pessoaFisicaId(), chave.id()))
                    .orElse(null);
        }
        if (categoria == null && transacao.getClassePrincipal() != null) {
            categoria = ctx.categoriaPorClasse
                    .computeIfAbsent(new ChaveCategoria(pessoaFisicaId, transacao.getClassePrincipal().getId()),
                            chave -> categoriaRepository.findByPessoaFisicaIdAndClassePrincipalId(chave.pessoaFisicaId(), chave.id()))
                    .orElse(null);
        }
        if (categoria == null) {
            categoria = ctx.categoriaPadrao
                    .computeIfAbsent(pessoaFisicaId,
                            id -> Optional.ofNullable(categoriaRepository.findFirstByPessoaFisicaId(id)))
                    .orElse(null);
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
