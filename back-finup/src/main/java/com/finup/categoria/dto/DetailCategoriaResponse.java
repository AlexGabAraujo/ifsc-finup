package com.finup.categoria.dto;

import com.finup.categoria.Categoria;
import com.finup.transacao.Transacao;

import java.math.BigDecimal;

public record DetailCategoriaResponse(
        Long id,
        //String icone,
       // String cor,
        BigDecimal orcamento,
        Long pessoaFisicaId,
        Long subClasseId,
        Long classePrincipalId
) {
    public DetailCategoriaResponse(Categoria categoria) {
        this(
                categoria.getId(),
                //categoria.getIcone(),
               // categoria.getCor(),
                categoria.getOrcamento(),
                categoria.getPessoaFisica() != null ? categoria.getPessoaFisica().getId() : null,
                categoria.getSubClasse() != null ? categoria.getSubClasse().getId() : null,
                categoria.getClassePrincipal() != null ? categoria.getClassePrincipal().getId() : null
        );
    }
}