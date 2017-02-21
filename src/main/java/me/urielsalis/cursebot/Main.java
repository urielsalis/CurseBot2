package me.urielsalis.cursebot;

import me.urielsalis.cursebot.api.*;
import me.urielsalis.cursebot.extensions.ExtensionHandler;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;


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
    public static ArrayList<String> authedLinkers = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        new Main();
    }

    public Main() {
        loadProperties();

        api = new CurseApi(groupID, username, password, clientID, machineKey);
        api.startMessageLoop();

        api.postMessage(api.resolveChannel("bot-log"), "*Bot startup:* Bot is starting up");

        //wait 5 seconds so all messages are flushed and only new ones shown
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        api.postMessage(api.resolveChannel("bot-log"), "*Bot startup:* Bot online!");

        ExtensionHandler handler = new ExtensionHandler();
        handler.init();
        loadMembers();
        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        service.scheduleAtFixedRate(() -> updateMembersTable(), 0, 5, TimeUnit.MINUTES);
        service.scheduleAtFixedRate(() -> showStats(), 1, 1, TimeUnit.DAYS);
        service.scheduleAtFixedRate(() -> fetchDomains(), 0, 7, TimeUnit.DAYS);
    }

    private void showStats() {
        api.postMessage(api.resolveChannel("bot-stats"), "[*Stats:* ~" + new Date().toString() + "~ ]"
                                                                            + "\n-*I===============================I*-"
                                                                            + "\n*Net users joined:*        |     " + api.userJoins
                                                                            + "\n*Unique joins:*          |     " + api.userUniqueJoins
                                                                            + "\n*Messages posted:*   |     " + api.messages
                                                                            + "\n*Removed users:*      |     " + api.removedUsers
                                                                            + "\n*Left users:*                |     " + api.leftUsers);

        api.userJoins = 0;
        api.messages = 0;
        api.removedUsers = 0;
        api.leftUsers = 0;
    }

    private void loadMembers() {
        try {
            FileInputStream f_in = new FileInputStream("members.data");
            ObjectInputStream obj_in = new ObjectInputStream(f_in);
            Object obj = obj_in.readObject();
            api.setMembers((ArrayList<Member>) obj);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    private void updateMembersTable() {
        try {
            FileOutputStream f_out = new FileOutputStream("members.data");
            ObjectOutputStream obj_out = new ObjectOutputStream(f_out);
            obj_out.writeObject(api.getMembersList());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fetchDomains() {
        try {
            URL website = new URL("https://data.iana.org/TLD/tlds-alpha-by-domain.txt");
            try (InputStream in = website.openStream()) {
                Files.copy(in, new File("filters\\domains.txt").toPath(), StandardCopyOption.REPLACE_EXISTING);
                me.urielsalis.cursebot.extensions.Profanity.Main.loadTLDs();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
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

                Util.botName = prop.getProperty("username");

                Util.botlogChannel = prop.getProperty("loggingChannel");
                Util.botstatChannel = prop.getProperty("statsChannel");
                Util.botcmdChannel = prop.getProperty("commandChannel");

                Util.authorizedUsers = prop.getProperty("authorizedUsers").split(",");
                
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
