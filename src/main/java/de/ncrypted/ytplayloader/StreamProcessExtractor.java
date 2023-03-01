package de.ncrypted.ytplayloader;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StreamProcessExtractor extends Thread {

  private static final String GROUP_PERCENT = "percent";
  private static final String GROUP_MINUTES = "minutes";
  private static final String GROUP_SECONDS = "seconds";
  private static final String GROUP_CUR_ITEM = "curitem";
  private static final String GROUP_TOTAL_ITEMS = "totalitems";
  private final InputStream stream;
  private final StringBuffer buffer;
  private final DownloadProgressCallback callback;

  private static final Pattern patternSingleProgress = Pattern.compile(
      "\\[download\\]\\s+(?<percent>\\d+\\.\\d)% .* ETA (?<minutes>\\d+):(?<seconds>\\d+)");
  private static final Pattern patternPlaylistProgress = Pattern.compile(
      ".*\\[download\\] Downloading item (?<curitem>\\d+) of (?<totalitems>\\d+).*",
      Pattern.DOTALL);

  private int curItem = -1;
  private int totalItems = -1;

  public StreamProcessExtractor(StringBuffer buffer, InputStream stream,
      DownloadProgressCallback callback) {
    this.stream = stream;
    this.buffer = buffer;
    this.callback = callback;
    this.start();
  }

  public void run() {
    try {
      StringBuilder currentLine = new StringBuilder();
      int nextChar;
      while ((nextChar = stream.read()) != -1) {
        buffer.append((char) nextChar);
        if (nextChar == '\r' && callback != null) {
          processOutputLine(currentLine.toString());
          currentLine.setLength(0);
          continue;
        }
        currentLine.append((char) nextChar);
      }
    } catch (IOException ignored) {
    }
  }

  private void processOutputLine(String line) {
    Matcher playlistMatcher = patternPlaylistProgress.matcher(line);
    if (playlistMatcher.matches()) {
      curItem = Integer.parseInt(playlistMatcher.group(GROUP_CUR_ITEM));
      totalItems = Integer.parseInt(playlistMatcher.group(GROUP_TOTAL_ITEMS));
    }
    Matcher singleMatcher = patternSingleProgress.matcher(line);
    if (singleMatcher.matches()) {
      float progress = Float.parseFloat(singleMatcher.group(GROUP_PERCENT));
      long eta = convertToSeconds(singleMatcher.group(GROUP_MINUTES),
          singleMatcher.group(GROUP_SECONDS));
      callback.onProgressUpdate(progress, eta, curItem, totalItems);
    }
  }

  private int convertToSeconds(String minutes, String seconds) {
    return Integer.parseInt(minutes) * 60 + Integer.parseInt(seconds);
  }
}
