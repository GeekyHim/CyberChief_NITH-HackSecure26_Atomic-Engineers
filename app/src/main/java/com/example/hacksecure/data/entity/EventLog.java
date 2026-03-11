package com.example.hacksecure.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "event_logs",
        indices = {
                @Index(value = "appId"),
                @Index(value = "timestamp"),
                @Index(value = "eventType")
        }
)
public class EventLog {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private int appId;

    @NonNull
    private String eventType;

    private long timestamp;

    private int severity;

    @NonNull
    private String metadata;

    private String sourceIp;
    private String destinationIp;
    private String protocol;
    private int port;

    @Ignore
    @NonNull
    private String packageNameForScoring = "";

    @Ignore
    public EventLog(int appId,
                    @NonNull String eventType,
                    long timestamp,
                    int severity,
                    @NonNull String metadata) {
        this(appId, eventType, timestamp, severity, metadata, "", "", "", 0);
    }

    public EventLog(int appId,
                    @NonNull String eventType,
                    long timestamp,
                    int severity,
                    @NonNull String metadata,
                    String sourceIp,
                    String destinationIp,
                    String protocol,
                    int port) {
        this.appId = appId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.severity = severity;
        this.metadata = metadata;
        this.sourceIp = sourceIp != null ? sourceIp : "";
        this.destinationIp = destinationIp != null ? destinationIp : "";
        this.protocol = protocol != null ? protocol : "";
        this.port = port;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    @NonNull
    public String getEventType() {
        return eventType;
    }

    public void setEventType(@NonNull String eventType) {
        this.eventType = eventType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getSeverity() {
        return severity;
    }

    public void setSeverity(int severity) {
        this.severity = severity;
    }

    @NonNull
    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(@NonNull String metadata) {
        this.metadata = metadata;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public String getDestinationIp() {
        return destinationIp;
    }

    public void setDestinationIp(String destinationIp) {
        this.destinationIp = destinationIp;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @NonNull
    public String getPackageNameForScoring() {
        return packageNameForScoring != null ? packageNameForScoring : "";
    }

    public void setPackageNameForScoring(String packageNameForScoring) {
        this.packageNameForScoring = packageNameForScoring != null ? packageNameForScoring : "";
    }
}

