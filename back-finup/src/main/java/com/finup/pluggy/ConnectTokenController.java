package com.finup.pluggy;

import ai.pluggy.client.PluggyClient;
import ai.pluggy.client.request.CreateConnectTokenRequest;
import ai.pluggy.client.request.Options;
import com.finup.auth.AuthService;
import com.finup.pluggy.itemBancario.ItemBancario;
import com.finup.pluggy.itemBancario.ItemBancarioRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ConnectTokenController {

    /** Id do conector "MeuPluggy" na Pluggy. */
    private static final Integer CONNECTOR_MEU_PLUGGY = 200;

    @Value("${CLIENT_ID_PLUGGY}")
    private String clientId;

    @Value("${CLIENT_SECRET_PLUGGY}")
    private String clientSecret;

    private PluggyClient pluggy;

    @Autowired
    private AuthService authService;

    @Autowired
    private PluggySyncService pluggySyncService;

    @Autowired
    private ItemBancarioRepository itemBancarioRepository;

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

    @PostMapping("/connect-token/update")
    @SecurityRequirement(name = "bearer-key")
    public Map<String, String> createUpdateConnectToken(@RequestBody Map<String, String> body) throws IOException {
        String itemId = body.get("itemId");
        if (itemId == null || itemId.isBlank()) {
            throw new RuntimeException("itemId é obrigatório");
        }

        var pessoaFisica = authService.getUsuarioAutenticado();
        String clientUserId = String.valueOf(pessoaFisica.getId());

        Options options = new Options(null, clientUserId);

        CreateConnectTokenRequest request = CreateConnectTokenRequest.builder()
                .itemId(itemId)
                .options(options)
                .build();

        var response = pluggy.service()
                .createConnectToken(request)
                .execute();

        if (!response.isSuccessful()) {
            throw new RuntimeException("Erro ao criar connect token de atualização: " + pluggy.parseError(response));
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

    @GetMapping("/pluggy/itens")
    @SecurityRequirement(name = "bearer-key")
    public List<Map<String, String>> listarItens() {
        var pessoaFisica = authService.getUsuarioAutenticado();
        return itemBancarioRepository.findAllByPessoaFisicaId(pessoaFisica.getId())
                .stream()
                .map(item -> Map.of(
                        "id", String.valueOf(item.getId()),
                        "pluggyItemId", item.getPluggyItemId(),
                        "nomeInstituicao", item.getNomeInstituicao() != null ? item.getNomeInstituicao() : ""
                ))
                .toList();
    }

    /**
     * Estado atual do item na Pluggy. O front consulta antes de abrir o widget de atualização:
     * item que já está atualizando ou que não aceita atualização manual (Meu Pluggy) faz o
     * PATCH do widget falhar com 400.
     */
    @GetMapping("/pluggy/itens/{pluggyItemId}/status")
    @SecurityRequirement(name = "bearer-key")
    public ResponseEntity<Map<String, Object>> statusItem(@PathVariable String pluggyItemId) throws IOException {
        var pessoaFisica = authService.getUsuarioAutenticado();
        ItemBancario item = itemBancarioRepository.findByPluggyItemId(pluggyItemId).orElse(null);

        if (item == null || !item.getPessoaFisica().getId().equals(pessoaFisica.getId())) {
            return ResponseEntity.notFound().build();
        }

        var response = pluggy.service().getItem(pluggyItemId).execute();
        if (!response.isSuccessful()) {
            throw new RuntimeException("Erro ao buscar item: " + pluggy.parseError(response));
        }

        var pluggyItem = response.body();
        Integer connectorId = pluggyItem.getConnector() != null ? pluggyItem.getConnector().getId() : null;

        Map<String, Object> corpo = new HashMap<>();
        corpo.put("status", pluggyItem.getStatus() != null ? pluggyItem.getStatus().name() : null);
        corpo.put("executionStatus",
                pluggyItem.getExecutionStatus() != null ? pluggyItem.getExecutionStatus().name() : null);
        corpo.put("ultimaAtualizacao",
                pluggyItem.getLastUpdatedAt() != null ? pluggyItem.getLastUpdatedAt().toInstant().toString() : null);
        // Item do Meu Pluggy é um espelho da conexão original: a Pluggy não deixa atualizá-lo
        // pela API nem pelo widget; ele acompanha as atualizações feitas dentro do Meu Pluggy.
        corpo.put("atualizacaoManual", !CONNECTOR_MEU_PLUGGY.equals(connectorId));
        return ResponseEntity.ok(corpo);
    }

    @DeleteMapping("/pluggy/itens/{id}")
    @SecurityRequirement(name = "bearer-key")
    public ResponseEntity<Void> desconectarItem(@PathVariable Long id) {
        var pessoaFisica = authService.getUsuarioAutenticado();
        ItemBancario item = itemBancarioRepository.findById(id).orElse(null);

        if (item == null || !item.getPessoaFisica().getId().equals(pessoaFisica.getId())) {
            return ResponseEntity.notFound().build();
        }

        pluggySyncService.desconectarItem(item);
        return ResponseEntity.noContent().build();
    }
}
