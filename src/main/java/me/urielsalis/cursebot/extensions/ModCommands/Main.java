package me.urielsalis.cursebot.extensions.ModCommands;

import me.urielsalis.cursebot.api.*;
import me.urielsalis.cursebot.events.CommandEvent;
import me.urielsalis.cursebot.extensions.Extension;
import me.urielsalis.cursebot.extensions.ExtensionApi;
import me.urielsalis.cursebot.extensions.ExtensionHandler;
import me.urielsalis.cursebot.extensions.Handle;

import java.io.*;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Extension(name = "ModCommands", version = "1.0.0", id = "Modcommands/1.0.0")
public class Main {
    static ExtensionApi extApi;
    static CurseApi api;
    private static ScheduledThreadPoolExecutor unbanUpdater = new ScheduledThreadPoolExecutor(4);

    @ExtensionHandler.ExtensionInit("Profanity/1.0.0")
    public static void init(ExtensionApi api2) {
        extApi = api2;
        extApi.addListener("command", new ModCommandsListener());
        Timer timer = new Timer();
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
                handleCommand(commandEvent.getCommand());
            }
        }
    }

    private static void handleCommand(Command command) {

        Member cmdSender = api.resolveMember(command.message.senderName);
        Channel cmdChannel = api.resolveChannelUUID(command.message.channelUUID);

        //:: Command sender information
        //:: Sender Information
        String uniqueName = cmdSender.username;     //- The user's unique name. This is special to the user
        String displayName = cmdSender.displayName; //- The user's display name in a server.
        String senderName = cmdSender.senderName;   //- The user's nickname/name to refernce a command sent by
        long cmdSenderID = cmdSender.senderID;           //- The user's unique numeric id. This is special to the user
        long cmdSenderRole = cmdSender.bestRole;         //- The best role a user currently holds. infinity to 1.

        //:: Channel Information
        String channelName = cmdChannel.groupTitle; //- The name of the channel command was sent in.
        String channelID = cmdChannel.groupID;      //- The special ID of a channel in which to refer to a channel.

        //:: Command Information
        String stringArgs = (command.args != null) ? Util.spaceSeparatedString(command.args) : "null";

        if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
        switch (command.command) {
            case "delete":
            {
                //:: Command Argument variables
                String cmdArgChannelName = "";
                String cmdArgUserName = "";

                int cmdArgDeletMessagesX = 1;
                boolean canDelete = true;

                //:: assign arguments to variables
                if(command.args != null && command.args.length > 0) {
                    cmdArgChannelName = command.args[0];
                    if (command.args.length > 1) {
                        cmdArgUserName = command.args[1];
                        if (command.args.length > 2) {
                            try {
                                cmdArgDeletMessagesX = Integer.parseInt(command.args[2]);
                            }
                            catch (NumberFormatException ignored) {

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
                    Channel channel = api.resolveChannel(cmdArgChannelName);
                    Message message1;

                    if (channel != null) {
                        Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "delete", channelName, stringArgs);

                        for (int i = channel.messages.size() - 1; i >= 0; i--) {
                            message1 = (Message) channel.messages.toArray()[i];
                            if (counter >= cmdArgDeletMessagesX) break;
                            if (Util.equals(cmdArgUserName, message1.senderName)) {
                                api.deleteMessage(message1);
                                counter++;
                            }
                        }
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), cmdArgDeletMessagesX + " of " + api.mention(cmdArgUserName) + "'s latest messages have been successfuly deleted from the channel " + cmdArgChannelName);
                    }
                    else {
                        api.postMessage(api.resolveChannelUUID(Util.botcmdChannel), "Specified channel does not exist or could not be deleted from!");
                        Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "delete", channelName, "Error: Un-existent channel");
                    }
                }
                else {
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "~*[ERROR: Invalid command syntax!]*~\n*Command:* .delete");
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "delete", channelName, "Invalid command syntax!");
                }
            }
            break;
            case "kick":
            {
                String cmdArgUsername = (command.args != null && command.args.length > 0) ? command.args[0] : "";
                Member cmdArgMember = api.resolveMember(cmdArgUsername);

                if(cmdArgMember != null && !Util.isUserAuthorized(api, cmdArgMember)) {
                    api.kickUser(api.resolveMember(cmdArgUsername));
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "kick", channelName, stringArgs);
                    Util.dataBase.addWarning(cmdSenderID, uniqueName, cmdArgMember.senderID, cmdArgMember.senderName, "Standard kick", "User kicked from server!");
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "The user " + cmdArgUsername + " was successfully removed from the server!");
                }
                else {
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "~*[ERROR: Invalid user!]*~\n*Details:* Could not kick *'" + cmdArgUsername + "'*");
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "kick", channelName, "Unable to kick " + cmdArgUsername);
                }
            }
            break;
            case "tmpban":
            {
                boolean canBan = false;
                String cmdArgUsername = "";
                int cmdArgHours = 0;
                int cmdArgMinutes = 5;
                String cmdArgReason = "No reason given!";

                if(command.args != null && command.args.length > 0) {
                    cmdArgUsername = command.args[0];
                    if(command.args.length > 1) {
                        try {
                            command.args[1] = command.args[1].toLowerCase();
                            cmdArgHours = (command.args[1].contains("h") && (command.args[1].indexOf("h") == command.args[1].lastIndexOf("h"))) ? Integer.parseInt(command.args[1].replaceFirst("((hours)(?!hours)|(hrs)(?!hrs)|(hr)(?!hr)|(h)(?!h))(([0-9]+((minutes)(?!minutes)|(mins)(?!mins)|(min)(?!min)|(m)(?!m))|[0-9]+))?", "")) * 60 : -1;
                            cmdArgMinutes = (command.args[1].contains("m") && (command.args[1].indexOf("m") == command.args[1].lastIndexOf("m"))) ? Integer.parseInt(command.args[1].replaceFirst("[0-9]+((hours)(?!hours)|(hrs)(?!hrs)|(hr)(?!hr)|(h)(?!h))", "").replaceFirst("((minutes)(?!minutes)|(mins)(?!mins)|(min)(?!min)|(m)(?!m))", "")) : -1;
                            if(cmdArgHours == -1 && cmdArgMinutes == -1) {
                                throw new NumberFormatException();
                            }
                            if (command.args.length > 2) {
                                cmdArgReason = Util.spaceSeparatedString(Arrays.copyOfRange(command.args, 2, command.args.length)).replaceAll("/n", "\n");
                            }

                            if(cmdArgHours == -1) {
                                cmdArgHours = 0;
                            }
                        }
                        catch (NumberFormatException e) {
                            if(command.args.length > 1) {
                                if(command.args.length > 2) {
                                    cmdArgReason = Util.spaceSeparatedString(Arrays.copyOfRange(command.args, 2, command.args.length)).replaceAll("/n", "\n");
                                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "Invalid time given! Defaulting to 5 minutes!");
                                }
                                else {
                                    cmdArgReason = Util.spaceSeparatedString(Arrays.copyOfRange(command.args, 1, command.args.length)).replaceAll("/n", "\n");
                                }
                            }

                            if(cmdArgHours == -1) {
                                cmdArgHours = 0;
                            }
                            if(cmdArgMinutes == -1) {
                                cmdArgMinutes = 5;
                            }
                        }
                    }
                }

                Member member = api.resolveMember(cmdArgUsername);
                if(member != null) {
                    int hoursBanned = (int)(Math.floor((cmdArgHours + cmdArgMinutes)/60));
                    int minutesBanned = (cmdArgMinutes % 60);
                    api.banMember(member.senderID,cmdArgReason + "\nYou can rejoin in: " + hoursBanned + "hr " + minutesBanned + "mins!\nBanned by: " + command.message.senderName);
                    unbanUpdater.schedule(() -> api.unBanMember(member.senderID, member.senderName), (cmdArgMinutes + cmdArgHours), TimeUnit.MINUTES);

                    Util.dataBase.addBanRecord(cmdSenderID, uniqueName, member.senderID, member.senderName, cmdArgReason, hoursBanned*60+minutesBanned);
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "tmpban", channelName, stringArgs);
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "Successfully banned " + cmdArgUsername + " for '" + (cmdArgMinutes + cmdArgHours) + "mins' : '" + hoursBanned + "hr " + minutesBanned + "mins'");
                }
                else {
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "~*[ERROR: Invalid user!]*~\n*Details:* Could not temp ban *'" + cmdArgUsername + "'*");
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "tmpban", channelName, "Unable to temporarily ba " + cmdArgUsername);
                }
            }
            break;
            case "quit":
            {
                api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Shut down command executed!]*~\n*Command Sender:* [ " + senderName + " ]");
                Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "quit", channelName, stringArgs);
                saveStats();
                Util.dataBase.closeDB();
                System.exit(0);
            }
            break;
            case "send":
            {
                String channel = (command.args != null && command.args.length > 0) ? command.args[0] : "";
                String body = (command.args != null && command.args.length > 1) ? (Util.spaceSeparatedString(Arrays.copyOfRange(command.args, 1, command.args.length)).replaceAll("/n", "\n")) : "";
                if(body.startsWith(".")) break;
                if (api.resolveChannel(channel) != null && !body.equals("")) {
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "send", channelName, stringArgs);
                    api.postMessage(api.resolveChannel(channel), body);
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "Your message has been sent to #" + channel);
                }
                else {
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "~*[ERROR: Invalid channel!]*~\n*Details:* Could not send message to channel *'" + channel + "'*");
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "quit", channelName, "Failed to send message to " + channel + "!");
                }
            }
            break;

            case "sender":
            {
                api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Executing sender command]*~\n*Command Sender:* [ " + command.message.senderName + " ]");
                api.postMessage(api.resolveChannelUUID(command.message.channelUUID), "Hello there " + api.mention(command.message.senderName) + "!");
                Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "sender", channelName, stringArgs);
            }
            break;

            case "mode":
            {
                String cmdArgMode = (command.args != null && command.args.length > 0) ? command.args[0] : "-1";
                String cmdArgStatus = (command.args != null && command.args.length > 1) ? command.args[1] : "-1";

                switch(cmdArgMode) {
                    case "hide": {
                        if(cmdArgStatus.equalsIgnoreCase("status")) {
                            if(Util.unhidden) {
                                Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "mode status", channelName, stringArgs);
                                api.postMessage(api.resolveChannel(Util.botcmdChannel), "Bot is currently unhidden, and is able to take actions upon users!");
                            }
                            else {
                                Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "mode status", channelName, stringArgs);
                                api.postMessage(api.resolveChannel(Util.botcmdChannel), "Bot is currently hidden, and is unable to take actions upon users, but is logging actions as normal!");
                            }
                        }
                        else {
                            if (Util.unhidden) { //hide
                                Util.unhidden = false;
                                Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "mode hide", channelName, stringArgs);
                                api.postMessage(api.resolveChannel(Util.botcmdChannel), "Bot has been suppressed from invoking actions, but will continue logging as normal.");
                            }
                            else { //unhide
                                Util.unhidden = true;
                                Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "mode unhide", channelName, stringArgs);
                                api.postMessage(api.resolveChannel(Util.botcmdChannel), "Bot has been unsuppressed from invoking actions, and will continue logging as normal.");
                            }
                        }
                    }
                    break;
                }
            }
            break;

            case "resolve":
            {
                String resolve = (command.args != null && command.args.length > 0) ? command.args[0] : "";
                String resolveThis = (command.args != null && command.args.length > 1) ? command.args[1] : "";
                if (!(resolve.equals("") || resolveThis.equals("")) && (resolve.equalsIgnoreCase("member") || resolve.equalsIgnoreCase("user"))) {
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "resolve member", channelName, stringArgs);
                    if (api.resolveMember(resolveThis) != null) {
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "*Username '" + resolveThis + "':* " + api.resolveMember(resolveThis).senderName + "\n*Nickname:* " + api.resolveMember(resolveThis).username);
                    }
                    else {
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "*Username:* Unable to resolve '" + resolveThis + "' to a username!");
                    }
                }
                else if (!(resolve.equals("") || resolveThis.equals("")) && (resolve.equalsIgnoreCase("userID") || resolve.equalsIgnoreCase("memberID"))) {
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "resolve userid", channelName, stringArgs);
                    if (api.resolveMember(resolveThis) != null) {
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "*User ID '" + resolveThis + "':* " + api.resolveMember(resolveThis).senderID);
                    }
                    else {
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "*User ID:* Unable to resolve '" + resolveThis + "' to a user ID!");
                    }
                }
                else if (!(resolve.equals("") || resolveThis.equals("")) && (resolve.equalsIgnoreCase("userRole") || resolve.equalsIgnoreCase("memberRole") || resolve.equalsIgnoreCase("role"))) {
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "resolve user role", channelName, stringArgs);
                    if (api.resolveMember(resolveThis) != null) {
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "*User role '" + resolveThis + "':* " + api.resolveMember(resolveThis).bestRole);
                    }
                    else {
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "*User role:* Unable to resolve '" + resolveThis + "' to a user role!");
                    }
                }
                else if (!(resolve.equals("") || resolveThis.equals("")) && resolve.equalsIgnoreCase("channel")) {
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "resolve channel", channelName, stringArgs);
                    if (api.resolveChannel(resolveThis) != null) {
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "*Channel name '" + resolveThis + "':* " + api.resolveChannel(resolveThis).groupTitle);
                    }
                    else {
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "*Channel name:* Unable to resolve '" + resolveThis + "' to a channel name!");
                    }
                }
                else if (!(resolve.equals("") || resolveThis.equals("")) && resolve.equalsIgnoreCase("channelID")) {
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "resolve channelid", channelName, stringArgs);
                    if (api.resolveChannel(resolveThis) != null) {
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "*Channel ID '" + resolveThis + "':* " + api.resolveChannel(resolveThis).groupID);
                    }
                    else {
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "*Channel ID:* Unable to resolve '" + resolveThis + "' to a channel ID!");
                    }
                }
                else {
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "resolve", channelName, "Resolve failed to resolve any information!");
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "Unresolved:* Unable to resolve anything from given the parameters!");
                }
            }
            break;

            case "help":
            {
                int index = 1;

                Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "help", channelName, stringArgs);
                if(!(command.args == null) && (command.args.length == 1))
                {
                    try  {
                        index = Integer.parseInt(command.args[0]);
                    }
                    catch(NumberFormatException e)  {
                        index = command.args.length + 5;
                    }
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

                    api.postMessage(api.resolveChannel(Util.botcmdChannel), helpMsg);
                }
                else
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "The requested page does not exist!");
                Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "help", channelName, "Failed to get page " + index + "!");
            }
            break;

            case "shrug":
            {
                String shrug = "not shrug";
                try {
                    shrug = new String("¯\\_(ツ)_/¯".getBytes(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    Util.dataBase.addLogMessage("SEVERE", "UTF-8 is not supported", sw.toString(), "");

                }
                api.postMessage(api.resolveChannelUUID(command.message.channelUUID), shrug);
            }
            break;

            case "banLeft":
            {
                int cmdArgUserID = -1;
                String cmdArgReason = (command.args != null && command.args.length > 1 ? Util.spaceSeparatedString(Arrays.copyOfRange(command.args, 1, command.args.length)).replaceAll("/n", "\n") : "No reason given!" );
                try {
                    cmdArgUserID = (command.args != null && command.args.length > 0) ? Integer.parseInt(command.args[0]) : -1;
                    if(cmdArgUserID != -1) {
                        Util.dataBase.addBanRecord(cmdSenderID, uniqueName, cmdArgUserID, cmdArgUserID + "", cmdArgReason, 0);
                        Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "banLeft", channelName, stringArgs);
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "The user with the user ID of " + cmdArgUserID + " has been prevented from joining back onto the server!");
                        api.banMember(cmdArgUserID, "No reason provided!");
                    }
                    else {
                        throw new NumberFormatException();
                    }
                }
                catch (NumberFormatException e) {
                    String uuid = cmdArgUserID + "";
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "~*[ERROR: Invalid user!]*~\n*Details:* Could not ban *'" + uuid + "'*");
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "banLeft", channelName, "Failed to ban " + uuid);
                }
            }
            break;
            case "unbanLeft":
            {
                int cmdArgUserID = -1;
                try {
                    cmdArgUserID = (command.args != null && command.args.length > 0) ? Integer.parseInt(command.args[0]) : -1;
                    if(cmdArgUserID != -1) {
                        Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "unbanLeft", channelName, stringArgs);
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "The user with the user ID of " + cmdArgUserID + " has been allowed to joining back onto the server!");
                        api.unBanMember(cmdArgUserID, "unavailable");
                    }
                    else {
                        throw new NumberFormatException();
                    }
                }
                catch (NumberFormatException e) {
                    String uuid = cmdArgUserID + "";
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "~*[ERROR: Invalid user!]*~\n*Details:* Could not ban *'" + uuid + "'*");
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "unbanLeft", channelName, "Failed to unban " + uuid);
                }
            }
            case "addWarning":
            {
                String cmdArgUsername = "";
                String cmdArgReason = "";

                if ((command.args != null) && (command.args.length > 0)) {
                    cmdArgUsername = command.args[0];
                    if(command.args.length > 1) {
                        cmdArgReason = Util.spaceSeparatedString(Arrays.copyOfRange(command.args, 1, command.args.length)).replaceAll("/n", "\n");
                    }
                }

                Member cmdArgMember = api.resolveMember(cmdArgUsername);
                if(cmdArgMember != null) {
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "addWarning", channelName, stringArgs);
                    if(Util.canRemoveUser(cmdArgMember.senderID)) {
                        //api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Manual warning]*~\n*Username:* [ " + api.mention(cmdArgUsername) + " ]\n*Reason:* " + cmdArgReason + "\n*Total Warnings:* " + Util.removeUserWhen.get(member.senderID));
                        Util.dataBase.addWarning(cmdSenderID, uniqueName, cmdArgMember.senderID, cmdArgMember.senderName, cmdArgReason, "Kicked");
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "Warning added to " + cmdArgUsername + ", user was removed");
                    } else {
                        Util.dataBase.addWarning(cmdSenderID, uniqueName, cmdArgMember.senderID, cmdArgMember.senderName, cmdArgReason, "Warned");
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "Warning added to " + cmdArgUsername);
                    }
                } else {
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "~*[ERROR: Invalid user!]*~\n*Details:* Could not add warning to *'" + cmdArgUsername + "'*");
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "addWarning", channelName, "Unable to issue a warning to " + cmdArgUsername + "!");
                }
            }
            break;
            case "listWarning":
            {
                String cmdArgUsername = "";
                if ((command.args != null) && (command.args.length > 0)) {
                    cmdArgUsername = command.args[0];
                }

                Member member = api.resolveMember(cmdArgUsername);
                if(member != null) {
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "listWarnings", channelName, stringArgs);
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "~*[Warnings]*~\n*Username:* [ " + api.mention(cmdArgUsername) + " ]" + "\n*Total Warnings:* " + Util.removeUserWhen.get(member.senderID));
                } else {
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "~*[ERROR: Invalid user!]*~\n*Details:* Could not show warnings of *'" + cmdArgUsername + "'*");
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "listWarnings", channelName, "Failed to get any warnings from " + cmdArgUsername);
                }
            }
            break;
            case "showStats":
            {
                Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "showStats", channelName, stringArgs);
                api.postMessage(api.resolveChannel(Util.botstatChannel), "[*Stats:* ~" + new Date().toString() + "~ ]"
                        + "\n-*I===============================I*-"
                        + "\n*Net users joined:*        |     " + api.userJoins
                        + "\n*Unique joins:*          |     " + api.userUniqueJoins
                        + "\n*Messages posted:*   |     " + api.messages
                        + "\n*Removed users:*      |     " + api.removedUsers
                        + "\n*Left users:*                |     " + api.leftUsers);

            }
            break;
            case "loadStats":
            {
                Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "loadStats", channelName, stringArgs);
                loadStats();
                api.postMessage(api.resolveChannel(Util.botcmdChannel), "Stats loaded");
            }
            break;
        }
    }

    private static void loadStats() {
        try {
            Scanner myReader = new Scanner(new File("stats.sav"));
            api.userJoins = Long.parseLong(myReader.nextLine());
            api.userUniqueJoins = Long.parseLong(myReader.nextLine());
            api.messages = Long.parseLong(myReader.nextLine());
            api.removedUsers = Long.parseLong(myReader.nextLine());
            api.leftUsers = Long.parseLong(myReader.nextLine());
            myReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveStats() {
        try {
            PrintWriter writer = new PrintWriter("stats.sav");
            writer.println(api.userJoins);
            writer.println(api.userUniqueJoins);
            writer.println(api.messages);
            writer.println(api.removedUsers);
            writer.println(api.leftUsers);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
