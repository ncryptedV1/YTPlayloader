package de.ncrypted.ytplayloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author ncrypted
 */
public class Utils {

  public static void handleYTDLUpdateStream(Process proc) throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("Updating to version")) {
          Logger.log("Youtube-DL updated to version " + line.split(" ")[3]);
        } else if (line.startsWith("youtube-dl is up-to-date")) {
          Logger.log("Youtube-dl is up-to-date " + line.split(" ")[3]);
        }
      }
    }
  }

  public static void readErrorStream(Process proc) throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        System.out.println(line);
      }
    }
  }
}
