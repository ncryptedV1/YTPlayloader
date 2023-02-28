package de.ncrypted.ytplayloader;

import java.io.File;
import java.net.URISyntaxException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.Getter;

public class YTPlayloader extends Application {

  @Getter
  private static YTPLController controller;
  @Getter
  private static String jarDir;

  static {
    try {
      jarDir = new File(YTPlayloader.class.getProtectionDomain()
          .getCodeSource()
          .getLocation()
          .toURI())
          .getParentFile()
          .getPath();
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    launch(args);
  }

  public static void runOnUIThread(Runnable runnable) {
    Platform.runLater(runnable);
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/YTPLGui.fxml"));
    Parent root = loader.load();
    primaryStage.setTitle("Made with {} and <3 by Oliver Schirmer");
    primaryStage.getIcons()
        .add(new Image(
            YTPlayloader.class.getClassLoader().getResourceAsStream("blackyt_arrow.png")));
    primaryStage.setScene(new Scene(root));
    primaryStage.show();

    controller = loader.getController();
    controller.setStageAndSetup(primaryStage);
  }

  @Override
  public void stop() throws Exception {
    System.out.println("Stage is shutting down...");
    controller.stop();
    super.stop();
  }
}
