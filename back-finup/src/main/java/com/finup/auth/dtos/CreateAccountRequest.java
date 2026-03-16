package com.finup.auth.dtos;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CreateAccountRequest (
        String email,
        String senha,
        String username,
        String nome,
        String cpf,
        String telefone,
        LocalDate dataNascimento
) {
}
