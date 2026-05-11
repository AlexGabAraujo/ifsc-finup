package com.finup.categoria.dto;

import java.math.BigDecimal;
import java.util.List;

public record DetailGraficoCategoriaResponse (
        List<String> labels,
        List<DatasetCategoria> datasets
) {
    public record DatasetCategoria(
            Long categoriaId,
            String nome,
            List<BigDecimal> data
    ) {
    }
}
