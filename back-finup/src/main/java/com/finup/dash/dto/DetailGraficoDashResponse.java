package com.finup.dash.dto;

import java.math.BigDecimal;
import java.util.List;

public record DetailGraficoDashResponse (
        List<String> categoriasX,
        List<BigDecimal> seriesReceita,
        List<BigDecimal> seriesDespesa
){
}
