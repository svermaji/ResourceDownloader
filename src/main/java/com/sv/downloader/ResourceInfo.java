package com.sv.downloader;

import com.sv.core.MyLogger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

public class ResourceInfo {
    private FileOutputStream fos;
    private ReadableByteChannel rbc;
    private FileInfo fileInfo;
    private FileStatus fileStatus;
    private int rowNum;
    private static MyLogger logger;
    private final String url;
    private final ResourceDownLoader rdl;

    public ResourceInfo(String url, MyLogger myLogger, ResourceDownLoader rdl) {
        this.url = url;
        if (logger == null) {
            logger = myLogger;
        }
        fileStatus = FileStatus.IN_QUEUE;
        this.rdl = rdl;
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

    public int getRowNum() {
        return rowNum;
    }

    public void setRowNum(int rowNum) {
        this.rowNum = rowNum;
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
        try {
            if (canMarkCancelled(fileStatus)) {
                logger.log("Marking status cancel for " + getUrl());
                fileStatus = FileStatus.CANCELLED;
                rdl.markDownloadCancelled(this);
            }
            if (fos != null) {
                fos.flush();
                fos.close();
                if (rbc != null) {
                    rbc.close();
                }
                fos = null;
                rbc = null;
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private boolean canMarkCancelled(FileStatus fileStatus) {
        return !fileStatus.equals(FileStatus.DOWNLOADED) &&
                !fileStatus.equals(FileStatus.EXISTS) &&
                !fileStatus.equals(FileStatus.FAILED);
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

    public boolean exists() {
        return fileStatus.equals(FileStatus.EXISTS);
    }
}

