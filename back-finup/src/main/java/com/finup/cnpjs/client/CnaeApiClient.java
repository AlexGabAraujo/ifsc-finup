package com.finup.cnpjs.client;

import com.finup.cnpjs.client.dto.CnaeClassificacaoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Cliente HTTP para a cnae-api, que valida um CNAE e retorna sua classe principal e subclasse.
 *
 * Endpoint utilizado: GET {cnae.api.url}/cnae/{codigo}
 */
@Component
public class CnaeApiClient {

    private static final Logger log = LoggerFactory.getLogger(CnaeApiClient.class);

    private final RestClient restClient;

    public CnaeApiClient(@Value("${cnae.api.url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * @param codigoCnae código CNAE (com ou sem pontuação)
     * @return Optional com a classificação, vazio se o CNAE for inválido ou a cnae-api estiver indisponível
     */
    public Optional<CnaeClassificacaoResponse> classificar(String codigoCnae) {
        String cnaeLimpo = codigoCnae.replaceAll("[^0-9]", "");

        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/cnae/{codigo}", cnaeLimpo)
                    .retrieve()
                    .body(CnaeClassificacaoResponse.class));

        } catch (Exception e) {
            log.error("Falha ao classificar CNAE {} na cnae-api: {}", cnaeLimpo, e.getMessage());
            return Optional.empty();
        }
    }
}
