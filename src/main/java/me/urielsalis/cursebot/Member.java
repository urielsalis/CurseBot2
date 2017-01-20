package me.urielsalis.cursebot;

/**
 * CurseApi
 * @Author: Urielsalis
 * License: GPL 3.0
 */
public class Member {
    String senderName;
    long senderID;

    public Member(Object senderName, Object senderID) {
        this.senderName = (String) senderName;
        this.senderID = (Long) senderID;
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
