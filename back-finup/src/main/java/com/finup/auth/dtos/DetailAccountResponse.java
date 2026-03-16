package com.finup.auth.dtos;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DetailAccountResponse (
        Long id_credencial,
        String email,
        String username,
        Long id_pessoa,
        String nome,
        String cpf,
        String telefone,
        LocalDate dataNascimento,
        LocalDateTime data_inicio,
        LocalDateTime data_fim,
        Boolean ativo
){
}
