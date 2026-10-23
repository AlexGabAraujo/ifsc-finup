package com.finup.cnaeapi.cnae;

import com.finup.cnaeapi.cnae.dto.CnaeClassificacaoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/cnae")
public class CnaeController {

    private final CnaeService cnaeService;

    public CnaeController(CnaeService cnaeService) {
        this.cnaeService = cnaeService;
    }

    /**
     * Valida o CNAE e retorna sua classe principal e subclasse.
     * Aceita o código só com dígitos ("4711301") ou com hífen ("4711-301").
     * 400 se o código não tiver 7 dígitos; CNAE não cadastrado retorna "Indefinido".
     */
    @GetMapping("/{codigo}")
    public ResponseEntity<CnaeClassificacaoResponse> classificar(@PathVariable String codigo) {
        return ResponseEntity.ok(cnaeService.classificar(codigo));
    }

    @ExceptionHandler(CnaeInvalidoException.class)
    public ResponseEntity<Map<String, String>> tratarCnaeInvalido(CnaeInvalidoException ex) {
        return ResponseEntity.badRequest().body(Map.of("erro", ex.getMessage()));
    }
}
