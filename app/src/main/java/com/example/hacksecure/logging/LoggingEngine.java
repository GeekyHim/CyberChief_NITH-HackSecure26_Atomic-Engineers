package com.example.hacksecure.logging;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.hacksecure.data.entity.EventLog;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main entry point for the high-performance logging engine.
 *
 * Pipeline (after deduplication layer):
 *   LoggingEngine.logEvent(EventLog)
 *   -> LogBufferManager
 *   -> BatchInsertManager (Room / SQLite)
 */
public class LoggingEngine {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_BUFFER_SIZE = 1000;
    private static final long FLUSH_INTERVAL_SECONDS = 5L;

    private final LogBufferManager logBufferManager;
    private final BatchInsertManager batchInsertManager;
    private final CleanupScheduler cleanupScheduler;
    private final ScheduledExecutorService flushScheduler;

    private final Object flushLock = new Object();

    public LoggingEngine(@NonNull Context context) {
        this.logBufferManager = new LogBufferManager();
        this.batchInsertManager = new BatchInsertManager(context);
        this.cleanupScheduler = new CleanupScheduler(context);
        this.flushScheduler = Executors.newSingleThreadScheduledExecutor();

        // Start periodic flush every 5 seconds.
        flushScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                flushLogs();
            }
        }, FLUSH_INTERVAL_SECONDS, FLUSH_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // Start periodic cleanup every hour.
        cleanupScheduler.start();
    }

    /**
     * Extremely lightweight event ingestion method.
     *
     * Called after the deduplication layer. This method must not perform any
     * database operations or blocking work; it only buffers events and may
     * trigger asynchronous flushes.
     *
     * @param event deduplicated EventLog to be recorded.
     */
    public void logEvent(@NonNull EventLog event) {
        int currentSize = logBufferManager.addEvent(event);

        // If buffer reaches or exceeds batch size, trigger a flush.
        if (currentSize >= BATCH_SIZE) {
            asyncFlushIfNeeded(false);
        }

        // Backpressure: if buffer is at or above max size, force immediate flush.
        if (currentSize >= MAX_BUFFER_SIZE) {
            asyncFlushIfNeeded(true);
        }
    }

    /**
     * Public flush method. Inserts all remaining logs from the buffer
     * into the database asynchronously.
     */
    public void flushLogs() {
        asyncFlushIfNeeded(true);
    }

    /**
     * Coordinates graceful shutdown:
     *  - flushes remaining logs
     *  - stops periodic flush and cleanup schedulers
     */
    public void shutdown() {
        // Flush any remaining logs before shutdown.
        flushLogs();

        // Stop schedulers to avoid further tasks.
        flushScheduler.shutdownNow();
        cleanupScheduler.shutdown();
    }

    /**
     * Internal helper to drain the buffer and schedule a batch insert.
     *
     * @param force if true, always attempt a flush; if false, only flush
     *              when there is at least one pending log.
     */
    private void asyncFlushIfNeeded(boolean force) {
        synchronized (flushLock) {
            int pending = logBufferManager.size();
            if (!force && pending < BATCH_SIZE) {
                return;
            }

            List<EventLog> batch = logBufferManager.drainAll();
            if (!batch.isEmpty()) {
                batchInsertManager.insertBatch(batch);
            }
        }
    }
}

