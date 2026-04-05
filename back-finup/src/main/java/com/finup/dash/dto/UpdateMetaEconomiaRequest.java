package com.finup.dash.dto;

import java.math.BigDecimal;

public record UpdateMetaEconomiaRequest (
        BigDecimal valorEco,
        BigDecimal novaMetaEco,
        int id
) {

}
