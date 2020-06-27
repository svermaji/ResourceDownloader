package com.sv.downloader;

class FileInfo {
    private final String src, dest;
    private final long size;
    private long timeToDownloadInSec;

    FileInfo(String src, String dest, int size) {
        this.src = src;
        this.dest = dest;
        this.size = size;
    }

    public String getSrc() {
        return src;
    }

    public String getDest() {
        return dest;
    }

    public long getSize() {
        return size;
    }

    public long getTimeToDownloadInSec() {
        return timeToDownloadInSec;
    }

    public void setTimeToDownloadInSec(long timeToDownloadInSec) {
        this.timeToDownloadInSec = timeToDownloadInSec;
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "src='" + src + '\'' +
                ", dest='" + dest + '\'' +
                ", size=" + size +
                ", timeToDownloadInSec=" + timeToDownloadInSec +
                '}';
    }
}
