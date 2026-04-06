package com.finup.subclasse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/subClasse")
@SecurityRequirement(name = "bearer-key")
public class SubClasseController {

    @Autowired
    private SubClasseRepository subClasseRepository;

    @GetMapping
    public ResponseEntity<List<SubClasse>> listar() {
        return ResponseEntity.ok(subClasseRepository.findAll());
    }
}
