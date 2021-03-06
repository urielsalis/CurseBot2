package me.urielsalis.cursebot.extensions.Profanity;

import me.urielsalis.cursebot.api.*;
import me.urielsalis.cursebot.events.CommandEvent;
import me.urielsalis.cursebot.events.MessageEvent;
import me.urielsalis.cursebot.extensions.Extension;
import me.urielsalis.cursebot.extensions.ExtensionApi;
import me.urielsalis.cursebot.extensions.ExtensionHandler;
import me.urielsalis.cursebot.extensions.Handle;

import java.io.*;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by urielsalis on 1/28/2017
 */
@Extension(name = "Profanity",version = "1.0.0", id = "Profanity/1.0.0")
public class Main{

    //:: API Vars
    static ExtensionApi extApi;
    static  CurseApi api;

    //:: Lists and filter elements
    private static String[] swearWords;
    private static List<String> linkBlacklist;
    private static HashSet<String> tlds = new HashSet<>();

    //:: INIT
    @ExtensionHandler.ExtensionInit("Profanity/1.0.0")
    public static void init(ExtensionApi api2) {
        System.out.println("Loading Profanity init");
        extApi = api2;
        extApi.addListener("message", new ProfanityListener());
        extApi.addListener("command", new ProfanityCommandListener());
        api = extApi.getCurseAPI();
        loadProfanities(getFilterElements("profanities.txt"));
        loadLinkBlacklist(getFilterElements("linkblacklist.txt"));
    }

    private static class ProfanityListener implements ExtensionApi.Listener {
        @Override
        public String name() {
            return "ProfanityListener/1.0.0";
        }

        @Handle
        public void handle(ExtensionApi.Event event) {
            if(event instanceof MessageEvent) {
                MessageEvent messageEvent = (MessageEvent) event;
                parseMessage(messageEvent.getMessage());
            }
        }
    }

    private static  class ProfanityCommandListener implements ExtensionApi.Listener {
        @Override
        public String name() {
            return "ProfanityCommandListener/1.0.0";
        }

        @Handle
        public void handle(ExtensionApi.Event event) {
            if(event instanceof CommandEvent) {
                CommandEvent commandEvent = (CommandEvent) event;
                handleCommand(commandEvent);
            }
        }
    }

    //:: END INIT :://

