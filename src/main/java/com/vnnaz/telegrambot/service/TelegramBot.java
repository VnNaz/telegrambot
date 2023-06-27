package com.vnnaz.telegrambot.service;

import com.vdurmont.emoji.EmojiParser;
import com.vnnaz.telegrambot.config.BotConfig;
import com.vnnaz.telegrambot.config.StringConstant;
import com.vnnaz.telegrambot.model.event.Event;
import com.vnnaz.telegrambot.model.event.EventRepository;
import com.vnnaz.telegrambot.model.task.Task;
import com.vnnaz.telegrambot.model.task.TaskRepository;
import com.vnnaz.telegrambot.model.user.User;
import com.vnnaz.telegrambot.model.user.UserRepository;
import com.vnnaz.telegrambot.model.userevent.UserEvent;
import com.vnnaz.telegrambot.model.userevent.UserEventId;
import com.vnnaz.telegrambot.model.userevent.UserEventRespository;
import io.github.flashvayne.chatgpt.service.ChatgptService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;

import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listCommand = new ArrayList<>();
        listCommand.add(new BotCommand("/start", "Start your bot"));
        listCommand.add(new BotCommand("/mydata", "Get user data"));
        listCommand.add(new BotCommand("/deletedata", "Delete user data"));
        listCommand.add(new BotCommand("/register", "Register your account"));
        listCommand.add(new BotCommand("/mytask", "Show your current tasks"));
        listCommand.add(new BotCommand("/addtask", "Add new task"));
        listCommand.add(new BotCommand("/event", "Add new event"));
        listCommand.add(new BotCommand("/join", "join an event"));
        listCommand.add(new BotCommand("/myevent", "show all your event"));
        listCommand.add(new BotCommand("/about", EmojiParser.parseToUnicode("Info bot creator :black_heart:")));
        try{
            execute(new SetMyCommands(listCommand, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error while setting list command: " + e.getMessage());
        }
    }
    private UserRepository userRepository;
    private final BotConfig config;
    private MessageSender messageSender;
    private TaskRepository taskRepository;
    private UserEventRespository userEventRespository;
    private EventRepository eventRepository;
    private ChatgptService chatgptService;
    @Autowired
    public void setUserEventRepository(UserEventRespository userEventRespository) {
        this.userEventRespository = userEventRespository;
    }
    @Autowired
    public void setEventRepository(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }
    @Autowired
    public void setMessageSender(MessageSender messageSender) {
        this.messageSender = messageSender;
    }
    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Autowired
    public void setChatgptService(ChatgptService chatgptService) {
        this.chatgptService = chatgptService;
    }

    @Autowired
    public void setTaskRepository(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
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
        // wait to get request from user then return a response
        if(update.hasMessage() && update.getMessage().hasText())
        {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String fullname = update.getMessage().getChat().getFirstName() + " " + update.getMessage().getChat().getLastName();

            // send a message to all user in system
            if(messageText.contains("/send"))
            {
                var textToSend = EmojiParser.parseToUnicode(":bell: " + messageText.replace("/send", ""));
                messageSender.sendALL(textToSend);
                return;
            }
            // send a question to chat gpt
            else if(messageText.contains("/ask")) {
                var question = messageText.replace("/ask", "");
                String response = chatgptService.sendMessage(question);
                messageSender.sendMessage(chatId,response);
                return;
            }
            // generated an image using AI
            else if(messageText.contains("/draw")){
                var question = messageText.replace("/draw", "");
                String response = chatgptService.imageGenerate(question);
                messageSender.sendMessage(chatId, response);
                return;
            }
            // add new task into user's todo list
            else if(messageText.contains("/addtask")){
                var taskBody = messageText.replace("/addtask", "");
                addNewTask(chatId, taskBody);
                String response = chatgptService.sendMessage("/ask user "+ fullname +" added new task to his todo-list about "+ taskBody +",  and you need  to write an announcement about it. Also, he can use command /mytask to show all his task (informal, teenage, with several icons)");
                messageSender.sendMessage(chatId, response);
                return;
            }
            // create new task
            else if(messageText.contains("/event"))
            {
                var eventName = messageText.replace("/event", "");
                if(eventName.isEmpty())
                {
                    messageSender.sendMessage(chatId, "Event name can't not be empty");
                    return;
                }
                String creatorName = update.getMessage().getChat().getFirstName();
                addNewEvent(creatorName,eventName);
                return;
            }
            switch (messageText) {
                // start using system, ie registed into system
                case "/start" -> {
                    registerUser(update.getMessage());
                    startCommandReceived(chatId, fullname);
                }
                // get information about bot
                case "/about" -> messageSender.sendMessage(chatId, StringConstant.ABOUT);
                // get information about current user
                case "/mydata" ->{
                    var opt = userRepository.findById(chatId);

                    opt.ifPresentOrElse(
                            user -> messageSender.sendMessage(chatId,user.getData()),
                            () -> messageSender.sendMessage(chatId,"You're not registered in system, please use /register")
                    );
                }
                // exit from system
                case "/deletedata" ->{
                    if(isRegistered(chatId)){
                        userRepository.deleteById(chatId);
                        messageSender.sendMessage(chatId,EmojiParser.parseToUnicode("You are unregistered from now :sob:"));
                    }
                }
                // show all user task
                case "/mytask" -> messageSender.sendUserTasks(chatId);
                // show all current event
                case "/join" -> messageSender.sendAllCurrentEvent(chatId);
                // sign in system
                case "/register" ->{
                    registerUser(update.getMessage());
                    messageSender.sendMessage(chatId, EmojiParser.parseToUnicode("You're registered from now :fire:"));
                }
                case "/myevent" -> messageSender.sendAllUserEvent(chatId);
                // send help menu if all command didn't match
                default -> sendHelpMenu(chatId);
            }
            // handle button data return
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            long messageID = update.getCallbackQuery().getMessage().getMessageId();

            if (callbackData.matches(StringConstant.TASK_REGEX)) {
                // from callback data -> get info about task -> send to user
                Long taskID = Long.valueOf(callbackData.replace(StringConstant.TASK, ""));
                messageSender.sendUserTask(chatId, (int) messageID, taskID);
            } else if (callbackData.matches(StringConstant.TASK_DONE_REGEX)) {
                // from callback data -> get info about task -> remove it from todo list
                Long taskID = Long.valueOf(callbackData.replace(StringConstant.TASK_DONE, ""));
                removeUserTask(chatId, taskID, (int) messageID);
            } else if (callbackData.contains(StringConstant.TASK_BACK)){
                // return menu todo list
                messageSender.removeMessage(chatId, (int) messageID);
                messageSender.sendUserTasks(chatId);
            } else if(callbackData.matches(StringConstant.EVENT_REGEX)){
                // user join in an event
                Long eventId = Long.valueOf(callbackData.replace(StringConstant.EVENT, ""));
                messageSender.removeMessage(chatId, (int) messageID);
                registerUserEvent(chatId, eventId);
            } else if(callbackData.matches(StringConstant.EVENT_DETAIL_REGEX)){
                // show user detail about event, which he joined
                Long eventId = Long.valueOf(callbackData.replace(StringConstant.EVENT_DETAIL,""));
                messageSender.sendUserEvent(chatId, eventId, (int) messageID);
            } else if(callbackData.contains(StringConstant.EVENT_BACK))
            {
                // return menu user's event
                messageSender.removeMessage(chatId, (int) messageID);
                messageSender.sendAllUserEvent(chatId);
            } else if(callbackData.matches(StringConstant.EVENT_LEAVE_REGEX)){
                // remove an event from user list event
                Long eventId = Long.valueOf(callbackData.replace(StringConstant.EVENT_LEAVE, ""));
                removeUserEvent(chatId, eventId, (int) messageID);
            }
        }
    }

    @Transactional
    private void removeUserEvent(long chatId, Long eventId, int messageId) {
        String eventName = eventRepository.findById(eventId).get().getName();
        User user = userRepository.findById(chatId).get();
        String fullname = user.getFirstName() + " " + user.getLastName();
        UserEventId userEventId = new UserEventId(chatId, eventId);
        userEventRespository.deleteById(userEventId);

        Event event = eventRepository.findById(eventId).get();
        Long currentMember = event.getTotalMember();
        event.setTotalMember(currentMember -1);

        String response = chatgptService.sendMessage("announce user "+ fullname +" after he leave an event about \""+ eventName +"\", also tell him that he can use /myevent to show again all his event, which he joined (informal, teenage, with several icons)");
        messageSender.sendEditMessage(chatId, messageId, response);
    }

    @Transactional
    private void removeUserTask(long chatID, Long taskID, int messageID) {
        String taskBody = taskRepository.findById(taskID).get().getBody();
        taskRepository.deleteById(taskID);
        User user = userRepository.findById(chatID).get();
        String fullname = user.getFirstName() + " " + user.getLastName();
        String response = chatgptService.sendMessage("cheering user "+ fullname +" after he finished his own task \""+ taskBody +"\", also tell him that he can use /mytask to show all his task again (informal, teenage, with several icons)");
        messageSender.sendEditMessage(chatID, messageID, response);
    }

    @Transactional
    private void registerUserEvent(long chatID, Long eventId) {
        Event event = eventRepository.findById(eventId).get();
        User user = userRepository.findById(chatID).get();

        UserEvent userEvent = new UserEvent();

        userEvent.setUser(user);
        userEvent.setEvent(event);
        userEvent.setJoinAt(new Date(System.currentTimeMillis()));

        if(userEventRespository.existsById(new UserEventId(chatID, eventId)))
        {
            messageSender.sendMessage(chatID, "You already joined this event !!");
            return;
        }

        userEventRespository.save(userEvent);

        // increasing member joined
        Long currMembers = event.getTotalMember();
        event.setTotalMember(currMembers+1);
        eventRepository.save(event);

        String response = chatgptService.sendMessage("user with name " + user.getFirstName().toUpperCase() + " successfully join into event with name " + event.getName() + ",write an announcement, shortly, not formal, teenager, with icons");
        messageSender.sendMessage(chatID, response);

    }
    @Transactional
    private void addNewEvent(String creatorName, String eventName) {
        Event event = new Event(eventName, new Date(System.currentTimeMillis()));
        eventRepository.save(event);
        String response = chatgptService.sendMessage("/ask a user named " + creatorName + " created an event about " + eventName + ", i need a announcement about it. To join, other users need to write /join. write it for me (informal, teenage, with several icons)");
        messageSender.sendALL(response);
    }
    @Transactional
    private void addNewTask(long chatId, String taskBody) {
        Optional<User> opt = userRepository.findById(chatId);
        if(!opt.isPresent()){
            messageSender.sendMessage(chatId,"You're not registered in system, please use /register");
            return;
        }
        User user = opt.get();
        Task task = new Task(taskBody, user);
        taskRepository.save(task);
    }
    private boolean isRegistered(long chatID){
        if(userRepository.existsById(chatID)){
            return true;
        }else{
            messageSender.sendMessage(chatID,"You're not registered in system, please use /register");
            return false;
        }
    }

    private void registerUser(Message msg) {
        if(userRepository.findById(msg.getChatId()).isEmpty())
        {
            var chat = msg.getChat();
            User user = new User();
            user.setChatId(chat.getId());
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUsername(chat.getUserName());
            user.setRegisterAt(new Timestamp(System.currentTimeMillis()));
            userRepository.save(user);
            log.info("User saved: " + user);
        }
    }

    private void startCommandReceived(long chatId, String userFullname){
        // send a greeting message to user
        String response = chatgptService.sendMessage("write a greeting to user "+ userFullname +" (informal, teenage, with several icons)");
        messageSender.sendMessage(chatId, response);
    }
    private void sendHelpMenu(long chatID){
        // send help menu when user used incorrect commands, and also announce user about his wrong command

        // create an virtual keyboard
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows =  new ArrayList<>();

        // first row contain button with command /about, and /start
        KeyboardRow row = new KeyboardRow();
        row.add("/about");
        row.add("/start");
        keyboardRows.add(row);

        // second row contain button with command /deletedata, and /mydata
        row = new KeyboardRow();
        row.add("/deletedata");
        row.add("/mydata");
        keyboardRows.add(row);

        replyKeyboardMarkup.setKeyboard(keyboardRows);
        messageSender.sendMessageWithMarkup(chatID, StringConstant.NO_EXIST_COMMAND, replyKeyboardMarkup);
    }
}
