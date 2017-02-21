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
import java.util.*;
import java.util.logging.Logger;
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

    //:: Users in trouble
    private static Map<Long, Integer> removeUserWhen = new HashMap<Long, Integer>();

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
                parseMessage(messageEvent.message);
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
        try
        {
            try {
                message.body = new String(message.body.getBytes("UTF-8"), "UTF-8");
            } catch (UnsupportedEncodingException e2) {
                e2.printStackTrace();
            }

            Member userSender = api.resolveMember(message.senderName);

            if(message.channelUUID.equals(api.resolveChannel(Util.botlogChannel))||message.channelUUID.equals(api.resolveChannel(Util.botstatChannel))) return;

            if(message.isPM) {
                System.out.println(Util.timestampToDate(message.timestamp) + "  [" + message.senderName + "] " + message.body);
            }
            else {
                System.out.println(Util.timestampToDate(message.timestamp) + "  <" + message.senderName + "> " + message.body);
            }

            //:: Detection for the 3 major filters :://

            if (!userSender.senderName.equals(Util.botName)) {
                if (!isAuthorizedLinker(api, message)) {
                    api.deleteMessage(message);
                    if (canRemoveUser(userSender.senderID)) {
                        api.kickUser(userSender);
                        api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Link Filter]*~\n*Sender:* [ " + api.mention(message.senderName) + " ]\n*Said:* " + message.body + "\n*Channel:* " + api.resolveChannelUUID(message.channelUUID).groupTitle + "\n*Issued Warnings:* " + removeUserWhen.get(userSender.senderID) + "\n*Removals:* " + (removeUserWhen.get(userSender.senderID) / 4) + "\n*Action:* message auto-deleted! User was removed from the server!");
                    } else {
                        api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Link Filter]*~\n*Sender:* [ " + api.mention(message.senderName) + " ]\n*Said:* " + message.body + "\n*Channel:* " + api.resolveChannelUUID(message.channelUUID).groupTitle + "\n*Issued Warnings:* " + removeUserWhen.get(userSender.senderID) + "\n*Removals:* " + (removeUserWhen.get(userSender.senderID) / 4) + "\n*Action:* message auto-deleted! Verbal warning received!");
                        api.postMessage(api.resolveChannelUUID(message.channelUUID), api.mention(message.senderName) + ", please don't post that link. Those types of links aren't welcome here!");
                    }
                } else if (containsCurseWord(message.body) && !(Util.isUserAuthorized(api, api.resolveMember(message.senderName)))) {
                    api.deleteMessage(message);
                    if (canRemoveUser(userSender.senderID)) {
                        api.kickUser(userSender);
                        api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Profanity Filter]*~\n*Sender:* [ " + api.mention(message.senderName) + " ]\n*Said:* " + message.body + "\n*Channel:* " + api.resolveChannelUUID(message.channelUUID).groupTitle + "\n*Issued Warnings:* " + removeUserWhen.get(userSender.senderID) + "\n*Removals:* " + (removeUserWhen.get(userSender.senderID) / 4) + "\n*Action:* message auto-deleted! User was removed from the server!");
                    } else {
                        api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Profanity Filter]*~\n*Sender:* [ " + api.mention(message.senderName) + " ]\n*Said:* " + message.body + "\n*Channel:* " + api.resolveChannelUUID(message.channelUUID).groupTitle + "\n*Issued Warnings:* " + removeUserWhen.get(userSender.senderID) + "\n*Removals:* " + (removeUserWhen.get(userSender.senderID) / 4) + "\n*Action:* message auto-deleted! Verbal warning received!");
                        api.postMessage(api.resolveChannelUUID(message.channelUUID), api.mention(message.senderName) + ", please don't use profanities. This is a kid friendly chat server!");
                    }
                } /*else if (isUpperCase(message.body) && !(Util.isUserAuthorized(api, api.resolveMember(message.senderName)))) {
                    api.deleteMessage(message);
                    if (canRemoveUser(userSender.senderID)) {
                        api.kickUser(userSender);
                        api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Capital Letters Filter]*~\n*Sender:* [ " + api.mention(message.senderName) + " ]\n*Said:* " + message.body + "\n*Channel:* " + api.resolveChannelUUID(message.channelUUID).groupTitle + "\n*Issued Warnings:* " + removeUserWhen.get(userSender.senderID) + "\n*Removals:* " + (removeUserWhen.get(userSender.senderID) / 4) + "\n*Action:* message auto-deleted! User was removed from the server!");
                    } else {
                        api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Capital Letters Filter]*~\n*Sender:* [ " + api.mention(message.senderName) + " ]\n*Said:* " + message.body + "\n*Channel:* " + api.resolveChannelUUID(message.channelUUID).groupTitle + "\n*Issued Warnings:* " + removeUserWhen.get(userSender.senderID) + "\n*Removals:* " + (removeUserWhen.get(userSender.senderID) / 4) + "\n*Action:* message auto-deleted! Verbal warning received!");
                        api.postMessage(api.resolveChannelUUID(message.channelUUID), api.mention(message.senderName) + ", please lay off the caps.");
                    }
                }*/
            }
        }
        catch (UnsupportedEncodingException e1)
        {e1.printStackTrace();}
    }

    //:: Commands: Filter

    private static void handleCommand(CommandEvent commandEvent) {
        switch (commandEvent.command.command) {
            case "addProfanity":
            {
                api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Executing add profanity]*~");

                String profanities = "";

                profanities = getFilterElements("profanities.txt");

                boolean addProfanity = true;
                if(commandEvent.command.args != null && commandEvent.command.args.length > 0) {
                    try {
                        for (String s : swearWords)
                            if (s.equalsIgnoreCase(commandEvent.command.args[0]))
                                addProfanity = false;

                        if (addProfanity) {
                            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("filters\\profanities.txt"), "UTF-8"));

                            profanities = profanities.trim().replaceFirst("\\s\\]", "");
                            profanities += ",," + commandEvent.command.args[0] + " ]";

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
                            api.postMessage(api.resolveChannel(Util.botlogChannel), "*[Success]*\nprofanity list reloaded!\n- Added *'" + commandEvent.command.args[0] + "'* to the filter!\n- Added by " + api.mention(commandEvent.command.message.senderName));
                        } else
                            api.postMessage(api.resolveChannel(Util.botlogChannel), "*[Failed]*\n- *'" + commandEvent.command.args[0] + "'* is already in the filter!\n- Attempted to be added by " + api.mention(commandEvent.command.message.senderName));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    api.postMessage(api.resolveChannel(Util.botlogChannel), "*[Failed]*\n- No profanity was specified!\n- Attempted to be added by " + api.mention(commandEvent.command.message.senderName));
                }
            }
            break;

            case "rmProfanity":
            {
                api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Executing remove profanity]*~");

                String profanities = "";

                profanities = getFilterElements("profanities.txt");

                boolean removeProfanity = false;
                String remove = "";
                if(commandEvent.command.args != null && commandEvent.command.args.length > 0) {
                    try {
                        for (String s : swearWords) {
                            if (s.equalsIgnoreCase(commandEvent.command.args[0])) {
                                removeProfanity = true;
                                remove = s;
                            }
                        }

                        if (removeProfanity) {
                            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("filters\\profanities.txt"), "UTF-8"));

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
                            api.postMessage(api.resolveChannel(Util.botlogChannel), "*[Success]**\nprofanity list reloaded!\n- Removed *'" + commandEvent.command.args[0] + "'* to the filter!\n- Removed by " + api.mention(commandEvent.command.message.senderName));
                        } else
                            api.postMessage(api.resolveChannel(Util.botlogChannel), "*[Failed]*\n- *'" + commandEvent.command.args[0] + "'* is not in the filter!\n- Attempted to be removed by " + api.mention(commandEvent.command.message.senderName));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    api.postMessage(api.resolveChannel(Util.botlogChannel), "*[Failed]*\n- No profanity was specified!\n- Attempted to be added by " + api.mention(commandEvent.command.message.senderName));
                }
            }
            break;

            case "blacklistLink": {
                api.postMessage(api.resolveChannel(Util.botlogChannel), "~*[Executing blacklisting link]*~");

                String links = "";

                links = getFilterElements("linkblacklist.txt");

                if(commandEvent.command.args != null && commandEvent.command.args.length > 0) {
                    try {
                        if (isLink(commandEvent.command.args[0])) {
                            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("filters\\linkblacklist.txt"), "UTF-8"));

                            links = links.trim().replaceFirst("\\s\\]", "");
                            links += ",," + commandEvent.command.args[0] + " ]";

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
                            api.postMessage(api.resolveChannel(Util.botlogChannel), "*[Success]*\nlink blacklist reloaded!\n- Added *'```" + commandEvent.command.args[0] + "```'* to the blacklist!\n- Added by " + api.mention(commandEvent.command.message.senderName));
                        } else {
                            api.postMessage(api.resolveChannel(Util.botlogChannel), "*[Failed]*\n- '```" + commandEvent.command.args[0] + "```' is already in the filter or was an invalid link!\n- Attempted to be added by " + api.mention(commandEvent.command.message.senderName));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    api.postMessage(api.resolveChannel(Util.botlogChannel), "*[Failed]*\n- No link was specified!\n- Attempted to be added by " + api.mention(commandEvent.command.message.senderName));
                }
            }
            break;
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
            Scanner in = resetScanner("filters\\" + file);
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
                Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("filters\\" + file), "UTF-8"));

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
                e.printStackTrace();
            }
            in.close();

            return prof;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean canRemoveUser(long userID) {
        if(!removeUserWhen.containsKey(userID)) {
            removeUserWhen.put(userID, 1);
        }
        else {
            removeUserWhen.replace(userID, removeUserWhen.get(userID) + 1);
        }

        //return removeUserWhen.get(userID).intValue() % 4 == 0;
        return false;
    }

    //:: FILTER DETECTORS :://


    /**
     * Returns true if a message is longer than 10 characters long and is 85% upper case
     * @param s
     * @return boolean
     */
    /*
    public static boolean isUpperCase(String s)
    {
        System.out.println("SPAM: " + s + " " + s.length());
        s = s.replaceAll("[\\s\n]+", "").trim();
        System.out.println("SPAM: " + s + " " + s.length());
        double all = s.length();
        System.out.println("SPAM: " + all);
        double caps = s.replaceAll("[a-z]+", "").length();
        System.out.println("SPAM: " + caps);
        /*System.out.println("BEFORE: " + s);
        s = s.replaceAll("\\s+", "").replaceAll("\n","").replaceAll("[a-z]","").trim();
        System.out.println("AFTER: " + s);
        double all = s.length();
        double upperCase = 0;
        for (int ch : s.toCharArray()) {
            if (ch >= 65 && ch <=90) {
                upperCase++;
            }
        }
        return (caps/all) >= 0.75;
    }*/

    /**
     * Returms true if a profanity is detected in a message
     * @param body
     * @return boolean
     * @throws UnsupportedEncodingException
     */
    private static boolean containsCurseWord(String body) throws UnsupportedEncodingException {
        //String message = new String(body.getBytes("UTF-8"), "UTF-8").split("\\s+}");
        String message = new String(body.getBytes("UTF-8"));
        String nmMsg = message;

        for (String rmvInword : swearWords) {
            nmMsg = nmMsg.replaceAll("\\b" + rmvInword + "\\b", "[¤]");
        }
        //nmMsg = nmMsg.replaceAll("\\s+", "%");
        for (String rmvInword : swearWords) {
            nmMsg = nmMsg.replaceAll("\\b" + rmvInword + "\\S{1}", "").replaceAll("\\B" + rmvInword, "");
        }
        for (String rmvInWord : swearWords) {
            String regex = "";
            for (int i = 0; i < rmvInWord.length(); i++) {
                if (i != rmvInWord.length() - 1) {
                    regex += rmvInWord.charAt(i) + "+\\s*";
                }
                else {
                    regex += rmvInWord.charAt(i) + "+";
                }
            }

            //System.out.println("REGEX: " + regex);
            nmMsg = nmMsg.replaceAll("\\b" + regex + "\\b", "[¤]");
        }

        //nmMsg = nmMsg.replaceAll("\\w", "+");

        System.out.println(nmMsg);

        if(nmMsg.contains("[¤]")) {
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
            System.out.println("String contains URL");
        }
        return false;
    }

    //:: Utility Methods:

    /**
     * Loads a list of domains for the bot to check for.
     */
    public static void loadTLDs() {
        try {
            Scanner s = new Scanner(new File("filters\\domains.txt"));
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
