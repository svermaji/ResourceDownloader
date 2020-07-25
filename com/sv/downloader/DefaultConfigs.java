package com.sv.downloader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DefaultConfigs {

    enum Config {
        DOWNLOAD_LOC("default-download-location"),
        URLS_TO_DOWNLOAD("default-path-to-download-location");

        String val;
        Config (String val) {
            this.val = val;
        }

        public String getVal() {
            return val;
        }
    }
    private final Properties configs = new Properties();
    private final String propFileName = "com/sv/downloader/downloader.config";
    private MyLogger logger;

    public DefaultConfigs(MyLogger logger) {
        this.logger = logger;
        initialize();
    }

    public void initialize() {
        readConfig();
    }

    public String getConfig(Config config) {
        if (configs.containsKey(config.getVal()))
            return configs.getProperty(config.getVal());
        return Utils.EMPTY;
    }

    private void readConfig() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(propFileName)) {
            configs.load(is);
        } catch (IOException e) {
            logger.log ("Error in loading properties.");
        }
    }

    public void saveConfig(ResourceDownLoader rsd) {
        logger.log ("Saving properties.");
        configs.clear();
        configs.put(Config.DOWNLOAD_LOC.getVal(), rsd.getDownloadLoc());
        configs.put(Config.URLS_TO_DOWNLOAD.getVal(), rsd.getUrlsToDownload());
        logger.log ("Config is " + configs);
        try {
            configs.store(new FileOutputStream(propFileName), null);
        } catch (IOException e) {
            logger.log ("Error in saving properties.");
        }
    }
}
