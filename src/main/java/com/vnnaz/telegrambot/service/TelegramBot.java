package com.vnnaz.telegrambot.service;

import com.vnnaz.telegrambot.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig config;
    @Autowired
    public TelegramBot(BotConfig config) {
        this.config = config;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }
    @Override
    public String getBotToken(){
        return config.getToken();
    }
    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText())
        {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String userFirstname = update.getMessage().getChat().getFirstName();

            switch (messageText){
                case "/start":
                {
                    startCommandReceived(chatId, userFirstname);
                    break;
                }
                default:
                {
                    sendMessage(chatId, "Sorry, your command was not recognized");
                }
            }

        }
    }
    private void startCommandReceived(long chatId, String userFirstname){
        String answer = "Hi, " + userFirstname + ", nice to meet you";
        sendMessage(chatId, answer);
    }
    private void sendMessage(long chatId, String textToSend){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(textToSend);

        try{
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("""
                    Error while sending message:
                    To: %d
                    Body: %s
                    """.formatted(chatId, textToSend));
        }
        log.info("Sent message to user: " + chatId);

    }
}
