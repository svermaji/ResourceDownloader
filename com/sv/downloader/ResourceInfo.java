package com.sv.downloader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

public class ResourceInfo {
    private FileOutputStream fos;
    private ReadableByteChannel rbc;
    private FileInfo fileInfo;
    private FileStatus fileStatus;
    private static MyLogger logger;
    private final String url;

    public ResourceInfo(String url, MyLogger myLogger) {
        this.url = url;
        if (logger == null) {
            logger = myLogger;
        }
        fileStatus = FileStatus.IN_QUEUE;
    }

    public void updateResourceInfo(FileOutputStream fos, ReadableByteChannel rbc, FileInfo fileInfo) {
        if (!url.equals(fileInfo.getSrc())) {
            logger.log(url + " mismatched with " + fileInfo);
            throw new ResourceDownLoaderException("Url and file information mismatch");
        }
        this.fos = fos;
        this.rbc = rbc;
        this.fileInfo = fileInfo;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public String getUrl() {
        return url;
    }

    public FileOutputStream getFos() {
        return fos;
    }

    public ReadableByteChannel getRbc() {
        return rbc;
    }

    public FileStatus getFileStatus() {
        return fileStatus;
    }

    public void setFileStatus(FileStatus fileStatus) {
        this.fileStatus = fileStatus;
    }

    public void closeResource() {
        if (fileStatus.equals(FileStatus.DOWNLOADING)) {
            fileStatus = FileStatus.CANCELLED;
        }
        if (fos != null) {
            try {
                fos.flush();
                fos.close();
                if (rbc != null) {
                    rbc.close();
                }
                fos = null;
                rbc = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void markDownload() {
        if (fileStatus != FileStatus.CANCELLED) {
            fileStatus = FileStatus.DOWNLOADED;
        }
        logger.log("File status set to " + fileStatus);
        closeResource();
    }

    @Override
    public String toString() {
        return "ResourceInfo{" +
                "fileInfo=" + fileInfo +
                ", fileStatus=" + fileStatus +
                '}';
    }

    public boolean isCancelled() {
        return fileStatus.equals(FileStatus.CANCELLED);
    }
}

