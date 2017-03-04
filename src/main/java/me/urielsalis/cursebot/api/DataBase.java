package me.urielsalis.cursebot.api;

import me.urielsalis.cursebot.Main;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by ZDesk on 3/3/2017.
 */
public class DataBase
{
    public Connection con = null;

    //:: Creates connection to the database
    public void initDB()
    {
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
}
