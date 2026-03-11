package com.example.hacksecure.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "apps")
public class MonitoredApp {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private String packageName;

    @NonNull
    private String appName;

    private boolean isActive;

    public MonitoredApp(@NonNull String packageName,
                        @NonNull String appName,
                        boolean isActive) {
        this.packageName = packageName;
        this.appName = appName;
        this.isActive = isActive;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(@NonNull String packageName) {
        this.packageName = packageName;
    }

    @NonNull
    public String getAppName() {
        return appName;
    }

    public void setAppName(@NonNull String appName) {
        this.appName = appName;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}

