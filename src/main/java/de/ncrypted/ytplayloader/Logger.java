package de.ncrypted.ytplayloader;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import javafx.scene.control.TextArea;

/**
 * @author ncrypted
 */
public class Logger {

  private static TextArea logArea;

  protected static void setLogArea(TextArea area) {
    logArea = area;
  }

  public static void log(String message) {
    log(message, false);
  }

  public static void log(String message, boolean replaceLine) {
    if (logArea == null) {
      return;
    }
    Calendar calendar = Calendar.getInstance();
    SimpleDateFormat dateFormat = new SimpleDateFormat("[HH:mm:ss] ");
    String msg = dateFormat.format(calendar.getTime()) + message + "\n";
    YTPlayloader.runOnUIThread(() -> {
      if (replaceLine) {
        int end = logArea.getLength();
        String[] lines = logArea.getText().split("\\n");
        int start = end - lines[lines.length - 1].length() - 1;
        logArea.replaceText(start, end, msg);
      } else {
        logArea.appendText(msg);
      }
    });
  }

  public static void warn(String message) {
    log("[WARN] " + message);
  }

  public static void err(String message) {
    log("[ERROR] " + message);
  }
}
