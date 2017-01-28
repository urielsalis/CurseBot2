package me.urielsalis.cursebot.api;

import java.util.Arrays;

/**
 * Created by urielsalis on 1/28/2017
 */
public class Command {
    public String command;
    public String[] args;
    public Message message;

    public Command(Message message) {
        this.message = message;
        String[] s = message.body.split("\\s+");
        command = s[0].substring(1);
        if(s.length > 1) {
            args = Arrays.copyOfRange(s, 1, s.length);
        }
    }
}
