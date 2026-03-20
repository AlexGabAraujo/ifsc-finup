package com.finup.pessoaFisica.dto;

import com.finup.pessoaFisica.PessoaFisica;

import java.time.LocalDate;

public record DetailPessoaFisicaResponse (
        String nome,
        String cpf,
        String telefone,
        LocalDate dataNascimento
){
    public DetailPessoaFisicaResponse(PessoaFisica pessoa){
        this(pessoa.getNome(), pessoa.getCpf(), pessoa.getTelefone(), pessoa.getDataNascimento());
    }
}
