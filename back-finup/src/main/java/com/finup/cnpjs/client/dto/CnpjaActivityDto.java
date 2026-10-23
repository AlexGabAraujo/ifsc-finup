package com.finup.cnpjs.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Representa uma atividade econômica (CNAE) retornada pela API do CNPJá.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CnpjaActivityDto(

        /**
         * Código da CNAE. O CNPJá envia como número, então zeros à esquerda se perdem
         * (ex: 600001 para a CNAE 0600-0/01). Use {@link #codigoCnae()}.
         */
        @JsonProperty("id")
        String id,

        /**
         * Descrição textual da atividade (ex: "Comércio varejista de mercadorias em geral...").
         */
        @JsonProperty("text")
        String text
) {

    /**
     * Código da CNAE com 7 dígitos, restaurando os zeros à esquerda (ex: "0600001").
     * Retorna null se o código não vier preenchido.
     */
    public String codigoCnae() {
        if (id == null) {
            return null;
        }
        String digitos = id.replaceAll("[^0-9]", "");
        if (digitos.isEmpty() || digitos.length() > 7) {
            return digitos.isEmpty() ? null : digitos;
        }
        return "0".repeat(7 - digitos.length()) + digitos;
    }
}
