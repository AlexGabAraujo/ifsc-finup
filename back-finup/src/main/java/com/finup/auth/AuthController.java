package com.finup.auth;

import com.finup.auth.dtos.CreateAccountRequest;
import com.finup.auth.dtos.DetailAccountResponse;
import com.finup.auth.dtos.LoginRequest;
import com.finup.auth.dtos.UpdateAccountRequest;
import com.finup.credencial.Credencial;
import com.finup.credencial.CredencialRepository;
import com.finup.credencial.dtos.UpdateCredencialRequest;
import com.finup.infra.exceptions.ValidacaoException;
import com.finup.infra.security.JWTUtil;
import com.finup.pessoaFisica.PessoaFisica;
import com.finup.pessoaFisica.PessoaFisicaRepository;
import com.finup.pessoaFisica.dto.DetailPessoaFisicaResponse;
import com.finup.pessoaFisica.dto.UpdatePessoaFisicaRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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

    @Autowired
    private PessoaFisicaRepository repository;

    //--------------------------------------------------------------
    //ENDPOINTS PARA REALIZAÇÃO DE AUTHENTICAÇÃO
    //--------------------------------------------------------------
    @PostMapping("/register")
    public ResponseEntity<DetailAccountResponse> criarConta(@RequestBody @Valid CreateAccountRequest dados) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.criarConta(dados));
    }

    @PostMapping("/login")
    public Map<String, Object> loginHandler(@RequestBody @Valid LoginRequest usuario) {
        var authInputToken = new UsernamePasswordAuthenticationToken(usuario.login(), usuario.senha());
        authenticationManager.authenticate(authInputToken);

        var usuarioT = credencialRepository.findByUsername(usuario.login());

        if(usuarioT.get().getPessoaFisica().getAtivo() == false)
            throw new ValidacaoException("Usuário está inativo.");

        String token = jwtUtil.gerarToken(usuarioT.get());
        return Collections.singletonMap("jwt-token", token);
    }


    //--------------------------------------------------------------
    //BUSCAR INFORMAÇÕES DO USUÁRIO AUTENTICADO
    //--------------------------------------------------------------
    @GetMapping("/account")
    @SecurityRequirement(name = "bearer-key")
    public ResponseEntity<DetailAccountResponse> buscarConta() {
        return ResponseEntity.ok(authService.getContaAutenticada());
    }

    //--------------------------------------------------------------
    //ATUALIZAR INFORMAÇÕES DO USUÁRIO (PESSOA FISICA E CREDENCIAIS)
    //--------------------------------------------------------------
    @PutMapping
    @Transactional
    @SecurityRequirement(name = "bearer-key")
    public ResponseEntity editar(@RequestBody @Valid UpdateAccountRequest dados) {
        authService.atualizarUsuario(dados);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Transactional
    @SecurityRequirement(name = "bearer-key")
    public ResponseEntity excluir() {
        var usuarioAutenticado = authService.getUsuarioAutenticado();

        var pessoa = repository.getReferenceById(usuarioAutenticado.getId());

        pessoa.excluir();

        return ResponseEntity.noContent().build();
    }
}
