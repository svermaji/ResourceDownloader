package com.sv.downloader;

import com.sv.core.Utils;
import com.sv.core.config.DefaultConfigs;
import com.sv.core.logger.MyLogger;
import com.sv.downloader.helpers.DownloadFileCallable;
import com.sv.downloader.helpers.TrackAllDownloadsCallable;
import com.sv.swingui.SwingUtils;
import com.sv.swingui.component.AppButton;
import com.sv.swingui.component.AppExitButton;
import com.sv.swingui.component.AppFrame;
import com.sv.swingui.component.AppLabel;
import com.sv.swingui.component.table.AppTable;
import com.sv.swingui.component.table.CellRendererCenterAlign;
import com.sv.swingui.component.table.CellRendererLeftAlign;
import com.sv.swingui.component.table.CellRendererProgressBar;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.sv.core.Constants.*;
import static com.sv.swingui.UIConstants.BLUE_BORDER;
import static com.sv.swingui.UIConstants.EMPTY_BORDER;

/**
 * This class will help in downloading bunch of urls that
 * may point to resource like images, pdfs etc.
 */
public class ResourceDownLoader extends AppFrame {

    enum Configs {
        DownloadLocation, PathToDownload
    }

    public enum COLS {
        PATH(0, "Path", "left", 0),
        IDX(1, "#", "center", 50),
        NAME(2, "Name", "left", -1),
        STATUS(3, "Status", "center", 100),
        PERCENT(4, "Percent", "left", 100),
        SIZE(5, "Size", "center", 150),
        TIME(6, "Time in sec", "center", 100);

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

    private JTextField txtDest, txtSource;
    private JButton btnDownload, btnOpenDest, btnCancel, btnExit;
    private JTable tblInfo;
    private JTextArea taUrls;
    private DefaultTableModel model;
    private Map<String, ResourceInfo> urlsToDownload;
    private List<String> urlsFromFile;
    private final int DEFAULT_NUM_ROWS = 6;

    //TODO: if site unreachable then cancel after some time
    private final MyLogger logger = MyLogger.createLogger("resource-downloader.log");
    private final DefaultConfigs configs = new DefaultConfigs(logger, Utils.getConfigsAsArr(Configs.class));
    private TrustManager[] trustAllCerts;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(5);

    private static String lastClipboardText = "";

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

    public ResourceDownLoader() {
        super("Resource Downloader");
    }

