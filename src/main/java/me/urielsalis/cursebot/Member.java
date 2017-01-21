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

        if (senderID != member.senderID) return false;
        return senderName != null ? senderName.equals(member.senderName) : member.senderName == null;
    }

    @Override
    public int hashCode() {
        int result = senderName != null ? senderName.hashCode() : 0;
        result = 31 * result + (int) (senderID ^ (senderID >>> 32));
        return result;
    }
}
