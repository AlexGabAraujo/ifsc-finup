package com.finup.openai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private static final String CATEGORIAS_VALIDAS =
            "Alimentação, Transporte, Saúde, Educação, Moradia, Academia, Lazer, Pet";

    private final ChatModel chatModel;

    public ChatService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String getResponse(String prompt) {
        return chatModel.call(prompt);
    }

    public String extrairDadosOcr(String textoOcr) {
        String prompt = """
                Você é um assistente financeiro. A partir do texto abaixo, extraído via OCR de uma nota fiscal ou comprovante, identifique:
                1. O valor TOTAL da transação (somente o número, sem "R$").
                2. O CNPJ do estabelecimento (somente os 14 dígitos, sem pontuação). Se não encontrar, responda null.
                3. A categoria mais adequada dentre: %s. Se não conseguir identificar, responda null.
                
                Responda EXATAMENTE neste formato, sem nenhum texto adicional:
                VALOR: <valor>
                CNPJ: <cnpj>
                CATEGORIA: <categoria>
                
                Texto da nota:
                %s
                """.formatted(CATEGORIAS_VALIDAS, textoOcr);

        return chatModel.call(prompt);
    }

    public String extrairDadosTexto(String descricao) {
        String prompt = """
                Você é um assistente financeiro. O usuário descreveu uma transação abaixo.
                Identifique:
                1. O valor da transação (somente o número, sem "R$"). Se não mencionado, responda null.
                2. A categoria mais adequada dentre: %s. Se não conseguir identificar, responda null.
                
                Responda EXATAMENTE neste formato, sem nenhum texto adicional:
                VALOR: <valor>
                CATEGORIA: <categoria>
                
                Descrição do usuário:
                %s
                """.formatted(CATEGORIAS_VALIDAS, descricao);

        return chatModel.call(prompt);
    }
}
