package me.urielsalis.cursebot.api;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import me.urielsalis.cursebot.api.Util;
import me.urielsalis.cursebot.events.CommandEvent;
import me.urielsalis.cursebot.events.MessageEvent;
import me.urielsalis.cursebot.extensions.Extension;
import me.urielsalis.cursebot.extensions.ExtensionApi;
import me.urielsalis.cursebot.extensions.ExtensionHandler;
import me.urielsalis.cursebot.extensions.Profanity.Main;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;


/**
 * CurseApi
 * @Author: Urielsalis
 * License: GPL 3.0
 */
public class CurseApi {
    private String authToken = "";
    private String groupID = "";
    private String clientID = "";
    private String machineKey = "";

    public HashMap<Long, Long> roles = new HashMap<>();
    public long bestRank;
    private HashMap<String, Channel> channels = new HashMap<String, Channel>();
    private ArrayList<Member> members = new ArrayList<Member>();

    static boolean isDeleteInProgress = false;

    private ArrayList<Listener> listeners = new ArrayList<Listener>();

    public WebSocket websocket;

    private long userID;
    private String sessionID;

    //:: Stats
    public long userJoins = 0;
    public long userUniqueJoins = 0;
    public long messages = 0;
    public long removedUsers = 0;
    public long leftUsers = 0;
    private ArrayList<Long> removedUsersList = new ArrayList<>();

