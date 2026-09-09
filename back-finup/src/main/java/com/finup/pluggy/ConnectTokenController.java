package com.finup.pluggy;
// ConnectTokenController.java
import ai.pluggy.client.PluggyClient;
import ai.pluggy.client.request.CreateConnectTokenRequest;
import org.springframework.beans.factory.annotation.Value;
import ai.pluggy.client.request.Options;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ConnectTokenController {

    private final PluggyClient pluggy = PluggyClient.builder()
            .clientIdAndSecret(System.getenv("CLIENT_ID"), System.getenv("CLIENT_SECRET"))
            .build();

    @PostMapping("/connect-token")
    public Map<String, String> createConnectToken(@RequestBody Map<String, String> body) throws IOException {
        String clientUserId = body.get("clientUserId");

        // Options(webhookUrl, clientUserId) - passe null no webhookUrl se não for usar
        Options options = new Options(null, clientUserId);

        CreateConnectTokenRequest request = CreateConnectTokenRequest.builder()
                .options(options)
                .build();

        var response = pluggy.service()
                .createConnectToken(request)
                .execute();

        if (!response.isSuccessful()) {
            throw new RuntimeException("Erro ao criar connect token: " + pluggy.parseError(response));
        }

        return Map.of("accessToken", response.body().getAccessToken());
    }
}
