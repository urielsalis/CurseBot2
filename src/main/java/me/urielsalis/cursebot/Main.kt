package me.urielsalis.cursebot

import com.natpryce.konfig.*
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import me.urielsalis.cursebot.api.*
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI

fun main(args: Array<String>) {

    val config = systemProperties() overriding
            EnvironmentVariables() overriding
            ConfigurationProperties.fromResource("cursebot.properties")


    val api = object : CurseAPI(config[auth.username], config[auth.password], config[server.host], config[auth.clientID], config[auth.machineKey], URI.create("wss://notifications-na-v1.curseapp.net/")) {
        override fun onOpen(serverHandshake: ServerHandshake) {
            println("Connection opened!")
        }

        override fun onClose(code: Int, reason: String, remote: Boolean) {
            println("Connection closed by " + if (remote) "remote peer" else "us. $reason($code)")
            //Restart here?
        }

        override fun onError(exception: Exception) {
            exception.printStackTrace()
        }

        override fun onConnect(connectInfo: ConnectInfo) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onJoin(user: User) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onMessage(message: Message) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onDelete(message: Message, deleted: User, issuer: User) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onNickChange(userOld: User, user: User) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onLeave(user: User, reason: String) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }


    }

}
object server : PropertyGroup() {
    val host by stringType
    val logChannel by stringType
    val statsChannel by stringType
}
object auth: PropertyGroup() {
    val username by stringType
    val password by stringType
    val clientID by stringType
    val machineKey by stringType
}