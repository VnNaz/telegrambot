package com.vnnaz.telegrambot.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Chat;

import javax.swing.plaf.synth.SynthUI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@AllArgsConstructor
public class MessageFilterService {

    private ChatGPTService chatGPTService;
    public String handler(String request){

        String regex = "^\\\\([\\w]+) ([\\w]+)$";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(request);

        String prefix = "";
        String context = "";
        if(matcher.find())
        {
            prefix = matcher.group(1);
            context = matcher.group(2);
        }else {
            System.out.println( getClass() + "request not match !!");
            return "";
        }

        switch (prefix){
            case "rw":
                return chatGPTService.getRewriteTextResponse(context);
            default:
                return "";
        }
    }
}
