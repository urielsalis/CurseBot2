package me.urielsalis.cursebot.api

public class User(val username: String, val id: Long, val nickname: String, val bestRole: Long, val displayName: String) {
    override fun equals(other: Any?): Boolean {
        return other is User && ((other as User).username==this.username)
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}