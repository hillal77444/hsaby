package com.hillal.acc.data.model;

import com.google.gson.annotations.SerializedName;

public class ServerAppUpdateInfo {
    @SerializedName("version")
    private String version;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("download_url")
    private String downloadUrl;
    
    @SerializedName("force_update")
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