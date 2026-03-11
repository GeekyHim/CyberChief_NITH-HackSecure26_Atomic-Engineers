package com.example.hacksecure.data.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.hacksecure.data.dao.AppDao;
import com.example.hacksecure.data.dao.EventLogDao;
import com.example.hacksecure.data.entity.EventLog;
import com.example.hacksecure.data.entity.MonitoredApp;

@Database(
        entities = {
                MonitoredApp.class,
                EventLog.class
        },
        version = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract AppDao appDao();

    public abstract EventLogDao eventLogDao();
}

