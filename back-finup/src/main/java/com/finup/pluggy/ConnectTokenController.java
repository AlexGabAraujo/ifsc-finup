package com.finup.pluggy;

import ai.pluggy.client.PluggyClient;
import ai.pluggy.client.request.CreateConnectTokenRequest;
import ai.pluggy.client.request.Options;
import com.finup.auth.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ConnectTokenController {

    @Value("${CLIENT_ID}")
    private String clientId;

    @Value("${CLIENT_SECRET}")
    private String clientSecret;

    private PluggyClient pluggy;

    @Autowired
    private AuthService authService;

    @Autowired
    private PluggySyncService pluggySyncService;

    @PostConstruct
    public void init() {
        this.pluggy = PluggyClient.builder()
                .clientIdAndSecret(clientId, clientSecret)
                .build();
    }

    @PostMapping("/connect-token")
    @SecurityRequirement(name = "bearer-key")
    public Map<String, String> createConnectToken(@RequestBody Map<String, String> body) throws IOException {
        var pessoaFisica = authService.getUsuarioAutenticado();
        String clientUserId = String.valueOf(pessoaFisica.getId());

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

    @PostMapping("/pluggy/sync")
    @SecurityRequirement(name = "bearer-key")
    public ResponseEntity<Void> syncItem(@RequestBody Map<String, String> body) throws Exception {
        String pluggyItemId = body.get("pluggyItemId");
        if (pluggyItemId == null || pluggyItemId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        var pessoaFisica = authService.getUsuarioAutenticado();

        pluggySyncService.registrarItem(pluggyItemId, pessoaFisica.getId());
        pluggySyncService.sincronizarTransacoes(pluggyItemId);

        return ResponseEntity.ok().build();
    }
}
