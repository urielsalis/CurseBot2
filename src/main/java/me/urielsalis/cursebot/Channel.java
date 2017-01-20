package me.urielsalis.cursebot;

/**
 * BattleCode 2017
 * Team: Mate
 * License: GPL 3.0
 */
public class Channel {
    String groupTitle;
    String groupID;
    public Channel(Object groupTitle, Object groupID) {
        this.groupID = (String) groupID;
        this.groupTitle = (String) groupTitle;
    }
}
