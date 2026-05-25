package com.finup.relatorio.dto;

import java.math.BigDecimal;

public record RelatorioResumoResponse(
        BigDecimal totalReceitas,
        BigDecimal totalDespesas,
        BigDecimal saldo,
        Long totalTransacoes
) {
}
