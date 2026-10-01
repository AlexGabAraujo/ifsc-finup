package com.finup.cnpjs.client;

import com.finup.cnpjs.client.dto.CnpjaOfficeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Cliente HTTP para a API pública do CNPJá.
 *
 * Endpoint utilizado: GET https://open.cnpja.com/office/{cnpj}
 * Limite da API pública: 5 requisições por minuto por IP (sem autenticação obrigatória).
 * Se CNPJA_API_KEY estiver configurada, ela é enviada no header Authorization
 * para aumentar os limites de uso.
 */
@Component
public class CnpjaClient {

    private static final Logger log = LoggerFactory.getLogger(CnpjaClient.class);
    private static final String BASE_URL = "https://open.cnpja.com";

    private final RestClient restClient;

    public CnpjaClient(@Value("${cnpja.api-key:}") String apiKey) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE);

        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("Authorization", apiKey);
        }

        this.restClient = builder.build();
    }

    /**
     * Consulta os dados de um estabelecimento pelo CNPJ.
     *
     * @param cnpj número do CNPJ (somente dígitos ou formatado — a API aceita ambos)
     * @return Optional com os dados do estabelecimento, vazio se não encontrado ou em caso de erro
     */
    public Optional<CnpjaOfficeResponse> consultarCnpj(String cnpj) {
        String cnpjLimpo = cnpj.replaceAll("[^0-9]", "");

        try {
            CnpjaOfficeResponse response = restClient.get()
                    .uri("/office/{cnpj}", cnpjLimpo)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        log.warn("CNPJ não encontrado na API do CNPJá: {} — status {}", cnpjLimpo, res.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.error("Erro no servidor do CNPJá ao consultar {}: status {}", cnpjLimpo, res.getStatusCode());
                    })
                    .body(CnpjaOfficeResponse.class);

            return Optional.ofNullable(response);

        } catch (Exception e) {
            log.error("Falha ao consultar CNPJ {} na API do CNPJá: {}", cnpjLimpo, e.getMessage());
            return Optional.empty();
        }
    }
}
