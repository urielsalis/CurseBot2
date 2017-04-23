package me.urielsalis.cursebot.extensions.ModCommands;

import me.urielsalis.cursebot.api.*;
import me.urielsalis.cursebot.events.CommandEvent;
import me.urielsalis.cursebot.extensions.Extension;
import me.urielsalis.cursebot.extensions.ExtensionApi;
import me.urielsalis.cursebot.extensions.ExtensionHandler;
import me.urielsalis.cursebot.extensions.Handle;

import java.io.*;
import java.sql.SQLException;
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
        Message message = command.getMessage();
        Member cmdSender = api.resolveMember(message.senderName);
        Channel cmdChannel = api.resolveChannelUUID(message.channelUUID);

        //:: Command sender information
        //:: Sender Information
        String uniqueName = cmdSender.username;     //- The user's unique name. This is special to the user
        String displayName = cmdSender.displayName; //- The user's display name in a server.
        String senderName = cmdSender.senderName;   //- The user's nickname/name to refernce a command sent by
        long cmdSenderID = cmdSender.senderID;           //- The user's unique numeric id. This is special to the user
        long cmdSenderRole = cmdSender.bestRole;         //- The best role a user currently holds. infinity to 1.

        //:: Channel Information
        String channelName = cmdChannel.getGroupTitle(); //- The name of the channel command was sent in.
        String channelID = cmdChannel.getGroupID();      //- The special ID of a channel in which to refer to a channel.
        Channel botLogChannel = api.resolveChannel(Util.botlogChannel);
        Channel botCommandChannel = api.resolveChannel(Util.botcmdChannel);

        //:: Command Information
        String[] args = command.getArgs();
        String stringArgs = (args != null) ? Util.spaceSeparatedString(args) : null;

        if(!Util.isUserAuthorized(api, api.resolveMember(senderName))) return;
        switch (command.getCommand()) {
            case "delete":
            {
                //:: Command Argument variables
                String cmdArgChannelName = "";
                String cmdArgUserName = "";

                int cmdArgDeletMessagesX = 1;
                boolean canDelete = true;

                //:: assign arguments to variables
                if(args != null && args.length > 0) {
                    cmdArgChannelName = args[0];
                    if (args.length > 1) {
                        cmdArgUserName = args[1];
                        if (args.length > 2) {
                            try {
                                cmdArgDeletMessagesX = Integer.parseInt(args[2]);
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

                        for (int i = channel.getMessages().size() - 1; i >= 0; i--) {
                            message1 = (Message) channel.getMessages().toArray()[i];
                            if (counter >= cmdArgDeletMessagesX) break;
                            if (Util.equals(cmdArgUserName, message1.senderName)) {
                                api.deleteMessage(message1);
                                counter++;
                            }
                        }
                        api.postMessage(botCommandChannel, cmdArgDeletMessagesX + " of " + api.mention(cmdArgUserName) + "'s latest messages have been successfuly deleted from the channel " + cmdArgChannelName);
                    }
                    else {
                        api.postMessage(botCommandChannel, "Specified channel does not exist or could not be deleted from!");
                        Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "delete", channelName, stringArgs);
                    }
                }
                else {
                    api.postMessage(botCommandChannel, "~*[ERROR: Invalid command syntax!]*~\n*Command:* .delete");
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "delete", channelName, stringArgs);
                }
            }
            break;
            case "kick":
            {
                String cmdArgUsername = (args != null && args.length > 0) ? args[0] : "";
                Member cmdArgMember = api.resolveMember(cmdArgUsername);

                if(cmdArgMember != null && !Util.isUserAuthorized(api, cmdArgMember)) {
                    api.kickUser(api.resolveMember(cmdArgUsername));
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "kick", channelName, stringArgs);
                    try {
                        Util.dataBase.addWarning(cmdSenderID, uniqueName, cmdArgMember.senderID, cmdArgMember.senderName, "kick", "User kicked from server!");
                    } catch (Exception e) {
                        api.postMessage(botLogChannel, "Database error while adding warning");
                    }
                    api.postMessage(botCommandChannel, "The user " + cmdArgUsername + " was successfully removed from the server!");
                }
                else {
                    api.postMessage(botCommandChannel, "~*[ERROR: Invalid user!]*~\n*Details:* Could not kick *'" + cmdArgUsername + "'*");
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "kick", channelName, stringArgs);
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

                if(args != null && args.length > 0) {
                    cmdArgUsername = args[0];
                    if(args.length > 1) {
                        try {
                            args[1] = args[1].toLowerCase();
                            cmdArgHours = (args[1].contains("h") && (args[1].indexOf("h") == args[1].lastIndexOf("h"))) ? Integer.parseInt(args[1].replaceFirst("((hours)(?!hours)|(hrs)(?!hrs)|(hr)(?!hr)|(h)(?!h))(([0-9]+((minutes)(?!minutes)|(mins)(?!mins)|(min)(?!min)|(m)(?!m))|[0-9]+))?", "")) * 60 : -1;
                            cmdArgMinutes = (args[1].contains("m") && (args[1].indexOf("m") == args[1].lastIndexOf("m"))) ? Integer.parseInt(args[1].replaceFirst("[0-9]+((hours)(?!hours)|(hrs)(?!hrs)|(hr)(?!hr)|(h)(?!h))", "").replaceFirst("((minutes)(?!minutes)|(mins)(?!mins)|(min)(?!min)|(m)(?!m))", "")) : -1;
                            if(cmdArgHours == -1 && cmdArgMinutes == -1) {
                                throw new NumberFormatException();
                            }
                            if (args.length > 2) {
                                cmdArgReason = Util.spaceSeparatedString(Arrays.copyOfRange(args, 2, args.length)).replaceAll("/n", "\n");
                            }

                            if(cmdArgHours == -1) {
                                cmdArgHours = 0;
                            }
                        }
                        catch (NumberFormatException e) {
                            if(args.length > 1) {
                                if(args.length > 2) {
                                    cmdArgReason = Util.spaceSeparatedString(Arrays.copyOfRange(args, 2, args.length)).replaceAll("/n", "\n");
                                    api.postMessage(botCommandChannel, "Invalid time given! Defaulting to 5 minutes!");
                                }
                                else {
                                    cmdArgReason = Util.spaceSeparatedString(Arrays.copyOfRange(args, 1, args.length)).replaceAll("/n", "\n");
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
                    api.banMember(member.senderID,cmdArgReason + "\nYou can rejoin in: " + hoursBanned + "hr " + minutesBanned + "mins!\nBanned by: " + senderName);
                    unbanUpdater.schedule(() -> api.unBanMember(member.senderID, member.senderName), (cmdArgMinutes + cmdArgHours), TimeUnit.MINUTES);

                    try {
                        Util.dataBase.addBanRecord(cmdSenderID, uniqueName, member.senderID, member.senderName, cmdArgReason, hoursBanned*60+minutesBanned);
                    } catch (Exception e) {
                        e.printStackTrace();
                        api.postMessage(botLogChannel, "Database error while adding ban");
                    }
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "tmpban", channelName, stringArgs);
                    api.postMessage(botCommandChannel, "Successfully banned " + cmdArgUsername + " for '" + (cmdArgMinutes + cmdArgHours) + "mins' : '" + hoursBanned + "hr " + minutesBanned + "mins'");
                }
                else {
                    api.postMessage(botCommandChannel, "~*[ERROR: Invalid user!]*~\n*Details:* Could not temp ban *'" + cmdArgUsername + "'*");
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "tmpban", channelName, stringArgs);
                }
            }
            break;
            case "quit":
            {
                api.postMessage(botLogChannel, "~*[Shut down command executed!]*~\n*Command Sender:* [ " + senderName + " ]");
                Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "quit", channelName, stringArgs);
                saveStats();
                Util.dataBase.closeDB();
                System.exit(0);
            }
            break;
            case "send":
            {
                String channel = (args != null && args.length > 0) ? args[0] : "";
                String body = (args != null && args.length > 1) ? (Util.spaceSeparatedString(Arrays.copyOfRange(args, 1, args.length)).replaceAll("/n", "\n")) : "";
                if(body.startsWith(".")) break;
                if (api.resolveChannel(channel) != null && !body.equals("")) {
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "send", channelName, stringArgs);
                    api.postMessage(cmdChannel, body);
                    api.postMessage(botCommandChannel, "Your message has been sent to #" + channel);
                }
                else {
                    api.postMessage(botCommandChannel, "~*[ERROR: Invalid channel!]*~\n*Details:* Could not send message to channel *'" + channel + "'*");
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "quit", channelName, stringArgs);
                }
            }
            break;

            case "sender":
            {
                api.postMessage(botLogChannel, "~*[Executing sender command]*~\n*Command Sender:* [ " + senderName + " ]");
                api.postMessage(cmdChannel, "Hello there " + api.mention(senderName) + "!");
                Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "sender", channelName, stringArgs);
            }
            break;

            case "mode":
            {
                String cmdArgMode = (args != null && args.length > 0) ? args[0] : "-1";
                String cmdArgStatus = (args != null && args.length > 1) ? args[1] : "-1";

                switch(cmdArgMode) {
                    case "hide": {
                        if(cmdArgStatus.equalsIgnoreCase("status")) {
                            if(Util.unhidden) {
                                Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "mode status", channelName, stringArgs);
                                api.postMessage(botCommandChannel, "Bot is currently unhidden, and is able to take actions upon users!");
                            }
                            else {
                                Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "mode status", channelName, stringArgs);
                                api.postMessage(botCommandChannel, "Bot is currently hidden, and is unable to take actions upon users, but is logging actions as normal!");
                            }
                        }
                        else {
                            if (Util.unhidden) { //hide
                                Util.unhidden = false;
                                Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "mode hide", channelName, stringArgs);
                                api.postMessage(botCommandChannel, "Bot has been suppressed from invoking actions, but will continue logging as normal.");
                            }
                            else { //unhide
                                Util.unhidden = true;
                                Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "mode unhide", channelName, stringArgs);
                                api.postMessage(botCommandChannel, "Bot has been unsuppressed from invoking actions, and will continue logging as normal.");
                            }
                        }
                    }
                    break;
                }
            }
            break;

            case "resolve":
            {
                String resolve = (args != null && args.length > 0) ? args[0] : "";
                String resolveThis = (args != null && args.length > 1) ? args[1] : "";
                if (!(resolve.equals("") || resolveThis.equals("")) && (resolve.equalsIgnoreCase("member") || resolve.equalsIgnoreCase("user"))) {
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "resolve member", channelName, stringArgs);
                    if (api.resolveMember(resolveThis) != null) {
                        api.postMessage(botCommandChannel, "*Username '" + resolveThis + "':* " + api.resolveMember(resolveThis).senderName + "\n*Nickname:* " + api.resolveMember(resolveThis).username);
                    }
                    else {
                        api.postMessage(botCommandChannel, "*Username:* Unable to resolve '" + resolveThis + "' to a username!");
                    }
                }
                else if (!(resolve.equals("") || resolveThis.equals("")) && (resolve.equalsIgnoreCase("userID") || resolve.equalsIgnoreCase("memberID"))) {
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "resolve userid", channelName, stringArgs);
                    if (api.resolveMember(resolveThis) != null) {
                        api.postMessage(botCommandChannel, "*User ID '" + resolveThis + "':* " + api.resolveMember(resolveThis).senderID);
                    }
                    else {
                        api.postMessage(botCommandChannel, "*User ID:* Unable to resolve '" + resolveThis + "' to a user ID!");
                    }
                }
                else if (!(resolve.equals("") || resolveThis.equals("")) && (resolve.equalsIgnoreCase("userRole") || resolve.equalsIgnoreCase("memberRole") || resolve.equalsIgnoreCase("role"))) {
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "resolve user role", channelName, stringArgs);
                    if (api.resolveMember(resolveThis) != null) {
                        api.postMessage(botCommandChannel, "*User role '" + resolveThis + "':* " + api.resolveMember(resolveThis).bestRole);
                    }
                    else {
                        api.postMessage(botCommandChannel, "*User role:* Unable to resolve '" + resolveThis + "' to a user role!");
                    }
                }
                else if (!(resolve.equals("") || resolveThis.equals("")) && resolve.equalsIgnoreCase("channel")) {
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "resolve channel", channelName, stringArgs);
                    if (api.resolveChannel(resolveThis) != null) {
                        api.postMessage(botCommandChannel, "*Channel name '" + resolveThis + "':* " + api.resolveChannel(resolveThis).getGroupID());
                    }
                    else {
                        api.postMessage(botCommandChannel, "*Channel name:* Unable to resolve '" + resolveThis + "' to a channel name!");
                    }
                }
                else if (!(resolve.equals("") || resolveThis.equals("")) && resolve.equalsIgnoreCase("channelID")) {
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "resolve channelid", channelName, stringArgs);
                    if (api.resolveChannel(resolveThis) != null) {
                        api.postMessage(botCommandChannel, "*Channel ID '" + resolveThis + "':* " + api.resolveChannel(resolveThis).getGroupID());
                    }
                    else {
                        api.postMessage(botCommandChannel, "*Channel ID:* Unable to resolve '" + resolveThis + "' to a channel ID!");
                    }
                }
                else {
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "resolve", channelName, stringArgs);
                    api.postMessage(botCommandChannel, "Unresolved:* Unable to resolve anything from given the parameters!");
                }
            }
            break;

            case "help": {
                int index = 1;

                Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "help", channelName, stringArgs);
                if (!(args == null) && (args.length == 1)) {
                    try {
                        index = Integer.parseInt(args[0]);
                    } catch (NumberFormatException e) {
                        index = args.length + 5;
                    }
                }

                StringBuilder helpMsg = new StringBuilder();
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
                        "\n - Adds a link to the link filter."};

                int maxPages = (int) Math.ceil((double) commandList.length / 12.0);

                String header = "~*[HELP: Commands page (" + index + " / " + maxPages + ")]*~:\n-*I===============================I*-";

                if (!(index > maxPages)) {
                    helpMsg.append(header);
                    for (int i = (((index * 11) - 11)); i <= (index * 11); i++) {
                        if (index > 1 && i == (((index * 11) - 11)))
                            i++;

                        if (i == commandList.length)
                            break;
                        else
                            helpMsg.append(commandList[i]);
                    }

                    api.postMessage(botCommandChannel, helpMsg.toString());
                } else {
                    api.postMessage(botCommandChannel, "The requested page does not exist!");
                }
            }
            break;

            case "shrug":
            {
                String shrug = "not shrug";
                try {
                    shrug = new String("¯\\_(ツ)_/¯".getBytes(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    Util.dataBase.addLogMessage("SEVERE", "UTF-8 is not supported", e);

                }
                api.postMessage(cmdChannel, shrug);
            }
            break;

            case "banLeft":
            {
                Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "banLeft", channelName, stringArgs);
                int cmdArgUserID = -1;
                String cmdArgReason = (args != null && args.length > 1 ? Util.spaceSeparatedString(Arrays.copyOfRange(args, 1, args.length)).replaceAll("/n", "\n") : "No reason given!" );
                try {
                    cmdArgUserID = (args != null && args.length > 0) ? Integer.parseInt(args[0]) : -1;
                    if(cmdArgUserID != -1) {
                        try {
                            Util.dataBase.addBanRecord(cmdSenderID, uniqueName, cmdArgUserID, cmdArgUserID + "", cmdArgReason, 0);
                        } catch (Exception e) {
                            e.printStackTrace();
                            api.postMessage(botLogChannel, "Database error while adding ban, user was banned");
                        }
                        api.postMessage(botCommandChannel, "The user with the user ID of " + cmdArgUserID + " has been prevented from joining back onto the server!");
                        api.banMember(cmdArgUserID, "No reason provided!");
                    }
                    else {
                        throw new NumberFormatException();
                    }
                }
                catch (NumberFormatException e) {
                    String uuid = cmdArgUserID + "";
                    api.postMessage(botCommandChannel, "~*[ERROR: Invalid user!]*~\n*Details:* Could not ban *'" + uuid + "'*");
                }
            }
            break;
            case "unbanLeft":
            {
                int cmdArgUserID = -1;
                try {
                    cmdArgUserID = (args != null && args.length > 0) ? Integer.parseInt(args[0]) : -1;
                    if(cmdArgUserID != -1) {
                        Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "unbanLeft", channelName, stringArgs);
                        api.postMessage(botCommandChannel, "The user with the user ID of " + cmdArgUserID + " has been allowed to joining back onto the server!");
                        api.unBanMember(cmdArgUserID, "unavailable");
                    }
                    else {
                        throw new NumberFormatException();
                    }
                }
                catch (NumberFormatException e) {
                    String uuid = cmdArgUserID + "";
                    api.postMessage(botCommandChannel, "~*[ERROR: Invalid user!]*~\n*Details:* Could not ban *'" + uuid + "'*");
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "unbanLeft", channelName, stringArgs);
                }
            }
            case "addWarning":
            {
                String cmdArgUsername = "";
                String cmdArgReason = "";

                if ((args != null) && (args.length > 0)) {
                    cmdArgUsername = args[0];
                    if(args.length > 1) {
                        cmdArgReason = Util.spaceSeparatedString(Arrays.copyOfRange(args, 1, args.length)).replaceAll("/n", "\n");
                    }
                }

                Member cmdArgMember = api.resolveMember(cmdArgUsername);
                Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "addWarning", channelName, stringArgs);
                if(cmdArgMember != null) {
                    if(Util.canRemoveUser(cmdArgMember.senderID)) {
                        try {
                            Util.dataBase.addWarning(cmdSenderID, uniqueName, cmdArgMember.senderID, cmdArgMember.senderName, cmdArgReason, "Kicked");
                            api.postMessage(botCommandChannel, "Warning added to " + cmdArgUsername + ", user was removed");
                        } catch (Exception e) {
                            e.printStackTrace();
                            api.postMessage(botCommandChannel, "Database error, nothing added");
                        }
                    } else {
                        try {
                            Util.dataBase.addWarning(cmdSenderID, uniqueName, cmdArgMember.senderID, cmdArgMember.senderName, cmdArgReason, "Warned");
                            api.postMessage(botCommandChannel, "Warning added to " + cmdArgUsername);
                        } catch (Exception e) {
                            e.printStackTrace();
                            api.postMessage(botCommandChannel, "Database error, nothing added");
                        }
                    }
                } else {
                    api.postMessage(botCommandChannel, "~*[ERROR: Invalid user!]*~\n*Details:* Could not add warning to *'" + cmdArgUsername + "'*");
                }
            }
            break;
            case "listWarning":
            {
                String cmdArgUsername = "";
                if ((args != null) && (args.length > 0)) {
                    cmdArgUsername = args[0];
                }

                Member member = api.resolveMember(cmdArgUsername);
                Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "listWarnings", channelName, stringArgs);
                if(member != null) {
                    if (Util.removeUserWhen.get(member.senderID) == null) {
                        api.postMessage(botCommandChannel, "~*[Warnings]*~\n*Username:* [ " + api.mention(cmdArgUsername) + " ]" + "\n*Total Warnings:* 0");
                    }
                    else {
                        api.postMessage(botCommandChannel, "~*[Warnings]*~\n*Username:* [ " + api.mention(cmdArgUsername) + " ]" + "\n*Total Warnings:* " + Util.removeUserWhen.get(member.senderID));
                    }
                } else {
                    api.postMessage(botCommandChannel, "~*[ERROR: Invalid user!]*~\n*Details:* Could not show warnings of *'" + cmdArgUsername + "'*");
                }
            }
            break;
            case "ipBan":
            {
                String cmdArgUsername = "";
                if ((args != null) && (args.length > 0)) {
                    cmdArgUsername = args[0];
                }

                Member member = api.resolveMember(cmdArgUsername);
                if(member != null) {
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "ipBan", channelName, stringArgs);
                    api.postMessage(botCommandChannel, "ip banned " + api.mention(cmdArgUsername));
                    api.banIpMember(member);
                } else {
                    api.postMessage(botCommandChannel, "couldnt ip ban " + cmdArgUsername);
                    Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "ipBan", channelName, stringArgs);
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
                api.postMessage(botCommandChannel, "Stats loaded");
            }
            break;
            case "joined":
            {
                String cmdArgUsername = "";
                if ((args != null) && (args.length > 0)) {
                    cmdArgUsername = args[0];
                }

                Member member = api.resolveMember(cmdArgUsername);
                Util.dataBase.addCommandHistory(cmdSenderID, uniqueName, "joined", channelName, stringArgs);
                if(member != null) {
                    if(member.joined==0) {
                        api.postMessage(botCommandChannel, "~*[Joined]*~\n*Username:* [ " + api.mention(cmdArgUsername) + " ]" + "\n*Joined on:* Before bot was turned on!");
                    } else {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(member.joined);
                        String date = calendar.getTime().toString();
                        api.postMessage(botCommandChannel, "~*[Joined]*~\n*Username:* [ " + api.mention(cmdArgUsername) + " ]" + "\n*Joined on:* " + date);
                    }
                } else {
                    api.postMessage(botCommandChannel, "~*[ERROR: Invalid user!]*~\n*Details:* Could not show join time of *'" + cmdArgUsername + "'*");
                }

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