    public CurseApi(String groupID, String username, String password, String clientID, String machineKey) {
        this.groupID = groupID;
        this.clientID = clientID;
        this.machineKey = machineKey;
        login(username, password);
        getSessionID();
        openWebSocket();
        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                websocket.disconnect();
            }
        });
        getInfoFromGroup();
    }
    
    private void getSessionID() {
        try {
            String parameters = "MachineKey="+machineKey+"&Platform=6&DeviceID=null&PushKitToken=null";
            String json = Util.sendPost("https://sessions-v1.curseapp.net/sessions", parameters, getAuthToken());
            JSONObject object = (JSONObject) new JSONParser().parse(json);
            sessionID = (String) object.get("SessionID");
            JSONObject user = (JSONObject) object.get("User");
            userID = (long) user.get("UserID");
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    private void openWebSocket() {
        final long SIGNAL_ID = -476754606;
        final long MESSAGE_ID = -635182161;
        final long RESPONSELOGIN1_ID = -815187584;
        final long RESPONSELOGIN2_ID = 937250613;
        final long USERCHANGE_ID = 149631008;
        try {
            websocket = new WebSocketFactory()
                    .createSocket("wss://notifications-na-v1.curseapp.net/")
                    .addListener(new WebSocketAdapter() {
                        @Override
                        public void onTextMessage(WebSocket ws, String s) {
                            try {
                                JSONObject obj = (JSONObject) new JSONParser().parse(s);
                                long TypeID = (long) obj.get("TypeID");
                                if (TypeID==SIGNAL_ID) {
                                    //We received a signal, good. Might want to check this for a while
                                } else if(TypeID==MESSAGE_ID) {
                                    //check if its private message
                                    JSONObject body = (JSONObject) obj.get("Body");
                                    clientID = (String) body.get("ClientID");
                                    long conversationType = (long) body.get("ConversationType");
                                    boolean isPM = conversationType==3;
                                    String channelUUID = (String) body.get("ConversationID");
                                    addMemberIfNotFound((String) body.get("SenderName"), (long) body.get("SenderID"));
                                    updateMember((long) body.get("SenderID"), (long) body.get("SenderVanityRole"), (String) body.get("SenderName"));
                                    Message message = new Message(body.get("SenderName"), body.get("Body"), body.get("Timestamp"), body.get("ServerID"), channelUUID, isPM);
                                    messages++;
                                    if(isPM) {
                                        updateListeners(message);
                                    } else {
                                        Channel channel = resolveChannelUUID(channelUUID);
                                        if (!channel.messages.contains(message)) {
                                            channel.messages.add(message);
                                            updateListeners(message);
                                        }
                                    }
                                } else if(TypeID==RESPONSELOGIN1_ID) {
                                    if(((int)obj.get("Status"))!=1) {
                                        System.err.println("Non 1 status on websocket");
                                    }
                                } else if(TypeID==RESPONSELOGIN2_ID) {
                                    //More data about the user, we ignore it for now
                                } else if(TypeID==USERCHANGE_ID) {
                                    JSONObject body = (JSONObject) obj.get("Body");
                                    long changeType = (long)body.get("ChangeType");
                                    if(changeType==2) {
                                        //user joined
                                        JSONArray members2 = (JSONArray) body.get("Members");
                                        for(Object object: members2) {
                                            JSONObject member = (JSONObject) object;
                                            Object nickname = member.get("Nickname");
                                            Object username = member.get("Username");
                                            Object userIDMember = member.get("UserID");
                                            Object bestRole = member.get("BestRole");
                                            Object displayName = member.get("DisplayName");
                                            Member m = new Member(nickname, username, userIDMember, bestRole, displayName);
                                            addMemberIfNotFound(m.senderName, m.senderID);
                                            if(!members.contains(m)) {
                                                //new user, TODO
                                                userUniqueJoins++;
                                                userJoins++;
                                                if (Util.unhidden)
                                                    postMessage(resolveChannel(Util.defaultChannel), "Welcome " + mention(m.senderName) + ", don't forget to read the rules in the *#rules* channel!. Enjoy your stay! :)");
                                                members.add(m);
                                            } else {
                                                if(removedUsersList.contains(m.senderID)) {
                                                    userJoins++;
                                                    postMessage(resolveChannel(Util.botlogChannel), "~*[Rejoin]*~\n*Details:* " + mention(m.senderName) + " re-joined the server after being kicked!");
                                                    removedUsersList.remove(m.senderID);
                                                }
                                                else {
                                                    userJoins++;
                                                    //postMessage(resolveChannel(Util.botlogChannel), "~*[Join]*~\n*Details:* " + mention(m.senderName) + " joined the server!");
                                                }
                                            }
                                            Util.logger.log(Level.INFO, m.senderName + " joined!");
                                        }
                                    } else if(changeType==3) {
                                        String sender = (String) body.get("SenderName");
                                        JSONArray members2 = (JSONArray) body.get("Members");
                                        JSONObject object = (JSONObject) members2.get(0);

                                        long removedid = (long) object.get("UserID");
                                        String removedname = (String) object.get("Username");
                                        if(removedname == null) {
                                            removedname = (String) object.get("Nickname");
                                        }
                                        if(removedname.equals(sender)) {
                                            leftUsers++;
                                            //postMessage(resolveChannel(Util.botlogChannel), "~*[User left!]*~\n*User:* " + removedname + ".");
                                        }
                                        else {
                                            removedUsers++;
                                            removedUsersList.add(removedid);
                                            if (!resolveMember(sender).senderName.equals(Util.botName)) {
                                                postMessage(resolveChannel(Util.botlogChannel), "~*[User Kicked!]*~\n*Command Sender:* [ " + sender + " ]\n*Kicked user:* " + removedname + ".");
                                            }
                                        }
                                    }
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    })
                    .connect();
            Util.logger.log(Level.INFO, "Websocket: " + websocket.isOpen());
            Util.logger.log(Level.INFO, "{\"TypeID\":-2101997347,\"Body\":{\"CipherAlgorithm\":0,\"CipherStrength\":0,\"ClientVersion\":\"7.0.138\",\"PublicKey\":null,\"MachineKey\":\""+machineKey+"\",\"UserID\":"+userID+",\"SessionID\":\""+sessionID+"\",\"Status\":1}}");
            websocket.sendText("{\"TypeID\":-2101997347,\"Body\":{\"CipherAlgorithm\":0,\"CipherStrength\":0,\"ClientVersion\":\"7.0.138\",\"PublicKey\":null,\"MachineKey\":\""+machineKey+"\",\"UserID\":"+userID+",\"SessionID\":\""+sessionID+"\",\"Status\":1}}");
        } catch ( WebSocketException | IOException e) {
            e.printStackTrace();
        }
    }

    private void updateMember(long senderID, long senderVanityRole, String senderName) {
        for(Member member: members) {
            if(member.senderID==senderID) {
                member.bestRole = senderVanityRole;
                member.senderName = senderName;
            }
        }
    }

    private void login(String username, String password) {
       try {
           String parameters = "Username=" + username + "&Password=" + password;
           String json = Util.sendPost("https://logins-v1.curseapp.net/login", parameters, getAuthToken());
           JSONObject object = (JSONObject) new JSONParser().parse(json);
           if(((long)object.get("Status"))==1) {
                JSONObject session = (JSONObject) object.get("Session");
                authToken = (String) session.get("Token");
               Util.logger.log(Level.INFO, authToken);
                userID = (long) session.get("UserID");
           } else {
               System.err.println("Wrong Password");
               System.exit(1);
           }
       } catch (ParseException e) {
           e.printStackTrace();
       }
    }


    /**
     * Updates the message table every second
     */
    public void startMessageLoop() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    while(true) {
                        websocket.sendText("{\"TypeID\":-476754606,\"Body\":{\"Signal\":true}}");
                        TimeUnit.SECONDS.sleep(1); //Sleep 1 second
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void getInfoFromGroup() {
        try {
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
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void getMembers() {
        try {
            String json = Util.sendGet("https://groups-v1.curseapp.net/groups/" + groupID + "/members?actives=true&page=1&pageSize=50", getAuthToken());
            JSONArray array = (JSONArray) new JSONParser().parse(json);
            for(Object obj: array) {
                JSONObject memberObj = (JSONObject) obj;
                Member member = new Member(memberObj.get("Nickname"), memberObj.get("Username"), memberObj.get("UserID"), memberObj.get("BestRole"), memberObj.get("DisplayName"));
                members.add(member);
            }
            json = Util.sendGet("https://groups-v1.curseapp.net/groups/" + groupID + "/members?actives=false&page=1&pageSize=50", getAuthToken());
            array = (JSONArray) new JSONParser().parse(json);
            for(Object obj: array) {
                JSONObject memberObj = (JSONObject) obj;
                Member member = new Member(memberObj.get("Nickname"), memberObj.get("Username"), memberObj.get("UserID"), memberObj.get("BestRole"), memberObj.get("DisplayName"));
                members.add(member);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void updateMessages()  {
        try {
            while (isDeleteInProgress) TimeUnit.SECONDS.sleep(1); //wait for deletion to finish so we dont delete new posts
            for (Channel channel : channels.values()) {
                getMessages(channel);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void getMessages(Channel channel) {
        try {
            String json;
            if (channel.messages.isEmpty()) {
                json = Util.sendGet("https://conversations-v1.curseapp.net/conversations/" + channel.groupID + "?endTimestamp=0&pageSize=30&startTimestamp=0", getAuthToken()); //Assume we start in 0
            } else {
                json = Util.sendGet("https://conversations-v1.curseapp.net/conversations/" + channel.groupID + "?endTimestamp=0&pageSize=30&startTimestamp=" + channel.messages.peek().timestamp, getAuthToken()); //Get since last message to save some work to curse servers
            }
            JSONArray array = (JSONArray) new JSONParser().parse(json);
            for (Object obj : array) {
                JSONObject messageObject = (JSONObject) obj;
                Message message = new Message(messageObject.get("SenderName"), messageObject.get("Body"), messageObject.get("Timestamp"), messageObject.get("ServerID"), channel.groupID);
                if (!channel.messages.contains(message)) {
                    channel.messages.add(message);
                    updateListeners(message);
                    addMemberIfNotFound((String) messageObject.get("SenderName"), (long) messageObject.get("SenderID"));
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void addMemberIfNotFound(String senderName, long senderID) {
        try {
            boolean found = false;
            Member todelete = null;
            for (Member member : members) {
                if (member.senderID == senderID) {
                    found = true;
                    if (!member.senderName.equals(senderName)) {
                        found = false;
                        todelete = member;
                        //we will update member data
                    }
                }
            }
            if (todelete != null)
                members.remove(todelete);
            if (!found) {
                String json = Util.sendGet("https://groups-v1.curseapp.net/groups/" + groupID + "/members/" + senderID, getAuthToken());
                JSONObject object = (JSONObject) new JSONParser().parse(json);
                Member member = new Member(object.get("Nickname"), object.get("Username"), object.get("UserID"), object.get("BestRole"), object.get("DisplayName"));
                members.add(member);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void updateListeners(Message message) {
        ExtensionHandler.api.fire("message", new MessageEvent(message));
        String[] commandExodus = message.body.split("\n");

        if(message.body.startsWith(".")) {
            for(String cmd : commandExodus) {
                Message cmdEx = message;
                cmdEx.body = cmd;
                ExtensionHandler.api.fire("command", new CommandEvent(cmdEx));
            }
        }
        synchronized (listeners) {
            for(Listener listener: listeners)
                listener.run(message);
        }
    }




    /* Main methods start*/

    /**
     * Posts message on supplied channel
     * @param channel channel where message is going to be posted
     * @param message message to post
     * @return action suceeded
     */
    public boolean postMessage(Channel channel, String message) {
        if(channel==null) return false;
        //String div = (channel.groupTitle.equalsIgnoreCase(Util.botlogChannel) || channel.groupTitle.equalsIgnoreCase(Util.botstatChannel)) ? "\n-*I================================================I*-\n" : "";
        String div = "";
        String parameters = "AttachmentID=00000000-0000-0000-0000-000000000000&Body=" + (div + message) + "&AttachmentRegionID=0&MachineKey=" + machineKey + "&ClientID=" + clientID;
        Util.sendPost("https://conversations-v1.curseapp.net/conversations/"+channel.groupID, parameters, getAuthToken());
        return true;
    }

    /**
     * Removes a user from the server
     * @param member member to be removed
     * @return action succedeed
     */
    public boolean kickUser(Member member) {
        if(member==null) return false;
        long userID = member.senderID;
        String url = "https://groups-v1.curseapp.net/groups/"+groupID+"/members/"+userID;
        Util.sendDelete(url, getAuthToken());
        return true;
    }

    /**
     * Removes a message, then waits 1 second
     * @param message Message to be deleted
     * @return action succedeed
     */
    public boolean deleteMessage(Message message) {
        if(message==null) return false;
        Channel channel = resolveChannelUUID(message.channelUUID);
        if(channel==null) return false;
        isDeleteInProgress = true;
        String url = "https://conversations-v1.curseapp.net/conversations/"+channel.groupID+"/"+message.serverID+"-"+message.timestamp;
        Util.sendDelete(url, getAuthToken());
        isDeleteInProgress = false;
        return true;
    }

    /**
     * @param message message to edit
     * @param body New body
     * @return action suceeded
     */
    public boolean editMessage(Message message, String body) {
        if(message==null) return false;
        Channel channel = resolveChannelUUID(message.channelUUID);
        if(channel==null) return false;
        String url = "https://conversations-v1.curseapp.net/conversations/"+channel.groupID+"/"+message.serverID+"-"+message.timestamp;
        String parameters = "Body="+body;
        Util.sendPost(url, parameters, getAuthToken());
        return true;
    }

    /**
     * @param message Message to like
     * @return Action suceeded
     */
    public boolean likeMessage(Message message) {
        if(message==null) return false;
        Channel channel = resolveChannelUUID(message.channelUUID);
        if(channel==null) return false;
        String url = "https://conversations-v1.curseapp.net/conversations/"+channel.groupID+"/"+message.serverID+"-"+message.timestamp+"/like";
        String parameters = "";
        Util.sendPost(url, parameters, getAuthToken());
        return true;
    }

    /**
     * Adds a new listener for new messages
     * @param listener Listener to be added
     */
    public void addNewListener(Listener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Bans a member
     * @param userID userid of member to ban
     * @param reason reason
     */
    public void banMember(long userID, String reason) {
        String url = "https://groups-na-v1.curseapp.net/servers/"+groupID+"/bans";
        String parameters = "UserID="+userID+"&Reason="+reason+"&MessageDeleteMode=0";
        Util.sendPost(url, parameters, getAuthToken());
    }

    /**
     * Unbans a member
     * @param userID userid of member to unban
     */
    public void unBanMember(long userID, String bannedUsername) {
        Util.sendDelete("https://groups-na-v1.curseapp.net/servers/"+groupID+"/bans/"+userID, getAuthToken());
        postMessage(resolveChannel(Util.botlogChannel), "~*[Unbanning: Timer is up]*~\nThe user *'" + userID + ":" + bannedUsername + "'* has been unbanned!");
    }
    
    /**
     * Returns a string to mention a member in a chat message (pinging a member)
     * @return
     */
    public String mention(String memberName)
    {
    	Member member = resolveMember(memberName);
    	return "@" + member.senderID + ":" + member.username;
    }

    /**
     * Returns a string to mention a member in a chat message (pinging a member)
     * with a custom mention message
     * @return
     */
    public String mention(String memberName, String message)
    {
    	Member member = resolveMember(memberName);
    	return "@" + member.senderID + ":" + message;
    }


    //getters and setters
    public String getGroupID() {
        return groupID;
    }

    public void setGroupID(String groupID) {
        this.groupID = groupID;
    }

    public String getAuthToken() {
        return authToken;

    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public Channel resolveChannel(String channelName) {
        for(Map.Entry<String, Channel> entry: channels.entrySet()) {
            if(Util.equals(entry.getKey(), channelName))
                return entry.getValue();
        }
        return null;
    }

    public Member resolveMember(String username) {
        for(Member member: members) {
            if(Util.equals(member.senderName, username))
                return member;
        }
        return null;
    }
    
    
    //- temp
    public Member resolveRole(String roleName) {
    	return null;
    }

    public Channel resolveChannelUUID(String uuid) {
        for(Map.Entry<String, Channel> entry: channels.entrySet()) {
            if(Util.equals(entry.getValue().groupID, uuid))
                return entry.getValue();
        }
        return null;
    }

    public void setMembers(ArrayList<Member> members) {
        this.members = members;
    }

    public ArrayList<Member> getMembersList() {
        return members;
    }
}
