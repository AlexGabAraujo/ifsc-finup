package com.finup.telegramBot;

/**
 * Estados possíveis da conversa com o bot.
 *
 * IDLE             → aguardando /start ou mensagem inicial
 * MENU             → menu principal exibido, aguardando escolha
 * AGUARDANDO_IMAGEM → usuário escolheu "ler imagem", aguardando foto
 * CONFIRMANDO_IMAGEM → OCR feito, dados extraídos, aguardando confirmação
 * AGUARDANDO_TEXTO  → usuário escolheu "adicionar por texto", aguardando descrição
 * CONFIRMANDO_TEXTO → IA interpretou o texto, aguardando confirmação
 * AGUARDANDO_TIPO   → dados confirmados, aguardando DÉBITO ou CRÉDITO
 * AGUARDANDO_VINCULO → usuário não vinculado, aguardando username do app
 */
public enum BotState {
    IDLE,
    MENU,
    AGUARDANDO_IMAGEM,
    CONFIRMANDO_IMAGEM,
    AGUARDANDO_TEXTO,
    CONFIRMANDO_TEXTO,
    AGUARDANDO_TIPO
}
