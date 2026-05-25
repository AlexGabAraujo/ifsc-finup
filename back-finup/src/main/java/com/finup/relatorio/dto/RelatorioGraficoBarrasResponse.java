package com.finup.relatorio.dto;

import java.math.BigDecimal;
import java.util.List;

public record RelatorioGraficoBarrasResponse(
        List<String> meses,
        List<BigDecimal> receitas,
        List<BigDecimal> despesas
) {
}
