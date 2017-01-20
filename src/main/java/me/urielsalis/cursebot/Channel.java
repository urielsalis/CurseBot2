package me.urielsalis.cursebot;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;


/**
 * CurseApi
 * @Author: Urielsalis
 * License: GPL 3.0
 */
public class Channel {
    String groupTitle;
    String groupID;
    public Queue<Message> messages = new CircularFifoQueue<Message>(100);
    public Channel(Object groupTitle, Object groupID) {
        this.groupID = (String) groupID;
        this.groupTitle = (String) groupTitle;
    }
}
