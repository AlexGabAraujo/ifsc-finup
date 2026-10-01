package com.finup.cnpjs.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Representa uma atividade econômica (CNAE) retornada pela API do CNPJá.
 */
public record CnpjaActivityDto(

        /**
         * Código numérico da CNAE (ex: "4711301").
         */
        @JsonProperty("id")
        String id,

        /**
         * Descrição textual da atividade (ex: "Comércio varejista de mercadorias em geral...").
         */
        @JsonProperty("text")
        String text
) {
}
