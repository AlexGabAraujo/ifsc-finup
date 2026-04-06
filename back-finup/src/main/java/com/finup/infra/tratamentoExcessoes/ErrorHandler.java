package com.finup.infra.tratamentoExcessoes;

import com.finup.infra.exceptions.AutorizacaoException;
import com.finup.infra.exceptions.ValidacaoException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ErrorHandler {

    /**
     * Erros de validação dos atributos @Valid
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> tratarErro400(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            errors.put(((FieldError) error).getField(), error.getDefaultMessage());
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity tratarErro400(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body("Formato de dados inválido na requisição.");
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity tratarErro404() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(AutorizacaoException.class)
    public ResponseEntity tratarErroAutorizacao(AutorizacaoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(ValidacaoException.class)
    public ResponseEntity tratarErroRegraDeNegocio(ValidacaoException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity tratarErroBadCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciais inválidas.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity tratarErroAuthentication() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Falha na autenticação.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity tratarErroAcessoNegado() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acesso negado.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity tratarErro500(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erro interno do servidor. Tente novamente mais tarde.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity tratarErroDeIntegridade(DataIntegrityViolationException ex) {
        return ResponseEntity.badRequest().body("Algum dado enviado viola uma regra do banco (duplicidade, relacionamento inválido, etc).");
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity cabecalhoVazioDeletar(EmptyResultDataAccessException ex) {
        return ResponseEntity.notFound().build();
    }
}
