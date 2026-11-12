package com.finup.telegramBot;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BotSessionState {

    private BotState state = BotState.IDLE;

    private String textoOcr;
    private String cnpjIdentificado;
    private String nomeEmpresa;

    private BigDecimal valor;
    private String categoriaSugerida;
    private Long classePrincipalId;
    private Long subClasseId;
    private Long cnpjId;
    private String respostaIa;

    public void reset() {
        this.state = BotState.IDLE;
        this.textoOcr = null;
        this.cnpjIdentificado = null;
        this.nomeEmpresa = null;
        this.valor = null;
        this.categoriaSugerida = null;
        this.classePrincipalId = null;
        this.subClasseId = null;
        this.cnpjId = null;
        this.respostaIa = null;
    }
}
