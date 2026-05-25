package com.finup.categoria.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateCategoriaRequest(

        //implementação futura
        /*@NotBlank(message = "É obrigatório informar um ícone.")
        String icone,

        @NotBlank(message = "É obrigatório informar uma cor.")
        String cor,*/

        BigDecimal orcamento,

        Long subClasseId,

        Long classePrincipalId
) {
}