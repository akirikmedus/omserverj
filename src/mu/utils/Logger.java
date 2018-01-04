package mu.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Logger {

    private static String logDir; // The directory where log file is written to

    private static String logName; // The log file name. A date will be appended to it.

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm:ss.SSS"); // Date formatter to produce file name and time string.

    private static boolean debug = false; // Debug flag. If true, log message will be sent to screen.

    public static void setLogDir(String ld) { logDir = ld; }

    public static void setLogName(String ln) {
        logName = ln;
    }

    public static void error(String text) {
        error(text, "log");
    }
    public static void error(String text, String dest) {
        error("              ", " ", text, dest);
    }
    public static void error(String objId, String objType, String text) {
        writeLog("ERROR", objId, objType, text, "log");
    }
    public static void error(String objId, String objType, String text, String dest) {
        writeLog("ERROR", objId, objType, text, dest);
    }

    public static void info(String text) {
        info(text, "log");
    }
    public static void info(String text, String dest) {
        info("              ", " ", text, dest);
    }
    public static void info(String objId, String objType, String text) {
        writeLog("INFO", objId, objType, text, "log");
    }
    public static void info(String objId, String objType, String text, String dest) {
        writeLog("INFO", objId, objType, text, dest);
    }

    public static void setDebug(boolean debugOn) {
        debug = debugOn;
    }

    private static synchronized String getDateTimeString() {
        return sdf.format(Calendar.getInstance().getTime());
    }

    private static synchronized void writeLog(String msgType, String objId, String objType, String text, String dest) {
        String dtStr = getDateTimeString(); // Get the date string in the format of "19980215 13:44:56.432"
        String dateStr = dtStr.substring(0, 8); // Just the date part "19980215" to be used as the file name
        String timeStr = dtStr.substring(9); // The time part "13:44:56.432" to be written with the message
        String logFilePathName = null;

        logFilePathName = logDir + logName + "." + dateStr + ".log"; // The file name.

        String contentStr = msgType + "\t" + ((objId == null) ? "" : objId) + "\t" + ((objType == null) ? "" : objType) + "\t" + text;
        String logStr = timeStr + "\t" + contentStr;

        try {
            FileOutputStream logFileStrm = new FileOutputStream(logFilePathName, true);
            PrintWriter pw = new PrintWriter(logFileStrm, true);

            // A log message line is separated with tabs.
            pw.println(logStr);
            logFileStrm.close();
        } catch (IOException e) {
            System.out.println("in TMLog.writeLog(): " + e.getMessage());
            System.out.println("Original message: " + logStr);
        }

        // When debug is true, write the log message to standard output
        if (debug) {
            System.out.println(logStr);
        }

    }

}
