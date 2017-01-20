package me.urielsalis.cursebot;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * CurseApi
 * @Author: Urielsalis
 * License: GPL 3.0
 */
public class CurseApi {
    private String authToken;
    private String groupID;
    private String clientID;
    private String machineKey;
    private HashMap<String, Channel> channels = new HashMap<String, Channel>();
    private ArrayList<Member> members = new ArrayList<Member>();
    static boolean isDeleteInProgress = false;
    private ArrayList<Listener> listeners = new ArrayList<Listener>();

    public CurseApi(String groupID, String authToken, String clientID, String machineKey) {
        this.groupID = groupID;
        this.authToken = authToken;
        this.clientID = clientID;
        this.machineKey = machineKey;
        getInfoFromGroup();
    }


    /**
     * Updates the message table every second
     */
    public void startMessageLoop() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    while(true) {
                        updateMessages(); //Run update cycle
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
            JSONArray array = (JSONArray) object.get("Channels");
            for (Object obj : array) {
                JSONObject channel = (JSONObject) obj;
                String title = (String) channel.get("GroupTitle");
                String id = (String) channel.get("GroupID");
                channels.put(title, new Channel(title, id));
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
                    Member member = new Member(messageObject.get("SenderName"), messageObject.get("SenderID"));
                    if (!members.contains(member))
                        members.add(member);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void updateListeners(Message message) {
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
     * @return
     */
    public boolean postMessage(Channel channel, String message) {
        if(channel==null) return false;
        String parameters = "AttachmentID=00000000-0000-0000-0000-000000000000&Body="+message+"&AttachmentRegionID=0&MachineKey="+machineKey+"&ClientID="+clientID;
        Util.sendPost("https://conversations-v1.curseapp.net/conversations/"+channel.groupID, parameters, getAuthToken());
        return true;
    }


    /**
     * Removes a user from the server
     * @param member member to be removed
     * @return
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
     * @param channel channel where the message is
     * @param message Message to be deleted
     * @return
     */
    public boolean deleteMessage(Channel channel, Message message) {
        if(channel==null) return false;
        if(message==null) return false;
        isDeleteInProgress = true;
        String url = "https://conversations-v1.curseapp.net/conversations/"+channel.groupID+"/"+message.serverID+"-"+message.timestamp;
        Util.sendDelete(url, getAuthToken());
        isDeleteInProgress = false;
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

    public Channel resolveChannelUUID(String uuid) {
        for(Map.Entry<String, Channel> entry: channels.entrySet()) {
            if(Util.equals(entry.getValue().groupID, uuid))
                return entry.getValue();
        }
        return null;
    }
}
