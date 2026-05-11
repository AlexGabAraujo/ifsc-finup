package com.finup.categoria.dto;

import java.math.BigDecimal;

public record DetailCategoriaCardResponse (

        Long id,
        String nome,
        String descricao,
        BigDecimal orcamento,
        BigDecimal gasto,
        BigDecimal disponivel,
        BigDecimal percentualGasto,
        BigDecimal gastoPeriodoAnterior,
        BigDecimal variacaoPercentual,
        Long quantidadeTransacoes

){
}
