package com.finup.telegramBot;

import com.finup.pessoaFisica.PessoaFisica;
import com.finup.transacao.TipoGasto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.List;
import java.util.Optional;

@Component
public class Bot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(Bot.class);

    private static final String CB_IMAGEM    = "OPCAO_IMAGEM";
    private static final String CB_TEXTO     = "OPCAO_TEXTO";
    private static final String CB_DEBITO    = "TIPO_DEBITO";
    private static final String CB_CREDITO   = "TIPO_CREDITO";
    private static final String CB_CONFIRMAR = "CONFIRMAR";
    private static final String CB_CANCELAR  = "CANCELAR";

    @Value("${telegram.bot.token}")
    private String botToken;

    private final OcrService ocrService;
    private final TelegramTransacaoService transacaoService;
    private final BotSessionManager sessionManager;

    public Bot(OcrService ocrService,
               TelegramTransacaoService transacaoService,
               BotSessionManager sessionManager) {
        this.ocrService = ocrService;
        this.transacaoService = transacaoService;
        this.sessionManager = sessionManager;
    }

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
        if (update == null) return;

        if (update.hasCallbackQuery()) {
            handleCallback(update);
            return;
        }

        if (!update.hasMessage()) return;

        var message = update.getMessage();
        Long chatId = message.getChatId();
        BotSessionState sessao = sessionManager.get(chatId);

        if (message.hasText() && message.getText().equalsIgnoreCase("/cancelar")) {
            sessao.reset();
            enviar(chatId, "Operacao cancelada. Digite /start para recomecar.");
            return;
        }

        if (!transacaoService.isVinculado(chatId)) {
            handleVinculo(chatId, sessao, message.hasText() ? message.getText() : null);
            return;
        }

        switch (sessao.getState()) {
            case IDLE, MENU    -> handleIdle(chatId, sessao, message);
            case AGUARDANDO_IMAGEM -> handleAguardandoImagem(chatId, sessao, message);
            case AGUARDANDO_TEXTO  -> handleAguardandoTexto(chatId, sessao, message);
            default -> enviarMenu(chatId);
        }
    }

    private void handleVinculo(Long chatId, BotSessionState sessao, String texto) {
        if (sessao.getState() != BotState.AGUARDANDO_TIPO) {
            sessao.setState(BotState.AGUARDANDO_TIPO);
            enviar(chatId,
                    "Ola! Sou o FinUp Bot.\n\n" +
                    "Para comecar, me informe o seu username cadastrado no aplicativo FinUp:");
            return;
        }

        if (texto == null || texto.isBlank()) {
            enviar(chatId, "Por favor, envie o seu username em texto.");
            return;
        }

        boolean vinculado = transacaoService.vincularConta(chatId, texto.trim());
        if (vinculado) {
            sessao.reset();
            enviar(chatId, "Conta vinculada com sucesso! Bem-vindo ao FinUp Bot.");
            enviarMenu(chatId);
        } else {
            enviar(chatId, "Username nao encontrado. Verifique e tente novamente:");
        }
    }

    private void handleIdle(Long chatId, BotSessionState sessao,
                             org.telegram.telegrambots.meta.api.objects.Message message) {
        enviarMenu(chatId);
    }

    private void handleAguardandoImagem(Long chatId, BotSessionState sessao,
                                        org.telegram.telegrambots.meta.api.objects.Message message) {
        if (!message.hasPhoto()) {
            enviar(chatId, "Por favor, envie uma foto da nota fiscal ou comprovante.");
            return;
        }

        enviar(chatId, "Processando a imagem, aguarde...");

        try {
            var foto = message.getPhoto().get(message.getPhoto().size() - 1);
            GetFile getFile = new GetFile(foto.getFileId());
            org.telegram.telegrambots.meta.api.objects.File telegramFile = execute(getFile);
            File imagemLocal = downloadFile(telegramFile);

            String textoOcr = ocrService.extrairTexto(imagemLocal);
            imagemLocal.delete();

            if (textoOcr == null || textoOcr.isBlank()) {
                enviar(chatId, "Nao consegui extrair texto da imagem. Tente uma foto com melhor iluminacao ou use a opcao de texto.");
                sessao.reset();
                enviarMenu(chatId);
                return;
            }

            sessao.setTextoOcr(textoOcr);
            transacaoService.processarOcr(textoOcr, sessao);

            String confirmacao = transacaoService.montarMensagemConfirmacao(sessao);

            if (sessao.getValor() == null) {
                enviar(chatId, confirmacao + "\nNao foi possivel identificar o valor. Deseja tentar pelo texto?\n\nUse /cancelar para recomecar.");
                sessao.reset();
                enviarMenu(chatId);
                return;
            }

            Optional<PessoaFisica> pessoaOpt = transacaoService.buscarPessoa(chatId);
            if (pessoaOpt.isPresent()) {
                transacaoService.resolverCategoria(sessao, pessoaOpt.get().getId());
            }

            sessao.setState(BotState.CONFIRMANDO_IMAGEM);
            enviarConfirmacao(chatId, confirmacao);

        } catch (TelegramApiException e) {
            log.error("Erro ao processar imagem do chatId {}: {}", chatId, e.getMessage());
            enviar(chatId, "Ocorreu um erro ao processar a imagem. Tente novamente.");
        }
    }

    private void handleAguardandoTexto(Long chatId, BotSessionState sessao,
                                       org.telegram.telegrambots.meta.api.objects.Message message) {
        if (!message.hasText()) {
            enviar(chatId, "Por favor, descreva a transacao em texto.\nEx.: Paguei R$ 50 no mercado");
            return;
        }

        enviar(chatId, "Analisando a descricao...");

        transacaoService.processarTexto(message.getText(), sessao);

        String confirmacao = transacaoService.montarMensagemConfirmacao(sessao);

        if (sessao.getValor() == null) {
            enviar(chatId, confirmacao + "\nNao consegui identificar o valor. Por favor, informe no formato ex.: 45.90 e tente novamente:");
            return;
        }

        Optional<PessoaFisica> pessoaOpt = transacaoService.buscarPessoa(chatId);
        if (pessoaOpt.isPresent()) {
            transacaoService.resolverCategoria(sessao, pessoaOpt.get().getId());
        }

        sessao.setState(BotState.CONFIRMANDO_TEXTO);
        enviarConfirmacao(chatId, confirmacao);
    }

    private void handleCallback(Update update) {
        var query = update.getCallbackQuery();
        Long chatId = query.getMessage().getChatId();
        String data = query.getData();
        BotSessionState sessao = sessionManager.get(chatId);

        switch (data) {
            case CB_IMAGEM -> {
                sessao.reset();
                sessao.setState(BotState.AGUARDANDO_IMAGEM);
                enviar(chatId, "Envie uma foto da nota fiscal ou comprovante:");
            }
            case CB_TEXTO -> {
                sessao.reset();
                sessao.setState(BotState.AGUARDANDO_TEXTO);
                enviar(chatId, "Descreva a transacao:\nEx.: Paguei R$ 45 no restaurante ou recebi R$ 500 de freela");
            }
            case CB_CONFIRMAR -> handleConfirmar(chatId, sessao);
            case CB_CANCELAR -> {
                sessao.reset();
                enviar(chatId, "Operacao cancelada.");
                enviarMenu(chatId);
            }
            case CB_DEBITO  -> handleSalvar(chatId, sessao, TipoGasto.DEBITO);
            case CB_CREDITO -> handleSalvar(chatId, sessao, TipoGasto.CREDITO);
        }
    }

    private void handleConfirmar(Long chatId, BotSessionState sessao) {
        if (sessao.getState() != BotState.CONFIRMANDO_IMAGEM && sessao.getState() != BotState.CONFIRMANDO_TEXTO) {
            enviarMenu(chatId);
            return;
        }
        sessao.setState(BotState.AGUARDANDO_TIPO);
        enviarSelecaoTipo(chatId);
    }

    private void handleSalvar(Long chatId, BotSessionState sessao, TipoGasto tipoGasto) {
        if (sessao.getState() != BotState.AGUARDANDO_TIPO) {
            enviarMenu(chatId);
            return;
        }

        Optional<PessoaFisica> pessoaOpt = transacaoService.buscarPessoa(chatId);
        if (pessoaOpt.isEmpty()) {
            enviar(chatId, "Nao foi possivel identificar seu usuario. Use /cancelar e tente novamente.");
            sessao.reset();
            return;
        }

        try {
            transacaoService.salvarTransacao(sessao, pessoaOpt.get(), tipoGasto);
            String tipo = tipoGasto == TipoGasto.DEBITO ? "GASTO" : "RECEBIMENTO";
            enviar(chatId, "Transacao salva como " + tipo + " de R$ " + sessao.getValor() + "!\n\nDeseja registrar outra?");
            sessao.reset();
            enviarMenu(chatId);
        } catch (Exception e) {
            log.error("Erro ao salvar transacao para chatId {}: {}", chatId, e.getMessage());
            enviar(chatId, "Erro ao salvar: " + e.getMessage() + "\n\nUse /cancelar para recomecar.");
            sessao.reset();
        }
    }

    private void enviarMenu(Long chatId) {
        BotSessionState sessao = sessionManager.get(chatId);
        sessao.setState(BotState.MENU);

        InlineKeyboardButton btnImagem = InlineKeyboardButton.builder()
                .text("Ler nota fiscal / comprovante")
                .callbackData(CB_IMAGEM)
                .build();

        InlineKeyboardButton btnTexto = InlineKeyboardButton.builder()
                .text("Descrever transacao por texto")
                .callbackData(CB_TEXTO)
                .build();

        InlineKeyboardMarkup teclado = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(btnImagem))
                .keyboardRow(List.of(btnTexto))
                .build();

        try {
            execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("FinUp Bot - O que deseja fazer?")
                    .replyMarkup(teclado)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Erro ao enviar menu para chatId {}: {}", chatId, e.getMessage());
        }
    }

    private void enviarConfirmacao(Long chatId, String textoConfirmacao) {
        InlineKeyboardButton btnConfirmar = InlineKeyboardButton.builder()
                .text("Confirmar")
                .callbackData(CB_CONFIRMAR)
                .build();

        InlineKeyboardButton btnCancelar = InlineKeyboardButton.builder()
                .text("Cancelar")
                .callbackData(CB_CANCELAR)
                .build();

        InlineKeyboardMarkup teclado = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(btnConfirmar, btnCancelar))
                .build();

        try {
            execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(textoConfirmacao + "\nOs dados estao corretos?")
                    .replyMarkup(teclado)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Erro ao enviar confirmacao para chatId {}: {}", chatId, e.getMessage());
        }
    }

    private void enviarSelecaoTipo(Long chatId) {
        InlineKeyboardButton btnDebito = InlineKeyboardButton.builder()
                .text("Gasto (Debito)")
                .callbackData(CB_DEBITO)
                .build();

        InlineKeyboardButton btnCredito = InlineKeyboardButton.builder()
                .text("Recebimento (Credito)")
                .callbackData(CB_CREDITO)
                .build();

        InlineKeyboardMarkup teclado = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(btnDebito, btnCredito))
                .build();

        try {
            execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("Essa transacao e um gasto ou um recebimento?")
                    .replyMarkup(teclado)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Erro ao enviar selecao de tipo para chatId {}: {}", chatId, e.getMessage());
        }
    }

    private void enviar(Long chatId, String texto) {
        try {
            execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(texto)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Erro ao enviar mensagem para chatId {}: {}", chatId, e.getMessage());
        }
    }
}
