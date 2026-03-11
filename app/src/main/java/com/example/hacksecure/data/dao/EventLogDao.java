package com.example.hacksecure.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.hacksecure.data.entity.EventLog;

import java.util.List;

@Dao
public interface EventLogDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertLogs(List<EventLog> logs);

    @Query("SELECT * FROM event_logs WHERE appId = :appId ORDER BY timestamp DESC")
    List<EventLog> getLogsForApp(int appId);

    @Query("SELECT * FROM event_logs ORDER BY timestamp DESC")
    List<EventLog> getLatestLogs();

    @Query("SELECT * FROM event_logs ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<EventLog>> observeLatestLogs(int limit);

    @Query("SELECT * FROM event_logs WHERE appId = :appId ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<EventLog>> observeLogsForApp(int appId, int limit);

    @Query("SELECT * FROM event_logs WHERE timestamp > :timestamp ORDER BY timestamp DESC")
    List<EventLog> getLogsSince(long timestamp);

    @Query("DELETE FROM event_logs WHERE timestamp < :threshold")
    int deleteOldLogs(long threshold);
}

