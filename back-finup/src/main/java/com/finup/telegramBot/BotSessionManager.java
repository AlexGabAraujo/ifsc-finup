package com.finup.telegramBot;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BotSessionManager {

    private final Map<Long, BotSessionState> sessions = new ConcurrentHashMap<>();

    public BotSessionState get(Long chatId) {
        return sessions.computeIfAbsent(chatId, id -> new BotSessionState());
    }

    public void remove(Long chatId) {
        sessions.remove(chatId);
    }
}
