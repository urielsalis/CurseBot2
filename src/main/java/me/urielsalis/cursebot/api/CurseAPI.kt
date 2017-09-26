package me.urielsalis.cursebot.api

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import me.urielsalis.cursebot.api.util.GroupInfo
import me.urielsalis.cursebot.api.util.TypeID
import org.java_websocket.client.WebSocketClient
import java.net.URI
import kotlin.system.exitProcess

public abstract class CurseAPI(val username: String, val password: String, val server: String, val clientID: String, val machineKey: String, serverUri: URI?): WebSocketClient(serverUri) {
    val messages: HashMap<Channel, ArrayList<Message>> = HashMap()

    private val parser: Parser = Parser()

    abstract fun onConnect(connectInfo: ConnectInfo)
    abstract fun onMessage(message: Message)
    abstract fun onDelete(message: Message, deleted: User, issuer: User)
    abstract fun onJoin(user: User)
    abstract fun onNickChange(userOld: User, user: User)
    abstract fun onLeave(user: User, reason: String)

    override fun onMessage(message: String) {
        curseWSMessage(message)
    }

    private var authToken: String
    private var userID: String
    private var sessionID: String
    private var groupInfo: GroupInfo

    init {
        var result: Pair<String, String> = login(username, password)
        authToken = result.first
        userID = result.second

        sessionID = getSessionID()
        groupInfo = getGroupInfo()
        connectBlocking()
    }

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

    private fun login(username: String, password: String): Pair<String, String> {
        val parameters: List<Pair<String, String>> = listOf(Pair("Username", username), Pair("Password", password))
        val (_, response, _) = "https://logins-v1.curseapp.net/login"
                .httpPost(parameters)
                .header(Pair("AuthenticationToken", authToken))
                .responseString()

        val json: JsonObject = parser.parse(response.responseMessage) as JsonObject

        if(json.getOrDefault("Status", 0) == 1) {
            val session: JsonObject = json.get("Session") as JsonObject
            return Pair(session["Token"].toString(), session["UserID"].toString())
        } else {
            println("Wrong Password!")
            exitProcess(-1)
        }
    }

    private fun getSessionID(): String {
        val parameters: List<Pair<String, String>> = listOf(Pair("MachineKey", machineKey), Pair("Platform", "6"), Pair("DeviceID", "null"), Pair("PushKitToken", "null"))
        val (_, response, _) = "https://sessions-v1.curseapp.net/sessions"
                .httpPost(parameters)
                .header(Pair("AuthenticationToken", authToken))
                .responseString()

        val json: JsonObject = parser.parse(response.responseMessage) as JsonObject
        return json["SessionID"].toString()
    }

    private fun curseWSMessage(messageWS: String) {
        val json: JsonObject = parser.parse(messageWS) as JsonObject
        val typeID: TypeID = TypeID.values()[json["TypeID"] as Int]
        when(typeID) {
            TypeID.MESSAGE -> {
                val body = json["Body"] as JsonObject
                val isPM = body["ConversationType"] as Int == 3
                val conversationID = body["ConversationID"] as String//channelUUID
                val senderName = body["SenderName"] as String
                val senderID = body["SenderID"] as Long
                val senderVanityRole = body["SenderVanityRole"] //Update member role with this?
                val isDeleted = body["IsDeleted"] as Boolean
                val serverID = body["ServerID"] as String
                val timestamp = body["Timestamp"] as Long

                val channel = groupInfo.channels[conversationID]
                if(channel != null) {
                    val sender = User(senderName, senderID, senderName, 9999, senderName)
                    val message = Message(body["Body"] as String, serverID, timestamp, channel, sender, isDeleted)

                    if (isDeleted) {
                        val deletedUser = User(body["DeletedUsername"] as String, body["DeletedUserID"] as Long, senderName, 9999, senderName)
                        channel.messages.remove(body["DeletedTimestamp"] as Long)
                        onDelete(channel.messages[body["DeletedTimestamp"] as Long]!!, deletedUser, sender)
                    } else {
                        channel.messages.put(timestamp, message)
                        onMessage(message)
                    }
                } else if(isPM){
                    val sender = User(senderName, senderID, senderName, 9999, senderName)
                    val message = Message(body["Body"] as String, serverID, timestamp, Channel(senderName, conversationID), sender, isDeleted)
                    onMessage(message)
                }
            }
            TypeID.USERCHANGE -> {
                val body = json["Body"] as JsonObject
                val changeType = body["ChangeType"] as Int
                val senderName = json["SenderName"] as String
                val members = body["Members"] as JsonArray<JsonObject>
                val users = ArrayList<User>()

                for(member in members) {
                    val nickname = member["Nickname"] as String
                    val username = member["Username"] as String
                    val userID = member["UserID"] as Long
                    val bestRole = member["BestRole"] as Long
                    val displayName = member["DisplayName"] as String

                    val user = User(username, userID, nickname, bestRole, displayName)
                }
                if(changeType==2) { //User join
                    users.map { user -> onJoin(user) }
                } else if(changeType==3) { //User left
                    users.map { user -> onLeave(user, (if (senderName === username) "Left" else "Kicked")) }
                }

            }
            TypeID.SIGNAL -> {
                //Received signal, might want to check this happens regularly}
            }
            TypeID.RESPONSELOGIN1 -> run {
                println("Non 1 status on websocket")
            }
            TypeID.RESPONSELOGIN2 -> {
                //more info about user, ignoring for now
            }
        }
    }

    private fun getGroupInfo(): GroupInfo {
        /*
        String json = Util.sendGet("https://groups-v1.curseapp.net/groups/" + groupID + "?showDeletedChannels=false", getAuthToken());
            JSONObject object = (JSONObject) new JSONParser().parse(json);
            JSONArray arrayRoles = (JSONArray) object.get("Roles");
            for(Object obj2: arrayRoles) {
                JSONObject role = (JSONObject) obj2;
                roles.put((long)role.get("RoleID"), (long)role.get("Rank"));
            }
            JSONObject membership = (JSONObject) object.get("Membership");
            bestRank = roles.get(membership.get("BestRole"));
            JSONArray array = (JSONArray) object.get("Channels");
            for (Object obj : array) {
                JSONObject channel = (JSONObject) obj;
                String title = (String) channel.get("GroupTitle");
                String id = (String) channel.get("GroupID");
                channels.put(title, new Channel(title, id));
            }
            getMembers();
         */
        val roles = HashMap<Long, Long>()
        val channels = HashMap<String, Channel>()

        val parameters: List<Pair<String, String>> = listOf(Pair("showDeletedChannels", "false"))
        val (_, response, _) = "https://groups-v1.curseapp.net/groups/$server"
                .httpGet(parameters)
                .header(Pair("AuthenticationToken", authToken))
                .responseString()
        val json: JsonObject = parser.parse(response.responseMessage) as JsonObject

        val rolesArray = json["Roles"] as JsonArray<JsonObject>
        rolesArray.map {role: JsonObject -> roles.put(role["RoleID"] as Long, role["Rank"] as Long)}
        val channelsArray = json["Channels"] as JsonArray<JsonObject>
        channelsArray.map {channel: JsonObject -> channels.put(channel["GroupID"] as String, Channel(channel["GroupTitle"] as String, channel["GroupID"] as String))}

        return GroupInfo(roles, channels)
    }

}

