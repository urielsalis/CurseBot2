package me.urielsalis.cursebot.events;

import me.urielsalis.cursebot.api.Command;
import me.urielsalis.cursebot.api.Message;
import me.urielsalis.cursebot.extensions.ExtensionApi;

/**
 * Created by urielsalis on 1/28/2017
 */
public class CommandEvent extends ExtensionApi.Event {
    public Command command;

    public CommandEvent(Message message) {
        super();
        this.command = new Command(message);
    }
}
