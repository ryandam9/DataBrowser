package com.data.browser;

import java.io.PrintWriter;
import java.io.StringWriter;

import static com.data.browser.AppLogger.logger;

public class Utils {
    /**
     * Log an Exception
     *
     * @param e Exception Object
     */
    public static void logStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String sStackTrace = sw.toString(); // stack trace as a string
        logger.debug(sStackTrace);
    }
}