    /**
     * This method initializes the form.
     */
    private void initComponents() {

        createTrustManager();
        trustAllHttps();
        urlsToDownload = new HashMap<>();

        Container parentContainer = getContentPane();
        JPanel controlsPanel = new JPanel();

        parentContainer.setLayout(new BorderLayout());

        txtSource = new JTextField();
        JLabel lblSource = new AppLabel("Download from", txtSource, 'F');
        btnDownload = new AppButton("DownLoad", 'D');
        btnOpenDest = new AppButton("Open", 'O');
        btnOpenDest.setIcon(new ImageIcon("./icons/open-icon.png"));
        btnOpenDest.setToolTipText("Open Destination");
        btnOpenDest.addActionListener(e -> {
            try {
                Runtime.getRuntime().exec("explorer.exe \"" + txtDest.getText() + "\"");
            } catch (IOException ioException) {
                logger.error("Unable to open folder " + txtDest.getText());
            }
        });
        btnCancel = new AppButton("Cancel", 'C');
        btnExit = new AppExitButton();
        txtDest = new JTextField(configs.getConfig(Configs.DownloadLocation.name()));
        JLabel lblDest = new AppLabel("Location To Save", txtDest, 'N');
        txtDest.setColumns(10);

        controlsPanel.setLayout(new FlowLayout());

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                exitForm();
            }
        });

        controlsPanel.add(lblSource);
        controlsPanel.add(txtSource);
        txtSource.setText(configs.getConfig(Configs.PathToDownload.name()));
        txtSource.setColumns(20);

        controlsPanel.add(lblDest);
        controlsPanel.add(txtDest);
        controlsPanel.add(btnOpenDest);
        btnDownload.addActionListener(evt -> startDownLoad(txtSource.getText()));
        controlsPanel.add(btnDownload);
        btnCancel.addActionListener(evt -> cancelDownLoad());
        controlsPanel.add(btnCancel);
        btnExit.addActionListener(evt -> exitForm());
        controlsPanel.add(btnExit);

        createTable();
        JScrollPane jspTable = new JScrollPane(tblInfo);
        jspTable.setBorder(EMPTY_BORDER);

        taUrls = new JTextArea(getUrls(), 5, 1);
        taUrls.setBorder(BLUE_BORDER);
        JScrollPane jspUrls = new JScrollPane(taUrls);
        jspUrls.setBorder(EMPTY_BORDER);

        JPanel jpUrls = new JPanel(new BorderLayout());
        jpUrls.add(new JLabel(" Urls to download"), BorderLayout.NORTH);
        jpUrls.add(jspUrls, BorderLayout.CENTER);

        JPanel jpTblAndUrls = new JPanel(new GridLayout(2, 1));
        jpTblAndUrls.add(jspTable);
        jpTblAndUrls.add(jpUrls);

        parentContainer.add(controlsPanel, BorderLayout.NORTH);
        parentContainer.add(jpTblAndUrls, BorderLayout.CENTER);

        applyWindowActiveCheck(new WindowChecks[]{
                WindowChecks.WINDOW_ACTIVE, WindowChecks.CLIPBOARD});

        setControlsToEnable();
        setToCenter();
        setSize(getWidth(), getHeight() / 2);
        this.setLocationRelativeTo(null);
        logger.info("Program initialized");
    }

    @Override
    public void startClipboardAction() {
        copyClipboard(logger);
    }

    @Override
    public void copyClipboardYes(String data) {
        taUrls.setText(checkLineEndings(data));
    }

    private String checkLineEndings(String data) {
        String NEW_LINE_REGEX = "\r?\n";
        String SYS_LINE_END = System.lineSeparator();
        return data.replaceAll(NEW_LINE_REGEX, SYS_LINE_END);
    }

    private String getUrls() {
        urlsFromFile = getUrlsFromFile(txtSource.getText());
        StringBuilder sb = new StringBuilder();
        urlsFromFile.forEach(s -> sb.append(s).append(System.lineSeparator()));
        return sb.toString();
    }

    private void createTable() {
        model = SwingUtils.getTableModel(
                Arrays.stream(COLS.class.getEnumConstants()).map(COLS::getName).toArray(String[]::new)
        );

        createDefaultRows();

        tblInfo = new AppTable(model);
        tblInfo.setBorder(BLUE_BORDER);

        CellRendererLeftAlign leftRenderer = new CellRendererLeftAlign();
        CellRendererCenterAlign centerRenderer = new CellRendererCenterAlign();

        for (COLS col : COLS.values()) {
            tblInfo.getColumnModel().getColumn(col.getIdx()).setCellRenderer(
                    col.getAlignment().equals("center") ? centerRenderer : leftRenderer);

            if (col.getWidth() != -1) {
                TableColumn colIdx = tblInfo.getColumnModel().getColumn(col.getIdx());
                colIdx.setMinWidth(col.getWidth());
                colIdx.setMaxWidth(col.getWidth());
            }
        }

        tblInfo.getColumnModel().getColumn(COLS.PERCENT.getIdx()).setCellRenderer(new CellRendererProgressBar());
    }

    private void createDefaultRows() {
        SwingUtils.removeAndCreateEmptyRows(COLS.values().length, DEFAULT_NUM_ROWS, model);
    }

    private void trustAllHttps() {
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            logger.error("Unable to setup https. Details: " + e.getMessage());
        }
    }

    /**
     * Exit the Application
     */
    private void exitForm() {
        cancelDownLoad();
        configs.saveConfig(this);
        logger.info("Goodbye");
        logger.dispose();
        setVisible(false);
        dispose();
        System.exit(0);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new ResourceDownLoader().initComponents();
    }

    private void downLoad(ResourceInfo resourceInfo) {
        String url = resourceInfo.getUrl();
        logger.info("Trying url " + Utils.addBraces(url));
        ReadableByteChannel rbc;
        FileOutputStream fos;
        FileInfo fileInfo;
        try {
            threadPool.submit(new TrackAllDownloadsCallable(this));
            logger.info("Tracking initiated...");
            URL u = new URL(url);
            String urlWithBraces = Utils.addBraces(url);
            URLConnection uc = u.openConnection();
            fileInfo = new FileInfo(url, extractPath(url), uc.getContentLength());
            logger.info("Url [" + Utils.getFileName(url) + "] resource size is " + Utils.getSizeString(fileInfo.getSize()));

            if (fileInfo.getSize() < 0) {
                resourceInfo.setFileStatus(FileStatus.FAILED);
                logger.error("Unable to reach url " + urlWithBraces);
                markDownloadFailed(resourceInfo);
                removeFromUrlsToDownload(url);
            } else {
                boolean exists = checkIfExists(fileInfo);
                if (exists && sizeMatched(fileInfo)) {
                    logger.info("File exists with same size for url " + urlWithBraces);
                    resourceInfo.setFileStatus(FileStatus.EXISTS);
                    setStatusCellValue(FileStatus.EXISTS.getVal(), resourceInfo.getRowNum());
                    removeFromUrlsToDownload(resourceInfo.getUrl());
                } else {
                    if (exists) {
                        logger.info("File exists but not of size from url. Downloading for " + urlWithBraces);
                    }
                    rbc = Channels.newChannel(u.openStream());
                    fos = getFOS(fileInfo, resourceInfo.getRowNum());
                    resourceInfo.updateResourceInfo(fos, rbc, fileInfo);

                    // ignoring boolean status from Callable
                    threadPool.submit(new DownloadFileCallable(this, resourceInfo));
                    startTracking(resourceInfo);
                }
            }
        } catch (FileNotFoundException e) {
            logger.error("No file at given url. Details: " + e.getMessage());
            markDownloadFailed(resourceInfo);
        } catch (Exception e) {
            logger.error("Unable to download. Details: " + e.getMessage());
            markDownloadFailed(resourceInfo);
        }
    }

    private boolean checkIfExists(FileInfo fileInfo) {
        return Files.exists(Utils.createPath(fileInfo.getDestination()));
    }

    // reach here after exists is checked
    private boolean sizeMatched(FileInfo fileInfo) {
        boolean result = new File(fileInfo.getDestination()).length() == fileInfo.getSize();
        logger.info("Result of matching local file size and url file size is " + Utils.addBraces(result + ""));
        return result;
    }

    private FileOutputStream getFOS(FileInfo fileInfo, int row) throws Exception {
        try {
            return new FileOutputStream(fileInfo.getDestination());
        } catch (Exception e) {
            logger.error("Destination [" + fileInfo.getDestination()
                    + "] does not have name.  Trying from url itself. Error: " + e.getMessage());
        }
        return getFOSFromUrl(fileInfo, row);
    }

    private FileOutputStream getFOSFromUrl(FileInfo fileInfo, int row) throws Exception {
        URLConnection conn = new URL(fileInfo.getSrc()).openConnection();
        String disposition = conn.getHeaderField("Content-disposition");
        fileInfo.setFilename(extractPathFromName(extractFileNameFromCD(disposition)));

        updateFileNameInTable(fileInfo, row);
        return new FileOutputStream(fileInfo.getFilename());
    }

    private String extractFileNameFromCD(String cdStr) {
        logger.info("Content disposition from url obtained as " + Utils.addBraces(cdStr));
        String FN_STR = "filename=\"";
        if (cdStr.contains(FN_STR)) {
            cdStr = cdStr.substring(cdStr.indexOf(FN_STR) + FN_STR.length());
            if (cdStr.contains("\"")) {
                cdStr = cdStr.substring(0, cdStr.indexOf("\""));
            }
        }
        logger.info("Returning name extracted from content disposition as " + Utils.addBraces(cdStr));
        return cdStr;
    }

    public void markDownloadFailed(ResourceInfo info) {
        info.markFailed();
        markDownloadForError(info, FAILED);
    }

    public void markDownloadCancelled(ResourceInfo info) {
        info.markCancel();
        markDownloadForError(info, CANCELLED);
    }

    private void markDownloadForError(ResourceInfo info, String msg) {
        removeFromUrlsToDownload(info.getUrl());
        logger.info("Marking [" + msg + "] in UI for " + info.nameAndStatus());
        int i = info.getRowNum();
        if (isPathMatched(info.getUrl(), i)) {
            String nameVal = tblInfo.getValueAt(i, COLS.NAME.getIdx()).toString();
            boolean takeAction = canCancel(info.getFileStatus().getVal()) &&
                    !nameVal.startsWith(CANCELLED) &&
                    !nameVal.startsWith(FAILED);
            logger.info("For url [" + info.getOnlyName() + "], decision to try deleting incomplete download [" + takeAction + "]");
            // timer over-rides some time
            setStatusCellValue(info.getFileStatus().getVal(), i);
            if (takeAction) {
                setCellValue(msg + nameVal, i, COLS.NAME.getIdx());
                try {
                    boolean result = Files.deleteIfExists(Utils.createPath(info.getFileInfo().getDestination()));
                    logger.info("Result for trying to delete incomplete download for ["
                            + info.getOnlyName() + "] is [" + result + "]");
                } catch (NullPointerException | IOException e) {
                    logger.error("File not exists or unable to delete file: " + info.getUrl());
                }
            }
        }
    }

    private void setStatusCellValue(String val, int row) {
        int col = COLS.STATUS.getIdx();
        if (canCancel(tblInfo.getValueAt(row, col).toString())) {
            setCellValue(val, row, col);
        }
    }

    private boolean canCancel(String status) {
        return !status.equals(FileStatus.DOWNLOADED.getVal()) &&
                !status.equals(FileStatus.EXISTS.getVal());
    }

    private void setCellValue(long val, int row, int col) {
        tblInfo.setValueAt(val, row, col);
    }

    private void setCellValue(String val, int row, int col) {
        tblInfo.setValueAt(val, row, col);
    }

    private void startTracking(ResourceInfo resourceInfo) {
        // ignoring boolean status from Callable
        threadPool.submit(new DownloadStatusCallable(this, resourceInfo));
    }

    private boolean isHttpUrl(String s) {
        return s.startsWith("http");
    }

    private String extractPath(String url) {
        return extractPathFromName(
                url.substring(url.lastIndexOf(F_SLASH) + F_SLASH.length())
        );
    }

    private String extractPathFromName(String name) {
        String destFolder = txtDest.getText();
        if (!Utils.hasValue(destFolder)) {
            destFolder = ".";
        }
        if (!name.startsWith("\\") || !name.startsWith("/")) {
            name = "\\" + name;
        }
        String path = destFolder + name;
        logger.info("Destination path is [" + Utils.addBraces(path));
        return path;
    }

    private void cancelDownLoad() {
        disableCancelButton();
        if (!isUrlsToDownloadEmpty()) {
            logger.info("Cancelling all downloads. Remaining downloads: " + urlsToDownload.size());
            synchronized (ResourceDownLoader.class) {
                try {
                    urlsToDownload.forEach((key, ri) -> ri.closeResource());
                } catch (RuntimeException e) {
                    logger.error("Error in closing resources. Details: " + e.getMessage());
                }
            }
            urlsToDownload.clear();
            enableControls();
            updateTitle("Cancelled download!!");

            logger.info("Cancelling done.");
        }
        enableCancelButton();
    }

    private void enableCancelButton() {
        btnCancel.setEnabled(true);
    }

    private void disableCancelButton() {
        btnCancel.setEnabled(false);
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
        logger.info("Urls to download are [" + urlsToDownload.size()
                + "]. Urls are " + urlsToDownload.keySet());

        createRowsInTable(urlsToDownload);
        threadPool.submit(new StartDownloadCallable(this, urlsToDownload));
    }

    private List<String> readUrlsFromTextArea() {
        return Arrays.asList(taUrls.getText().split(System.lineSeparator()));
    }

    private void clearOldRun() {
        urlsToDownload.clear();
        createDefaultRows();
    }

    private void createRowsInTable(Map<String, ResourceInfo> urlsToDownload) {
        int i = 0;
        for (Map.Entry<String, ResourceInfo> entry : urlsToDownload.entrySet()) {
            String k = entry.getKey();
            ResourceInfo v = entry.getValue();
            v.setRowNum(i);
            if (i < DEFAULT_NUM_ROWS) {
                setCellValue(k, i, COLS.PATH.getIdx());
                setCellValue(i + 1, i, COLS.IDX.getIdx());
                setCellValue(v.getOnlyName(), i, COLS.NAME.getIdx());
                setCellValue(FileStatus.IN_QUEUE.getVal(), i, COLS.STATUS.getIdx());
                setCellValue(0, i, COLS.PERCENT.getIdx());
                setCellValue(0, i, COLS.TIME.getIdx());
            } else {
                // maintain the order
                String[] row = {k, (i + 1) + "", v.getOnlyName(), "0", "0"};
                model.addRow(row);
            }
            i++;
        }
    }

    private void setControlsToEnable() {
        Component[] components = {
                txtSource, txtDest, btnDownload
        };
        setComponentToEnable(components);
        setComponentContrastToEnable(new Component[]{btnCancel});
        enableControls();
    }

    private java.util.List<String> getUrlsFromFile(String filePath) {
        logger.info("Current directory is: " + System.getProperty("user.dir") + ", file path is " + filePath);
        Path path = Utils.createPath(filePath);
        try {
            return Files.readAllLines(path);
        } catch (IOException e) {
            logger.error("Unable to read urls from file. Details: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean isDownloadable(ResourceInfo resourceInfo) {
        return !resourceInfo.isCancelled() && !resourceInfo.exists();
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
            rd.logger.info("Download status tracking start for " + resourceInfo.getOnlyName());
            this.resourceInfo = resourceInfo;
            this.fileInfo = resourceInfo.getFileInfo();
            this.rd = rd;
        }

        @Override
        public Boolean call() {
            int percent;
            long lastSize = 0, fileSize = fileInfo.getSize();
            String speedStr;
            StringBuilder sbLogInfo;

            do {
                fileInfo.setDownloadStartTime(System.currentTimeMillis());
                sbLogInfo = new StringBuilder(resourceInfo.nameAndStatus());
                if (resourceInfo.isCancelled() || resourceInfo.exists()) {
                    rd.logger.info(sbLogInfo.toString());
                    break;
                }
                try {
                    String dest = Utils.hasValue(fileInfo.getFilename()) ? fileInfo.getFilename() : fileInfo.getDestination();
                    long size = Files.size(Utils.createPath(dest));
                    sbLogInfo.append(", size ")
                            .append(Utils.getSizeString(size)).append("/")
                            .append(Utils.getSizeString(fileSize));
                    percent = (int) ((size * 100) / fileSize);
                    resourceInfo.getFileInfo().setDownloadedSize(size);
                    sbLogInfo.append(", [").append(percent).append("%]");

                    // Since thread.sleep is 250, so multiplying by 4, Now trying 1000 so * 1
                    speedStr = Utils.getSizeString(size - lastSize);
                    lastSize = size;
                    sbLogInfo.append(" @ ").append(speedStr);
                    if (!rd.isUrlsToDownloadEmpty()) {
                        rd.updateTitle(percent + "% at " + speedStr);
                    }
                    rd.logger.info(sbLogInfo.toString());
                } catch (Exception e) {
                    rd.logger.error("Error in downloading file. Details: " + e.getMessage());
                    break;
                }

                rd.updateFileStatus(percent, resourceInfo);
                Utils.sleep1Sec();
            } while (percent < 100);

            return true;
        }

    }

    public boolean isUrlsToDownloadEmpty() {
        return urlsToDownload.isEmpty();
    }

    public void removeFromUrlsToDownload(String key) {
        synchronized (ResourceDownLoader.class) {
            urlsToDownload.remove(key);
        }
        logger.info("Removed url [" + key + "] from download.");
        if (urlsToDownload.size() == 0) {
            updateTitle("Done !!");
        }
    }

    public void log(String m) {
        logger.info(m);
    }

    public void updateDownloadTime(FileInfo fileInfo, long time, int i) {
        if (isPathMatched(fileInfo.getSrc(), i)) {
            setCellValue(time, i, COLS.TIME.getIdx());
        }
    }

    private void updateFileStatus(int percent, ResourceInfo info) {
        FileInfo fileInfo = info.getFileInfo();
        int i = info.getRowNum();
        if (isPathMatched(fileInfo.getSrc(), i)) {
            setCellValue(info.getFileStatus().getVal(), i, COLS.STATUS.getIdx());
            setCellValue(percent, i, COLS.PERCENT.getIdx());
            setCellValue(getDownloadSize(fileInfo), i, COLS.SIZE.getIdx());
            setCellValue(getDownloadTime(fileInfo), i, COLS.TIME.getIdx());
        }
    }

    private void updateFileNameInTable(FileInfo fileInfo, int i) {
        if (isPathMatched(fileInfo.getSrc(), i)) {
            setCellValue(Utils.getFileName(fileInfo.getFilename()), i, COLS.NAME.getIdx());
        }
    }

    private boolean isPathMatched(String src, int row) {
        return Utils.hasValue(src) && tblInfo.getValueAt(row, COLS.PATH.getIdx()).toString().equals(src);
    }

    private String getDownloadSize(FileInfo fileInfo) {
        return Utils.getSizeString(fileInfo.getDownloadedSize()) + "/" + Utils.getSizeString(fileInfo.getSize());
    }

    private String getDownloadTime(FileInfo fileInfo) {
        return fileInfo.getDownloadInSec() + "";
    }

    public String getPathToDownload() {
        return txtSource.getText();
    }

    public String getDownloadingUrls() {
        return taUrls.getText();
    }

    public String getDownloadLocation() {
        return txtDest.getText();
    }
}

