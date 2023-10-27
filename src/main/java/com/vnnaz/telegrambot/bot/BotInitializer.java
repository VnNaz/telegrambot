package com.vnnaz.telegrambot.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
public class BotInitializer {
    private TelegramBot bot;
    @Autowired
    public BotInitializer(TelegramBot bot) {
        this.bot = bot;
    }
    @EventListener({ApplicationReadyEvent.class})
    public void init() throws TelegramApiException {

        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);

        try{
            telegramBotsApi.registerBot(bot);
            System.out.println(getClass() + ": bot is register");
        }catch (TelegramApiException e)
        {
            System.out.println(getClass() + ": can't register bot");
        }
    }
}
