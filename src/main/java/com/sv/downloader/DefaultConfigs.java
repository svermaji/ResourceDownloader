package com.sv.downloader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    private final String propFileName = "./conf.config";
    private final MyLogger logger;
    private URL propUrl;

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
        logger.log ("Loading properties from path: " + propFileName);
        try (InputStream is = Files.newInputStream(Paths.get(propFileName))) {
            propUrl = Paths.get(propFileName).toUri().toURL();
            configs.load(is);
        } catch (Exception e) {
            logger.log ("Error in loading properties via file path, trying class loader. Message: " + e.getMessage());
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(propFileName)) {
                propUrl = Paths.get(propFileName).toUri().toURL();
                configs.load(is);
            } catch (IOException ioe) {
                logger.log ("Error in loading properties via class loader. Message: " + ioe.getMessage());
            } catch (RuntimeException exp) {
                logger.log ("Error in loading properties. Message: " + exp.getMessage());
            }
        }
        logger.log ("Prop url calculated as: " + propUrl);
    }

    public void saveAllConfigs(ResourceDownLoader rsd) {
        saveConfig(rsd);
        saveUrls(rsd);
    }

    private void saveUrls(ResourceDownLoader rsd) {
        logger.log ("Saving urls.");
        try {
            FileOutputStream fos = new FileOutputStream(rsd.getUrlsToDownload());
            fos.write(rsd.getDownloadingUrls().getBytes());
        } catch (IOException e) {
            logger.error ("Error in saving urls.");
            logger.error (e);
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
