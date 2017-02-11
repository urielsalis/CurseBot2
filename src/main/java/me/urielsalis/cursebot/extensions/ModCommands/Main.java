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

                String channelName = "";
                String username = "";
                int count = 1;
                boolean canDelete = true;
                if(command.args != null && command.args.length > 0) {
                    channelName = command.args[0];
                    if (command.args.length > 1) {
                        username = command.args[1];
                        if (command.args.length > 2) {
                            try {
                                count = Integer.parseInt(command.args[2]);
                            }
                            catch (NumberFormatException e) {

                            }
                        }
                    }
                    else {
                        canDelete = false;
                    }
                }
                else {
                    canDelete = false;
                }

                if (canDelete) {
                    int counter = 0;
                    Channel channel = api.resolveChannel(channelName);
                    Message message1;

                    api.postMessage(api.resolveChannel("bot-log"), "~*[Executing delete command!]*~\n*Command Sender:* [ " + api.mention(command.message.senderName) + " ]\n*Deleted messages:*\n-*I===============================I*-");
                    for (int i = channel.messages.size() - 1; i >= 0; i--) {
                        message1 = (Message) channel.messages.toArray()[i];
                        if (counter >= count) break;
                        if (Util.equals(username, message1.senderName)) {
                            if (message1.isPM) {
                                api.postMessage(api.resolveChannel("bot-log"), "[ " + api.mention(message1.senderName) + " ]: ```\"" + message1.body.replaceAll("\\s+|\\n", " ") + "\"```");
                            } else {
                                api.postMessage(api.resolveChannel("bot-log"), "< " + api.mention(message1.senderName) + " >: ```\"" + message1.body.replaceAll("\\s+|\\n", " ") + "\"```");
                            }
                            api.deleteMessage(message1);
                            counter++;
                        }
                    }
                }
                else {
                    api.postMessage(api.resolveChannel("bot-stats"), "~*[ERROR: Invalid command syntax!]*~\n*Command:* .delete");
                }
            }
            break;
            case "kick":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                String username = (command.args != null && command.args.length > 0) ? command.args[0] : "";

                if(api.resolveMember(username) != null && !Util.isUserAuthorized(api, api.resolveMember(username))) {
                    api.kickUser(api.resolveMember(username));
                    api.postMessage(api.resolveChannel("bot-log"), "~*[Executing kick command!]*~\n*Command Sender:* [ " + api.mention(command.message.senderName) + " ]\n*Kicked user:* " + username + ".");
                }
                else {
                    api.postMessage(api.resolveChannel("bot-stats"), "~*[ERROR: Invalid user!]*~\n*Details:* Could not kick *'" + username + "'*");
                }
            }
            break;
            case "tmpban":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;


                boolean canBan = false;
                String username = "";
                int minutes = 5;
                String reason = "No reason given!";

                if(command.args != null && command.args.length > 0) {
                    username = command.args[0];
                    if(command.args.length > 1) {
                        try {
                            minutes = Integer.parseInt(command.args[1]);
                            if (command.args.length > 2) {
                                reason = Util.spaceSeparatedString(Arrays.copyOfRange(command.args, 2, command.args.length)).replaceAll("/n", "\n");
                            }
                        }
                        catch (NumberFormatException e) {
                            reason = Util.spaceSeparatedString(Arrays.copyOfRange(command.args, 1, command.args.length)).replaceAll("/n", "\n");
                        }
                    }
                }

                Member member = api.resolveMember(username);
                if(member != null) {
                    api.banMember(member.senderID, reason);
                    banned.put(member.senderID, command.message.timestamp+(minutes*60));
                    api.postMessage(api.resolveChannel("bot-log"), "~*[Executing ban user command!]*~\n*Command Sender:* [ " + api.mention(command.message.senderName) + " ]\n*Banned user:* " + username + ".\n*Reason:* \"" + reason + "\"\n*Timeout(minutes):* " + minutes);
                }
                else {
                    api.postMessage(api.resolveChannel("bot-stats"), "~*[ERROR: Invalid user!]*~\n*Details:* Could not temp ban *'" + username + "'*");
                }
            }
            break;
            case "quit":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                api.postMessage(api.resolveChannel("bot-log"), "~*[Shut down command executed!]*~\n*Command Sender:* [ " + api.mention(command.message.senderName) + " ]");
                System.exit(0);
            }
            break;
            case "send":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                String channel = (command.args != null && command.args.length > 0) ? command.args[0] : "";
                String body = (command.args != null && command.args.length > 1) ? (Util.spaceSeparatedString(Arrays.copyOfRange(command.args, 1, command.args.length)).replaceAll("/n", "\n")) : "";
                if(body.startsWith(".")) break;
                if (api.resolveChannel(channel) != null && !body.equals("")) {
                    api.postMessage(api.resolveChannel("bot-log"), "~*[Executing send command]*~\n*Command Sender:* [ " + api.mention(command.message.senderName) + " ]\n*Sending:* \"" + body + "\"");
                    api.postMessage(api.resolveChannel(channel), body);
                }
                else {
                    api.postMessage(api.resolveChannel("bot-stats"), "~*[ERROR: Invalid channel!]*~\n*Details:* Could not send message to channel *'" + channel + "'*");
                }
            }
            break;

            case "sender":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                api.postMessage(api.resolveChannel("bot-log"), "~*[Executing sender command]*~\n*Command Sender:* [ " + api.mention(command.message.senderName) + " ]");
                api.postMessage(api.resolveChannelUUID(command.message.channelUUID), "Hello there " + api.mention(command.message.senderName));
            }
            break;

            case "resolve":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                String resolve = (command.args != null && command.args.length > 0) ? command.args[0] : "";
                String resolveThis = (command.args != null && command.args.length > 1) ? command.args[1] : "";
                if (!(resolve.equals("") || resolveThis.equals("")) && (resolve.equalsIgnoreCase("member") || resolve.equalsIgnoreCase("user"))) {
                    if (api.resolveMember(resolveThis) != null) {
                        api.postMessage(api.resolveChannel("bot-stats"), "~*[Executing resolving " + resolve + "]*~\n*Command Sender:* [ " + api.mention(command.message.senderName) + " ]\n*Username '" + resolveThis + "':* " + api.resolveMember(resolveThis).senderName + "\n*Nickname:* " + api.resolveMember(resolveThis).username);
                    }
                    else {
                        api.postMessage(api.resolveChannel("bot-stats"), "~*[Executing resolving " + resolve + "]*~\n*Command Sender:* [ " + api.mention(command.message.senderName) + " ]\n*Username:* Unable to resolve '" + resolveThis + "' to a username!");
                    }
                }
                else if (!(resolve.equals("") || resolveThis.equals("")) && (resolve.equalsIgnoreCase("userID") || resolve.equalsIgnoreCase("memberID"))) {
                    if (api.resolveMember(resolveThis) != null) {
                        api.postMessage(api.resolveChannel("bot-stats"), "~*[Executing resolving " + resolve + "]*~\n*Command Sender:* [ " + api.mention(command.message.senderName) + " ]\n*User ID '" + resolveThis + "':* " + api.resolveMember(resolveThis).senderID);
                    }
                    else {
                        api.postMessage(api.resolveChannel("bot-stats"), "~*[Executing resolving " + resolve + "]*~\n*Command Sender:* [ " + api.mention(command.message.senderName) + " ]\n*User ID:* Unable to resolve '" + resolveThis + "' to a user ID!");
                    }
                }
                else if (!(resolve.equals("") || resolveThis.equals("")) && (resolve.equalsIgnoreCase("userRole") || resolve.equalsIgnoreCase("memberRole") || resolve.equalsIgnoreCase("role"))) {
                    if (api.resolveMember(resolveThis) != null) {
                        api.postMessage(api.resolveChannel("bot-stats"), "~*[Executing resolving " + resolve + "]*~\n*Command Sender:* [ " + api.mention(command.message.senderName) + " ]\n*User role '" + resolveThis + "':* " + api.resolveMember(resolveThis).bestRole);
                    }
                    else {
                        api.postMessage(api.resolveChannel("bot-stats"), "~*[Executing resolving " + resolve + "]*~\n*Command Sender:* [ " + api.mention(command.message.senderName) + " ]\n*User role:* Unable to resolve '" + resolveThis + "' to a user role!");
                    }
                }
                else if (!(resolve.equals("") || resolveThis.equals("")) && resolve.equalsIgnoreCase("channel")) {
                    if (api.resolveChannel(resolveThis) != null) {
                        api.postMessage(api.resolveChannel("bot-stats"), "~*[Executing resolving " + resolve + "]*~\n*Command Sender:* [ " + api.mention(command.message.senderName) + " ]\n*Channel name '" + resolveThis + "':* " + api.resolveChannel(resolveThis).groupTitle);
                    }
                    else {
                        api.postMessage(api.resolveChannel("bot-stats"), "~*[Executing resolving " + resolve + "]*~\n*Command Sender:* [ " + api.mention(command.message.senderName) + " ]\n*Channel name:* Unable to resolve '" + resolveThis + "' to a channel name!");
                    }
                }
                else if (!(resolve.equals("") || resolveThis.equals("")) && resolve.equalsIgnoreCase("channelID")) {
                    if (api.resolveChannel(resolveThis) != null) {
                        api.postMessage(api.resolveChannel("bot-stats"), "~*[Executing resolving " + resolve + "]*~\n*Command Sender:* [ " + api.mention(command.message.senderName) + " ]\n*Channel ID '" + resolveThis + "':* " + api.resolveChannel(resolveThis).groupID);
                    }
                    else {
                        api.postMessage(api.resolveChannel("bot-stats"), "~*[Executing resolving " + resolve + "]*~\n*Command Sender:* [ " + api.mention(command.message.senderName) + " ]\n*Channel ID:* Unable to resolve '" + resolveThis + "' to a channel ID!");
                    }
                }
                else {
                    api.postMessage(api.resolveChannel("bot-stats"), "~*[Executing resolving " + resolve + "]*~\n*Command Sender:* [ " + api.mention(command.message.senderName) + " ]\n*Unresolved:* Unable to resolve anything in given the parameters!");
                }
            }
            break;

            case "help":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                int index = 1;

                api.postMessage(api.resolveChannel("bot-log"), "~*[Executing help command]*~\n*Command Sender:* [ " + api.mention(command.message.senderName) + " ]");
                if(!(command.args == null) && (command.args.length == 1))
                {
                    try
                    {index = Integer.parseInt(command.args[0]);}
                    catch(NumberFormatException e)
                    {index = command.args.length + 5;}
                }

                String helpMsg = "";
                String[] commandList = {
                        //- Page 1 : 12 lines per page
                        "\n*.quit* ",
                        "\n - Shuts all bots down",

                        "\n*.send <channel> <message>* ",
                        "\n - Sends a message to a specified channel",

                        "\n*.sender* ",
                        "\n - Displays the sender of this command",

                        "\n*.resolver <\"...\"> <membername|channelname>*",
                        "\n - Resolves a member to a their name, nickname, role, or member id or ",
                        "\n a channel to a channel name or channel UUID. e.g. .resolve member myName",

                        "\n*.delete <channel> <username> [number]*",
                        "\n - Deletes [#] of recent messages posted by <username>.",
                        "\n Deletes the last post of a user if no number is given.",

                        //- New Page: Page 2
                        "\n*.kick <username>*",
                        "\n - Kicks a specified user from the server",

                        "\n*.tmpban <username> [[minutes|reason] reason]*",
                        "\n - Gives a user a time out for 5 minutes if no time is specified.",

                        "\n*.banLeft <userID>*",
                        "\n - Bans a user who isnt in the server with said user id",

                        "\n*.unbanLeft <userID>*",
                        "\n - Unbans a user who isnt in the server with said user id",

                        "\n*.addProfanity <word|phrase>* ",
                        "\n - Adds a word or phrase to the profanity filter.",
                        "\n WARNING: If you are adding a phrase, don't include spaces",

                        " ",

                        //- New Page: Page 3
                        "\n*.rmProfanity <word|phrase>*",
                        "\n - Removes a word or phrase from the profanity filter.",

                        "\n*.blacklistLink <link>*",
                        "\n - Adds a link to the link filter." };

                int maxPages = (int)Math.ceil((double)commandList.length/12.0);

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
                int userID = -1;
                try {
                    userID = (command.args != null && command.args.length > 0) ? Integer.parseInt(command.args[0]) : -1;
                    if(userID != -1) {
                        api.postMessage(api.resolveChannel("bot-log"), "~*[Executing banLeft command]*~\n*Command Sender:* [ " + api.mention(command.message.senderName) + " ]\n*Banned member not in the server:* " + userID);
                        api.banMember(userID, "No reason provided!");
                    }
                    else {
                        throw new NumberFormatException();
                    }
                }
                catch (NumberFormatException e) {
                    api.postMessage(api.resolveChannel("bot-stats"), "~*[ERROR: Invalid user!]*~\n*Details:* Could not ban *'" + userID + "'*");
                }
            }
            break;
            case "unbanLeft":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                int userID = -1;
                try {
                    userID = (command.args != null && command.args.length > 0) ? Integer.parseInt(command.args[0]) : -1;
                    if(userID != -1) {
                        api.postMessage(api.resolveChannel("bot-log"), "~*[Executing unbanLeft command]*~\n*Command Sender:* [ " + api.mention(command.message.senderName) + " ]\n*Unbanned member not in the server:* " + userID);
                        api.unBanMember(userID);
                    }
                    else {
                        throw new NumberFormatException();
                    }
                }
                catch (NumberFormatException e) {
                    api.postMessage(api.resolveChannel("bot-stats"), "~*[ERROR: Invalid user!]*~\n*Details:* Could not ban *'" + userID + "'*");
                }
            }
        }
    }

    private static void updateBans(long timestamp) {
        ArrayList<Long> toUnban = new ArrayList<>();
        for(Map.Entry<Long, Long> entry : banned.entrySet()) {
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