    //:: Filter actions: Detection :://
    private static void parseMessage(Message message) {
        Member cmdSender = api.resolveMember(message.senderName);
        Channel cmdChannel = api.resolveChannelUUID(message.channelUUID);

        //:: Sender Information
        String uniqueName = cmdSender.username;     //- The user's unique name. This is special to the user
        String displayName = cmdSender.displayName; //- The user's display name in a server.
        String senderName = cmdSender.senderName;   //- The user's nickname/name to refernce a command sent by
        long userID = cmdSender.senderID;           //- The user's unique numeric id. This is special to the user
        long userRole = cmdSender.bestRole;         //- The best role a user currently holds. infinity to 1.

        //:: Channel Information
        String channelName = cmdChannel.getGroupTitle(); //- The name of the channel command was sent in.
        String channelID = cmdChannel.getGroupID();      //- The special ID of a channel in which to refer to a channel.
        Channel botLogChannel = api.resolveChannel(Util.botlogChannel);
        Channel botCmdChannel = api.resolveChannel(Util.botcmdChannel);
        try
        {
            try {
                message.body = new String(message.body.getBytes("UTF-8"), "UTF-8");
            } catch (UnsupportedEncodingException e2) {
                e2.printStackTrace();
            }

            if(message.channelUUID.equals(botLogChannel.getGroupID())||message.channelUUID.equals(botLogChannel.getGroupID())) return;

            if(message.isPM) {
                System.out.println(Util.timestampToDate(message.timestamp) + "  [" + senderName + "] " + message.body);
            }
            else {
                System.out.println(Util.timestampToDate(message.timestamp) + "  <" + senderName + "> " + message.body);
            }

            //:: Detection for the 3 major filters :://

            if (!senderName.equals(Util.botName)) {
                if (!isAuthorizedLinker(api, message)) {
                    if(Util.unhidden) {
                        api.deleteMessage(message);
                    }

                    Integer warnings = Util.removeUserWhen.get(userID);
                    if(warnings==null) warnings = 1;

                    if(warnings > 1) {
                        api.postMessage(botLogChannel, senderName + " has " + warnings + " warnings!");
                    }

                    if (Util.canRemoveUser(userID)) {
                        api.kickUser(cmdSender);
                        try {
                            Util.dataBase.addWarning(0, "LinkFilter", userID, uniqueName, "Blacklisted link detected", "Kicked");
                        } catch (Exception e) {
                            e.printStackTrace();
                            api.postMessage(botLogChannel, "Database error while adding warning");
                        }
                        api.postMessage(botLogChannel, "~*[Link Filter]*~\n*Sender:* [ " + api.mention(senderName) + " ]\n*Said:* " + message.body + "\n*Channel:* " + channelName + "\n*Issued Warnings:* " + Util.removeUserWhen.get(userID) + "\n*Removals:* " + (Util.removeUserWhen.get(userID) / 4) + "\n*Action:* message auto-deleted! User removed from the server!");
                    }
                    else {
                        if(Util.unhidden) {
                            try {
                                Util.dataBase.addWarning(0, "LinkFilter", userID, uniqueName, "Blacklisted link detected", "Warned");
                            } catch (Exception e) {
                                e.printStackTrace();
                                api.postMessage(botLogChannel, "Database error while adding warning");
                            }
                            api.postMessage(botLogChannel, "~*[Link Filter]*~\n*Sender:* [ " + api.mention(senderName) + " ]\n*Said:* " + message.body + "\n*Channel:* " + channelName + "\n*Issued Warnings:* " + Util.removeUserWhen.get(userID) + "\n*Removals:* " + (Util.removeUserWhen.get(userID) / 4) + "\n*Action:* message auto-deleted! Verbal warning received!");
                            api.postMessage(api.resolveChannelUUID(message.channelUUID), api.mention(message.senderName) + ", please don't post that link. Those types of links aren't welcome here! Please review our rules over in the #rules channel.");
                        }
                        else {
                            try {
                                Util.dataBase.addWarning(0, "LinkFilter", userID, uniqueName, "Blacklisted link detected", "Nothing");
                            } catch (Exception e) {
                                e.printStackTrace();
                                api.postMessage(botLogChannel, "Database error while adding warning");
                            }
                            api.postMessage(botLogChannel, "~*[Link Filter]*~\n*Sender:* [ " + api.mention(senderName) + " ]\n*Said:* " + message.body + "\n*Channel:* " + channelName + "\n*Issued Warnings:* " + Util.removeUserWhen.get(userID) + "\n*Removals:* " + (Util.removeUserWhen.get(userID) / 4) + "\n*Action:* Hidden mode enabled! No action taken!");
                        }
                    }
                }

                if (containsCurseWord(message.body) && !(Util.isUserAuthorized(api, cmdSender))) {
                    if(Util.unhidden) {
                        api.deleteMessage(message);
                    }

                    Integer warnings = Util.removeUserWhen.get(userID);
                    if(warnings==null) warnings = 1;

                    if(warnings > 1) {
                        api.postMessage(botLogChannel, senderName + " has " + warnings + " warnings!");
                    }

                    if (Util.canRemoveUser(userID)) {
                        api.kickUser(cmdSender);
                        try {
                            Util.dataBase.addWarning(0, "ProfanityFilter", userID, uniqueName, "Profanity detected", "Kicked");
                        } catch (Exception e) {
                            e.printStackTrace();
                            api.postMessage(botLogChannel, "Database error while adding warning");
                        }
                        api.postMessage(botLogChannel, "~*[Profanity Filter]*~\n*Sender:* [ " + api.mention(senderName) + " ]\n*Said:* " + message.body + "\n*Censored:* "+ Util.tmpStringCensored + "\n*Channel:* " + channelName + "\n*Issued Warnings:* " + Util.removeUserWhen.get(userID) + "\n*Removals:* " + (Util.removeUserWhen.get(userID) / 4) + "\n*Action:* message auto-deleted! User was removed from the server!");
                    }
                    else {
                        if(Util.unhidden) {
                            try {
                                Util.dataBase.addWarning(0, "ProfanityFilter", userID, uniqueName, "Profanity detected", "Warned");
                            } catch (Exception e) {
                                e.printStackTrace();
                                api.postMessage(botLogChannel, "Database error while adding warning");
                            }
                            api.postMessage(botLogChannel, "~*[Profanity Filter]*~\n*Sender:* [ " + api.mention(senderName) + " ]\n*Said:* " + message.body + "\n*Censored:* "+ Util.tmpStringCensored + "\n*Channel:* " + channelName + "\n*Issued Warnings:* " + Util.removeUserWhen.get(userID) + "\n*Removals:* " + (Util.removeUserWhen.get(userID) / 4) + "\n*Action:* message auto-deleted! Verbal warning received!");
                            api.postMessage(api.resolveChannelUUID(message.channelUUID), api.mention(message.senderName) + ", please don't use profanities. This is a family friendly chat server! Please review our rules over in the #rules channel.");
                        }
                        else {
                            try {
                                Util.dataBase.addWarning(0, "ProfanityFilter", userID, uniqueName, "Profanity detected", "Nothing");
                            } catch (Exception e) {
                                e.printStackTrace();
                                api.postMessage(botLogChannel, "Database error while adding warning");
                            }
                            api.postMessage(botLogChannel, "~*[Profanity Filter]*~\n*Sender:* [ " + api.mention(senderName) + " ]\n*Said:* " + message.body + "\n*Censored:* "+ Util.tmpStringCensored + "\n*Channel:* " + channelName + "\n*Issued Warnings:* " + Util.removeUserWhen.get(userID) + "\n*Removals:* " + (Util.removeUserWhen.get(userID) / 4) + "\n*Action:* Hidden mode enabled! No Action was taken!");
                        }
                    }
                }
            }
        }
        catch (UnsupportedEncodingException e1)
        {e1.printStackTrace();}
    }

