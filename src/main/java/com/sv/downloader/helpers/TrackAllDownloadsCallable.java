package com.sv.downloader.helpers;

import com.sv.core.Utils;
import com.sv.downloader.ResourceDownLoader;

import java.util.concurrent.Callable;

public class TrackAllDownloadsCallable implements Callable<Boolean> {

    private final ResourceDownLoader rd;

    public TrackAllDownloadsCallable(ResourceDownLoader rd) {
        this.rd = rd;
    }

    @Override
    public Boolean call() {
        do {
            Utils.sleep(2000);
        } while (!rd.isUrlsToDownloadEmpty());

        rd.enableControls();
        return true;
    }

}
