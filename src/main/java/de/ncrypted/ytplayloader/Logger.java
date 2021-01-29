package de.ncrypted.ytplayloader;

import javafx.scene.control.TextArea;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author ncrypted
 */
public class Logger {

    private static TextArea logArea;

    protected static void setLogArea(TextArea area) {
        logArea = area;
    }

    public static void log(String message) {
        if (logArea == null) {
            return;
        }
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("[HH:mm:ss] ");
        YTPlayloader.runOnUIThread(() -> logArea.appendText(dateFormat.format(calendar.getTime()) + message + "\n"));
    }

    public static void warn(String message) {
        log("[WARN] " + message);
    }

    public static void err(String message) {
        log("[ERROR] " + message);
    }
}
