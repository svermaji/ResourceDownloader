package com.sv.downloader.helpers;

import com.sv.downloader.FileStatus;
import com.sv.downloader.ResourceDownLoader;
import com.sv.downloader.ResourceInfo;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class DownloadFileCallable implements Callable<Boolean> {

    private final ResourceDownLoader rd;
    private final ResourceInfo resourceInfo;

    public DownloadFileCallable(ResourceDownLoader rd, ResourceInfo resourceInfo) {
        this.rd = rd;
        this.resourceInfo = resourceInfo;
        if (rd.isDownloadable(resourceInfo)) {
            resourceInfo.setFileStatus(FileStatus.DOWNLOADING);
        }
    }

    @Override
    public Boolean call() throws Exception {
        long startTime = System.currentTimeMillis();

        rd.log("Starting download for " + resourceInfo);
        resourceInfo.getFos().getChannel().transferFrom
                (resourceInfo.getRbc(), 0, resourceInfo.getFileInfo().getSize());
        if (rd.isDownloadable(resourceInfo)) {
            long diffTime = (System.currentTimeMillis() - startTime);
            long diffTimeInSec = TimeUnit.MILLISECONDS.toSeconds(diffTime);
            resourceInfo.getFileInfo().setDownloadInSec(diffTimeInSec);
            rd.log("download complete for " + resourceInfo);
            rd.updateDownloadTime(resourceInfo.getFileInfo(), diffTimeInSec, resourceInfo.getRowNum());
        }
        resourceInfo.markDownload();
        rd.removeFromUrlsToDownload(resourceInfo.getUrl());

        return true;
    }
}

