package com.vnnaz.telegrambot.bot;

import com.vnnaz.telegrambot.configuration.BotConfiguration;
import com.vnnaz.telegrambot.service.MessageFilterService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@AllArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private BotConfiguration botConfiguration;
    private MessageFilterService messageFilterService;
    @Override
    public String getBotUsername() {
        return botConfiguration.getBotName();
    }
    @Override
    public String getBotToken(){
        return botConfiguration.getToken();
    }
    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() &&  update.getMessage().hasText()){
            Message message = update.getMessage();

            String request = message.getText();
            long chatId = message.getChatId();

            String response = "need update";
            sendMessage(chatId, response);
        }
    }
    public void sendMessage(long chatID, String text){
        SendMessage sendMessage = new SendMessage();

        sendMessage.setText(text);
        sendMessage.setChatId(chatID);

        try {
            this.execute(sendMessage);
        } catch (TelegramApiException e) {
            System.out.println(getClass() + ": error while sending response");
        }
    }
}
