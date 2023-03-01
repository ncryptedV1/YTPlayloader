package de.ncrypted.ytplayloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class YTPLController {

  private static final List<Process> ytdlProcesses = new ArrayList<>();
  private static String ytdlPath;
  public TextField downloadLink;
  public Label downloadPathLabel;
  public Label downloaded;
  public ChoiceBox mp3Bitrate;
  public CheckBox convertToMp3;
  public TextArea logArea;
  private Stage stage;
  private File downloadFolder;
  private final ExecutorService ytdlExecutorService = Executors.newSingleThreadExecutor();

  private static List<String> extractFileNames(String ytdlOutput) {
    if (ytdlOutput == null) {
      return null;
    }
    String lineStart1 = "[ExtractAudio] Destination: ";
    List<String> names = new ArrayList<>();
    names.addAll(Arrays.stream(ytdlOutput.split("\n")).filter(s -> s.startsWith(lineStart1))
        .map(s -> s.substring(lineStart1.length())).toList());
    String lineStart2 = "[ExtractAudio] Not converting audio ";
    String lineEnd2 = "; file is already in target format mp3";
    names.addAll(Arrays.stream(ytdlOutput.split("\n")).filter(s -> s.startsWith(lineStart2))
        .map(s -> s.substring(lineStart2.length(), s.length() - lineEnd2.length()))
        .toList());
    return names;
  }

  public static void infoDownloaded(String fileName) {
    Logger.log("Downloaded: " + fileName);
    YTPLApplication.runOnUIThread(() -> getInstance().downloaded.setText(
        Integer.valueOf(getInstance().downloaded.getText()) + 1 + ""));
  }

  public static YTPLController getInstance() {
    return YTPLApplication.getController();
  }

  public void setStageAndSetup(Stage stage) {
    this.stage = stage;

    downloadFolder = new File(System.getProperty("user.home") + "/Downloads/YTPlayloader");
    if (!downloadFolder.exists()) {
      downloadFolder.mkdirs();
    }
    downloadPathLabel.setText(downloadFolder.getAbsolutePath());
    ytdlPath = Utils.setupResource("yt-dlp.exe");
    Utils.setupResource("ffmpeg.exe");
    Utils.setupResource("ffprobe.exe");
    Utils.setupResource("AtomicParsley.exe");

    ObservableList<String> bitrates = FXCollections.observableArrayList();
    bitrates.addAll(
        Bitrate.getValues().stream().map(Bitrate::getReadable).toList());
    mp3Bitrate.setItems(bitrates);
    mp3Bitrate.getSelectionModel().select(Bitrate.VBR2.getReadable());

    Logger.setLogArea(logArea);
    try {
      Process ytdlUpdate = new ProcessBuilder(ytdlPath, "-U").start();
      Utils.handleYTDLUpdateStream(ytdlUpdate);
    } catch (IOException e) {
      Logger.err("Couldn't update Youtube-DL - Stacktrace:");
      for (StackTraceElement traceElement : e.getStackTrace()) {
        Logger.log(traceElement.getClassName() + "." + traceElement.getMethodName() + ":"
            + traceElement.getLineNumber());
      }
    }
  }

  public void stop() {
    ytdlProcesses.forEach(process -> {
      System.out.println("Stopping YTDL process with PID " + process.pid() + "...");
      process.destroyForcibly();
    });
    System.out.println("Shutting down thread pool for bulk downloading...");
    ytdlExecutorService.shutdownNow();
  }

  public void download(ActionEvent actionEvent) {
    String url = downloadLink.getText();
    download(url);
  }

  private static String youtubeDlDownload(String url, File downloadFolder, String format,
      String audioQuality) {
    ProcessBuilder processBuilder = new ProcessBuilder(ytdlPath, "--ignore-errors", "--output",
        "%(title)s.%(ext)s", "--extract-audio", "--audio-format", format, "--audio-quality",
        audioQuality, "--embed-thumbnail", "--embed-metadata",
        url).directory(downloadFolder);

    Process process;
    try {
      process = processBuilder.start();
      ytdlProcesses.add(process);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    InputStream outStream = process.getInputStream();
    InputStream errStream = process.getErrorStream();

    StringBuffer outBuffer = new StringBuffer(); //stdout
    StringBuffer errBuffer = new StringBuffer(); //stderr
    Logger.log("Progress...");
    StreamProcessExtractor stdOutProcessor = new StreamProcessExtractor(outBuffer, outStream,
        (progress, etaInSec, curItem, totalItems) -> {
          String statusMsg = "Progress: ";
          if (curItem != -1) {
            statusMsg += curItem + "/" + totalItems + " | ";
          }
          statusMsg += progress + "% | ETA: " + etaInSec + "s";
          Logger.log(statusMsg, true);
        });
    StreamGobbler stdErrProcessor = new StreamGobbler(errBuffer, errStream);

    int exitCode = 0;
    try {
      stdOutProcessor.join();
      stdErrProcessor.join();
      exitCode = process.waitFor();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      ytdlProcesses.remove(process);
    }

    String out = outBuffer.toString();
    String err = errBuffer.toString();

    if (exitCode > 0) {
      throw new RuntimeException(err);
    }

    return out;
  }

  private void download(String url) {
    Logger.log("Downloading " + url + "...");
    ytdlExecutorService.submit(() -> {
      String youtubeDlOut = null;
      try {
        youtubeDlOut = youtubeDlDownload(url, downloadFolder,
            convertToMp3.isSelected() ? "mp3" : "aac",
            convertToMp3.isSelected() ? Bitrate.getByReadable(
                (String) mp3Bitrate.getSelectionModel().getSelectedItem()).getFfmpegArg() : "0");
      } catch (RuntimeException ex) {
        String msg = ex.getMessage();
        Logger.err("An error occured during the download:");
        if (msg.contains(" is not a valid URL.")) {
          Logger.err("'" + url + "' isn't a valid URL!");
        } else if (msg.contains("Unable to download webpage")) {
          Logger.err("Can't download webpage. Perhaps there is no internet connection?");
        } else {
          Logger.err("An error occured - Stacktrace:");
          for (StackTraceElement traceElement : ex.getStackTrace()) {
            Logger.log(traceElement.getClassName() + "." + traceElement.getMethodName() + ":"
                + traceElement.getLineNumber());
          }
          Logger.err(msg);
        }
      }

      if (youtubeDlOut != null) {
        for (String fileName : extractFileNames(youtubeDlOut)) {
          infoDownloaded(fileName);
        }
      }
    });
  }

  public void downloadList(ActionEvent actionEvent) {
    FileChooser fileChooser = new FileChooser();
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files", "*.txt"));
    File urlsFile = fileChooser.showOpenDialog(stage);
    if (urlsFile == null) {
      return;
    }
    try {
      BufferedReader reader = new BufferedReader(new FileReader(urlsFile));
      try {
        StringBuilder urls = new StringBuilder();
        String url;
        while ((url = reader.readLine()) != null) {
          urls.append("\"").append(url).append("\"").append(" ");
        }
        download(urls.toString());
      } catch (IOException e) {
      }
    } catch (FileNotFoundException e) {
    }
  }

  public void openDownloadPathSelector(ActionEvent actionEvent) {
    DirectoryChooser dirChooser = new DirectoryChooser();
    dirChooser.setInitialDirectory(downloadFolder);
    downloadFolder = dirChooser.showDialog(stage);
    if (downloadFolder == null) {
      return;
    }
    downloadPathLabel.setText(downloadFolder.getAbsolutePath());
  }
}
