package de.ncrypted.ytplayloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

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

  public static String setupResource(String fileName) {
    URL resource = YTPLController.class.getClassLoader().getResource(fileName);
    if (resource == null) {
      throw new IllegalArgumentException("The resource " + fileName + " is no valid resource!");
    }

    File targetFile = new File(YTPlayloader.getJarDir() + File.separator + fileName);
    if (!targetFile.exists()) {
      try {
        Files.copy(Path.of(resource.toURI()), targetFile.toPath());
      } catch (IOException | URISyntaxException e) {
        // can safely be ignored, as resource URL is ensured to be correct
      }
    }
    return targetFile.getAbsolutePath();
  }
}
