package me.urielsalis.cursebotold.api;

import java.util.Arrays;


public class Command {
    private String command;
    private String[] args;
    private Message message;

    public Command(Message message) {
        this.message = message;
        String[] s = message.body.split("\\s+");
        command = s[0].substring(1);
        if(s.length > 1) {
            args = Arrays.copyOfRange(s, 1, s.length);
        }
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
