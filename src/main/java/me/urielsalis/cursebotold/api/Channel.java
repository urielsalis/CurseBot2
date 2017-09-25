package me.urielsalis.cursebotold.api;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.Queue;


public class Channel {
    private String groupTitle;
    private String groupID;
    private Queue<Message> messages = new CircularFifoQueue<Message>(100);
    public Channel(Object groupTitle, Object groupID) {
        this.groupID = (String) groupID;
        this.groupTitle = (String) groupTitle;
    }

    public String getGroupTitle() {
        return groupTitle;
    }

    public void setGroupTitle(String groupTitle) {
        this.groupTitle = groupTitle;
    }

    public String getGroupID() {
        return groupID;
    }

    public void setGroupID(String groupID) {
        this.groupID = groupID;
    }

    public Queue<Message> getMessages() {
        return messages;
    }

    public void setMessages(Queue<Message> messages) {
        this.messages = messages;
    }
}
