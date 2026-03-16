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
        @Size(min = 6)
        String senha,

        @NotBlank
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
