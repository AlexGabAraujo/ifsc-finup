package com.finup.classePrincipal;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/classePrincipal")
@SecurityRequirement(name = "bearer-key")
public class ClassePrincipalController {

    @Autowired
    private ClassePrincipalRepository classePrincipalRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar() {
        List<Map<String, Object>> lista = classePrincipalRepository.findAll().stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "nome", c.getNome()))
                .toList();
        return ResponseEntity.ok(lista);
    }
}
