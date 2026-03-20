package com.finup.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CreateAccountRequest (
        @Email
        @NotBlank
        String email,

        @NotBlank
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
        String senha,

        @NotBlank
        @Size(min = 3, max = 45, message = "Username deve ter entre 3 e 45 caracteres")
        String username,

        @NotBlank
        String nome,

        @NotBlank
        String cpf,

        @NotBlank
        String telefone,

        LocalDate dataNascimento
) {
}
