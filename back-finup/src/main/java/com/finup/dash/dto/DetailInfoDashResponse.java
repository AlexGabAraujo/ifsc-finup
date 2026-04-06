package com.finup.dash.dto;

import java.math.BigDecimal;

public record DetailInfoDashResponse(
        BigDecimal ReceitaTotal,
        BigDecimal metaEconomia,
        BigDecimal saldoAtual,
        BigDecimal totalDespesa,
        BigDecimal valorMetaAtual,
        Double estatisticaSaldoAtual,
        Double estatisticaDespesasAtual,
        Double estatisticaReceitaAtual,
        Double porcenMetaEco
) {

}
