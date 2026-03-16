package com.finup.pessoaFisica.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreatePessoaFisicaRequest (
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotBlank(message = "CPF é obrigatório")
        String cpf,

        @NotBlank(message = "Telefone é obrigatório")
        String telefone,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dataNascimento,

        @Email(message = "Email inválido")
        @NotBlank(message = "Email é obrigatório")
        String email,

        @NotBlank(message = "Username é obrigatório")
        @Size(min = 3, max = 45, message = "Username deve ter entre 3 e 45 caracteres")
        String username,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
        String senha
){
}
