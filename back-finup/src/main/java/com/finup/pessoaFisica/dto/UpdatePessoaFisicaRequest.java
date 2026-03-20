package com.finup.pessoaFisica.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdatePessoaFisicaRequest (
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotBlank(message = "Telefone é obrigatório")
        String telefone,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dataNascimento
){
}
