package com.finup.categoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateCategoriaRequest(

        /*@NotBlank(message = "É obrigatório informar um ícone.")
        String icone,

        @NotBlank(message = "É obrigatório informar uma cor.")
        String cor,*/

        @NotNull(message = "É obrigatório informar o orçamento.")
        BigDecimal orcamento

) {
}
