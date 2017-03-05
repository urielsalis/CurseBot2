package me.urielsalis.cursebot.events;

import me.urielsalis.cursebot.api.Message;
import me.urielsalis.cursebot.extensions.ExtensionApi;

public class MessageEvent extends ExtensionApi.Event {
    private Message message;
    String eventName = "message";
    String value = "";

    public MessageEvent(Message message) {
        super();
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
