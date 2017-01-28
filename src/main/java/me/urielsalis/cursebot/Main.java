package me.urielsalis.cursebot;

import me.urielsalis.cursebot.api.*;
import me.urielsalis.cursebot.extensions.ExtensionHandler;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * CurseApi
 * @Author: Urielsalis
 * License: GPL 3.0
 */
public class Main {
    public static CurseApi api;
    private String groupID = "";
    private String username = "";
    private String password = "";
    private String clientID = "";
    private String machineKey = "";
    private ArrayList<String> authedLinkers = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        new Main();
    }

    public Main() {
        loadProperties();

        api = new CurseApi(groupID, username, password, clientID, machineKey);
        api.startMessageLoop();
        //wait 5 seconds so all messages are flushed and only new ones shown
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ExtensionHandler handler = new ExtensionHandler();
        handler.init();

        api.addNewListener(new Listener() {
            @Override
            public void run(Message message) {
            	try {
					message.body = new String(message.body.getBytes("UTF-8"), "UTF-8");
				} catch (UnsupportedEncodingException e2) {
					e2.printStackTrace();
				}

                if(message.isPM) {
                    System.out.println(Util.timestampToDate(message.timestamp) + "  [" + message.senderName + "] " + message.body);
                } else {
                    System.out.println(Util.timestampToDate(message.timestamp) + "  <" + message.senderName + "> " + message.body);
                }
                
                if(isLinkAndNotAuthed(message.body, message.senderName)) {
                    api.deleteMessage(message);
                    api.postMessage(api.resolveChannelUUID(message.channelUUID), "@"+message.senderName+", please get permission before posting links");
                }
                if(Util.isUserAuthorized(message.senderName)) {
                    String[] args = message.body.split(" ");
                    if(args.length > 0) {
                        switch (args[0]) {

                            
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
                            
                            case ".help":
                            {	
                            	int index = 1;
                            	
                            	System.out.println("Test: " + args.length);
                            	if(args.length == 2)
                            	{
                            		try
                            		{index = Integer.parseInt(args[1]);}
                            		catch(NumberFormatException e)
                            		{index = args.length + 5;}
                            	}

                            	String helpMsg = "";	 //- Page 1
                            	String[] commandList = { "\n*.quit* ",
											    	 	 "\n - Shuts all bots down",

											    	 	 "\n*.send <channel> <message>* ",
											    	 	 "\n - Sends a message to a specified channel",

											    	 	 "\n*.sender* ",
											    	 	 "\n - Displays the sender of this command",

											    	 	 "\n*.resolver <uuid>* ",
											    	 	 "\n - Resolves a UUID to channel name",

											    	 	 "\n*.delete30 <channel> <username>* ",
											    	 	 "\n - Deletes 30 last messages of user",

											    	 	 "\n*.kick <username>* ",
											    	 	 "\n - Kicks a specified user from the server",

											    	 	 //- New Page: Page 2
											    	 	 "\n*.addProfanity <word|phrase>* ",
											   		 	 "\n - Adds a word or phrase to the profanity filter.", 
											   		 	 "\nWARNING: If you are adding a phrase, dont add spaces" };
								
                            	int maxPages = ((commandList.length/2) - 5);

                            	String header = "~*[HELP: Commands page (" + index + " / " + maxPages + ")]*~:\n-*I===============================I*-";

                            	if(!(index > maxPages))
                            	{
                            		helpMsg += header;
                            		for(int i = (((index * 11) - 11)); i <= (index * 11); i++)
                            		{
                            			if(index > 1 && i == (((index * 11) - 11)))
                            				i++;

                            			if(i == commandList.length)
                            				break;
                            			else
                            				helpMsg += commandList[i];
                            		}

                            		api.postMessage(api.resolveChannelUUID(message.channelUUID), helpMsg);
                            	}
                            	else
                            		api.postMessage(api.resolveChannelUUID(message.channelUUID), "The requested page does not exist!");
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


                            
                            case "shrug":
                            {
                            	String shrug = "not shrug";
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


    private boolean isLinkAndNotAuthed(String body, String username) {
        if(body.contains("https") || body.contains(".com") || body.contains(".net") || body.contains("http") || body.contains(".org") || body.contains(".ly")) {
            if(!authedLinkers.contains(username.toLowerCase().trim())) return true;
        }
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
                Util.authorizedUsers = prop.getProperty("authorizedUsers").split(",");
                
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
