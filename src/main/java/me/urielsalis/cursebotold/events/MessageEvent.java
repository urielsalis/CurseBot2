package me.urielsalis.cursebotold.events;

import me.urielsalis.cursebotold.api.Message;
import me.urielsalis.cursebotold.extensions.ExtensionApi;

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
