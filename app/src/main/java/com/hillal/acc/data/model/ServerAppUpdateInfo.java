package com.hillal.acc.data.model;

public class ServerAppUpdateInfo {
    private String version;
    private String description;
    private String downloadUrl;
    private boolean forceUpdate;

    public ServerAppUpdateInfo(String version, String description, String downloadUrl, boolean forceUpdate) {
        this.version = version;
        this.description = description;
        this.downloadUrl = downloadUrl;
        this.forceUpdate = forceUpdate;
    }

    // Getters
    public String getVersion() { return version; }
    public String getDescription() { return description; }
    public String getDownloadUrl() { return downloadUrl; }
    public boolean isForceUpdate() { return forceUpdate; }
} 