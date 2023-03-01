package de.ncrypted.ytplayloader;

public interface DownloadProgressCallback {

  void onProgressUpdate(float progress, long etaInSeconds, int curItem, int totalItems);
}
