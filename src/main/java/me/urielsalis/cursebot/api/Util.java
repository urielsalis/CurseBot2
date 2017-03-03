package me.urielsalis.cursebot.api;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 * BattleCode 2017
 * Team: Mate
 * License: GPL 3.0
 */
public class Util {
    public static String[] authorizedUsers;
    public static Logger logger = Logger.getLogger("CurseBot");

    //:: Bot
    public static String botName;

    //:: Bot channels
    public static String defaultChannel;
    public static String botlogChannel;
    public static String botstatChannel;
    public static String botcmdChannel;

    //:: Bote modes
    public static boolean unhidden = false;

    //:: Users in trouble
    public static Map<Long, Integer> removeUserWhen = new HashMap<Long, Integer>();
    public static String tmpStringCensored = "";
    public static String databaseURL;
    public static String databaseTable;
    public static String databaseUsername;
    public static String databasePassword;


    public static String sendGet(String url, String auth) {
        try {
            URL obj = new URL(url);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "CurseApi/1.0.0");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setRequestProperty("AuthenticationToken", auth);
            //int responseCode = con.getResponseCode();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);

            in.close();

            return response.toString();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String sendDelete(String url, String auth) {
        try {
            URL obj = new URL(url);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
            con.setRequestMethod("DELETE");
            con.setRequestProperty("User-Agent", "CurseApi/1.0.0");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setRequestProperty("AuthenticationToken", auth);
            //int responseCode = con.getResponseCode();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "UTF-8"));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);

            in.close();

            return response.toString();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String sendPost(String url, String parameters, String auth) {
        try {
            URL obj = new URL(url);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "CurseApi/1.0.0");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setRequestProperty("AuthenticationToken", auth);
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(wr, "UTF-8"));
            //writer.writeBytes(parameters);
            writer.write(parameters);
            writer.flush();
            writer.close();
            int responseCode = con.getResponseCode();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "UTF-8"));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);
            in.close();

            return response.toString();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean equals(String senderName, String urielsalis) {
        return senderName.trim().toLowerCase().equals(urielsalis.trim().toLowerCase());
    }

    public static String spaceSeparatedString(String[] strings) {
        StringBuilder builder = new StringBuilder();
        for(String s : strings) {
            builder.append(s + " ");
        }
        return builder.toString().trim();

    }

    public static String timestampToDate(long timestamp) {
        Date date = new Date(timestamp); // *1000 is to convert seconds to milliseconds
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"); // the format of your date
        sdf.setTimeZone(TimeZone.getTimeZone("GMT-3")); // give a timezone reference for formating (see comment at the bottom
        return sdf.format(date);
    }
    
    public static boolean containsUTF8(CharSequence s, String cont) throws UnsupportedEncodingException
    {
    	String test = new String(cont.getBytes("UTF-8"), "UTF-8");
    	return test.toString().indexOf(new String(s.toString().getBytes("UTF-8"), "UTF-8")) > 1;
    }

    public static boolean isUserAuthorized(CurseApi api, Member member) {
        return member != null && api.bestRank >= api.roles.get(member.bestRole);
    }

    public static boolean canRemoveUser(long userID) {
        if (!Util.removeUserWhen.containsKey(userID)) {
            Util.removeUserWhen.put(userID, 1);
        } else {
            Util.removeUserWhen.replace(userID, Util.removeUserWhen.get(userID) + 1);
        }

        return Util.unhidden && Util.removeUserWhen.get(userID) % 4 == 0;
    }

}
