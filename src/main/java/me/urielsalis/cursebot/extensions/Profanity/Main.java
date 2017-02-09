package me.urielsalis.cursebot.extensions.Profanity;

import me.urielsalis.cursebot.api.*;
import me.urielsalis.cursebot.events.CommandEvent;
import me.urielsalis.cursebot.events.MessageEvent;
import me.urielsalis.cursebot.extensions.Extension;
import me.urielsalis.cursebot.extensions.ExtensionApi;
import me.urielsalis.cursebot.extensions.ExtensionHandler;
import me.urielsalis.cursebot.extensions.Handle;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by urielsalis on 1/28/2017
 */
@Extension(name = "Profanity",version = "1.0.0", id = "Profanity/1.0.0")
public class Main{
    static ExtensionApi extApi;
    static  CurseApi api;
    private static String[] swearWords;
    private static String[] whiteListedChannels;
    private static String[] authroizedLinks;
    private static HashSet<String> tlds = new HashSet<>();
    //private static String tldRegex = "";

    @ExtensionHandler.ExtensionInit("Profanity/1.0.0")
    public static void init(ExtensionApi api2) {
        System.out.println("Loading Profanity init");
        extApi = api2;
        extApi.addListener("message", new ProfanityListener());
        extApi.addListener("command", new ProfanityCommandListener());
        api = extApi.getCurseAPI();
        loadProfanities(getFilterElements("profanities.txt"));
        loadWhitelistedLinkingChannels(getFilterElements("linkerchannels.txt"));
        loadAuthorizedLinksUniversal(getFilterElements("authorizedlinks.txt"));
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

    public static boolean isUpperCase(String s)
    {
        long all = s.length();
        long upperCase = 0;
        for (int i=0; i<s.length(); i++)
        {
            if (Character.isUpperCase(s.charAt(i)))
            {
                upperCase++;
            }
        }
        return ((double)upperCase/(double)all) >= 0.8;
    }


    private static void parseMessage(Message message) {
        try
        {
            try {
                message.body = new String(message.body.getBytes("UTF-8"), "UTF-8");
            } catch (UnsupportedEncodingException e2) {
                e2.printStackTrace();
            }

            if(message.channelUUID.equals(api.resolveChannel("bot-log"))||message.channelUUID.equals(api.resolveChannel("bot-stats"))) return;
            if(containsCurseWord(message.body) && !(Util.isUserAuthorized(api, api.resolveMember(message.senderName)))) {
                api.deleteMessage(message);
                api.postMessage(api.resolveChannel("bot-log"), "~*[Profanity Filter]*~\n*Sender:* [ " + api.mention(message.senderName) + " ]\n*Said:* " + message.body + "\n*Action:* message auto-deleted! Verbal warning received!");
                api.postMessage(api.resolveChannelUUID(message.channelUUID), api.mention(message.senderName) + ", please don't use profanities. This is a kid friendly chat server!");
            }

            if(message.isPM) {
                System.out.println(Util.timestampToDate(message.timestamp) + "  [" + message.senderName + "] " + message.body);
            } else {
                System.out.println(Util.timestampToDate(message.timestamp) + "  <" + message.senderName + "> " + message.body);
            }

            if(!isAuthorizedLinker(api, message)) {
                api.deleteMessage(message);
                api.postMessage(api.resolveChannel("bot-log"), "~*[Link Filter]*~\n*Sender:* [ " + api.mention(message.senderName) + " ]\n*Said:* " + message.body + "\n*Channel:* " + api.resolveChannelUUID(message.channelUUID) + "\n*Action:* message auto-deleted! Verbal warning received!");
                api.postMessage(api.resolveChannelUUID(message.channelUUID), api.mention(message.senderName) + ", please get permission before posting those types of links.");
            }

            if(isUpperCase(message.body)) {
                api.deleteMessage(message);
                api.postMessage(api.resolveChannel("bot-log"), "~*[Capital Letters Filter]*~\n*Sender:* [ " + api.mention(message.senderName) + " ]\n*Said:* " + message.body + "\n*Channel:* " + api.resolveChannelUUID(message.channelUUID) + "\n*Action:* message auto-deleted! Verbal warning received!");
                api.postMessage(api.resolveChannelUUID(message.channelUUID), api.mention(message.senderName) + ", please lay off the caps.");
            }
        }
        catch (UnsupportedEncodingException e1)
        {e1.printStackTrace();}
    }


    private static void handleCommand(CommandEvent commandEvent) {
        switch (commandEvent.command.command) {
            case "addProfanity":
            {
                api.postMessage(api.resolveChannel("bot-log"), "~*[Executing add profanity]*~");

                String profanities = "";

                profanities = getFilterElements("profanities.txt");

                boolean addProfanity = true;
                try
                {
                    for(String s : swearWords)
                        if(s.equalsIgnoreCase(commandEvent.command.args[0]))
                            addProfanity = false;

                    if(addProfanity)
                    {
                        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("filters\\profanities.txt"), "UTF-8"));

                        profanities = profanities.trim().replaceFirst("\\s\\]", "");
                        profanities += ",," + commandEvent.command.args[0] + " ]";

                        String[] swears = profanities.split(",+");
                        for(String s : swears)
                        {
                            if(!(s.contains("]")))
                                out.write(s + ",\n");
                            else
                                out.write(s);
                        }

                        out.flush();
                        out.close();

                        loadProfanities(getFilterElements("profanities.txt"));
                        api.postMessage(api.resolveChannel("bot-log"), "*[Success]*\nprofanity list reloaded!\n- Added *'" + commandEvent.command.args[0] + "'* to filter!\n- Added by " + api.mention(commandEvent.command.message.senderName));
                    }
                    else
                        api.postMessage(api.resolveChannel("bot-log"), "*[Failed]*\n- *'" + commandEvent.command.args[0] + "'* is already in the filter!\n- Attempted to be added by " + api.mention(commandEvent.command.message.senderName));
                } catch(IOException e)
                {e.printStackTrace();}

            }
            break;
            case "rmProfanity":
            {
                api.postMessage(api.resolveChannel("bot-log"), "~*[Executing remove profanity]*~");

                String profanities = "";

                profanities = getFilterElements("profanities.txt");

                boolean removeProfanity = false;
                String remove = "";
                try
                {
                    for(String s : swearWords) {
                        if (s.equalsIgnoreCase(commandEvent.command.args[0])) {
                            removeProfanity = true;
                            remove = s;
                        }
                    }

                    if(removeProfanity)
                    {
                        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("filters\\profanities.txt"), "UTF-8"));

                        profanities = profanities.trim().replaceFirst(" \\]", ",," + commandEvent.command.args[0] + " ]");

                        String[] swears = profanities.split(",+");
                        for(String s : swears)
                        {
                            if(s.equals(remove)) continue;
                            if(!(s.contains("]")))
                                out.write(s + ",\n");
                            else
                                out.write(s);
                        }

                        out.flush();
                        out.close();

                        loadProfanities(getFilterElements("profanities.txt"));
                        api.postMessage(api.resolveChannel("bot-log"), "*[Success]**\nprofanity list reloaded!\n- Removed *'" + commandEvent.command.args[0] + "'* to filter!\n- Removed by " + api.mention(commandEvent.command.message.senderName));
                    }
                    else
                        api.postMessage(api.resolveChannel("bot-log"), "*[Failed]*\n- *'" + commandEvent.command.args[0] + "'* is not in the filter!\n- Attempted to be removed by " + api.mention(commandEvent.command.message.senderName));
                } catch(IOException e)
                {e.printStackTrace();}
            }
            break;

            /*
            case "addLink":
            {
            }
            break;
            case "rmLink":
            {
            }
            break;

            case "whitelistChannel":
            {
            }
            break;

            case "blacklistChannel":
            {
            }
            break;*/
        }
    }


    private static boolean containsCurseWord(String body) throws UnsupportedEncodingException {
        String message = new String(body.getBytes("UTF-8"), "UTF-8").replaceAll("((\\s+)|(\\-)|(_))", "").trim();

        for(String str : swearWords)
            if(message.toLowerCase().contains(str.toLowerCase()))
                return true;

        return false;
    }

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

                case "linkerchannels.txt":
                {
                    if (!(in.hasNextLine())) {
                        prof = "[ add-channels-here,";
                    } else {
                        while (in.hasNextLine()) {
                            prof += in.nextLine() + ",";
                        }
                    }
                }
                break;

                case "authorizedlinks.txt":
                {
                    if (!(in.hasNextLine())) {
                        prof = "[ paste.ee,";
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

    private static void loadProfanities(String profanities) {
        swearWords = profanities.replaceAll("(\\[ )|( \\])", "").split(",+");
    }

    private static void loadWhitelistedLinkingChannels(String channels) {
        whiteListedChannels = channels.replaceAll("(\\[ )|( \\])", "").split(",+");
    }

    private static void loadAuthorizedLinksUniversal(String links) {
        authroizedLinks = links.replaceAll("(\\[ )|( \\])", "").split(",+");
    }

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

    private static Scanner resetScanner(String file) throws FileNotFoundException {
        return new Scanner(new FileInputStream(file), "UTF-8");
    }

    private static boolean isAuthorizedLinker(CurseApi api, Message message) {
        Member member = api.resolveMember(message.senderName);
        Channel channel = api.resolveChannelUUID(message.channelUUID);
        String[] body = message.body.split("\\s+");


        String url_regex = "(((http|ftp|https):\\/\\/)?([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?)";
        Pattern p = Pattern.compile(url_regex);

        if (Util.isUserAuthorized(api, member)) {
            return true;
        }

        for (String s : body) {
            Matcher m = p.matcher(s);

            if (m.find()) {
                s = m.group(1);
                if (!(s.startsWith("http://") || s.startsWith("https://") || s.startsWith("ftp://"))) {
                    s = "http://" + s;
                }

                try {
                    URL url = new URL(s);
                    String host = url.getHost();
                    String tld = host.substring(host.lastIndexOf('.'), host.length()).toLowerCase();

                    if (tlds.contains(tld)) {
                        for (String c : whiteListedChannels) {
                            if (api.resolveChannel(c).equals(channel)) {
                                return true;
                            }
                        }

                        if (!(Arrays.asList(authroizedLinks).contains(host) || Arrays.asList(authroizedLinks).contains(url))) {
                            return false;
                        }
                    }
                } catch (IOException e) {
                    System.out.println("unable to open connection");
                }
                System.out.println("String contains URL");
            }
        }

        return true;
    }
}
