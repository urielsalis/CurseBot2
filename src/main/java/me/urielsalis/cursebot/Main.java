package me.urielsalis.cursebot;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Observable;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * BattleCode 2017
 * Team: Mate
 * License: GPL 3.0
 */
public class Main {
    static String auth = "yourauth"; //groupuuid
    static String groupID = "72a413b0-f8ed-4851-8c61-c6b13097bbb8";
    static ArrayList<Channel> channels = new ArrayList<Channel>();
    static boolean isDeleteInProgress = false;
    static boolean isWaitingOnInput = false;
    static StringBuffer buffer = new StringBuffer();
    public static void main(String[] args) throws Exception {
        getInfo();
        new Thread(new Runnable() {
            public void run() {
                try {
                    while(true) {
                        updateMessages();
                        TimeUnit.SECONDS.sleep(1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        isWaitingOnInput = true;
        Scanner scanner = new Scanner(System.in);
        System.out.print("Channel: ");
        String channel = scanner.nextLine();
        System.out.print("Message: ");
        String message = scanner.nextLine();
        sendMessage(channel, message);
        System.out.print("Delete all messages by: ");
        deleteAllMessagesBy(channel, scanner.nextLine());
        isWaitingOnInput = false;
        System.out.print(buffer.toString());
        buffer = new StringBuffer();
        while(true) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    private static void deleteAllMessagesBy(String channelName, String username) throws Exception {
        updateMessages();
        isDeleteInProgress = true;
        for(Channel channel: channels) {
            if(channel.groupTitle.toLowerCase().trim().equals(channelName.toLowerCase().trim())) {
                for(Message message: channel.messages) {
                    if(message.senderName.trim().toLowerCase().equals(username.toLowerCase().trim())) {
                        delete(channel.groupID, message.serverID, message.timestamp);
                        TimeUnit.SECONDS.sleep(1);
                    }
                }
            }
        }
        isDeleteInProgress = false;
    }

    private static void updateMessages() throws Exception {
        while(isDeleteInProgress) TimeUnit.SECONDS.sleep(1);
        for(Channel channel: channels) {
            getMessages(channel);
        }
    }

    private static void delete(String channel, String serverID, long timestamp) throws Exception {
        String url = "https://conversations-v1.curseapp.net/conversations/"+channel+"/"+serverID+"-"+timestamp;
        URL obj = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
        con.setRequestMethod("DELETE");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 4.0.4; Galaxy Nexus Build/IMM76B) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.133 Mobile Safari/535.19");
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setRequestProperty("AuthenticationToken", auth);
        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'DELETE' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
    }

    private static void sendMessage(String channelName, String message) throws Exception {
        String channel = resolveChannel(channelName);
        if(channel==null) return;
        String parameters = "AttachmentID=00000000-0000-0000-0000-000000000000&Body="+message+"&AttachmentRegionID=0&MachineKey=99149aae-1588-45a8-a1fc-d7ee78d2101c&ClientID=3fc35821-57e2-46e7-b6d8-8549930f1171\n";
        sendPost("https://conversations-v1.curseapp.net/conversations/"+channel, parameters);
    }

    private static String resolveChannel(String channelName) {
        for(Channel channel: channels) {
            if(channel.groupTitle.toLowerCase().trim().equals(channelName.toLowerCase().trim())) return channel.groupID;
        }
        return null;
    }

    private static void getMessages(Channel channel) throws Exception {
        String json;
        if(channel.messages.isEmpty()) {
            json = sendGet("https://conversations-v1.curseapp.net/conversations/"+channel.groupID+"?endTimestamp=0&pageSize=30&startTimestamp=0");
        } else {
            json = sendGet("https://conversations-v1.curseapp.net/conversations/"+channel.groupID+"?endTimestamp=0&pageSize=30&startTimestamp="+channel.messages.get(channel.messages.size()-1).timestamp);
        }
        JSONArray array = (JSONArray) new JSONParser().parse(json);
        for(Object obj: array) {
            JSONObject messageObject = (JSONObject) obj;
            Message message = new Message(messageObject.get("SenderName"), messageObject.get("Body"), messageObject.get("Timestamp"), messageObject.get("ServerID"));
            if(!channel.messages.contains(message)) {
                channel.messages.add(message);
                if(isWaitingOnInput) {
                    buffer.append("["+channel.groupTitle+"]" + message.getString()+"\n");
                } else {
                    System.out.println("["+channel.groupTitle+"]" + message.getString());
                }
                //message.print();
            }
        }
//        Message message = new Message()
    }

    private static void getInfo() throws Exception {
        String json  = sendGet("https://groups-v1.curseapp.net/groups/"+groupID+"?showDeletedChannels=false");
        JSONObject object = (JSONObject) new JSONParser().parse(json);
        System.out.println("Group ID:"+ object.get("GroupID"));
        System.out.println("Group Name:"+ object.get("GroupTitle"));
        JSONArray array = (JSONArray) object.get("Channels");
        for(Object obj: array) {
            JSONObject channel = (JSONObject) obj;
            channels.add(new Channel(channel.get("GroupTitle"), channel.get("GroupID")));
            System.out.println("Channel " + channel.get("GroupTitle") + " ID " + channel.get("GroupID"));
        }
    }

    private static String sendGet(String url) throws Exception {
        URL obj = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 4.0.4; Galaxy Nexus Build/IMM76B) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.133 Mobile Safari/535.19");
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setRequestProperty("AuthenticationToken", auth);
        int responseCode = con.getResponseCode();

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    private static String sendPost(String url, String parameters) throws Exception {
        URL obj = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 4.0.4; Galaxy Nexus Build/IMM76B) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.133 Mobile Safari/535.19");
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setRequestProperty("AuthenticationToken", auth);
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(parameters);
        wr.flush();
        wr.close();
        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'POST' request to URL : " + url);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }
}
