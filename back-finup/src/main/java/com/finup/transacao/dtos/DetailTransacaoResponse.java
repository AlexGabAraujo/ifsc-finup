package com.finup.transacao.dtos;

import com.finup.transacao.TipoGasto;
import com.finup.transacao.TipoPagamento;
import com.finup.transacao.Transacao;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DetailTransacaoResponse (
        Long id,
        BigDecimal valor,
        Long pessoaFisicaId,
        TipoPagamento tipoPagamento,
        TipoGasto tipoGasto,
        Long subClasseId,
        Long classePrincipalId,
        Long cnpjId
){
    public DetailTransacaoResponse(Transacao transacao){
        this(transacao.getId(), transacao.getValor(), transacao.getPessoaFisica().getId(),
                transacao.getTipoPagamento(), transacao.getTipoGasto(), transacao.getSubClasse().getId(),
                transacao.getClassePrincipal().getId(), transacao.getCnpj().getId());
    }
}
