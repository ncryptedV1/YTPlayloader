package de.ncrypted.ytplayloader;

import com.sapher.youtubedl.YoutubeDL;
import com.sapher.youtubedl.YoutubeDLException;
import com.sapher.youtubedl.YoutubeDLRequest;
import com.sapher.youtubedl.YoutubeDLResponse;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class YTPLController {

    public TextField downloadLink;
    public Label downloadPathLabel;

    public Label downloaded;
    public Label converted;

    public ChoiceBox mp3Bitrate;
    public CheckBox convertToMp3;
    public TextArea logArea;


    private Stage stage;
    private File downloadPath;
    private ExecutorService pool = Executors.newCachedThreadPool();
    private static List<Process> ffmpegProcesses = new ArrayList<>();

    private static String YTDLPath;
    private static String FfmpegPath;
    private static String AtomicParsleyPath;

    public void setStageAndSetup(Stage stage) {
        this.stage = stage;

        downloadPath = new File(System.getProperty("user.home") + "/Downloads/YTPlayloader");
        if (!downloadPath.exists()) {
            downloadPath.mkdirs();
        }
        downloadPathLabel.setText(downloadPath.getAbsolutePath());
        YTDLPath = YTPlayloader.getJarDir() + File.separator + "youtube-dl.exe";
        FfmpegPath = YTPlayloader.getJarDir() + File.separator + "ffmpeg.exe";
        AtomicParsleyPath = YTPlayloader.getJarDir() + File.separator + "AtomicParsley.exe";
        try {
            File ffmpegTarget = new File(FfmpegPath);
            File ytdlTarget = new File(YTDLPath);
            File atomicParsleyTarget = new File(AtomicParsleyPath);
            if (!ffmpegTarget.exists()) {
                Files.copy(YTPLController.class.getClassLoader().getResource("ffmpeg.exe").openStream(),
                        ffmpegTarget.toPath());
            }
            if (!ytdlTarget.exists()) {
                Files.copy(YTPLController.class.getClassLoader().getResource("youtube-dl.exe").openStream(),
                        ytdlTarget.toPath());
            }
            if (!atomicParsleyTarget.exists()) {
                Files.copy(YTPLController.class.getClassLoader().getResource("AtomicParsley.exe").openStream(),
                        atomicParsleyTarget.toPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        YoutubeDL.setExecutablePath(YTDLPath);

        ObservableList<String> bitrates = FXCollections.observableArrayList();
        bitrates.addAll(Bitrate.getValues().stream().map(Bitrate::getReadable).collect(Collectors.toList()));
        mp3Bitrate.setItems(bitrates);
        mp3Bitrate.getSelectionModel().select(Bitrate.VBR2.getReadable());

        Logger.setLogArea(logArea);
        try {
            Process ytdlUpdate = Runtime.getRuntime().exec(YTDLPath + " -U");
            Utils.handleYTDLUpdateStream(ytdlUpdate);
        } catch (IOException e) {
            Logger.err("Couldn't update Youtube-DL - Stacktrace:");
            for (StackTraceElement traceElement : e.getStackTrace()) {
                Logger.log(traceElement.getClassName() + "." + traceElement.getMethodName() + ":" +
                        traceElement.getLineNumber());
            }
        }
    }

    public void stop() {
        ffmpegProcesses.forEach(process -> process.destroyForcibly());
        pool.shutdown();
    }

    public void download(ActionEvent actionEvent) {
        String url = downloadLink.getText();
        download(url);
    }

    private void download(String url) {
        Logger.log("Downloading " + url + "...");
        pool.submit(() -> {
            YoutubeDLRequest request = new YoutubeDLRequest(url, downloadPath.getAbsolutePath());
            request.setOption("ignore-errors");
            request.setOption("output", "%(title)s.%(ext)s");
            request.setOption("encoding", "utf-8");
            request.setOption("retries", 10);
            request.setOption("format", "bestaudio[ext=m4a]");
            request.setOption("add-metadata");
            request.setOption("embed-thumbnail");
            request.setOption("ffmpeg-location", "\"" + FfmpegPath + "\"");
            YoutubeDLResponse response = null;
            try {
                response = YoutubeDL.execute(request);
            } catch (YoutubeDLException e) {
                String msg = e.getMessage();
                if (msg.contains(" is not a valid URL.")) {
                    Logger.err("'" + url + "' isn't a valid URL!");
                } else if (msg.contains("Unable to download webpage")) {
                    Logger.err("Can't download webpage. Perhaps there is no internet connection?");
                } else {
                    Logger.err("An error occured - Stacktrace:");
                    for (StackTraceElement traceElement : e.getStackTrace()) {
                        Logger.log(traceElement.getClassName() + "." + traceElement.getMethodName() + ":" +
                                traceElement.getLineNumber());
                    }
                }
            }
            try {
                boolean convertMp3 = convertToMp3.isSelected();
                for (String fileName : extractFileNames(response.getOut())) {
                    infoDownloaded(fileName);
                    File downloaded = new File(
                            response.getDirectory() + File.separator + fileName);
                    if (convertMp3) {
                        pool.submit(() -> {
                            convertToMp3(downloaded, Bitrate.getByReadable(
                                    (String) mp3Bitrate.getSelectionModel().getSelectedItem()));
                        });
                    }
                }
            } catch (IllegalStateException ex) {
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
                String urls = "";
                String url;
                while ((url = reader.readLine()) != null) {
                    urls += "\"" + url + "\"" + " ";
                }
                download(urls);
            } catch (IOException e) {
            }
        } catch (FileNotFoundException e) {
        }
    }

    public void openDownloadPathSelector(ActionEvent actionEvent) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setInitialDirectory(downloadPath);
        downloadPath = dirChooser.showDialog(stage);
        if (downloadPath == null) {
            return;
        }
        downloadPathLabel.setText(downloadPath.getAbsolutePath());
    }

    private static void convertToMp3(File file, Bitrate bitrate) {
        if (!file.exists()) {
            return;
        }
        if (!file.getName().endsWith(".m4a")) {
            return;
        }
        String input = file.getAbsolutePath();
        Logger.log("Converting " + input + "...");
        String output = input.substring(0, input.length() - 3) + "mp3";
        execFfmpeg(() -> {
                    new File(input).delete();
                    infoConverted(output);
                }, "-i", "\"" + input +
                        "\"", "-vcodec", "mjpeg", "-vf", "scale=500:500,setsar=1:1,setdar=1:1", "-acodec", "libmp3lame",
                "-id3v2_version", "3", "-metadata:s:v", "comment=\"Cover", "(front)\"",
                (bitrate.isVBR() ? "-q:a" : "-b:a"),
                bitrate.getFfmpegArg(), "\"" + output + "\"");
    }

    private static void execFfmpeg(Runnable onFinish, String... args) {
        String[] cmd = new String[args.length + 1];
        cmd[0] = "\"" + FfmpegPath + "\"";
        for (int i = 0; i < args.length; i++) {
            cmd[i + 1] = args[i];
        }
        try {
            Process conversion = Runtime.getRuntime().exec(cmd);
            ffmpegProcesses.add(conversion);
            Utils.readErrorStream(conversion);
            ffmpegProcesses.remove(conversion);
            onFinish.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<String> extractFileNames(String ytdlOutput) {
        if (ytdlOutput == null) {
            return null;
        }
        List<String> nameLines = Arrays.stream(ytdlOutput.split("\n")).filter(
                s -> s.startsWith("[download] Destination:")).collect(Collectors.toList());
        List<String> names = new ArrayList<>();
        nameLines.forEach(line -> {
            String[] lineArr = line.split(" ");
            String fileName = "";
            for (int i = 2; i < lineArr.length; i++) {
                fileName += lineArr[i] + " ";
            }
            names.add(fileName.substring(0, fileName.length() - 1));
        });
        return names;
    }

    public static void infoDownloaded(String fileName) {
        Logger.log("Downloaded: " + fileName);
        YTPlayloader.runOnUIThread(
                () -> getInstance().downloaded.setText(Integer.valueOf(getInstance().downloaded.getText()) + 1 + ""));
    }

    public static void infoConverted(String fileName) {
        Logger.log("Converted - " + fileName);
        YTPlayloader.runOnUIThread(
                () -> getInstance().converted.setText(Integer.valueOf(getInstance().converted.getText()) + 1 + ""));
    }

    public static YTPLController getInstance() {
        return YTPlayloader.getController();
    }
}
