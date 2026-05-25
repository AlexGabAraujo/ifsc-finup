package com.finup.subclasse;

import com.finup.subclasse.dto.DetailSubClasseResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/subClasse")
@SecurityRequirement(name = "bearer-key")
public class SubClasseController{

    @Autowired
    private SubClasseRepository subClasseRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar() {
        List<Map<String, Object>> lista = subClasseRepository.findAll().stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "nome", c.getNome()))
                .toList();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/por-classe")
    public ResponseEntity<List<DetailSubClasseResponse>> getSubClassesPorClasse(
            @RequestParam Long classePrincipalId
    ) {
        List<DetailSubClasseResponse> lista = subClasseRepository
                .findByClassePrincipalId(classePrincipalId)
                .stream()
                .map(DetailSubClasseResponse::new)
                .toList();
        return ResponseEntity.ok(lista);
    }
}