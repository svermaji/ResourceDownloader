package com.sv.downloader;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
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

    private JTextField txtDest, txtSource;
    private JButton btnDownload, btnCancel, btnExit;
    private JTable tblInfo;
    private String[] emptyRow;
    private DefaultTableModel model;
    private Map<String, ResourceInfo> urlsToDownload;
    private final int DEFAULT_NUM_ROWS = 6;

    public enum COLS {
        IDX(0, "#", "center"),
        PATH(1, "Path", "left"),
        NAME(2, "Name", "left"),
        PERCENT(3, "Percent", "center"),
        TIME(4, "Time in sec", "center");

        String name, alignment;
        int idx;

        COLS(int idx, String name, String alignment) {
            this.name = name;
            this.idx = idx;
            this.alignment = alignment;
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
    }

    private MyLogger logger;
    private TrustManager[] trustAllCerts;
    private final String title = "Resource Downloader";
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(3);

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
        logger = MyLogger.createLogger("resource-downloader.log");
        urlsToDownload = new HashMap<>();
        DefaultConfigs configs = new DefaultConfigs(logger);

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
        JScrollPane pane = new JScrollPane(tblInfo);
        pane.setBorder(new LineBorder(Color.WHITE, 5));

        parentContainer.add(controlsPanel, BorderLayout.NORTH);
        parentContainer.add(pane, BorderLayout.CENTER);

        setToCenter();
        logger.log("Program initialized");
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
        tblInfo.setBorder(new LineBorder(Color.BLACK, 1));
        tblInfo.getColumnModel().getColumn(COLS.PERCENT.getIdx()).setMaxWidth(200);
        tblInfo.getColumnModel().getColumn(COLS.TIME.getIdx()).setMaxWidth(250);
        tblInfo.getColumnModel().getColumn(COLS.IDX.getIdx()).setMaxWidth(70);

        // For making contents non editable
        tblInfo.setDefaultEditor(Object.class, null);
        // For tooltip
        //tblInfo.setDefaultRenderer(Object.class, new CellRenderer());

        tblInfo.setAutoscrolls(true);
        tblInfo.setPreferredScrollableViewportSize(tblInfo.getPreferredSize());
        // this col contains tooltip
        tblInfo.getColumnModel().getColumn(COLS.PATH.getIdx()).setMinWidth(0);
        tblInfo.getColumnModel().getColumn(COLS.PATH.getIdx()).setMaxWidth(0);

        CellRendererLeftAlign leftRenderer = new CellRendererLeftAlign();
        CellRendererCenterAlign centerRenderer = new CellRendererCenterAlign();

        for (COLS col : COLS.values()) {
            tblInfo.getColumnModel().getColumn(col.getIdx()).setCellRenderer(
                    col.getAlignment().equals("center") ? centerRenderer : leftRenderer);
        }
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
            logger.log(e.getMessage());
            e.printStackTrace();
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
        logger.log("Goodbye");
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
        try {
            int KB = 1024;
            URL u = new URL(url);
            URLConnection uc = u.openConnection();
            FileInfo fileInfo = new FileInfo(url, getDestPath(url), uc.getContentLength());
            logger.log("Url resource size is [" + fileInfo.getSize() + "] Bytes i.e. [" + (fileInfo.getSize() / KB) + "] KB");

            ReadableByteChannel rbc = Channels.newChannel(u.openStream());
            FileOutputStream fos = new FileOutputStream(fileInfo.getDest());
            resourceInfo.updateResourceInfo(fos, rbc, fileInfo);

            // ignoring boolean status from Callable
            threadPool.submit(new DownloadFileCallable(this, resourceInfo));
            startTracking(resourceInfo);

        } catch (Exception e) {
            logger.log(e.getMessage());
            e.printStackTrace();
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
        String destFolder = txtDest.getText();
        if (!Utils.hasValue(destFolder)) {
            destFolder = ".";
        }
        String path = destFolder + url.substring(url.lastIndexOf("/"));
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
        urlsToDownload.put(srcPath, new ResourceInfo(srcPath, this.logger));

        if (!isHttpUrl(srcPath)) {
            urlsToDownload.clear();
            List<String> urlsToDownloadList = getUrlsFromFile(srcPath);
            urlsToDownloadList.forEach(f -> {
                if (Utils.hasValue(f)) {
                    urlsToDownload.put(f, new ResourceInfo(f, this.logger));
                }
            });
        }
        logger.log("Downloading urls are " + urlsToDownload.keySet());

        createRowsInTable(urlsToDownload);
        threadPool.submit(new StartDownloadCallable(this, urlsToDownload));
//        urlsToDownload.forEach((k, v) -> downLoad(v));
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
            logger.log(e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    static class DownloadFileCallable implements Callable<Boolean> {

        private final ResourceDownLoader rd;
        private final ResourceInfo resourceInfo;

        public DownloadFileCallable(ResourceDownLoader rd, ResourceInfo resourceInfo) {
            this.rd = rd;
            this.resourceInfo = resourceInfo;
        }

        @Override
        public Boolean call() throws Exception {
            long startTime = System.currentTimeMillis();

            resourceInfo.setFileStatus(FileStatus.DOWNLOADING);
            rd.logger.log("Starting download for " + resourceInfo);
            resourceInfo.getFos().getChannel().transferFrom
                    (resourceInfo.getRbc(), 0, resourceInfo.getFileInfo().getSize());
            if (!resourceInfo.isCancelled()) {
                long diffTime = (System.currentTimeMillis() - startTime);
                long diffTimeInSec = TimeUnit.MILLISECONDS.toSeconds(diffTime);
                resourceInfo.getFileInfo().setTimeToDownloadInSec(diffTimeInSec);
                rd.logger.log("download complete for " + resourceInfo);
                rd.updateDownloadTime(resourceInfo.getUrl(), diffTimeInSec);
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
        public Boolean call() throws Exception {
            urlsToDownload.forEach((k, v) -> rd.downLoad(v));
            return true;
        }
    }

    static class DownloadStatusCallable implements Callable<Boolean> {

        private final ResourceInfo resourceInfo;
        private final FileInfo fileInfo;
        private final ResourceDownLoader rd;

        DownloadStatusCallable(ResourceDownLoader rd, ResourceInfo resourceInfo) {
            this.resourceInfo = resourceInfo;
            this.fileInfo = resourceInfo.getFileInfo();
            this.rd = rd;
        }

        @Override
        public Boolean call() throws Exception {
            int percent, KB = 1024;
            long lastSize = 0, fileSize = fileInfo.getSize();
            String speedStr;
            StringBuilder sbLogInfo;

            do {
                sbLogInfo = new StringBuilder("Status: " + resourceInfo.getFileStatus().getVal());
                if (resourceInfo.isCancelled()) {
                    rd.logger.log(sbLogInfo.toString());
                    break;
                }
                long size = Files.size(Utils.createPath(fileInfo.getDest()));
                sbLogInfo.append(", Downloaded size [").append(size).append("/").append(fileSize).append("]");
                percent = (int) ((size * 100) / fileSize);
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

                rd.updatePercent(fileInfo.getSrc(), percent);

                Thread.sleep(250);
            } while (percent < 100);

            return true;
        }
    }

    private void updateDownloadTime(String src, long time) {
        for (int i = 0; i < tblInfo.getRowCount(); i++) {
            if (tblInfo.getValueAt(i, COLS.NAME.getIdx()).equals(Utils.getFileName(src))) {
                tblInfo.setValueAt(time, i, COLS.TIME.getIdx());
            }
        }
    }

    private void updatePercent(String src, int percent) {
        for (int i = 0; i < tblInfo.getRowCount(); i++) {
            if (tblInfo.getValueAt(i, COLS.NAME.getIdx()).equals(Utils.getFileName(src))) {
                tblInfo.setValueAt(percent, i, COLS.PERCENT.getIdx());
            }
        }
    }
}

abstract class CellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setToolTipText(table.getValueAt(row, ResourceDownLoader.COLS.PATH.getIdx()).toString());
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
}

class CellRendererLeftAlign extends CellRenderer {

    public CellRendererLeftAlign() {
        setHorizontalAlignment(SwingConstants.LEFT);
    }
}

class CellRendererCenterAlign extends CellRenderer {

    public CellRendererCenterAlign() {
        setHorizontalAlignment(SwingConstants.CENTER);
    }
}
