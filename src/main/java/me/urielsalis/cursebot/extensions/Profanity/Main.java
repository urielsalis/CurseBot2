package me.urielsalis.cursebot.extensions.Profanity;

import me.urielsalis.cursebot.api.CurseApi;
import me.urielsalis.cursebot.api.Message;
import me.urielsalis.cursebot.api.Util;
import me.urielsalis.cursebot.events.CommandEvent;
import me.urielsalis.cursebot.events.MessageEvent;
import me.urielsalis.cursebot.extensions.Extension;
import me.urielsalis.cursebot.extensions.ExtensionApi;
import me.urielsalis.cursebot.extensions.ExtensionHandler;
import net.engio.mbassy.listener.Handler;

import java.io.*;
import java.util.Scanner;

/**
 * Created by urielsalis on 1/28/2017
 */
@Extension(name = "Profanity",version = "1.0.0", id = "Profanity/1.0.0")
public class Main{
    ExtensionApi extApi;
    CurseApi api;
    private String[] swearWords;


    @ExtensionHandler.ExtensionInit("Profanity/1.0.0")
    public void init(ExtensionApi api2) {
        this.extApi = api2;
        extApi.addListener("message", new ProfanityListener());
        extApi.addListener("command", new ProfanityCommandListener());
        api = extApi.getCurseAPI();
        loadProfanities(getProfanities());
    }

    private class ProfanityListener implements ExtensionApi.Listener {
        @Override
        public String name() {
            return "ProfanityListener/1.0.0";
        }

        @Handler
        public void handle(ExtensionApi.Event event) {
            if(event instanceof MessageEvent) {
                MessageEvent messageEvent = (MessageEvent) event;
                parseMessage(messageEvent.message);
            }
        }
    }


    private class ProfanityCommandListener implements ExtensionApi.Listener {
        @Override
        public String name() {
            return "ProfanityCommandListener/1.0.0";
        }

        @Handler
        public void handle(ExtensionApi.Event event) {
            if(event instanceof CommandEvent) {
                CommandEvent commandEvent = (CommandEvent) event;
                handleCommand(commandEvent);
            }
        }
    }

    private void parseMessage(Message message) {
        try
        {
            if(containsCurseWord(message.body) && !(Util.isUserAuthorized(message.senderName))) {
                api.deleteMessage(message);
                api.postMessage(api.resolveChannelUUID(message.channelUUID), api.mention(message.senderName) + ", please dont swear");
            }
        }
        catch (UnsupportedEncodingException e1)
        {e1.printStackTrace();}
    }


    private void handleCommand(CommandEvent commandEvent) {
        switch (commandEvent.command.command) {
            case "addProfanity":
            {
                api.postMessage(api.resolveChannel("bot-log"), "adding profanity!");

                String profanities = "";

                profanities = getProfanities();

                boolean addProfanity = true;
                try
                {
                    for(String s : swearWords)
                        if(s.equalsIgnoreCase(commandEvent.command.args[0]))
                            addProfanity = false;

                    if(addProfanity)
                    {
                        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("profanities.txt"), "UTF-8"));

	                            		/*
	                            		String addProf = "";
	                            		for(int i = 1; i < args.length; i++)
	                            			addProf += args[i] + "_";
	                            		addProf = addProf.substring(0, addProf.length() - 1);*/

                        profanities = profanities.trim().replaceFirst(" \\]", ",," + commandEvent.command.args[0] + " ]");

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

                        loadProfanities(getProfanities());
                        api.postMessage(api.resolveChannel("bot-log"), "[Success]\nprofanity list reloaded!\n- Added *'" + commandEvent.command.args[0] + "'* to filter!\n- Added by " + api.mention(commandEvent.command.message.senderName));
                    }
                    else
                        api.postMessage(api.resolveChannel("bot-log"), "[Failed]\n- *'" + commandEvent.command.args[0] + "'* is already in the filter!\n- Attempted to be added by " + api.mention(commandEvent.command.message.senderName));
                } catch(IOException e)
                {e.printStackTrace();}

            }
            break;
        }
    }


    private boolean containsCurseWord(String body) throws UnsupportedEncodingException {
        String message = new String(body.getBytes("UTF-8"), "UTF-8").replaceAll(" +", "");

        for(String str : swearWords)
            if(message.toLowerCase().contains(str.toLowerCase()))
                return true;

        return false;
    }

    private String getProfanities()
    {
        try {
            Scanner in = resetScanner("profanities.txt");
            String prof = "";
            String[] swears;

            if (!(in.hasNextLine()))
                prof = "[ fuck,";
            else {
                while (in.hasNextLine()) {
                    prof += in.nextLine() + ",";
                }
            }

            prof = prof.substring(0, prof.length() - 1);
            if (!(prof.endsWith("]")))
                prof += " ]";

            try {
                Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("profanities.txt"), "UTF-8"));

                swears = prof.split(",+");
                for (String s : swears) {
                    if (!(s.contains("]")))
                        out.write(s + ",\n");
                    else
                        out.write(s);
                }

                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("T5");
            }
            in.close();

            return prof;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void loadProfanities(String profanities)
    {
        swearWords = profanities.replaceAll("(\\[ )|( \\])", "").split(",+");
    }

    private Scanner resetScanner(String file) throws FileNotFoundException
    {
        return new Scanner(new FileInputStream(file), "UTF-8");
    }
}
