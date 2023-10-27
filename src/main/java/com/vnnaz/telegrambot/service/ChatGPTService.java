package com.vnnaz.telegrambot.service;

import io.github.flashvayne.chatgpt.service.ChatgptService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ChatGPTService {
    private ChatgptService chatgptService;
    public String getResponse(String request){
        String response = chatgptService.sendMessage(request);
        return response;
    }
    public String getRewriteTextResponse(String context){
        String response = chatgptService.sendMessage("Rewrite this: " + context);
        return response;
    }
}
