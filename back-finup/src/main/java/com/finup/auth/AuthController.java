package com.finup.auth;

import com.finup.auth.dtos.CreateAccountRequest;
import com.finup.auth.dtos.DetailAccountResponse;
import com.finup.auth.dtos.LoginRequest;
import com.finup.credencial.CredencialRepository;
import com.finup.infra.security.JWTUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    @Autowired
    private CredencialRepository credencialRepository;


    @PostMapping("/register")
    public ResponseEntity<DetailAccountResponse> criarConta(@RequestBody CreateAccountRequest dados) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.criarConta(dados));
    }

    @PostMapping("/login")
    public Map<String, Object> loginHandler(@RequestBody @Valid LoginRequest usuario) {
        var authInputToken = new UsernamePasswordAuthenticationToken(usuario.login(), usuario.senha());
        authenticationManager.authenticate(authInputToken);

        var usuarioT = credencialRepository.findByUsername(usuario.login());

        String token = jwtUtil.gerarToken(usuarioT.get());
        return Collections.singletonMap("jwt-token", token);
    }
}
