package com.vnnaz.telegrambot.service;

import com.vdurmont.emoji.EmojiParser;
import com.vnnaz.telegrambot.config.StringConstant;
import com.vnnaz.telegrambot.model.event.Event;
import com.vnnaz.telegrambot.model.event.EventRepository;
import com.vnnaz.telegrambot.model.task.Task;
import com.vnnaz.telegrambot.model.task.TaskRepository;
import com.vnnaz.telegrambot.model.user.User;
import com.vnnaz.telegrambot.model.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class MessageSender {
    @Autowired
    public MessageSender(TelegramLongPollingBot bot) {
        this.bot = bot;
    }
    private final TelegramLongPollingBot bot;

    private UserRepository userRepository;
    private TaskRepository taskRepository;
    private EventRepository eventRepository;
    @Autowired
    public void setEventRepository(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }
    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Autowired
    public void setTaskRepository(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public void sendALL(String text){
        var users = userRepository.findAll();

        for(var user : users){
            sendMessage(user.getChatId(), text);
        }
    }
    public void sendMessage(long chatID, String text){
        SendMessage sendMessage = new SendMessage();

        sendMessage.setText(text);
        sendMessage.setChatId(chatID);

        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Error while sending message: " + e.getMessage());
        }
    }
    public void sendEditMessage(long chatID, long messID, String text){
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatID);
        editMessageText.setMessageId((int) messID);
        editMessageText.setText(text);
        editMessageText.enableMarkdown(true);

        try{
            bot.execute(editMessageText);
        } catch (TelegramApiException e) {
            log.error("Error while send edit message: " + e.getMessage());
        }
    }
    public void sendMessageWithMarkup(long chatID, String text, ReplyKeyboard keyboard){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatID);
        sendMessage.setText(text);
        sendMessage.setReplyMarkup(keyboard);

        try{
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Error when send: " + e.getMessage());
        }
    }
    @Transactional
    public void sendUserTasks(long chatId) {
        Optional<User> otp = userRepository.findById(chatId);
        List<Task> tasks = null;
        if(otp.isPresent())
        {
            tasks = otp.get().getTasks();
        }else {
            sendMessage(chatId, EmojiParser.parseToUnicode("You currently don't registed in system :crying_cat_face:, please use /register to registed in our system :love_you_gesture:"));
            return;
        }
        if(tasks == null || tasks.isEmpty())
        {
            sendMessage(chatId, EmojiParser.parseToUnicode("You currently don't have any task :crying_cat_face:, please use /addtask to add new task :love_you_gesture:"));
            return;
        }
        String text = EmojiParser.parseToUnicode(":dart: On Target and Ready to Go!");

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = null;

        // mapping inline button to keyboard, 2 button in one row
        for(var task : tasks){
            row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(task.getBody());
            String data = StringConstant.TASK+task.getId();
            button.setCallbackData(data);
            row.add(button);
            keyboard.add(row);
        }

        // send inline button menu
        inlineKeyboard.setKeyboard(keyboard);
        sendMessageWithMarkup(chatId, text, inlineKeyboard);
    }
    public void removeMessage(Long chatId, int messageId)
    {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(messageId);

        try {
            bot.execute(deleteMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
    public void sendUserTask(Long chatId, int messageId, Long taskId){
        Optional<Task> otp = taskRepository.findById(taskId);
        if(otp.isPresent())
        {
            Task task = otp.get();
            EditMessageText editMessageText = new EditMessageText();
            editMessageText.setChatId(chatId);
            editMessageText.setMessageId(messageId);
            editMessageText.setText(task.getBody());
            editMessageText.enableMarkdown(true);

            InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            List<InlineKeyboardButton> row = new ArrayList<>();

            // add back button in detail task
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(EmojiParser.parseToUnicode("Back :leftwards_arrow_with_hook:"));;
            button.setCallbackData(StringConstant.TASK_BACK);
            row.add(button);

            // add done button in detail task
            button = new InlineKeyboardButton();
            button.setText(EmojiParser.parseToUnicode("Done :heavy_check_mark:"));;
            button.setCallbackData(StringConstant.TASK_DONE + taskId);
            row.add(button);

            keyboard.add(row);
            inlineKeyboard.setKeyboard(keyboard);
            editMessageText.setReplyMarkup(inlineKeyboard);
            try{
                bot.execute(editMessageText);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }

        }else{
            sendEditMessage(chatId, messageId, "Task is no longer valid");        }
    }
    public void sendAllCurrentEvent(Long chatId){
        Optional<User> optUser = userRepository.findById(chatId);
        if(!optUser.isPresent()){
            sendMessage(chatId,"You are not registed in system, please use /start or /register to join");
        }
        List<Event> events = eventRepository.findByDate(new Date(System.currentTimeMillis()));
        if(events.isEmpty())
        {
            sendMessage(chatId, EmojiParser.parseToUnicode("currently don't have any event :crying_cat_face:"));
            return;
        }
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = null;

        for(var event : events){

            row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(event.getName());
            String data = StringConstant.EVENT +event.getEventId();
            button.setCallbackData(data);
            row.add(button);
            keyboard.add(row);
        }

        // send inline button menu
        inlineKeyboard.setKeyboard(keyboard);
        String text = EmojiParser.parseToUnicode(":date: All current event :");
        sendMessageWithMarkup(chatId, text, inlineKeyboard);
    }

    @Transactional
    public void sendAllUserEvent(long chatId) {
        Optional<User> optUser = userRepository.findById(chatId);
        if(!optUser.isPresent()){
            sendMessage(chatId,"You are not registed in system, please use /start or /register to join");
        }
        User user = optUser.get();
        List<Event> events = eventRepository.findByChatId(chatId);
        if(events.isEmpty())
        {
            sendMessage(chatId, EmojiParser.parseToUnicode("currently you didn't join any event :crying_cat_face:"));
            return;
        }
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = null;

        for(var event : events){

            row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(event.getName());
            String data = StringConstant.EVENT_DETAIL +event.getEventId();
            button.setCallbackData(data);
            row.add(button);
            keyboard.add(row);
        }

        // send inline button menu
        inlineKeyboard.setKeyboard(keyboard);
        String text = EmojiParser.parseToUnicode(":date: All your current event :");
        sendMessageWithMarkup(chatId, text, inlineKeyboard);
    }

    public void sendUserEvent(long chatId,  Long eventId, int messageId) {
        Optional<Event> otp = eventRepository.findById(eventId);
        if(!otp.isPresent())
        {
            sendEditMessage(chatId, messageId, "Event is no longer valid");
        }
        Event event = otp.get();
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId);
        editMessageText.setText("This event about " + event.getName() + "\nYou joined this event at " + event.getDate());
        editMessageText.enableMarkdown(true);

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();

        // add back button in detail task
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(EmojiParser.parseToUnicode("Back :leftwards_arrow_with_hook:"));;
        button.setCallbackData(StringConstant.EVENT_BACK);
        row.add(button);

        // add done button in detail task
        button = new InlineKeyboardButton();
        button.setText(EmojiParser.parseToUnicode("leave :crying_cat_face:"));;
        button.setCallbackData(StringConstant.EVENT_LEAVE + eventId);
        row.add(button);

        keyboard.add(row);
        inlineKeyboard.setKeyboard(keyboard);
        editMessageText.setReplyMarkup(inlineKeyboard);
        try{
            bot.execute(editMessageText);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
