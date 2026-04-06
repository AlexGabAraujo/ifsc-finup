package com.finup.transacao.dtos;

import com.finup.transacao.TipoGasto;
import com.finup.transacao.TipoPagamento;
import com.finup.transacao.Transacao;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DetailTransacaoResponse(
        Long id,
        BigDecimal valor,
        Long pessoaFisicaId,
        TipoPagamento tipoPagamento,
        TipoGasto tipoGasto,
        LocalDate dataTransacao,
        Long subClasseId,
        Long classePrincipalId,
        Long cnpjId
) {
    public DetailTransacaoResponse(Transacao transacao) {
        this(
                transacao.getId(),
                transacao.getValor(),
                transacao.getPessoaFisica() != null ? transacao.getPessoaFisica().getId() : null,
                transacao.getTipoPagamento(),
                transacao.getTipoGasto(),
                transacao.getDataTransacao(),
                transacao.getSubClasse() != null ? transacao.getSubClasse().getId() : null,
                transacao.getClassePrincipal() != null ? transacao.getClassePrincipal().getId() : null,
                transacao.getCnpj() != null ? transacao.getCnpj().getId() : null
        );
    }
}
