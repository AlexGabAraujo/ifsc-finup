package com.finup.cnpjs.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Representa a resposta completa do endpoint GET /office/{taxId}
 * da API pública do CNPJá (https://open.cnpja.com).
 *
 * Apenas os campos utilizados pela integração são mapeados aqui.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CnpjaOfficeResponse(

        /**
         * CNPJ somente com dígitos (ex: "33000167000101").
         */
        @JsonProperty("taxId")
        String taxId,

        /**
         * Nome fantasia do estabelecimento.
         */
        @JsonProperty("alias")
        String alias,

        /**
         * Dados da empresa (razão social).
         */
        @JsonProperty("company")
        CnpjaCompanyDto company,

        /**
         * Atividade econômica principal (CNAE principal).
         */
        @JsonProperty("mainActivity")
        CnpjaActivityDto mainActivity,

        /**
         * Lista de atividades econômicas secundárias.
         */
        @JsonProperty("sideActivities")
        List<CnpjaActivityDto> activities
) {

    /**
     * Retorna o nome de exibição preferencial: alias (nome fantasia) se disponível,
     * caso contrário a razão social da empresa.
     */
    public String nomeExibicao() {
        if (alias != null && !alias.isBlank()) {
            return alias;
        }
        if (company != null && company.name() != null) {
            return company.name();
        }
        return taxId;
    }
}
