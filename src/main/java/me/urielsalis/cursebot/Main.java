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
    private String auth = "";
    private String clientID = "";
    private String machineKey = "";
    private String[] authorizedUsers;

    public static void main(String[] args) throws Exception {
        new Main();
    }

    public Main() {
        loadPropierties();

        final CurseApi api = new CurseApi(groupID, auth, clientID, machineKey);
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
                                    String body = Util.spaceSeparatedString(Arrays.copyOfRange(args, 2, args.length));
                                    api.postMessage(api.resolveChannel(channel), body);
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
                                            api.deleteMessage(channel, message1);
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
                                    api.kickUser(api.resolveMember(username));
                                }
                                break;
                        }
                    }
                }
            }
        });
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
                auth = prop.getProperty("auth");
                clientID = prop.getProperty("clientID");
                machineKey = prop.getProperty("machineKey");
                authorizedUsers = prop.getProperty("authorizedUsers").split(",");
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
