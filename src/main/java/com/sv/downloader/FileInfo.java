package com.sv.downloader;

import com.sv.core.Utils;

import java.util.concurrent.TimeUnit;

public class FileInfo {
    private final String src, destination;
    private String filename = null;
    private final long size;
    private long downloadedSize = 0;
    private long downloadStartTime = 0;
    private long downloadInSec = 0;

    public FileInfo(String src, String destination, int size) {
        this.src = src;
        this.destination = destination;
        this.size = size;
    }

    public String getSrc() {
        return src;
    }

    public String getDestination() {
        return destination;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }

    public long getSize() {
        return size;
    }

    public long getDownloadStartTime() {
        return downloadStartTime;
    }

    public void setDownloadStartTime(long downloadStartTime) {
        if (this.downloadStartTime == 0) {
            this.downloadStartTime = downloadStartTime;
        }
    }

    public long getDownloadInSec() {
        downloadInSec = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - downloadStartTime);
        return downloadInSec;
    }

    public long getDownloadedSize() {
        return downloadedSize;
    }

    public void setDownloadedSize(long downloadedSize) {
        this.downloadedSize = downloadedSize;
    }

    public void setDownloadInSec(long downloadInSec) {
        this.downloadInSec = downloadInSec;
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "src='" + src + '\'' +
                ", dest='" + destination + '\'' +
                ", size=" + size +
                ", timeToDownloadInSec=" + downloadInSec +
                '}';
    }

}
