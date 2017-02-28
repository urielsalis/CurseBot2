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

/**
 * Created by urielsalis on 1/28/2017
 */
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

                    if (channel != null) {
                        StringBuilder builder = new StringBuilder();
                        api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Executing delete command!]*~\n*Command Sender:* [ " + command.message.senderName + " ]\n-*I===============================I*-");
                        for (int i = channel.messages.size() - 1; i >= 0; i--) {
                            message1 = (Message) channel.messages.toArray()[i];
                            if (counter >= count) break;
                            if (Util.equals(username, message1.senderName)) {
                                if (message1.isPM) {
                                    builder.append("[ " + api.mention(message1.senderName) + " ]: ```\"" + message1.body.replaceAll("\\s+|\\n", " ") + "\"```");
                                } else {
                                    builder.append("< " + api.mention(message1.senderName) + " >: ```\"" + message1.body.replaceAll("\\s+|\\n", " ") + "\"```");
                                }
                                api.deleteMessage(message1);
                                counter++;
                            }
                        }
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), count + " of " + api.mention(username) + "'s latest messages have been successfuly deleted from the channel " + channelName);
                        api.postMessage(api.resolveChannel(Util.botlogChannel), "Messages deleted: " + builder.toString());
                    }
                    else {
                        api.postMessage(api.resolveChannelUUID(Util.botcmdChannel), "Specified channel does not exist or could not be deleted from!");
                    }
                }
                else {
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "~*[ERROR: Invalid command syntax!]*~\n*Command:* .delete");
                }
            }
            break;
            case "kick":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                String username = (command.args != null && command.args.length > 0) ? command.args[0] : "";

                if(api.resolveMember(username) != null && !Util.isUserAuthorized(api, api.resolveMember(username))) {
                    api.kickUser(api.resolveMember(username));
                    api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Executing kick command!]*~\n*Command Sender:* [ " + command.message.senderName + " ]\n*Kicked user:* " + username + ".");
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "The user " + username + " was successfully removed from the server!");
                }
                else {
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "~*[ERROR: Invalid user!]*~\n*Details:* Could not kick *'" + username + "'*");
                }
            }
            break;
            case "tmpban":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;


                boolean canBan = false;
                String username = "";
                int hours = 0;
                int minutes = 5;
                String reason = "No reason given!";

                if(command.args != null && command.args.length > 0) {
                    username = command.args[0];
                    if(command.args.length > 1) {
                        try {
                            command.args[1] = command.args[1].toLowerCase();
                            hours = (command.args[1].contains("h") && (command.args[1].indexOf("h") == command.args[1].lastIndexOf("h"))) ? Integer.parseInt(command.args[1].replaceFirst("((hours)(?!hours)|(hrs)(?!hrs)|(hr)(?!hr)|(h)(?!h))(([0-9]+((minutes)(?!minutes)|(mins)(?!mins)|(min)(?!min)|(m)(?!m))|[0-9]+))?", "")) * 60 : -1;
                            minutes = (command.args[1].contains("m") && (command.args[1].indexOf("m") == command.args[1].lastIndexOf("m"))) ? Integer.parseInt(command.args[1].replaceFirst("[0-9]+((hours)(?!hours)|(hrs)(?!hrs)|(hr)(?!hr)|(h)(?!h))", "").replaceFirst("((minutes)(?!minutes)|(mins)(?!mins)|(min)(?!min)|(m)(?!m))", "")) : -1;
                            if(hours == -1 && minutes == -1) {
                                throw new NumberFormatException();
                            }
                            if (command.args.length > 2) {
                                reason = Util.spaceSeparatedString(Arrays.copyOfRange(command.args, 2, command.args.length)).replaceAll("/n", "\n");
                            }

                            if(hours == -1) {
                                hours = 0;
                            }
                        }
                        catch (NumberFormatException e) {
                            if(command.args.length > 1) {
                                if(command.args.length > 2) {
                                    reason = Util.spaceSeparatedString(Arrays.copyOfRange(command.args, 2, command.args.length)).replaceAll("/n", "\n");
                                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "Invalid time given! Defaulting to 5 minutes!");
                                }
                                else {
                                    reason = Util.spaceSeparatedString(Arrays.copyOfRange(command.args, 1, command.args.length)).replaceAll("/n", "\n");
                                }
                            }

                            if(hours == -1) {
                                hours = 0;
                            }
                            if(minutes == -1) {
                                minutes = 5;
                            }
                        }
                    }
                }

                Member member = api.resolveMember(username);
                if(member != null) {
                    int hoursBanned = (int)(Math.floor((hours + minutes)/60));
                    int minutesBanned = (minutes % 60);
                    api.banMember(member.senderID,reason + "\nYou can rejoin in: " + hoursBanned + "hr " + minutesBanned + "mins!\nBanned by: " + command.message.senderName);
                    unbanUpdater.schedule(() -> api.unBanMember(member.senderID, member.senderName), (minutes + hours), TimeUnit.MINUTES);
                    api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Executing ban user command!]*~\n*Command Sender:* [ " + command.message.senderName + " ]\n*Banned user:* " + username + ".\n*Reason:* \"" + reason + "\"\n*Timeout:* '" + (minutes + hours) + "mins' : '" + hoursBanned + "hr " + minutesBanned + "mins'");
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "Successfully banned " + username + " for '" + (minutes + hours) + "mins' : '" + hoursBanned + "hr " + minutesBanned + "mins'");
                }
                else {
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "~*[ERROR: Invalid user!]*~\n*Details:* Could not temp ban *'" + username + "'*");
                }
            }
            break;
            case "quit":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Shut down command executed!]*~\n*Command Sender:* [ " + command.message.senderName + " ]");
                saveStats();
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
                    api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Executing send command]*~\n*Command Sender:* [ " + command.message.senderName + " ]\n*Sending:* \"" + body + "\"");
                    api.postMessage(api.resolveChannel(channel), body);
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "Your message has been sent to #" + channel);
                }
                else {
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "~*[ERROR: Invalid channel!]*~\n*Details:* Could not send message to channel *'" + channel + "'*");
                }
            }
            break;

            case "sender":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Executing sender command]*~\n*Command Sender:* [ " + command.message.senderName + " ]");
                api.postMessage(api.resolveChannelUUID(command.message.channelUUID), "Hello there " + api.mention(command.message.senderName) + "!");
            }
            break;

            case "mode":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;

                String mode = (command.args != null && command.args.length > 0) ? command.args[0] : "-1";
                String status = (command.args != null && command.args.length > 1) ? command.args[1] : "-1";

                switch(mode) {
                    case "hide": {
                        if(status.equalsIgnoreCase("status")) {
                            if(Util.unhidden) {
                                api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Executing Mode status]*~\nMode: unhidden");
                                api.postMessage(api.resolveChannel(Util.botcmdChannel), "Bot is currently unhidden, and is able to take actions upon users!");
                            }
                            else {
                                api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Executing Mode status]*~\nMode: hidden");
                                api.postMessage(api.resolveChannel(Util.botcmdChannel), "Bot is currently hidden, and is unable to take actions upon users, but is logging actions as normal!");
                            }
                        }
                        else {
                            if (Util.unhidden) { //hide
                                Util.unhidden = false;
                                api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Toggling Hide mode]*~\n*Command Sender:* [ " + command.message.senderName + " ]\n*Mode:* hidden");
                                api.postMessage(api.resolveChannel(Util.botcmdChannel), "Bot has been suppressed from invoking actions, but will continue logging as normal.");
                            }
                            else { //unhide
                                Util.unhidden = true;
                                api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Toggling Hide mode]*~\n*Command Sender:* [ " + command.message.senderName + " ]\n*Mode:* Unhidden");
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
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                String resolve = (command.args != null && command.args.length > 0) ? command.args[0] : "";
                String resolveThis = (command.args != null && command.args.length > 1) ? command.args[1] : "";
                if (!(resolve.equals("") || resolveThis.equals("")) && (resolve.equalsIgnoreCase("member") || resolve.equalsIgnoreCase("user"))) {
                    api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Executing resolving " + resolve + "]*~\n*Command Sender:* [ " + command.message.senderName + " ]");
                    if (api.resolveMember(resolveThis) != null) {
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "*Username '" + resolveThis + "':* " + api.resolveMember(resolveThis).senderName + "\n*Nickname:* " + api.resolveMember(resolveThis).username);
                    }
                    else {
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "*Username:* Unable to resolve '" + resolveThis + "' to a username!");
                    }
                }
                else if (!(resolve.equals("") || resolveThis.equals("")) && (resolve.equalsIgnoreCase("userID") || resolve.equalsIgnoreCase("memberID"))) {
                    api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Executing resolving " + resolve + "]*~\n*Command Sender:* [ " + command.message.senderName + " ]");
                    if (api.resolveMember(resolveThis) != null) {
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "*User ID '" + resolveThis + "':* " + api.resolveMember(resolveThis).senderID);
                    }
                    else {
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "*User ID:* Unable to resolve '" + resolveThis + "' to a user ID!");
                    }
                }
                else if (!(resolve.equals("") || resolveThis.equals("")) && (resolve.equalsIgnoreCase("userRole") || resolve.equalsIgnoreCase("memberRole") || resolve.equalsIgnoreCase("role"))) {
                    api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Executing resolving " + resolve + "]*~\n*Command Sender:* [ " + command.message.senderName + " ]");
                    if (api.resolveMember(resolveThis) != null) {
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "*User role '" + resolveThis + "':* " + api.resolveMember(resolveThis).bestRole);
                    }
                    else {
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "*User role:* Unable to resolve '" + resolveThis + "' to a user role!");
                    }
                }
                else if (!(resolve.equals("") || resolveThis.equals("")) && resolve.equalsIgnoreCase("channel")) {
                    api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Executing resolving " + resolve + "]*~\n*Command Sender:* [ " + command.message.senderName + " ]");
                    if (api.resolveChannel(resolveThis) != null) {
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "*Channel name '" + resolveThis + "':* " + api.resolveChannel(resolveThis).groupTitle);
                    }
                    else {
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "*Channel name:* Unable to resolve '" + resolveThis + "' to a channel name!");
                    }
                }
                else if (!(resolve.equals("") || resolveThis.equals("")) && resolve.equalsIgnoreCase("channelID")) {
                    api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Executing resolving " + resolve + "]*~\n*Command Sender:* [ " + command.message.senderName + " ]");
                    if (api.resolveChannel(resolveThis) != null) {
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "*Channel ID '" + resolveThis + "':* " + api.resolveChannel(resolveThis).groupID);
                    }
                    else {
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "*Channel ID:* Unable to resolve '" + resolveThis + "' to a channel ID!");
                    }
                }
                else {
                    api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Executing resolving " + resolve + "]*~\n*Command Sender:* [ " + command.message.senderName + " ]\n*Unresolved:* Unable to resolve anything in given the parameters!");
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "Unresolved:* Unable to resolve anything from given the parameters!");
                }
            }
            break;

            case "help":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                int index = 1;

                api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Executing help command]*~\n*Command Sender:* [ " + command.message.senderName + " ]");
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

                    api.postMessage(api.resolveChannel(Util.botcmdChannel), helpMsg);
                }
                else
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "The requested page does not exist!");
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
                        api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Executing banLeft command]*~\n*Command Sender:* [ " + command.message.senderName + " ]\n*Banned member not in the server:* " + userID);
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "The user with the user ID of " + userID + " has been prevented from joining back onto the server!");
                        api.banMember(userID, "No reason provided!");
                    }
                    else {
                        throw new NumberFormatException();
                    }
                }
                catch (NumberFormatException e) {
                    String uuid = userID + "";
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "~*[ERROR: Invalid user!]*~\n*Details:* Could not ban *'" + uuid + "'*");
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
                        api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Executing unbanLeft command]*~\n*Command Sender:* [ " + command.message.senderName + " ]\n*Unbanned member not in the server:* " + userID);
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "The user with the user ID of " + userID + " has been allowed to joining back onto the server!");
                        api.unBanMember(userID, "unavailable");
                    }
                    else {
                        throw new NumberFormatException();
                    }
                }
                catch (NumberFormatException e) {
                    String uuid = userID + "";
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "~*[ERROR: Invalid user!]*~\n*Details:* Could not ban *'" + uuid + "'*");
                }
            }
            case "addWarning":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                String username = "";
                String reason = "";
                if ((command.args != null) && (command.args.length > 0)) {
                    username = command.args[0];
                    if(command.args.length > 1) {
                        reason = Util.spaceSeparatedString(Arrays.copyOfRange(command.args, 1, command.args.length)).replaceAll("/n", "\n");
                    }
                }

                Member member = api.resolveMember(username);
                if(member != null) {
                    if(Util.canRemoveUser(member.senderID)) {
                        api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Manual warning]*~\n*Username:* [ " + api.mention(username) + " ]\n*Reason:* " + reason + "\n*Total Warnings:* " + Util.removeUserWhen.get(member.senderID));
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "Warning added to " + username + ", user was removed");
                    } else {
                        api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Manual warning]*~\n*Username:* [ " + api.mention(username) + " ]\n*Reason:* " + reason + "\n*Total Warnings:* " + Util.removeUserWhen.get(member.senderID));
                        api.postMessage(api.resolveChannel(Util.botcmdChannel), "Warning added to " + username);
                    }
                } else {
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "~*[ERROR: Invalid user!]*~\n*Details:* Could not add warning to *'" + username + "'*");
                }
            }
            break;
            case "listWarning":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
                String username = "";
                if ((command.args != null) && (command.args.length > 0)) {
                    username = command.args[0];
                }

                Member member = api.resolveMember(username);
                if(member != null) {
                    api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Warnings]*~\n*Username:* [ " + api.mention(username) + " ]" + "\n*Total Warnings:* " + Util.removeUserWhen.get(member.senderID));
                } else {
                    api.postMessage(api.resolveChannel(Util.botcmdChannel), "~*[ERROR: Invalid user!]*~\n*Details:* Could not show warnings of *'" + username + "'*");
                }
            }
            break;
            case "showStats":
            {
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
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
                if(!Util.isUserAuthorized(api, api.resolveMember(command.message.senderName))) return;
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
