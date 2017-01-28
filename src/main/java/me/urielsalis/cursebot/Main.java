package me.urielsalis.cursebot;

import me.urielsalis.cursebot.api.*;

import java.io.*;
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
    private HashMap<Long, Long> banned = new HashMap<>();

    public static void main(String[] args) throws Exception {
        new Main();
    }

    public Main() {
        loadProperties();

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
                if(message.isPM) {
                    System.out.println(Util.timestampToDate(message.timestamp) + "  [" + message.senderName + "] " + message.body);
                } else {
                    System.out.println(Util.timestampToDate(message.timestamp) + "  <" + message.senderName + "> " + message.body);
                }
                updateBans(message.timestamp, api);
                
                try 
                {
					if(containsCurseWord(message.body)) {
					    api.deleteMessage(message);
					    api.postMessage(api.resolveChannelUUID(message.channelUUID), "@"+message.senderName+", please dont swear");
					}
					//else
						//api.postMessage(api.resolveChannelUUID(message.channelUUID), "@"+message.senderName+", you arent swearing properly");
				} 
                catch (UnsupportedEncodingException e1) 
                {e1.printStackTrace();}
                
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
                            	String helpMsg = " ```[filler: dont delete this. Line 1 starts after filler]"
										   + "\n.quit Quits bot" 
										   + "\n.send <channel> <message> Sends a message" 
										   + "\n.sender Shows a message to the caller" 
										   + "\n.resolver <uuid> resolver a UUID to channel name" 
										   + "\n .delete30 <channel> <username> Deletes 30 last messages of user" 
										   + "\n.kick <username> kicks a user from the server\n ``` ";
                            	
                                api.postMessage(api.resolveChannelUUID(message.channelUUID), helpMsg);
                            }
                            break;
                            
                            case ".addLinker":
                            {
                                String username = args[1];
                                authedLinkers.add(username.toLowerCase().trim());
                                api.postMessage(api.resolveChannelUUID(message.channelUUID), username+" its now authed to post links");
                            }
                            break;

                            case ".rmLinker":
                            {
                                String username = args[1];
                                authedLinkers.remove(username.toLowerCase().trim());
                                api.postMessage(api.resolveChannelUUID(message.channelUUID), username+" its now deauthed to post links");
                            }
                            break;

                            case ".ban":
                            {

                                String username = args[1];
                                int minutes = Integer.parseInt(args[2]);
                                String reason = "";
                                
                                if(args.length > 3)
                                    reason = Util.spaceSeparatedString(Arrays.copyOfRange(args, 3, args.length)).replaceAll("/n", "\n");
                                Member member = api.resolveMember(username);
                                if(member!=null) {
                                    api.banMember(member.senderID, reason);
                                    banned.put(member.senderID, message.timestamp+(minutes*60));
                                }
                            }
                            break;
                            
                            case "shrug":
                            {
                            	String shrug = "nut shrug";
								try {
									shrug = new String("¯\\_(ツ)_/¯".getBytes(), "UTF-8");
								} catch (UnsupportedEncodingException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
                            	api.postMessage(api.resolveChannelUUID(message.channelUUID), shrug);
                            }
                            break;

                            case ".banLeft":
                            {
                                int userID = Integer.parseInt(args[1]);
                                api.banMember(userID, "Reasons");
                            }
                            break;
                            case ".unbanLeft":
                            {
                                int userID = Integer.parseInt(args[1]);
                                api.unBanMember(userID);
                            }
                        }
                    }
                }
            }
        });
    }

    private void updateBans(long timestamp, CurseApi api) {
        ArrayList<Long> toUnban = new ArrayList<>();
        for(Map.Entry<Long, Long> entry: banned.entrySet()) {
            if(entry.getValue() < timestamp) {
                toUnban.add(entry.getKey());
            }
        }
        for(long id: toUnban) {
            banned.remove(id);
            api.unBanMember(id);
        }
    }

    private boolean isLinkAndNotAuthed(String body, String username) {
        if(body.contains("https") || body.contains(".com") || body.contains(".net") || body.contains("http") || body.contains(".org") || body.contains(".ly")) {
            if(!authedLinkers.contains(username.toLowerCase().trim())) return true;
        }
        return false;
    }

    //- WIP: working on UTF-8 checking
    //- Broken/ dont mess w/
    private boolean containsCurseWord(String body) throws UnsupportedEncodingException {
        String message = new String(body.getBytes("UTF-8"), "UTF-8");
    	for(String str: swearWords) {
            if(message.contains(new String(str.getBytes("UTF-8"), "UTF-8"))) {
            	return true;
            }
        }
        return false;
    }

    private boolean isUserAuthorized(String senderName) {
        for(String string: authorizedUsers)
            if(Util.equals(string, senderName)) return true;
        return false;
    }

    private void loadProperties() {
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
                
                String[] swears = prop.getProperty("swearWords").split(",");
                swearWords = new String[swears.length];
                
                for(int i = 0; i < swears.length; i++)
                	swearWords[i] = new String(swears[i].getBytes(), "UTF-8");
                
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
