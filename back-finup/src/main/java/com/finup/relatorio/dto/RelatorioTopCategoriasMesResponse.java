package com.finup.relatorio.dto;

import java.math.BigDecimal;
import java.util.List;

public record RelatorioTopCategoriasMesResponse(
        String categoria,
        List<String> meses,
        List<BigDecimal> valores
) {
}
