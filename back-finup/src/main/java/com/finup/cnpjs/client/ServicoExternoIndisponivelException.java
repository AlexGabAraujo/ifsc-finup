package com.finup.cnpjs.client;

/**
 * Falha temporária ao consultar um serviço externo (CNPJá ou cnae-api):
 * limite de requisições, erro 5xx ou serviço fora do ar.
 * Quem recebe esta exceção não deve salvar o resultado, para tentar de novo depois.
 */
public class ServicoExternoIndisponivelException extends RuntimeException {

    public ServicoExternoIndisponivelException(String message) {
        super(message);
    }

    public ServicoExternoIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}
