package com.finup.credencial;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collections;
import java.util.Map;

@Controller
public class CredencialController {
    @GetMapping("/me")
    @SecurityRequirement(name = "bearer-key")
    public ResponseEntity<DetailUsuarioResponse> getUsuarioDetails() {
        return ResponseEntity.ok(usuarioService.getDadosUsuario());
    }

    @PostMapping("/register")
    public ResponseEntity registerHandler(@RequestBody @Valid Create dados) {
        var usuario = new Usuario(dados);

        String encodedPassword = passwordEncoder.encode(usuario.getSenha());
        usuario.setSenha(encodedPassword);

        usuario = usuarioRepository.save(usuario);
        String token = jwtUtil.gerarToken(usuario);

        return ResponseEntity.ok(Collections.singletonMap("jwt-token", token));
    }

    @PostMapping("/login")
    public Map<String, Object> loginHandler(@RequestBody @Valid LoginUsuarioResponse usuario) {
        var authInputToken = new UsernamePasswordAuthenticationToken(usuario.username(), usuario.senha());
        authenticationManager.authenticate(authInputToken);

        var usuarioT = usuarioRepository.findByUsername(usuario.username());

        String token = jwtUtil.gerarToken(usuarioT.get());
        return Collections.singletonMap("jwt-token", token);
    }
}
