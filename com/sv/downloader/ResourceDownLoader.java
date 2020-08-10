package com.sv.downloader;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * This class will help in downloading bunch of urls that
 * may point to resource like images, pdfs etc.
 */
public class ResourceDownLoader extends AppFrame {

    public enum COLS {
        IDX(0, "#", "center", 50),
        PATH(1, "Path", "left", 0),
        NAME(2, "Name", "left", -1),
        PERCENT(3, "Percent", "left", 100),
        SIZE(4, "Size", "center", 150),
        TIME(5, "Time in sec", "center", 100);

        String name, alignment;
        int idx, width;

        COLS(int idx, String name, String alignment, int width) {
            this.name = name;
            this.idx = idx;
            this.alignment = alignment;
            this.width = width;
        }

        public String getName() {
            return name;
        }

        public int getIdx() {
            return idx;
        }

        public String getAlignment() {
            return alignment;
        }

        public int getWidth() {
            return width;
        }
    }

    private final int KB = 1024;
    private JTextField txtDest, txtSource;
    private JButton btnDownload, btnCancel, btnExit;
    private JTable tblInfo;
    private JTextArea txtUrls;
    private String[] emptyRow;
    private DefaultTableModel model;
    private Map<String, ResourceInfo> urlsToDownload;
    private List<String> urlsFromFile;
    private final int DEFAULT_NUM_ROWS = 6;

    private MyLogger logger = MyLogger.createLogger("resource-downloader.log");
    private DefaultConfigs configs = new DefaultConfigs(logger);
    private TrustManager[] trustAllCerts;
    private final String title = "Resource Downloader";
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(3);

    public static final String FAILED_MSG = "Failed !! - ";
    public static final String CANCELLED_MSG = "Cancelled !! - ";

    Border borderBlue = new LineBorder(Color.BLUE, 1);
    Border emptyBorder = new EmptyBorder(new Insets(5, 5, 5, 5));

