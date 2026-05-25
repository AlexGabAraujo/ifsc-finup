package com.finup.classePrincipal.dto;

import com.finup.classePrincipal.ClassePrincipal;

public record DetailClassePrincipalResponse (
        Long id,
        String nome
){

    public DetailClassePrincipalResponse(ClassePrincipal c) {
        this(c.getId(), c.getNome());
    }
}
