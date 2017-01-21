package me.urielsalis.cursebot;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * CurseApi
 * @Author: Urielsalis
 * License: GPL 3.0
 */
public class Main {
    private String groupID = "";
    private String username = "";
    private String password = "";
    private String clientID = "";
    private String machineKey = "";
    private String[] authorizedUsers;
    private String[] swearWords;
    private ArrayList<String> authedLinkers = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        new Main();
    }

    public Main() {
        loadPropierties();

        final CurseApi api = new CurseApi(groupID, username, password, clientID, machineKey);
        api.startMessageLoop();
        //wait 5 seconds so all messages are flushed and only new ones shown
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        api.addNewListener(new Listener() {
            @Override
            public void run(Message message) {
                System.out.println(Util.timestampToDate(message.timestamp) + "  <"+message.senderName+"> "+message.body);
                if(containsCurseWord(message.body)) {
                    api.deleteMessage(message);
                    api.postMessage(api.resolveChannelUUID(message.channelUUID), "@"+message.senderName+", please dont swear");
                }
                if(isLinkAndNotAuthed(message.body, message.senderName)) {
                    api.deleteMessage(message);
                    api.postMessage(api.resolveChannelUUID(message.channelUUID), "@"+message.senderName+", please get permission before posting links");
                }
                if(isUserAuthorized(message.senderName)) {
                    String[] args = message.body.split(" ");
                    if(args.length > 0) {
                        switch (args[0]) {
                            case ".quit":
                                {
                                    System.exit(0);
                                }
                                break;
                            case ".send":
                                {
                                    String channel = args[1];
                                    String body = Util.spaceSeparatedString(Arrays.copyOfRange(args, 2, args.length)).replaceAll("/n", "\n");
                                    if(body.startsWith(".")) break;
                                    api.postMessage(api.resolveChannel(channel), body);
                                }
                                break;
                            case ".sender":
                                {
                                    api.postMessage(api.resolveChannelUUID(message.channelUUID), "Hai "+message.senderName);
                                }
                                break;
                            case ".resolve":
                                {
                                    String resolve = args[1];
                                    api.postMessage(api.resolveChannelUUID(message.channelUUID), api.resolveChannelUUID(resolve).groupTitle);
                                }
                                break;
                            case ".delete30":
                                {
                                    String channelName = args[1];
                                    String username = args[2];
                                    int counter = 0;
                                    Channel channel = api.resolveChannel(channelName);
                                    for(Message message1: channel.messages) {
                                        if(counter > 30) break;
                                        if(Util.equals(username, message1.senderName)) {
                                            api.deleteMessage(message1);
                                            /*try {
                                                TimeUnit.SECONDS.sleep(1);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }*/
                                            counter++;
                                        }
                                    }
                                }
                                break;
                            case ".delete":
                                {
                                    String channelName = args[1];
                                    String username = args[2];
                                    int count = Integer.parseInt(args[3]);
                                    int counter = 0;
                                    Channel channel = api.resolveChannel(channelName);
                                    for(Message message1: channel.messages) {
                                        if(counter > count) break;
                                        if(Util.equals(username, message1.senderName)) {
                                            api.deleteMessage(message1);
                                            /*try {
                                                TimeUnit.SECONDS.sleep(1);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }*/
                                            counter++;
                                        }
                                    }
                                }
                                break;
                            case ".like30":
                                {
                                    String channelName = args[1];
                                    String username = args[2];
                                    int counter = 0;
                                    Channel channel = api.resolveChannel(channelName);
                                    for(Message message1: channel.messages) {
                                        if(counter > 30) break;
                                        if(Util.equals(username, message1.senderName)) {
                                            api.likeMessage(message1);
                                            try {
                                                TimeUnit.SECONDS.sleep(1);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            counter++;
                                        }
                                    }
                                }
                                break;
                            case ".edit30":
                                {
                                    String channelName = args[1];
                                    String username = args[2];
                                    String body = Util.spaceSeparatedString(Arrays.copyOfRange(args, 3, args.length)).replaceAll("/n", "\n");
                                    int counter = 0;
                                    Channel channel = api.resolveChannel(channelName);
                                    for(Message message1: channel.messages) {
                                        if(counter > 30) break;
                                        if(Util.equals(username, message1.senderName)) {
                                            api.editMessage(message1, body);
                                            try {
                                                TimeUnit.SECONDS.sleep(1);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            counter++;
                                        }
                                    }
                                }
                                break;
                            case ".kick":
                                {
                                    String username = args[1];
                                    if(!isUserAuthorized(username))
                                        api.kickUser(api.resolveMember(username));
                                }
                                break;
                            case ".help":
                                {
                                    api.postMessage(api.resolveChannelUUID(message.channelUUID), ".quit Quits bot\n.send <channel> <message> Sends a message\n.sender Shows a message to the caller\n.resolver <uuid> resolver a UUID to channel name\n .delete30 <channel> <username> Deletes 30 last messages of user\n.kick <username> kicks a user from the server");
                                }
                                break;
                            case ".addLinker": {
                                {
                                    String username = args[1];
                                    authedLinkers.add(username.toLowerCase().trim());
                                    api.postMessage(api.resolveChannelUUID(message.channelUUID), username+" its now authed to post links");
                                }
                                break;
                            }
                            case ".rmLinker": {
                                {
                                    String username = args[1];
                                    authedLinkers.remove(username.toLowerCase().trim());
                                    api.postMessage(api.resolveChannelUUID(message.channelUUID), username+" its now deauthed to post links");
                                }
                                break;
                            }
                        }
                    }
                }
            }
        });
    }

    private boolean isLinkAndNotAuthed(String body, String username) {
        if(body.contains("https") || body.contains(".com") || body.contains(".net") || body.contains("http") || body.contains(".org")) {
            if(!authedLinkers.contains(username)) return true;
        }
        return false;
    }

    private boolean containsCurseWord(String body) {
        for(String str: swearWords) {
            if(body.contains(str)) return true;
        }
        return false;
    }

    private boolean isUserAuthorized(String senderName) {
        for(String string: authorizedUsers)
            if(Util.equals(string, senderName)) return true;
        return false;
    }

    private void loadPropierties() {
        try {
            Properties prop = new Properties();
            InputStream inputStream = new FileInputStream("config.properties");

            if (inputStream != null) {
                prop.load(inputStream);
                groupID = prop.getProperty("groupID");
                username = prop.getProperty("username");
                password = prop.getProperty("password");
                clientID = prop.getProperty("clientID");
                machineKey = prop.getProperty("machineKey");
                authorizedUsers = prop.getProperty("authorizedUsers").split(",");
                swearWords = prop.getProperty("swearWords").split(",");
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
