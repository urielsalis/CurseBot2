package me.urielsalis.cursebot;

import me.urielsalis.cursebot.api.*;
import me.urielsalis.cursebot.extensions.ExtensionHandler;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


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
    Connection con = null;


    public static void main(String[] args) throws Exception {
        new Main();
    }

    public Main() {
        loadProperties();

        api = new CurseApi(groupID, username, password, clientID, machineKey);
        api.startMessageLoop();

        api.postMessage(api.resolveChannel(Util.botlogChannel), "*Bot startup:* Bot is starting up");

        //wait 5 seconds so all messages are flushed and only new ones shown
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        api.postMessage(api.resolveChannel(Util.botlogChannel), "*Bot startup:* Bot online!");

        ExtensionHandler handler = new ExtensionHandler();
        handler.init();
        loadMembers();
        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        service.scheduleAtFixedRate(() -> updateMembersTable(), 0, 5, TimeUnit.MINUTES);
        service.scheduleAtFixedRate(() -> showStats(), 1, 1, TimeUnit.DAYS);
        service.scheduleAtFixedRate(() -> fetchDomains(), 0, 7, TimeUnit.DAYS);


        String url = "jdbc:mysql://"+Util.databaseURL+":3306/"+Util.databaseTable;
        try {
            con = DriverManager.getConnection(url, Util.databaseUsername, Util.databasePassword);
        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(Main.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::closeDB));


    }

    //Adds new ban, username must be unique curse ID(memberobj.username), duration must be a unix posix of the duration of the ban(seconds)
    public void addBanRecord(long issuerID, String issuerUsername, long bannedID, String bannedUsername, String reason, long duration) {
        try {
            String simpleProc = "{ call addBanRecord(?, ?, ?, ?, ?, ?) }";
            CallableStatement cs = con.prepareCall(simpleProc);
            cs.setLong("issuerId", issuerID);
            cs.setString("issuerUsername", issuerUsername);
            cs.setLong("bannedId", bannedID);
            cs.setString("bannedUsername", bannedUsername);
            cs.setString("reason", reason);
            cs.setLong("banDuration", duration);
            cs.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    //Adds new command issuer, username must be unique curse ID(memberobj.username), args doesnt include the command, is a empty string if non existant
    public void addCommandHistory(long userId, String username, String command, String channel, String args) {
        try {
            String simpleProc = "{ call addCommandHistory(?, ?, ?, ?, ?) }";
            CallableStatement cs = con.prepareCall(simpleProc);
            cs.setLong("userId", userId);
            cs.setString("username", username);
            cs.setString("command", command);
            cs.setString("channel", channel);
            cs.setString("args", args);
            cs.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    //Adds new log message(in crashes) stackstrace can be get from exception(like .printStackTrace but to variable, cant remember the method)
    public void addLogMessage(String level, String message, String stacktrace, String data) {
        try {
            String simpleProc = "{ call addLogMessage(?, ?, ?, ?) }";
            CallableStatement cs = con.prepareCall(simpleProc);
            cs.setString("level", level);
            cs.setString("message", message);
            cs.setString("stacktrace", stacktrace);
            cs.setString("data", data);
            cs.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    //Adds new warning, might be manual, staff remove(auto adds as warning) or from filters, id and username is the same, reason is limited(keep it short, maybe show censored word), dont put too much text, action is "Removed user" or "Verbal warning: X total warnings"(has char limit, do go crazy, keep it really short)
    public void addWarning(long issuerId, String issuerUsername, long warnedId, String warnedUsername, String reason, String action) {
        try {
            String simpleProc = "{ call addWarning(?, ?, ?, ?, ?, ?) }";
            CallableStatement cs = con.prepareCall(simpleProc);
            cs.setLong("issuerId", issuerId);
            cs.setString("issuerUsername", issuerUsername);
            cs.setLong("warnedId", warnedId);
            cs.setString("warnedUsername", warnedUsername);
            cs.setString("reason", reason);
            cs.setString("action", action);
            cs.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void closeDB() {
        try {
            if (con != null) {
                con.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showStats() {
        api.postMessage(api.resolveChannel(Util.botstatChannel), "[*Stats:* ~" + new Date().toString() + "~ ]"
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

                Util.defaultChannel = prop.getProperty("defaultChannel");
                Util.botlogChannel = prop.getProperty("loggingChannel");
                Util.botstatChannel = prop.getProperty("statsChannel");
                Util.botcmdChannel = prop.getProperty("commandChannel");

                Util.authorizedUsers = prop.getProperty("authorizedUsers").split(",");

                Util.databaseURL = prop.getProperty("databaseURL");
                Util.databaseTable = prop.getProperty("databaseTable");
                Util.databaseUsername = prop.getProperty("databaseUsername");
                Util.databasePassword = prop.getProperty("databasePassword");

                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
