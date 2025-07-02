package org.example;

public class PrintRequest {
    private String tempFileBasePath;
    private String fileType;
    private String fileIdentifier;
    private String sslTrustStorePath;
    private String sslTrustStorePassword;
    private String ippsUri;
    private String username;
    private String printerName;

    // Getters and setters
    public String getTempFileBasePath() {
        return tempFileBasePath;
    }

    public void setTempFileBasePath(String tempFileBasePath) {
        this.tempFileBasePath = tempFileBasePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFileIdentifier() {
        return fileIdentifier;
    }

    public void setFileIdentifier(String fileIdentifier) {
        this.fileIdentifier = fileIdentifier;
    }

    public String getSslTrustStorePath() {
        return sslTrustStorePath;
    }

    public void setSslTrustStorePath(String sslTrustStorePath) {
        this.sslTrustStorePath = sslTrustStorePath;
    }

    public String getSslTrustStorePassword() {
        return sslTrustStorePassword;
    }

    public void setSslTrustStorePassword(String sslTrustStorePassword) {
        this.sslTrustStorePassword = sslTrustStorePassword;
    }

    public String getIppsUri() {
        return ippsUri;
    }

    public void setIppsUri(String ippsUri) {
        this.ippsUri = ippsUri;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPrinterName() {
        return printerName;
    }

    public void setPrinterName(String printerName) {
        this.printerName = printerName;
    }
}