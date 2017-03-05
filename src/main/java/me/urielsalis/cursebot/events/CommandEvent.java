package me.urielsalis.cursebot.events;

import me.urielsalis.cursebot.api.Command;
import me.urielsalis.cursebot.api.Message;
import me.urielsalis.cursebot.extensions.ExtensionApi;

public class CommandEvent extends ExtensionApi.Event {
    private Command command;

    public CommandEvent(Message message) {
        super();
        this.command = new Command(message);
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

}
