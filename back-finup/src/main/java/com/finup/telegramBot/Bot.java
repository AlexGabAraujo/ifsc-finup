package com.finup.telegramBot;

import com.finup.openai.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;

@Component
public class Bot extends TelegramLongPollingBot {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Autowired
    private OcrService ocrService;

    @Autowired
    private ChatService chatService;

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
        var chatId = message.getChatId().toString();

        if (message.hasText())
            System.out.println("Texto recebido: " + message.getText());

        if (message.hasVoice())
            System.out.println("Voice recebida: " + message.getVoice().getFileId());

        if (message.hasPhoto()) {
            var foto = message.getPhoto().get(message.getPhoto().size() - 1);
            String fileId = foto.getFileId();

            try {
                GetFile getFile = new GetFile(fileId);
                org.telegram.telegrambots.meta.api.objects.File telegramFile = execute(getFile);
                File imagemLocal = downloadFile(telegramFile);

                String texto = ocrService.extrairTexto(imagemLocal);
                imagemLocal.delete();

                String resposta = chatService.extrairDadosOcr(texto);
                execute(SendMessage.builder().chatId(chatId).text(resposta).build());

            } catch (TelegramApiException e) {
                System.out.println("Erro ao baixar imagem: " + e.getMessage());
            }
        }

        if (message.hasText()) {
            try {
                execute(SendMessage.builder().chatId(chatId).text("Olá, eu sou o FinUp Bot, o que você deseja?").build());
            } catch (TelegramApiException e) {
                System.out.println("Erro ao enviar mensagem: " + e.getMessage());
            }
        }
    }
}