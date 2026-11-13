package com.finup.pluggy;

import ai.pluggy.client.PluggyClient;
import ai.pluggy.client.request.TransactionsSearchRequest;
import com.finup.categoria.CategoriaRepository;
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

@Service
public class PluggySyncService {

    private static final Logger log = LoggerFactory.getLogger(PluggySyncService.class);
    private static final int PAGE_SIZE = 500;
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

    public PluggySyncService(
            ItemBancarioRepository itemBancarioRepository,
            ContaBancariaRepository contaBancariaRepository,
            TransacaoRepository transacaoRepository,
            PessoaFisicaRepository pessoaFisicaRepository) {
        this.itemBancarioRepository = itemBancarioRepository;
        this.contaBancariaRepository = contaBancariaRepository;
        this.transacaoRepository = transacaoRepository;
        this.pessoaFisicaRepository = pessoaFisicaRepository;
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

        // Busca os últimos 12 meses de transações
        String dataInicio = LocalDate.now().minusMonths(12).format(DATE_FMT);
        String dataFim    = LocalDate.now().format(DATE_FMT);

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

            // Itera todas as páginas
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

                log.info("Conta {} — página {}/{}, {} transações", account.getId(), paginaAtual, totalPaginas, pageResponse.getResults().size());

                for (var pluggyTx : pageResponse.getResults()) {
                    if (transacaoRepository.existsByExternalId(pluggyTx.getId())) {
                        continue;
                    }

                    TipoGasto tipoGasto = pluggyTx.getType() != null && "CREDIT".equals(pluggyTx.getType().name())
                            ? TipoGasto.CREDITO
                            : TipoGasto.DEBITO;

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
                            .categoria(categoriaRepository.findFirstByPessoaFisicaId(item.getPessoaFisica().getId()))
                            .externalId(pluggyTx.getId())
                            .contaBancaria(conta)
                            .dataTransacao(dataTransacao)
                            .build();

                    transacaoRepository.save(transacao);
                    totalImportadas++;
                }

                paginaAtual++;
            } while (paginaAtual <= totalPaginas);
        }

        log.info("Sincronização concluída para item {}: {} transações importadas.", pluggyItemId, totalImportadas);
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return LocalDateTime.now();
        try {
            // Formato ISO com offset: "2024-11-01T00:00:00.000Z" ou "2024-11-01"
            return LocalDate.parse(dateStr.substring(0, 10), DATE_FMT).atStartOfDay();
        } catch (DateTimeParseException e) {
            log.warn("Não foi possível parsear a data '{}', usando data atual.", dateStr);
            return LocalDateTime.now();
        }
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