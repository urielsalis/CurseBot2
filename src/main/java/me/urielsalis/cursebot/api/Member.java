package me.urielsalis.cursebot.api;

import java.io.Serializable;

public class Member implements Serializable {

    public String senderName;
    public String displayName;
    public String username;
    public long senderID;
    public long bestRole;

    public Member(Object nickname, Object username, Object userID, Object bestRole, Object displayName) {
        this.senderName = (String) displayName;
        if(senderName==null || ((String) displayName).isEmpty()) {
            this.senderName = (String) nickname;
            if(senderName==null || ((String) nickname).isEmpty()) {
                this.senderName = (String) username;
            }
        }
        this.username = (String) username;
        this.displayName = (String) displayName;
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