    private void createTrustManager() {
        trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };
    }

    /**
     * This method initializes the form.
     */
    private void initComponents() {

        emptyRow = new String[COLS.values().length];
        Arrays.fill(emptyRow, Utils.EMPTY);

        createTrustManager();
        trustAllHttps();
        urlsToDownload = new HashMap<>();

        Container parentContainer = getContentPane();
        JPanel controlsPanel = new JPanel();

        parentContainer.setLayout(new BorderLayout());
        setTitle(title);

        JLabel lblSource = new JLabel("Download from");
        txtSource = new JTextField();
        btnDownload = new JButton("DownLoad");
        btnCancel = new JButton("Cancel");
        btnExit = new JButton("Exit");
        JLabel lblDest = new JLabel("Location To Save");
        txtDest = new JTextField(configs.getConfig(DefaultConfigs.Config.DOWNLOAD_LOC));
        txtDest.setColumns(20);

        controlsPanel.setLayout(new FlowLayout());

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                exitForm();
            }
        });

        controlsPanel.add(lblSource);
        controlsPanel.add(txtSource);
        txtSource.setText(configs.getConfig(DefaultConfigs.Config.URLS_TO_DOWNLOAD));
        txtSource.setColumns(20);

        controlsPanel.add(lblDest);
        controlsPanel.add(txtDest);
        btnDownload.addActionListener(evt -> startDownLoad(txtSource.getText()));
        controlsPanel.add(btnDownload);
        btnCancel.addActionListener(evt -> cancelDownLoad());
        controlsPanel.add(btnCancel);
        btnExit.addActionListener(evt -> exitForm());
        controlsPanel.add(btnExit);

        createTable();
        JScrollPane jspTable = new JScrollPane(tblInfo);
        jspTable.setBorder(emptyBorder);

        txtUrls = new JTextArea(getUrls(), 5, 1);
        txtUrls.setBorder(borderBlue);
        JScrollPane jspUrls = new JScrollPane(txtUrls);
        jspUrls.setBorder(emptyBorder);

        JPanel jpUrls = new JPanel(new BorderLayout());
        jpUrls.add(new JLabel(" Urls to download"), BorderLayout.NORTH);
        jpUrls.add(jspUrls, BorderLayout.CENTER);

        JPanel jpTblAndUrls = new JPanel(new GridLayout(2, 1));
        jpTblAndUrls.add(jspTable);
        jpTblAndUrls.add(jpUrls);

        parentContainer.add(controlsPanel, BorderLayout.NORTH);
        parentContainer.add(jpTblAndUrls, BorderLayout.CENTER);

        setToCenter();
        logger.log("Program initialized");
    }

    private String getUrls() {
        urlsFromFile = getUrlsFromFile(txtSource.getText());
        StringBuilder sb = new StringBuilder();
        urlsFromFile.forEach(s -> sb.append(s).append(System.lineSeparator()));
        return sb.toString();
    }

    private void createTable() {
        model = new DefaultTableModel() {

            @Override
            public int getColumnCount() {
                return COLS.values().length;
            }

            @Override
            public String getColumnName(int index) {
                return COLS.values()[index].getName();
            }

        };

        createDefaultRows();

        tblInfo = new JTable(model);
        tblInfo.setBorder(borderBlue);

        // For making contents non editable
        tblInfo.setDefaultEditor(Object.class, null);

        tblInfo.setAutoscrolls(true);
        tblInfo.setPreferredScrollableViewportSize(tblInfo.getPreferredSize());
        // PATH col contains tooltip

        CellRendererLeftAlign leftRenderer = new CellRendererLeftAlign();
        CellRendererCenterAlign centerRenderer = new CellRendererCenterAlign();

        for (COLS col : COLS.values()) {
            tblInfo.getColumnModel().getColumn(col.getIdx()).setCellRenderer(
                    col.getAlignment().equals("center") ? centerRenderer : leftRenderer);

            if (col.getWidth() != -1) {
                tblInfo.getColumnModel().getColumn(col.getIdx()).setMinWidth(col.getWidth());
                tblInfo.getColumnModel().getColumn(col.getIdx()).setMaxWidth(col.getWidth());
            }
        }

        tblInfo.getColumnModel().getColumn(COLS.PERCENT.getIdx()).setCellRenderer(new CellRendererProgressBar());

    }

    private void createDefaultRows() {
        IntStream.range(0, DEFAULT_NUM_ROWS).forEach(i -> model.addRow(emptyRow));
    }

    private void trustAllHttps() {
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private void setToCenter() {
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Exit the Application
     */
    private void exitForm() {
        cancelDownLoad();
        configs.saveAllConfigs(this);
        logger.log("Goodbye");
        logger.dispose();
        setVisible(false);
        dispose();
        System.exit(0);
    }

    public void updateTitle(String info) {
        setTitle(Utils.hasValue(info) ? title + " - " + info : title);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new ResourceDownLoader().initComponents();
    }

    private void downLoad(ResourceInfo resourceInfo) {
        String url = resourceInfo.getUrl();
        logger.log("Trying url [" + url + "]");
        ReadableByteChannel rbc = null;
        FileOutputStream fos = null;
        FileInfo fileInfo = null;
        try {
            URL u = new URL(url);
            URLConnection uc = u.openConnection();
            fileInfo = new FileInfo(url, getDestPath(url), uc.getContentLength());
            logger.log("Url resource size is [" + fileInfo.getSize() + "] Bytes i.e. [" + (fileInfo.getSize() / KB) + "] KB");

            rbc = Channels.newChannel(u.openStream());
            fos = getFOS(fileInfo, resourceInfo.getRowNum());
            resourceInfo.updateResourceInfo(fos, rbc, fileInfo);

            // ignoring boolean status from Callable
            threadPool.submit(new DownloadFileCallable(this, resourceInfo));
            startTracking(resourceInfo);

        } catch (Exception e) {
            logger.error(e);
            markDownloadFailed(fileInfo.getSrc(), resourceInfo.getRowNum());
        }
    }

    private FileOutputStream getFOS(FileInfo fileInfo, int row) throws Exception {
        try {
            return new FileOutputStream(fileInfo.getDest());
        } catch (Exception e) {
            logger.warn("Destination [" + fileInfo.getDest() + "] does not have name.  Trying from url itself");
        }
        return getFOSFromUrl(fileInfo, row);
    }

    private FileOutputStream getFOSFromUrl(FileInfo fileInfo, int row) throws Exception {
        URLConnection conn = new URL(fileInfo.getSrc()).openConnection();
        String disposition = conn.getHeaderField("Content-disposition");
        fileInfo.setFilename(getDestPathForName(extractFileNameFromCD(disposition)));

        updateFileNameInTable(fileInfo, row);
        return new FileOutputStream(fileInfo.getFilename());
    }

    private String extractFileNameFromCD(String cdStr) {
        logger.log("Content disposition from url obtained as [" + cdStr + "]");
        String FN_STR = "filename=\"";
        if (cdStr.contains(FN_STR)) {
            cdStr = cdStr.substring(cdStr.indexOf(FN_STR) + FN_STR.length());
            if (cdStr.contains("\"")) {
                cdStr = cdStr.substring(0, cdStr.indexOf("\""));
            }
        }
        logger.log("Returning name extracted from content disposition as [" + cdStr + "]");
        return cdStr;
    }

    public void markDownloadFailed(String src, int row) {
        markDownloadForError(src, FAILED_MSG, row);
    }

    public void markDownloadCancelled(String src, int row) {
        markDownloadForError(src, CANCELLED_MSG, row);
    }

    private void markDownloadForError(String src, String msg, int i) {
        logger.log("Marking cancel for " + src);
        if (isPathMatched(src, i)) {
            if (!tblInfo.getValueAt(i, COLS.NAME.getIdx()).toString().startsWith(FAILED_MSG) &&
                    !tblInfo.getValueAt(i, COLS.NAME.getIdx()).toString().startsWith(CANCELLED_MSG)) {
                tblInfo.setValueAt(msg + tblInfo.getValueAt(i, COLS.NAME.getIdx()), i, COLS.NAME.getIdx());
            }
        }
    }

    private void startTracking(ResourceInfo resourceInfo) {
        // ignoring boolean status from Callable
        threadPool.submit(new DownloadStatusCallable(this, resourceInfo));
    }

    private boolean isHttpUrl(String s) {
        return s.startsWith("http");
    }

    private String getDestPath(String url) {
        return getDestPathForName(url.substring(url.lastIndexOf("/")));
    }

    private String getDestPathForName(String name) {
        String destFolder = txtDest.getText();
        if (!Utils.hasValue(destFolder)) {
            destFolder = ".";
        }
        if (!name.startsWith("\\") || !name.startsWith("/")) {
            name = "\\" + name;
        }
        String path = destFolder + name;
        logger.log("Destination path is [" + path + "]");
        return path;
    }

    private void cancelDownLoad() {
        if (!urlsToDownload.isEmpty()) {
            logger.log("Cancelling all downloads...");

            urlsToDownload.forEach((u, ri) -> ri.closeResource());
            urlsToDownload.clear();
            enableControls();
            updateTitle("Cancelled download!!");

            logger.log("Cancelling done.");
        }
    }

    private void startDownLoad(String srcPath) {

        disableControls();
        clearOldRun();
        urlsToDownload.put(srcPath, new ResourceInfo(srcPath, this.logger, this));

        if (!isHttpUrl(srcPath)) {
            urlsToDownload.clear();
            urlsFromFile = readUrlsFromTextArea();
            if (urlsFromFile.isEmpty()) {
                enableControls();
                updateTitle("No urls to download !!");
            }
            updateTitle("Starting download");
            urlsFromFile.forEach(f -> {
                if (Utils.hasValue(f)) {
                    urlsToDownload.put(f, new ResourceInfo(f, this.logger, this));
                }
            });
        }
        logger.log("Downloading urls are " + urlsToDownload.keySet());

        createRowsInTable(urlsToDownload);
        threadPool.submit(new StartDownloadCallable(this, urlsToDownload));
    }

    private List<String> readUrlsFromTextArea() {
        return Arrays.asList(txtUrls.getText().split(System.lineSeparator()));
    }

    private void clearOldRun() {
        urlsToDownload.clear();
        //empty table
        IntStream.range(0, tblInfo.getRowCount()).forEach(i -> model.removeRow(0));
        createDefaultRows();
    }

    private void createRowsInTable(Map<String, ResourceInfo> urlsToDownload) {
        int i = 0;
        for (Map.Entry<String, ResourceInfo> entry : urlsToDownload.entrySet()) {
            String k = entry.getKey();
            entry.getValue().setRowNum(i);
            if (i < DEFAULT_NUM_ROWS) {
                tblInfo.setValueAt(i + 1, i, COLS.IDX.getIdx());
                tblInfo.setValueAt(k, i, COLS.PATH.getIdx());
                tblInfo.setValueAt(Utils.getFileName(k), i, COLS.NAME.getIdx());
                tblInfo.setValueAt(0, i, COLS.PERCENT.getIdx());
                tblInfo.setValueAt(0, i, COLS.TIME.getIdx());
            } else {
                String[] row = {(i + 1) + "", k, Utils.getFileName(k), "0", "0"};
                model.addRow(row);
            }
            i++;
        }
    }

    private void updateControls(boolean enable) {
        txtSource.setEnabled(enable);
        txtDest.setEnabled(enable);
        btnDownload.setEnabled(enable);
    }

    private void enableControls() {
        updateControls(true);
    }

    private void disableControls() {
        updateControls(false);
    }

    private java.util.List<String> getUrlsFromFile(String filePath) {
        logger.log("Current directory is: " + System.getProperty("user.dir") + ", file path is " + filePath);
        Path path = Utils.createPath(filePath);
        try {
            return Files.readAllLines(path);
        } catch (IOException e) {
            logger.error(e);
            return new ArrayList<>();
        }
    }

    static class DownloadFileCallable implements Callable<Boolean> {

        private final ResourceDownLoader rd;
        private final ResourceInfo resourceInfo;

        public DownloadFileCallable(ResourceDownLoader rd, ResourceInfo resourceInfo) {
            this.rd = rd;
            this.resourceInfo = resourceInfo;
            if (!resourceInfo.isCancelled()) {
                resourceInfo.setFileStatus(FileStatus.DOWNLOADING);
            }
        }

        @Override
        public Boolean call() throws Exception {
            long startTime = System.currentTimeMillis();

            rd.logger.log("Starting download for " + resourceInfo);
            resourceInfo.getFos().getChannel().transferFrom
                    (resourceInfo.getRbc(), 0, resourceInfo.getFileInfo().getSize());
            if (!resourceInfo.isCancelled()) {
                long diffTime = (System.currentTimeMillis() - startTime);
                long diffTimeInSec = TimeUnit.MILLISECONDS.toSeconds(diffTime);
                resourceInfo.getFileInfo().setDownloadInSec(diffTimeInSec);
                rd.logger.log("download complete for " + resourceInfo);
                rd.updateDownloadTime(resourceInfo.getFileInfo(), diffTimeInSec, resourceInfo.getRowNum());
            }
            resourceInfo.markDownload();
            rd.enableControls();

            return true;
        }
    }

    static class StartDownloadCallable implements Callable<Boolean> {

        private final ResourceDownLoader rd;
        private final Map<String, ResourceInfo> urlsToDownload;

        public StartDownloadCallable(ResourceDownLoader rd, Map<String, ResourceInfo> urlsToDownload) {
            this.rd = rd;
            this.urlsToDownload = urlsToDownload;
        }


        @Override
        public Boolean call() {
            urlsToDownload.forEach((k, v) -> rd.downLoad(v));
            return true;
        }
    }

    static class DownloadStatusCallable implements Callable<Boolean> {

        private final ResourceInfo resourceInfo;
        private final FileInfo fileInfo;
        private final ResourceDownLoader rd;

        DownloadStatusCallable(ResourceDownLoader rd, ResourceInfo resourceInfo) {
            rd.logger.log("Download status tracking start for " + resourceInfo.getUrl());
            this.resourceInfo = resourceInfo;
            this.fileInfo = resourceInfo.getFileInfo();
            this.rd = rd;
        }

        @Override
        public Boolean call() throws Exception {
            int percent = 0, KB = 1024;
            long lastSize = 0, fileSize = fileInfo.getSize();
            String speedStr;
            StringBuilder sbLogInfo;

            do {
                fileInfo.setDownloadStartTime(System.currentTimeMillis());
                sbLogInfo = new StringBuilder("Status: " + resourceInfo.getFileStatus().getVal());
                if (resourceInfo.isCancelled()) {
                    rd.logger.log(sbLogInfo.toString());
                    break;
                }
                String dest = Utils.hasValue(fileInfo.getFilename()) ? fileInfo.getFilename() : fileInfo.getDest();
                long size = Files.size(Utils.createPath(dest));
                sbLogInfo.append(", Downloaded size [").append(size).append("/").append(fileSize).append("]");
                percent = (int) ((size * 100) / fileSize);
                resourceInfo.getFileInfo().setDownloadedSize(size);
                sbLogInfo.append(", percent ").append(percent).append("%");

                // since we are invoking thread every 200 ms - for now changing to 1sec
                float speed = (int) ((size - lastSize) / KB) * 4;
                lastSize = size;
                speedStr = String.format("%.2f", speed) + "KBs";

                sbLogInfo.append(", Speed ").append(speedStr);

                if (speed / KB > 1) {
                    // converting to MB
                    speed /= KB;
                    speedStr = String.format("%.2f", speed) + "MBs";
                    sbLogInfo.append(" or ").append(speedStr);
                }
                rd.updateTitle(percent + "% at [" + speedStr + "]");
                rd.logger.log(sbLogInfo.toString());

                rd.updateFileStatus(fileInfo, percent, resourceInfo.getRowNum());

                Thread.sleep(250);
            } while (percent < 100);

            return true;
        }

    }

    private void updateDownloadTime(FileInfo fileInfo, long time, int i) {
        if (isPathMatched(fileInfo.getSrc(), i)) {
            tblInfo.setValueAt(time, i, COLS.TIME.getIdx());
        }
    }

    private void updateFileStatus(FileInfo fileInfo, int percent, int i) {
        if (isPathMatched(fileInfo.getSrc(), i)) {
            tblInfo.setValueAt(percent, i, COLS.PERCENT.getIdx());
            tblInfo.setValueAt(getDownloadSize(fileInfo), i, COLS.SIZE.getIdx());
            tblInfo.setValueAt(getDownloadTime(fileInfo), i, COLS.TIME.getIdx());
        }
    }

    private void updateFileNameInTable(FileInfo fileInfo, int i) {
        if (isPathMatched(fileInfo.getSrc(), i)) {
            tblInfo.setValueAt(Utils.getFileNameForLocal(fileInfo.getFilename()), i, COLS.NAME.getIdx());
        }
    }

    private boolean isPathMatched(String src, int row) {
        return tblInfo.getValueAt(row, COLS.PATH.getIdx()).toString().equals(src);
    }

    private String getDownloadSize(FileInfo fileInfo) {
        float size = fileInfo.getSize() / KB;
        float dsize = fileInfo.getDownloadedSize() / KB;
        String suffix = "KB";
        if (size > KB) {
            // converting to MB
            size /= KB;
            dsize /= KB;
            suffix = "MB";
        }
        return String.format("%.2f", dsize) + "/" + String.format("%.2f", size) + suffix;

    }

    private String getDownloadTime(FileInfo fileInfo) {
        return fileInfo.getDownloadInSec() + "";
    }

    public String getUrlsToDownload() {
        return txtSource.getText();
    }

    public String getDownloadingUrls() {
        return txtUrls.getText();
    }

    public String getDownloadLoc() {
        return txtDest.getText();
    }

}

