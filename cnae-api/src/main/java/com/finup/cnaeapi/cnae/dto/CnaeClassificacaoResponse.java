package com.finup.cnaeapi.cnae.dto;

import com.finup.cnaeapi.classePrincipal.ClassePrincipal;
import com.finup.cnaeapi.cnae.Cnae;

/**
 * Resultado da classificação de um CNAE.
 * classificado = false quando o CNAE não está cadastrado (classe "Indefinido", sem subclasse).
 */
public record CnaeClassificacaoResponse(
        String cnae,
        String descricao,
        boolean classificado,
        ClasseResponse classePrincipal,
        ClasseResponse subClasse
) {

    public CnaeClassificacaoResponse(Cnae c) {
        this(
                c.getCnae(),
                c.getDescricao(),
                true,
                new ClasseResponse(c.getClassePrincipal().getId(), c.getClassePrincipal().getNome()),
                c.getSubClasse() != null ? new ClasseResponse(c.getSubClasse().getId(), c.getSubClasse().getNome()) : null
        );
    }

    public static CnaeClassificacaoResponse naoClassificado(String cnae, ClassePrincipal indefinido) {
        return new CnaeClassificacaoResponse(cnae, null, false,
                new ClasseResponse(indefinido.getId(), indefinido.getNome()), null);
    }
}
