package com.finup.auth.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.finup.credencial.dtos.UpdateCredencialRequest;
import com.finup.pessoaFisica.dto.UpdatePessoaFisicaRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record UpdateAccountRequest (
        @Email
        @NotBlank
        String email,

        String senha,
        String username,

        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotBlank(message = "CPF é obrigatório")
        String cpf,

        @NotBlank(message = "Telefone é obrigatório")
        String telefone,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dataNascimento
){
    public UpdatePessoaFisicaRequest toPessoaRequest() {
        return new UpdatePessoaFisicaRequest(nome, cpf, telefone, dataNascimento);
    }

    public UpdateCredencialRequest toCredencialRequest() {
        return new UpdateCredencialRequest(email, username, senha);
    }
}
