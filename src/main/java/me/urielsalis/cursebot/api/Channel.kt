package me.urielsalis.cursebot.api

public class Channel(val channelName: String, val channelUUID: String) {
    var messages: HashMap<Long, Message> = HashMap()

    override fun equals(other: Any?): Boolean {
        return other is Channel && ((other as Channel).channelUUID==this.channelUUID)
    }

    override fun hashCode(): Int {
        return channelUUID.hashCode()
    }
}