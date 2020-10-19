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
        //logger.log("setFileStatus - "+fileStatus.getVal() + ", fs = " + this.fileStatus);
        //if (!isCancelled() && !isFailed()) {
        //  logger.log("setting - ");
        this.fileStatus = fileStatus;
        //}
    }

    public void closeResource() {
        logger.log("Closing resources for [" + getUrl() + "]");
        if (canMarkCancelled(fileStatus)) {
            synchronized (ResourceInfo.class) {
                fileStatus = FileStatus.CANCELLED;
            }
        }
        try {
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

        if (fileStatus != FileStatus.DOWNLOADED) {
            // first close resource then try to delete
            rdl.markDownloadCancelled(this);
        }
    }

    private boolean canMarkCancelled(FileStatus fileStatus) {
        return !fileStatus.equals(FileStatus.DOWNLOADED) &&
                !exists() && !isFailed();
    }

    public void markDownload() {
        if (!isCancelled()) {
            fileStatus = FileStatus.DOWNLOADED;
        }
        logger.log(nameAndStatus());
        closeResource();
    }

    @Override
    public String toString() {
        return "ResourceInfo{" +
                "fileInfo=" + fileInfo +
                ", fileStatus=" + fileStatus.getVal() +
                '}';
    }

    public String nameAndStatus() {
        return "File [" + fileInfo.getOnlyName()
                + "], Status [" + fileStatus.getVal()
                + "]";
    }

    public boolean isCancelled() {
        return fileStatus.equals(FileStatus.CANCELLED);
    }

    public boolean isFailed() {
        return fileStatus.equals(FileStatus.FAILED);
    }

    public boolean exists() {
        return fileStatus.equals(FileStatus.EXISTS);
    }
}
