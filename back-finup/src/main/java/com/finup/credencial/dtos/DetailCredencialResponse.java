package com.finup.credencial.dtos;

import com.finup.credencial.Credencial;

public record DetailCredencialResponse (
        String email,
        String senha,
        String username
){
    public DetailCredencialResponse(Credencial credencial){
        this(credencial.getEmail(), credencial.getSenha(), credencial.getUsername());
    }
}
