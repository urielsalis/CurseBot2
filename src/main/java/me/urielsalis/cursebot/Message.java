package me.urielsalis.cursebot;

/**
 * BattleCode 2017
 * Team: Mate
 * License: GPL 3.0
 */
public class Message {
    String senderName;
    String body;
    long timestamp;
    public Message(Object senderName, Object body, Object timestamp) {
        this.senderName = (String) senderName;
        this.body = (String) body;
        this.timestamp = (Long) timestamp;
    }

    public void print() {
        System.out.println(timestamp+"  <"+senderName+">"+body);
    }
}
