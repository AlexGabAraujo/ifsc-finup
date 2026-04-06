package com.finup.infra.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // Pega apenas o primeiro erro para exibir no front-end, ou você pode concatenar todos
        FieldError firstError = ex.getBindingResult().getFieldErrors().get(0);

        Map<String, String> response = new HashMap<>();
        response.put("message", firstError.getDefaultMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ValidacaoException.class)
    public ResponseEntity<Map<String, String>> handleBusinessExceptions(Exception ex) {
        Map<String, String> response = new HashMap<>();

        // Pega a string "CPF já cadastrado" e envelopa no mesmo formato JSON
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
