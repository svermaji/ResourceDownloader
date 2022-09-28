package com.sv.downloader.helpers;

import com.sv.core.Utils;
import com.sv.downloader.ResourceDownLoader;

import java.util.concurrent.Callable;

public class TrackAllDownloadsCallable implements Callable<Boolean> {

    private final ResourceDownLoader rd;
    private static int timeToPrintTP;

    public TrackAllDownloadsCallable(ResourceDownLoader rd) {
        this.rd = rd;
        timeToPrintTP = 0;
    }

    @Override
    public Boolean call() {
        do {
            Utils.sleep(2000);
            timeToPrintTP++;
            if (timeToPrintTP == 10) {
                timeToPrintTP = 0;
                rd.printStatus();
            }
        } while (!rd.isUrlsToDownloadEmpty());

        rd.printStatusComplete();
        rd.enableControls();
        return true;
    }

}
