package me.urielsalis.cursebot

import com.natpryce.konfig.*
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import me.urielsalis.cursebot.api.*

fun main(args: Array<String>) {

    val config = systemProperties() overriding
            EnvironmentVariables() overriding
            ConfigurationProperties.fromResource("cursebot.properties")


    val api = object : CurseAPI(config[auth.username], config[auth.password], config[server.host], config[auth.clientID], config[auth.machineKey]) {
        override fun onConnect(connectInfo: ConnectInfo) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onDelete(message: Message, issuer: User) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onJoin(user: User) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onLeave(user: User, reason: Reason) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onMessage(message: Message) {
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