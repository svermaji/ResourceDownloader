package com.sv.downloader;

import java.util.concurrent.TimeUnit;

class FileInfo {
    private final String src, dest;
    private String filename = null;
    private final long size;
    private long downloadedSize;
    private long downloadStartTime;
    private long downloadInSec;

    FileInfo(String src, String dest, int size, long downloadStartTime) {
        this.src = src;
        this.dest = dest;
        this.size = size;
        this.downloadStartTime = downloadStartTime;
    }

    public String getSrc() {
        return src;
    }

    public String getDest() {
        return dest;
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
                ", dest='" + dest + '\'' +
                ", size=" + size +
                ", timeToDownloadInSec=" + downloadInSec +
                '}';
    }
}
