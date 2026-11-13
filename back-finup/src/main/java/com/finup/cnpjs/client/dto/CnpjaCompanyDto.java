package com.finup.cnpjs.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Dados da empresa (razão social e nome fantasia) dentro da resposta do CNPJá.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CnpjaCompanyDto(

        @JsonProperty("name")
        String name
) {
}
