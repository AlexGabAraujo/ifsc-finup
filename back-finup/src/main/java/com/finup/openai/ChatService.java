package com.finup.openai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatModel chatModel;

    public ChatService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String getResponse(String prompt){
        return chatModel.call(prompt);
    }

    public String extrairDadosOcr(String textoOcr){
        String prompt = """
        A partir do texto abaixo extraído de uma nota fiscal ou comprovante, identifique:
        - O valor total da transação
        - O CNPJ do estabelecimento (se houver)
        
        Após identificar, responda APENAS neste formato:
        "Identifiquei uma transação de R$ [valor] no CNPJ [cnpj]. Essa transação é um GASTO ou uma DESPESA?"
        
        Se não encontrar alguma informação, substitua por "não identificado".
        
        Texto da nota:
        """ + textoOcr;

        return chatModel.call(prompt);
    }
}
