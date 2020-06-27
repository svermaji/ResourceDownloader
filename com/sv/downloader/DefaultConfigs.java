package com.sv.downloader;

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
        String propFileName = "com/sv/downloader/downloader.config";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(propFileName)) {
            configs.load(is);
        } catch (IOException e) {
            logger.log ("Error in loading properties.");
        }
    }

}
