package com.finup.transacao.dtos;

import com.finup.transacao.TipoGasto;

import java.math.BigDecimal;
import java.util.Map;

public record InsightsResponse(
        Long totalTransacoes,
        BigDecimal totalReceitas,
        BigDecimal totalDespesas,
        Map<TipoGasto, Long> transacoesPorTipo
) {
}
