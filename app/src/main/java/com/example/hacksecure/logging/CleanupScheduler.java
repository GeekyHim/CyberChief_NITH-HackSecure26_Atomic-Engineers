package com.example.hacksecure.logging;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.hacksecure.data.database.DatabaseClient;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Schedules periodic cleanup of old logs based on the 72-hour retention policy.
 *
 * Uses DatabaseClient.cleanupOldLogsAsync() which runs on a single-threaded executor,
 * ensuring database operations are off the main thread and serialized.
 */
public class CleanupScheduler {

    // One hour in seconds for scheduling; actual retention threshold is computed in DatabaseClient.
    private static final long CLEANUP_INTERVAL_SECONDS = 60L * 60L;

    private final DatabaseClient databaseClient;
    private final ScheduledExecutorService scheduler;

    public CleanupScheduler(@NonNull Context context) {
        this.databaseClient = DatabaseClient.getInstance(context.getApplicationContext());
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Starts periodic cleanup task.
     */
    public void start() {
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                databaseClient.cleanupOldLogsAsync();
            }
        }, CLEANUP_INTERVAL_SECONDS, CLEANUP_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Stops scheduled cleanup tasks.
     */
    public void shutdown() {
        scheduler.shutdownNow();
    }
}

