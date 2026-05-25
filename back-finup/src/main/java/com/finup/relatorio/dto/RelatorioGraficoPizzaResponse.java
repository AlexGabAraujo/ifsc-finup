package com.finup.relatorio.dto;

import java.math.BigDecimal;

public record RelatorioGraficoPizzaResponse(
        String nome,
        BigDecimal valor
) {
}
