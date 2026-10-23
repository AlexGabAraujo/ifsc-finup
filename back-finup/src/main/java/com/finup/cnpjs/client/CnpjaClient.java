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
     * @return Optional com os dados do estabelecimento, vazio se o CNPJ não existir (400/404)
     * @throws ServicoExternoIndisponivelException em falha temporária (429, 5xx, rede) — não salvar o resultado
     */
    public Optional<CnpjaOfficeResponse> consultarCnpj(String cnpj) {
        String cnpjLimpo = cnpj.replaceAll("[^0-9]", "");

        try {
            return restClient.get()
                    .uri("/office/{cnpj}", cnpjLimpo)
                    .exchange((req, res) -> {
                        HttpStatusCode status = res.getStatusCode();
                        if (status.is2xxSuccessful()) {
                            return Optional.ofNullable(res.bodyTo(CnpjaOfficeResponse.class));
                        }
                        if (status.value() == 400 || status.value() == 404) {
                            log.warn("CNPJ não encontrado na API do CNPJá: {} — status {}", cnpjLimpo, status);
                            return Optional.<CnpjaOfficeResponse>empty();
                        }
                        throw new ServicoExternoIndisponivelException(
                                "CNPJá respondeu " + status.value() + " ao consultar " + cnpjLimpo);
                    });

        } catch (ServicoExternoIndisponivelException e) {
            log.error(e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Falha ao consultar CNPJ {} na API do CNPJá: {}", cnpjLimpo, e.getMessage());
            throw new ServicoExternoIndisponivelException("Falha ao consultar o CNPJá", e);
        }
    }
}
