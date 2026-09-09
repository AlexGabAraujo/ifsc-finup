package com.finup.pluggy;
import ai.pluggy.client.PluggyClient;
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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PluggySyncService {

    private final PluggyClient pluggy;
    private final ItemBancarioRepository itemBancarioRepository;
    private final ContaBancariaRepository contaBancariaRepository;
    private final TransacaoRepository transacaoRepository;
    private final PessoaFisicaRepository pessoaFisicaRepository;

    private long categoria = 9L;

    public PluggySyncService(
            ItemBancarioRepository itemBancarioRepository,
            ContaBancariaRepository contaBancariaRepository,
            TransacaoRepository transacaoRepository,
            PessoaFisicaRepository pessoaFisicaRepository) {
        this.pluggy = PluggyClient.builder()
                .clientIdAndSecret(System.getenv("CLIENT_ID"), System.getenv("CLIENT_SECRET"))
                .build();
        this.itemBancarioRepository = itemBancarioRepository;
        this.contaBancariaRepository = contaBancariaRepository;
        this.transacaoRepository = transacaoRepository;
        this.pessoaFisicaRepository = pessoaFisicaRepository;
    }

    public void registrarItem(String pluggyItemId, Long pessoaFisicaId) throws Exception {
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

            // ATENÇÃO: confirmar via IntelliJ o método exato (getTransactions?)
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
                }else if (pluggyTx.getPaymentData() != null) {
                    String metodo = pluggyTx.getPaymentData().getPaymentMethod();
                    if ("BOLETO".equals(metodo)) {
                        tipoPagamento = TipoPagamento.BOLETO;
                    } else if ("PIX".equals(metodo)) {
                        tipoPagamento = TipoPagamento.PIX;
                    } else {
                        // TED, DOC ou outros — sem opção específica no enum
                        tipoPagamento = TipoPagamento.TRANSFERENCIA;
                    }
                } else {
                    // débito automático, salário, e outras movimentações de conta sem paymentData
                    // (inclui o que seriam cartão de débito e cheque, que a Pluggy não distingue)
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

                transacaoRepository.save(transacao);
            }
        }
    }
}