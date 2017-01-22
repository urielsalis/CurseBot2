package me.urielsalis.cursebot;

/**
 * CurseApi
 * @Author: Urielsalis
 * License: GPL 3.0
 */
public class Member {
    String senderName;
    String username;
    long senderID;
    long bestRole;

    public Member(Object nickname, Object username, Object userID, Object bestRole) {
        this.senderName = (String) nickname;
        if(nickname==null)
            this.senderName = (String) username;
        this.username = (String) username;
        this.senderID = (long) userID;
        this.bestRole = (long) bestRole;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Member member = (Member) o;

        return senderID == member.senderID;
    }

    @Override
    public int hashCode() {
        int result = (int) (senderID ^ (senderID >>> 32));
        return result;
    }
}
