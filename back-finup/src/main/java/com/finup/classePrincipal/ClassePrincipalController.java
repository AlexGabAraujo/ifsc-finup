package com.finup.classePrincipal;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/classePrincipal")
@SecurityRequirement(name = "bearer-key")
public class ClassePrincipalController {

    @Autowired
    private ClassePrincipalRepository classePrincipalRepository;

    @GetMapping
    public ResponseEntity<List<ClassePrincipal>> listar() {
        return ResponseEntity.ok(classePrincipalRepository.findAll());
    }
}
