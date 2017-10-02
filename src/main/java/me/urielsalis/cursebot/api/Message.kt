package me.urielsalis.cursebot.api

public class Message(val body: String, val serverId: String, val timestamp: Long, val channel: Channel, val sender: User, val isDeleted: Boolean = false)