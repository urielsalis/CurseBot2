package me.urielsalis.cursebotold.events;

import me.urielsalis.cursebotold.api.Command;
import me.urielsalis.cursebotold.api.Message;
import me.urielsalis.cursebotold.extensions.ExtensionApi;

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
