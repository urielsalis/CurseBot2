package me.urielsalis.cursebot.extensions.ModCommands;

import me.urielsalis.cursebot.api.*;
import me.urielsalis.cursebot.events.CommandEvent;
import me.urielsalis.cursebot.events.MessageEvent;
import me.urielsalis.cursebot.extensions.Extension;
import me.urielsalis.cursebot.extensions.ExtensionApi;
import me.urielsalis.cursebot.extensions.ExtensionHandler;
import me.urielsalis.cursebot.extensions.Handle;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Created by urielsalis on 1/28/2017
 */
@Extension(name = "ModCommands", version = "1.0.0", id = "Modcommands/1.0.0")
public class Main {
    static ExtensionApi extApi;
    static CurseApi api;
    private static HashMap<Long, Long> banned = new HashMap<>();

    @ExtensionHandler.ExtensionInit("Profanity/1.0.0")
    public static void init(ExtensionApi api2) {
        extApi = api2;
        extApi.addListener("command", new ModCommandsListener());
        Timer timer = new Timer();
        TimerTask unban = new TimerTask() {
            @Override
            public void run() {
                updateBans(System.currentTimeMillis() / 1000L);
            }
        };

        timer.schedule(unban,0l,1000*60);
        api = extApi.getCurseAPI();
    }

    private static class ModCommandsListener implements ExtensionApi.Listener {
        @Override
        public String name() {
            return "ModCommandsListener/1.0.0";
        }

        @Handle
        public void handle(ExtensionApi.Event event) {
            if(event instanceof CommandEvent) {
                CommandEvent commandEvent = (CommandEvent) event;
                handleCommand(commandEvent.command);
            }
        }
    }

    private static void handleCommand(Command command) {
        switch (command.command) {
            case "delete":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                api.postMessage(api.resolveChannel("bot-log"), "Executing delete command! sender: " + command.message.senderName + " Deleted messages:");
                String channelName = command.args[0];
                String username = command.args[1];
                int count = Integer.parseInt(command.args[2]);
                int counter = 0;
                Channel channel = api.resolveChannel(channelName);
                for(Message message1 : channel.messages) {
                    if(counter > count) break;
                    if(Util.equals(username, message1.senderName)) {
                        if(message1.isPM) {
                            api.postMessage(api.resolveChannel("bot-log"), Util.timestampToDate(message1.timestamp) + "  [" + message1.senderName + "] " + message1.body);
                        } else {
                            api.postMessage(api.resolveChannel("bot-log"), Util.timestampToDate(message1.timestamp) + "  <" + message1.senderName + "> " + message1.body);
                        }
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
            case "kick":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                String username = command.args[0];
                if(!Util.isUserAuthorized(api, api.resolveMember(username))) {
                    api.kickUser(api.resolveMember(username));
                    api.postMessage(api.resolveChannel("bot-log"), "Kicked user "+username + ". Sender: "+command.message.senderName);
                }
            }
            break;
            case "ban":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                String username = command.args[0];
                int minutes = Integer.parseInt(command.args[1]);
                String reason = "";

                if(command.args.length > 2)
                    reason = Util.spaceSeparatedString(Arrays.copyOfRange(command.args, 2, command.args.length)).replaceAll("/n", "\n");
                Member member = api.resolveMember(username);
                if(member!=null) {
                    api.banMember(member.senderID, reason);
                    banned.put(member.senderID, command.message.timestamp+(minutes*60));
                    api.postMessage(api.resolveChannel("bot-log"), "Banned user "+username + ". Sender: "+command.message.senderName + " Reason: " + reason + " Minutes: " + minutes);
                }
            }
            break;
            case "quit":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                api.postMessage(api.resolveChannel("bot-log"), "Shut down command executed! sender: " + command.message.senderName);
                System.exit(0);
            }
            break;
            case "send":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                String channel = command.args[0];
                String body = Util.spaceSeparatedString(Arrays.copyOfRange(command.args, 1, command.args.length)).replaceAll("/n", "\n");
                if(body.startsWith(".")) break;
                api.postMessage(api.resolveChannel(channel), body);
            }
            break;

            case "sender":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                api.postMessage(api.resolveChannelUUID(command.message.channelUUID), "Hai "+command.message.senderName);
            }
            break;

            case "resolve":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                String resolve = command.args[0];
                api.postMessage(api.resolveChannelUUID(command.message.channelUUID), api.resolveChannelUUID(resolve).groupTitle);
            }
            break;

            case "help":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                int index = 1;

                System.out.println("Test: " + command.args.length);
                if(command.args.length == 1)
                {
                    try
                    {index = Integer.parseInt(command.args[0]);}
                    catch(NumberFormatException e)
                    {index = command.args.length + 5;}
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

                    api.postMessage(api.resolveChannelUUID(command.message.channelUUID), helpMsg);
                }
                else
                    api.postMessage(api.resolveChannelUUID(command.message.channelUUID), "The requested page does not exist!");
            }
            break;

            case "addLinker":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                String username = command.args[0];
                me.urielsalis.cursebot.Main.authedLinkers.add(username.toLowerCase().trim());
                api.postMessage(api.resolveChannelUUID(command.message.channelUUID), username+" its now authed to post links");
            }
            break;

            case "rmLinker":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                String username = command.args[0];
                me.urielsalis.cursebot.Main.authedLinkers.remove(username.toLowerCase().trim());
                api.postMessage(api.resolveChannelUUID(command.message.channelUUID), username+" its now deauthed to post links");
            }
            break;



            case "shrug":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                String shrug = "not shrug";
                try {
                    shrug = new String("¯\\_(ツ)_/¯".getBytes(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                api.postMessage(api.resolveChannelUUID(command.message.channelUUID), shrug);
            }
            break;

            case "banLeft":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                int userID = Integer.parseInt(command.args[0]);
                api.banMember(userID, "Reasons");
            }
            break;
            case "unbanLeft":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                int userID = Integer.parseInt(command.args[0]);
                api.unBanMember(userID);
            }
        }
    }

    private static void updateBans(long timestamp) {
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
}
