package com.example.hacksecure.logging;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.hacksecure.data.database.DatabaseClient;
import com.example.hacksecure.data.entity.EventLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles batch insertion of EventLog objects into the Room database.
 *
 * Delegates to DatabaseClient, which already uses a single-threaded ExecutorService
 * to ensure all database writes are off the main thread and serialized.
 */
public class BatchInsertManager {

    private final DatabaseClient databaseClient;

    public BatchInsertManager(@NonNull Context context) {
        this.databaseClient = DatabaseClient.getInstance(context.getApplicationContext());
    }

    /**
     * Enqueue a batch of logs for insertion.
     * This call is non-blocking; actual insert runs on DatabaseClient's single-thread executor.
     *
     * @param logs list of logs to insert; method safe to call from any thread.
     */
    public void insertBatch(@NonNull List<EventLog> logs) {
        if (logs.isEmpty()) {
            return;
        }
        // Use a defensive copy to avoid external modifications after scheduling.
        List<EventLog> copy = new ArrayList<>(logs);
        databaseClient.insertLogsAsync(copy);
    }
}

