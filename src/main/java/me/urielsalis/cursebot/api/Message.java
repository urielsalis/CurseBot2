package me.urielsalis.cursebot.api;

import java.util.Calendar;

/**
 * CurseApi
 * @Author: Urielsalis
 * License: GPL 3.0
 */
public class Message {
    public String senderName;
    public String body;
    public long timestamp;
    public String serverID;
    public String channelUUID;
    public boolean isPM;
    public Message(Object senderName, Object body, Object timestamp, Object serverID, Object channelUUID) {
        this(senderName, body, timestamp, serverID, channelUUID, false);
    }

    public Message(Object senderName, Object body, Object timestamp, Object serverID, Object conversationID, boolean isPM) {
        this.senderName = (String) senderName;
        this.body = (String) body;
        this.timestamp = (Long) timestamp;
        this.serverID = (String) serverID;
        this.channelUUID = (String) conversationID;
        this.isPM = isPM;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        if (timestamp != message.timestamp) return false;
        if (senderName != null ? !senderName.equals(message.senderName) : message.senderName != null) return false;
        if (body != null ? !body.equals(message.body) : message.body != null) return false;
        return serverID != null ? serverID.equals(message.serverID) : message.serverID == null;
    }

    @Override
    public int hashCode() {
        int result = senderName != null ? senderName.hashCode() : 0;
        result = 31 * result + (body != null ? body.hashCode() : 0);
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + (serverID != null ? serverID.hashCode() : 0);
        return result;
    }

    public void print() {
        System.out.println(getString());

    }

    public String getString() {
        return resolveTimestamp(timestamp)+"  <"+senderName+">"+body;
    }

    private String resolveTimestamp(long timestamp) {
        Calendar mydate = Calendar.getInstance();
        mydate.setTimeInMillis(timestamp*1000);
        return mydate.get(Calendar.HOUR) + ":"+mydate.get(Calendar.MINUTE)+":"+mydate.get(Calendar.SECOND) + ":" + mydate.get(Calendar.MILLISECOND);
    }
}
