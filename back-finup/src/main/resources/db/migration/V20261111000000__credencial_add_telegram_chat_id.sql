ALTER TABLE credencial
    ADD COLUMN telegram_chat_id BIGINT NULL,
    ADD UNIQUE KEY uk_credencial_telegram_chat_id (telegram_chat_id);
