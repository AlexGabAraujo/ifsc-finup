package com.finup.cnpjs.dto;

import com.finup.cnpjs.Cnpj;

public record DetailCnpjResponse(
        Long id,
        String cnpj,
        String nomeEmpresa,
        String classePrincipal,
        String subClasse
) {
    public DetailCnpjResponse(Cnpj cnpj) {
        this(
                cnpj.getId(),
                cnpj.getCnpj(),
                cnpj.getNomeEmpresa(),
                cnpj.getClassePrincipal() != null ? cnpj.getClassePrincipal().getNome() : null,
                cnpj.getSubClasse() != null ? cnpj.getSubClasse().getNome() : null
        );
    }
}
