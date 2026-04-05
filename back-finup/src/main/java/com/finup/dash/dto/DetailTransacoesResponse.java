package com.finup.dash.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DetailTransacoesResponse (
        String categoria,
        BigDecimal valor,
        LocalDate data,
        String tipoGasto
){
}
