package com.finup.subclasse.dto;

import com.finup.subclasse.SubClasse;

public record DetailSubClasseResponse (
        Long id,
        String nome
){

    public DetailSubClasseResponse(SubClasse s) {
        this(s.getId(), s.getNome());
    }
}
