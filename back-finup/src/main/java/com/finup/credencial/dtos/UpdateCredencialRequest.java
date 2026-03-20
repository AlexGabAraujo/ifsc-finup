package com.finup.credencial.dtos;

public record UpdateCredencialRequest (
        String email,
        String senha,
        String username
){
}
