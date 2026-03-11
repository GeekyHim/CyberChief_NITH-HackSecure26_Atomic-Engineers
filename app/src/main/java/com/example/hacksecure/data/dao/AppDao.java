package com.example.hacksecure.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.hacksecure.data.entity.MonitoredApp;

import java.util.List;

@Dao
public interface AppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertApp(MonitoredApp app);

    @Query("SELECT * FROM apps")
    List<MonitoredApp> getAllApps();

    @Query("SELECT * FROM apps")
    LiveData<List<MonitoredApp>> observeAllApps();

    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    MonitoredApp getAppByPackageName(String packageName);

    @Query("SELECT * FROM apps WHERE id = :id LIMIT 1")
    LiveData<MonitoredApp> observeAppById(int id);
}

