package me.urielsalis.cursebot.events;

import me.urielsalis.cursebot.api.Message;
import me.urielsalis.cursebot.extensions.ExtensionApi;

/**
 * Created by urielsalis on 1/28/2017
 */
public class MessageEvent extends ExtensionApi.Event {
    public Message message;
    String eventName = "message";
    String value = "";

    public MessageEvent(Message message) {
        super();
        this.message = message;
    }
}
