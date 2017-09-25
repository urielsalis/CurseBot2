package me.urielsalis.cursebot.api

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.fuel.httpPost
import kotlin.system.exitProcess

public abstract class CurseAPI(val username: String, val password: String, val server: String, val clientID: String, val machineKey: String) {
    val messages: HashMap<Channel, ArrayList<Message>> = HashMap()

    private val parser: Parser = Parser()
    private var authToken: String = ""
    private var userID: String = ""
    private var sessionID: String = ""

    abstract fun onConnect(connectInfo: ConnectInfo)
    abstract fun onMessage(message: Message)
    abstract fun onDelete(message: Message, issuer: User)
    abstract fun onJoin(user: User)
    abstract fun onLeave(user: User, reason: Reason)

    public fun postMessage(channel: Channel, message: Message) {

    }

    public fun kickUser(user: User, reason: String) {

    }

    public fun banUser(user: User, reason: String, timeout: Long) {

    }

    public fun deleteMessage(channel: Channel, message: Message) {

    }

    public fun editMessage(channel: Channel, message: Message, newBody: String) {

    }

    public fun connect() {
        login(username, password)
        getSessionID()
        openWebSocket()
        startMessageLoop()
    }

    private fun login(username: String, password: String) {
        val parameters: List<Pair<String, String>> = listOf(Pair("Username", username), Pair("Password", password))
        val (_, response, _) = "https://logins-v1.curseapp.net/login"
                .httpPost(parameters)
                .header(Pair("AuthenticationToken", authToken))
                .responseString()

        val json: JsonObject = parser.parse(response.responseMessage) as JsonObject

        if(json.getOrDefault("Status", 0) == 1) {
            val session: JsonObject = json.get("Session") as JsonObject
            authToken = session.get("Token").toString()
            userID = session.get("UserID").toString()
        } else {
            println("Wrong Password!")
            exitProcess(-1)
        }
    }

    private fun getSessionID() {
        val parameters: List<Pair<String, String>> = listOf(Pair("MachineKey", machineKey), Pair("Platform", "6"), Pair("DeviceID", "null"), Pair("PushKitToken", "null"))
        val (_, response, _) = "https://sessions-v1.curseapp.net/sessions"
                .httpPost(parameters)
                .header(Pair("AuthenticationToken", authToken))
                .responseString()

        val json: JsonObject = parser.parse(response.responseMessage) as JsonObject
        sessionID = json.get("SessionID").toString()
    }
}

