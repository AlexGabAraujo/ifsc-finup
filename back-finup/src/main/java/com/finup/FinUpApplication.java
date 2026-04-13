package com.finup;

import com.finup.telegramBot.Bot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class FinUpApplication {

    public static void main(String[] args) throws TelegramApiException {
        SpringApplication.run(FinUpApplication.class, args);
    }
}
