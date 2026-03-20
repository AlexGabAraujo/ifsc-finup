package com.finup.auth.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.finup.credencial.dtos.UpdateCredencialRequest;
import com.finup.pessoaFisica.dto.UpdatePessoaFisicaRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record UpdateAccountRequest (
        @Email
        @NotBlank
        String email,

        @Pattern(
                regexp = "^(?=.*[^A-Za-z0-9]).{6,}$",
                message = "Senha deve conter ao menos 1 caractere especial"
        )
        @Schema(example = "Senha@123")
        String senha,

        @NotBlank(message = "Username é obrigatório")
        @Min(value =6, message="Deve ter no mínimo 6 dígitos.")
        String username,

        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotBlank(message = "Telefone é obrigatório")
        @Pattern(
                regexp = "^(?:\\(?\\d{2}\\)?\\s?)?9?\\d{4}-?\\d{4}$",
                message = "Telefone inválido. Use o formato (11) 99999-9999 ou apenas números."
        )
        @Schema(example = "(11) 98888-7777")
        String telefone,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dataNascimento
){
    public UpdatePessoaFisicaRequest toPessoaRequest() {
        return new UpdatePessoaFisicaRequest(nome, telefone, dataNascimento);
    }

    public UpdateCredencialRequest toCredencialRequest() {
        return new UpdateCredencialRequest(email, username, senha);
    }
}
