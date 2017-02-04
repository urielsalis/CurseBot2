package me.urielsalis.cursebot;

import me.urielsalis.cursebot.api.*;
import me.urielsalis.cursebot.extensions.ExtensionHandler;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;


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
        //wait 5 seconds so all messages are flushed and only new ones shown
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ExtensionHandler handler = new ExtensionHandler();
        handler.init();
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
                Util.authorizedUsers = prop.getProperty("authorizedUsers").split(",");
                
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
