package me.urielsalis.cursebot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * CurseApi
 * @Author: Urielsalis
 * License: GPL 3.0
 */
public class Channel {
    String groupTitle;
    String groupID;
    public List<Message> messages = Collections.synchronizedList(new ArrayList<Message>());
    public Channel(Object groupTitle, Object groupID) {
        this.groupID = (String) groupID;
        this.groupTitle = (String) groupTitle;
    }
}
