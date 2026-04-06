package com.finup.transacao.dtos;

import com.finup.transacao.TipoGasto;
import com.finup.transacao.TipoPagamento;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateTransacaoRequest(
        @NotNull BigDecimal valor,
        @NotNull TipoPagamento tipoPagamento,
        @NotNull TipoGasto tipoGasto,
        @NotNull LocalDate dataTransacao,
        Long subClasseId,
        Long classePrincipalId,
        Long cnpjId
) {
}
