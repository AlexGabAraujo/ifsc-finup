package com.finup.cnpjs.client.dto;

/**
 * Resposta da cnae-api para GET /cnae/{codigo}.
 * classificado = false quando o CNAE não está cadastrado (classe "Indefinido", sem subclasse).
 */
public record CnaeClassificacaoResponse(
        String cnae,
        String descricao,
        boolean classificado,
        Classe classePrincipal,
        Classe subClasse
) {

    public record Classe(Long id, String nome) {
    }
}
