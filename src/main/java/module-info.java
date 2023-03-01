module de.ncrypted.ytplayloader {
  requires javafx.controls;
  requires javafx.fxml;
  requires lombok;

  opens de.ncrypted.ytplayloader to javafx.fxml;
  exports de.ncrypted.ytplayloader;
}