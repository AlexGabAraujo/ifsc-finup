package com.finup.pluggy;

import ai.pluggy.client.PluggyClient;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PluggySyncService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Value("${CLIENT_ID}")
    private String clientId;

    @Value("${CLIENT_SECRET}")
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
                if (transacaoRepository.existsByExternalId(pluggyTx.getId())) {
                    continue;
                }

                TipoGasto tipoGasto = "CREDIT".equals(pluggyTx.getType())
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

                Transacao transacao = Transacao.builder()
                        .valor(BigDecimal.valueOf(Math.abs(pluggyTx.getAmount())))
                        .pessoaFisica(item.getPessoaFisica())
                        .tipoPagamento(tipoPagamento)
                        .tipoGasto(tipoGasto)
                        .categoria(categoriaRepository.findFirstByPessoaFisicaId(item.getPessoaFisica().getId()))
                        .externalId(pluggyTx.getId())
                        .contaBancaria(conta)
                        .build();

                transacaoRepository.save(transacao);
            }
        }
    }
}
