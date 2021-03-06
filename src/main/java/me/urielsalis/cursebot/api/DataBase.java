package me.urielsalis.cursebot.api;

import me.urielsalis.cursebot.Main;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static me.urielsalis.cursebot.Main.api;

public class DataBase
{
    String url;
    public Logger logger = Logger.getLogger("ErrorLogger");

    //:: Creates connection to the database
    public void initDB()
    {
        url = "jdbc:mysql://"+Util.databaseURL+":3306/"+Util.databaseTable;

    }

    public Connection getConnection() {
        Connection con = null;
        try {
            con = DriverManager.getConnection(url, Util.databaseUsername, Util.databasePassword);
            while(con == null) {
                con = DriverManager.getConnection(url, Util.databaseUsername, Util.databasePassword);
            }
            Connection finalCon = con;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> closeDB(finalCon)));
        } catch (Exception ex) {
            Logger lgr = Logger.getLogger(Main.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return con;
    }



    //Adds new ban, username must be unique curse ID(memberobj.username), duration must be a unix posix of the duration of the ban(seconds)
    public void addBanRecord(long issuerID, String issuerUsername, long bannedID, String bannedUsername, String reason, long duration) throws SQLException {
        Connection con = getConnection();
        String simpleProc = "{ call addBanRecord(?, ?, ?, ?, ?, ?) }";
        CallableStatement cs = con.prepareCall(simpleProc);
        cs.setLong("issuerId", issuerID);
        cs.setString("issuerUsername", issuerUsername);
        cs.setLong("bannedId", bannedID);
        cs.setString("bannedUsername", bannedUsername);
        cs.setString("reason", reason);
        cs.setLong("banDuration", duration);
        cs.execute();
        closeDB(con);
    }
    //Adds new command issuer, username must be unique curse ID(memberobj.username), args doesnt include the command, is a empty string if non existant
    public void addCommandHistory(long userId, String username, String command, String channel, String args) {
        try {
            Connection con = getConnection();
            String simpleProc = "{ call addCommandHistory(?, ?, ?, ?, ?) }";
            CallableStatement cs = con.prepareCall(simpleProc);
            cs.setLong("userId", userId);
            cs.setString("username", username);
            cs.setString("command", command);
            cs.setString("channel", channel);
            cs.setString("args", args);
            cs.execute();
            closeDB(con);
        } catch (Exception e) {
            api.postMessage(api.resolveChannel(Util.botlogChannel), "Database error in command history, this might be bad");
            e.printStackTrace();
        }

    }
    //Adds new log message(in crashes) stackstrace can be get from exception(like .printStackTrace but to variable, cant remember the method)
    public void addLogMessage(String level, String message, Exception e) {
        try {
            Connection con = getConnection();
            if(con==null) {
                System.err.println(message);
                e.printStackTrace();
                return;
            }
            logger.log(Level.WARNING, "Expection thrown", e);
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String simpleProc = "{ call addLogMessage(?, ?, ?, ?) }";
            CallableStatement cs = con.prepareCall(simpleProc);
            cs.setString("level", level);
            cs.setString("message", message);
            cs.setString("stacktrace", sw.toString());
            cs.setString("data", "");
            cs.execute();
            closeDB(con);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
    //Adds new warning, might be manual, staff remove(auto adds as warning) or from filters, id and username is the same, reason is limited(keep it short, maybe show censored word), dont put too much text, action is "Removed user" or "Verbal warning: X total warnings"(has char limit, do go crazy, keep it really short)
    public void addWarning(long issuerId, String issuerUsername, long warnedId, String warnedUsername, String reason, String action) throws SQLException {
        Connection con = getConnection();
        String simpleProc = "{ call addWarning(?, ?, ?, ?, ?, ?) }";
        CallableStatement cs = con.prepareCall(simpleProc);
        cs.setLong("issuerId", issuerId);
        cs.setString("issuerUsername", issuerUsername);
        cs.setLong("warnedId", warnedId);
        cs.setString("warnedUsername", warnedUsername);
        cs.setString("reason", reason);
        cs.setString("action", action);
        cs.execute();
        closeDB(con);
    }

    public void closeDB(Connection con) {
        try {
            if (con != null) {
                con.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addDeletedMessage(Message message, Channel channel, long deletedUserID, String deletedUsername, long timestampDeleted) throws SQLException {
        Connection con = getConnection();
        String simpleProc = "{ call deletedMessage(?, ?, ?, ?, ?, ?) }";
        CallableStatement cs = con.prepareCall(simpleProc);
        cs.setString("body", message.body);
        cs.setString("issuerUsername", deletedUsername);
        cs.setLong("issuerID", deletedUserID);
        cs.setLong("warnedId", api.resolveMember(message.senderName).senderID);
        cs.setString("warnedUsername", message.senderName);
        cs.setLong("timestampDeleted", timestampDeleted);
        cs.setLong("timestampMessage", message.cursetimestamp);
        cs.setString("channel", channel.getGroupTitle());
        cs.execute();
        closeDB(con);
    }
}
