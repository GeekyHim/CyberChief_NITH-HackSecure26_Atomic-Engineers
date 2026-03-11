package com.example.hacksecure.data.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.hacksecure.data.dao.EventLogDao;
import com.example.hacksecure.data.entity.EventLog;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseClient {

    private static volatile DatabaseClient instance;

    private final AppDatabase appDatabase;
    private final ExecutorService executorService;

    private static final String DATABASE_NAME = "security_database";

    private static final long LOG_RETENTION_MS = 72L * 60L * 60L * 1000L;

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE event_logs ADD COLUMN sourceIp TEXT");
            database.execSQL("ALTER TABLE event_logs ADD COLUMN destinationIp TEXT");
            database.execSQL("ALTER TABLE event_logs ADD COLUMN protocol TEXT");
            database.execSQL("ALTER TABLE event_logs ADD COLUMN port INTEGER");
        }
    };

    private DatabaseClient(@NonNull Context context) {
        appDatabase = Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                DATABASE_NAME
        ).addMigrations(MIGRATION_1_2).build();

        executorService = Executors.newSingleThreadExecutor();
    }

    public static DatabaseClient getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (DatabaseClient.class) {
                if (instance == null) {
                    instance = new DatabaseClient(context);
                }
            }
        }
        return instance;
    }

    public AppDatabase getDatabase() {
        return appDatabase;
    }

    public void insertLogsAsync(@NonNull final List<EventLog> logs) {
        if (logs.isEmpty()) {
            return;
        }

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                EventLogDao dao = appDatabase.eventLogDao();
                dao.insertLogs(logs);
            }
        });
    }

    public void cleanupOldLogsAsync() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long threshold = now - LOG_RETENTION_MS;
                appDatabase.eventLogDao().deleteOldLogs(threshold);
            }
        });
    }

    public void shutdown() {
        executorService.shutdown();
    }
}

