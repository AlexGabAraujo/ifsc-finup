package com.finup.categoria.dto;

import java.math.BigDecimal;
import java.util.List;

public record DetailCategoriaOrcamentoResponse (

        BigDecimal orcamentoTotal,
        BigDecimal gastoTotal,
        BigDecimal disponivel,
        BigDecimal percentualUso,
        List<DetailCategoriaCardResponse> categorias

){
}
