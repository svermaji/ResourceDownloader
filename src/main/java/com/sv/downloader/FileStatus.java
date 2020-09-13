package com.sv.downloader;

public enum FileStatus {

    DOWNLOADING("downloading"),
    IN_QUEUE("in queue"),
    FAILED("failed"),
    DOWNLOADED("downloaded"),
    CANCELLED("cancelled");

    String val;

    FileStatus(String val) {
        this.val = val;
    }

    public String getVal() {
        return val;
    }
}
