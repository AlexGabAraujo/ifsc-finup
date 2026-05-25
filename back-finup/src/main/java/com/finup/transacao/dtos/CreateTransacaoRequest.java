package com.finup.transacao.dtos;

import com.finup.transacao.TipoGasto;
import com.finup.transacao.TipoPagamento;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransacaoRequest(
        @NotNull(message = "É obrigatório informar o valor da transação.")
        BigDecimal valor,

        @NotNull(message = "É obrigatório associar uma transação a uma pessoa.")
        Long pessoaFisicaId,

        @NotNull(message = "É obrigatório informar o tipo de pagamento.")
        TipoPagamento tipoPagamento,

        @NotNull(message = "É obrigatório informar o tipo de gasto.")
        TipoGasto tipoGasto,

        @NotNull(message = "É obrigatório informar a data da transação.")
        LocalDate dataTransacao,

        Long subClasseId,

        Long classePrincipalId,

        Long cnpjId,

        Long categoriaId

) {
}
