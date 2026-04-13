package com.finup.telegramBot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class Bot extends TelegramLongPollingBot {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Override
    public String getBotUsername() {
        return "FinUp_IfscBot";
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update == null || !update.hasMessage())
            return;

        var message = update.getMessage();

        if (message.hasText())
            System.out.println("Texto recebido: " + message.getText());

        if (message.hasVoice())
            System.out.println("Voice recebida: " + message.getVoice().getFileId());

        if (message.hasPhoto())
            System.out.println("Foto recebida: " + message.getPhoto().get(0).getFileId());

        var chatId = message.getChatId();

        var sendMassage = SendMessage.builder().chatId(chatId.toString()).text("Olá, eu sou o FinUp Bot, o que você deseja?").build();

        try{
            execute(sendMassage);
        } catch (TelegramApiException e) {
            System.out.println("Mensagem da api do telegram: " + e.getMessage());
        }
    }
}
