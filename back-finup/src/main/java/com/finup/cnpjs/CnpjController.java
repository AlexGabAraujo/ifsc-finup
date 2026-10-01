package com.finup.cnpjs;

import com.finup.cnpjs.dto.DetailCnpjResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cnpj")
@SecurityRequirement(name = "bearer-key")
public class CnpjController {

    @Autowired
    private CnpjService cnpjService;

    @GetMapping
    @Operation(summary = "Consulta um CNPJ via API do CNPJá",
               description = "Busca os dados do estabelecimento pelo CNPJ (com ou sem formatação). " +
                             "Verifica o cache local primeiro; se não encontrado, consulta a API do CNPJá e persiste o resultado.")
    public ResponseEntity<DetailCnpjResponse> buscarCnpj(@RequestParam String cnpj) {
        return cnpjService.buscarOuCriarCnpj(cnpj)
                .map(DetailCnpjResponse::new)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
