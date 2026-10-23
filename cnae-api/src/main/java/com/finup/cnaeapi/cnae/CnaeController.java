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

    @GetMapping("/{codigo}")
    public ResponseEntity<CnaeClassificacaoResponse> classificar(@PathVariable String codigo) {
        return ResponseEntity.ok(cnaeService.classificar(codigo));
    }

    @ExceptionHandler(CnaeInvalidoException.class)
    public ResponseEntity<Map<String, String>> tratarCnaeInvalido(CnaeInvalidoException ex) {
        return ResponseEntity.badRequest().body(Map.of("erro", ex.getMessage()));
    }
}