    //:: Commands: Filter

    private static void handleCommand(CommandEvent commandEvent) {
        Command command = commandEvent.getCommand();
        Message message = command.getMessage();
        Member cmdSender = api.resolveMember(message.senderName);
        Channel cmdChannel = api.resolveChannelUUID(message.channelUUID);

        //:: Sender Information
        String uniqueName = cmdSender.username;     //- The user's unique name. This is special to the user
        String displayName = cmdSender.displayName; //- The user's display name in a server.
        String senderName = cmdSender.senderName;   //- The user's nickname/name to refernce a command sent by
        long userID = cmdSender.senderID;           //- The user's unique numeric id. This is special to the user
        long userRole = cmdSender.bestRole;         //- The best role a user currently holds. infinity to 1.

        //:: Channel Information
        String channelName = cmdChannel.getGroupTitle(); //- The name of the channel command was sent in.
        String channelID = cmdChannel.getGroupID();      //- The special ID of a channel in which to refer to a channel.
        Channel botLogChannel = api.resolveChannel(Util.botlogChannel);
        Channel botCommandChannel = api.resolveChannel(Util.botcmdChannel);

        //:: Message information
        String[] args = command.getArgs();


        if(!Util.isUserAuthorized(api, api.resolveMember(message.senderName))) return;
        if(api.resolveMember(senderName).bestRole==1 || api.resolveMember(senderName).bestRole==4) {
            switch (command.getCommand()) {
                case "addProfanity": {
                    api.postMessage(botLogChannel, "~*[Executing add profanity]*~");

                    String profanities = "";

                    profanities = getFilterElements("profanities.txt");

                    boolean addProfanity = true;
                    if (args != null && args.length > 0) {
                        try {
                            for (String s : swearWords)
                                if (s.equalsIgnoreCase(args[0]))
                                    addProfanity = false;

                            if (addProfanity) {
                                Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("filters" + File.separator + "profanities.txt"), "UTF-8"));

                                profanities = profanities.trim().replaceFirst("\\s\\]", "");
                                profanities += ",," + args[0] + " ]";

                                String[] list = profanities.split(",+");
                                for (String s : list) {
                                    if (!(s.contains("]")))
                                        out.write(s + ",\n");
                                    else
                                        out.write(s);
                                }

                                out.flush();
                                out.close();

                                loadProfanities(getFilterElements("profanities.txt"));
                                Util.dataBase.addCommandHistory(userID, uniqueName, "addProfanity", channelID, args[0]);
                                api.postMessage(botCommandChannel, "Successfully added word to the filter!");
                            } else {
                                api.postMessage(botCommandChannel, "*[Failed]*\n- *'" + args[0] + "'* is already in the filter!\n- Attempted to be added by " + senderName);
                                api.postMessage(botCommandChannel, "Word already exists in the filter!");
                            }
                        } catch (IOException e) {
                            Util.dataBase.addLogMessage("INFO", "IO exception in addProfanity", e);

                        }
                    } else {
                        api.postMessage(botCommandChannel, "*[Failed]*\n- No profanity was specified!\n- Attempted to be added by " + senderName);
                        api.postMessage(botCommandChannel, "No word specified to add to the filter!");
                    }
                }
                break;

                case "rmProfanity": {
                    api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Executing remove profanity]*~");

                    String profanities = "";

                    profanities = getFilterElements("profanities.txt");

                    boolean removeProfanity = false;
                    String remove = "";
                    if (args != null && args.length > 0) {
                        try {
                            for (String s : swearWords) {
                                if (s.equalsIgnoreCase(args[0])) {
                                    removeProfanity = true;
                                    remove = s;
                                }
                            }

                            if (removeProfanity) {
                                Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("filters" + File.separator + "profanities.txt"), "UTF-8"));

                                profanities = profanities.trim().replaceFirst(",," + remove, "");

                                String[] list = profanities.split(",+");
                                for (String s : list) {
                                    if (s.equals(remove)) continue;
                                    if (!(s.contains("]")))
                                        out.write(s + ",\n");
                                    else
                                        out.write(s);
                                }

                                out.flush();
                                out.close();

                                loadProfanities(getFilterElements("profanities.txt"));
                                Util.dataBase.addCommandHistory(userID, uniqueName, "rmProfanity", channelID, args[0]);
                                api.postMessage(botCommandChannel, "Successfully removed word from the filter!");
                            } else {
                                api.postMessage(botCommandChannel, "*[Failed]*\n- *'" + args[0] + "'* is not in the filter!\n- Attempted to be removed by " + senderName);
                                api.postMessage(botCommandChannel, "Unable to remove non existent word from the filter!");
                            }
                        } catch (IOException e) {
                            Util.dataBase.addLogMessage("INFO", "IO exception in rmProfanity", e);
                        }
                    } else {
                        api.postMessage(botCommandChannel, "*[Failed]*\n- No profanity was specified!\n- Attempted to be added by " + senderName);
                        api.postMessage(botCommandChannel, "No word specified to remove from the filter!");
                    }
                }
                break;

                case "blacklistLink": {
                    api.postMessage(botLogChannel, "~*[Executing blacklisting link]*~");

                    String links = "";

                    links = getFilterElements("linkblacklist.txt");

                    if (args != null && args.length > 0) {
                        try {
                            if (!isLink(args[0])) {
                                Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("filters" + File.separator + "linkblacklist.txt"), "UTF-8"));

                                links = links.trim().replaceFirst("\\s\\]", "");
                                links += ",," + args[0] + " ]";

                                String[] list = links.split(",+");
                                for (String s : list) {
                                    if (!(s.contains("]")))
                                        out.write(s + ",\n");
                                    else
                                        out.write(s);
                                }

                                out.flush();
                                out.close();

                                loadLinkBlacklist(getFilterElements("linkblacklist.txt"));
                                Util.dataBase.addCommandHistory(userID, uniqueName, "blacklistLink", channelID, args[0]);
                                api.postMessage(botCommandChannel, "Link was successfully blacklisted!");
                            } else {
                                api.postMessage(botCommandChannel, "*[Failed]*\n- '```" + args[0] + "```' is already in the filter or was an invalid link!\n- Attempted to be added by " + senderName);
                                api.postMessage(botCommandChannel, "Unable to remove non existent link from blacklist!");
                            }
                        } catch (IOException e) {
                            Util.dataBase.addLogMessage("INFO", "IO exception in blacklistLink", e);

                        }
                    } else {
                        api.postMessage(botCommandChannel, "*[Failed]*\n- No link was specified!\n- Attempted to be added by " + senderName);
                        api.postMessage(botCommandChannel, "No link specified to add to the blacklist!");
                    }
                }
                break;
            }
        }
    }

    //:: END COMMANDS :://

    //:: Functionality Methods :://
    /**
     * Retrieves objects from a specified filter file and represents them as a java object
     * @param file the file you wish to pull from
     * @return a string of all of the elements withing a filter file
     */
    private static String getFilterElements(String file)
    {
        try {
            Scanner in = resetScanner("filters"+ File.separator + file);
            String prof = "";
            String[] elements;

            switch(file) {
                case "profanities.txt":
                {
                    if (!(in.hasNextLine())) {
                        prof = "[ fuck,";
                    } else {
                        while (in.hasNextLine()) {
                            prof += in.nextLine() + ",";
                        }
                    }
                }
                break;

                case "linkblacklist.txt":
                {
                    if (!(in.hasNextLine())) {
                        prof = "[ pornhub.com,";
                    } else {
                        while (in.hasNextLine()) {
                            prof += in.nextLine() + ",";
                        }
                    }
                }
                break;
                default:
                    throw new FileNotFoundException();
            }

            prof = prof.substring(0, prof.length() - 1);
            if (!(prof.endsWith("]")))
                prof += " ]";

            try {
                Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("filters" + File.separator + file), "UTF-8"));

                elements = prof.split(",+");
                for (String s : elements) {
                    if (!(s.contains("]")))
                        out.write(s + ",\n");
                    else
                        out.write(s);
                }

                out.flush();
                out.close();
            } catch (IOException e) {
                Util.dataBase.addLogMessage("INFO", "IO exception in getFilterElements", e);
            }
            in.close();

            return prof;
        } catch (FileNotFoundException e) {
            Util.dataBase.addLogMessage("INFO", "Profanity not found", e);

        }
        return null;
    }

    //:: FILTER DETECTORS :://


    /**
     * Returms true if a profanity is detected in a message
     * @param body
     * @return boolean
     * @throws UnsupportedEncodingException
     */
    private static boolean containsCurseWord(String body) throws UnsupportedEncodingException {
        //String message = new String(body.getBytes("UTF-8"), "UTF-8").split("\\s+}");
        String message = new String(body.getBytes("UTF-8"));
        String nmMsg = message.toLowerCase();
        Util.tmpStringCensored = "";

        for (String rmvInword : swearWords) {
            nmMsg = nmMsg.replaceAll("\\b" + rmvInword.toLowerCase() + "\\b", "[¤]");
        }
        //nmMsg = nmMsg.replaceAll("\\s+", "%");
        for (String rmvInword : swearWords) {
            nmMsg = nmMsg.replaceAll("\\b" + rmvInword.toLowerCase() + "\\S", "").replaceAll("\\B" + rmvInword.toLowerCase(), "");
        }
        for (String rmvInWord : swearWords) {
            String regex = "";
            for (int i = 0; i < rmvInWord.length(); i++) {
                if (i != rmvInWord.length() - 1) {
                    regex += rmvInWord.toLowerCase().charAt(i) + "+\\s*";
                }
                else {
                    regex += rmvInWord.toLowerCase().charAt(i) + "+";
                }
            }

            //System.out.println("REGEX: " + regex);
            nmMsg = nmMsg.replaceAll("\\b" + regex + "\\b", "[¤]");
        }

        //nmMsg = nmMsg.replaceAll("\\w", "+");
        if(nmMsg.contains("[¤]")) {
            Util.tmpStringCensored = nmMsg;
            return  true;
        }

        /*for(String str : swearWords) {
            Pattern p = Pattern.compile("\\b" + str);
            for(String s : message) {
                Matcher m = p.matcher(s);
                m.find();
                if(m.matches()) {
                    return true;
                }
            }
        }*/
        /*Pattern p = Pattern.compile(".");
        Matcher m = p.matcher("body");
        m.find();
        System.out.println("REGEX TESTER: " + body.replaceAll("\\Bto", "") + " " + body.matches("\\Bto"));*/

        return false;
    }

    /**
     * Returns true if a link contained within a message is allowed to be posted
     * @param api
     * @param message
     * @return
     */
    private static boolean isAuthorizedLinker(CurseApi api, Message message) {
        Member member = api.resolveMember(message.senderName);
        String[] body = message.body.split("\\s+");

        if (Util.isUserAuthorized(api, member)) {
            return true;
        }

        for (String s : body) {
            if (isLink(s)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the string given represents a link
     * @param link a link specified by a string
     * @return
     */
    private static boolean isLink(String link) {
        String url_regex = "(((http|ftp|https):\\/\\/)?([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?)";
        Pattern p = Pattern.compile(url_regex);
        Matcher m = p.matcher(link);

        if (m.find()) {
            link = m.group(1);

            if (!(link.startsWith("http://www.") || link.startsWith("https://www."))) {
                if(link.startsWith("www.")) {
                    link = link.replaceFirst("www.", "https://www.");
                }
                else if(link.startsWith("https://") || link.startsWith("http://")) {
                    link = link.replaceFirst("((https://)|(http://))", "https://www.");
                }
                else {
                    link = "https://www." + link;
                }
            }

            try {
                URL url = new URL(link);
                String host = url.getHost().toLowerCase();
                String tld = host.substring(host.lastIndexOf('.'), host.length());
                String tld2 = host.substring(host.indexOf('.', host.indexOf('.') + 1));
                host = "https://" + host.replaceFirst(tld2, ".[TLDEXISTS]");

                if (tlds.contains(tld)) {
                    link = link.replaceFirst(tld2, ".[TLDEXISTS]");
                    if ((linkBlacklist.contains(host) || linkBlacklist.contains(link))) {
                        return true;
                    }
                    else {
                        return false;
                    }
                }
            } catch (IOException e) {
                System.out.println("unable to open connection");
            }
        }
        return false;
    }

    //:: Utility Methods:

    /**
     * Loads a list of domains for the bot to check for.
     */
    public static void loadTLDs() {
        try {
            Scanner s = new Scanner(new File("filters"+File.separator+"domains.txt"));
            s.nextLine();
            while (s.hasNextLine()) {
                String add = "." + s.nextLine().toLowerCase();
                tlds.add(add);
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads a string of profanities into a list
     * @param profanities
     */
    private static void loadProfanities(String profanities) {
        swearWords = profanities.replaceAll("(\\[ )|( \\])", "").split(",+");
    }

    /**
     * Loads a string of links into a list
     * @param links
     */
    private static void loadLinkBlacklist(String links) {
        linkBlacklist = Arrays.asList(links.replaceAll("(\\[ )|( \\])", "").split(",+"));
        for (int i = 0; i < linkBlacklist.size(); i++) {
            if (!(linkBlacklist.get(i).startsWith("https://www.") || linkBlacklist.get(i).startsWith("http://www."))) {
                if(linkBlacklist.get(i).startsWith("www.")) {
                    linkBlacklist.set(i, linkBlacklist.get(i).replaceFirst("www.", "https://www."));
                }
                else if(linkBlacklist.get(i).startsWith("https://") || linkBlacklist.get(i).startsWith("http://")) {
                    linkBlacklist.set(i, linkBlacklist.get(i).replaceFirst("((https://)|(http://))", "https://www."));
                }
                else {
                    linkBlacklist.set(i, "https://www." + linkBlacklist.get(i));
                }
            }

            try {
                URL url = new URL(linkBlacklist.get(i));
                String host = url.getHost().toLowerCase();
                String tld = host.substring(host.indexOf('.', host.indexOf('.') + 1));

                linkBlacklist.set(i, linkBlacklist.get(i).replaceFirst(tld, ".[TLDEXISTS]"));
            }
            catch (IOException e) {
                System.out.println("Unable to load connection!");
            }
        }
    }

    /**
     * Returns a new scanner to parse a specified file
     * @param file the file the scanner is to look at
     * @return Scanner
     * @throws FileNotFoundException
     */
    private static Scanner resetScanner(String file) throws FileNotFoundException {
        return new Scanner(new FileInputStream(file), "UTF-8");
    }
}
