package com.example.hacksecure.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.example.hacksecure.data.dao.AppDao
import com.example.hacksecure.data.dao.EventLogDao
import com.example.hacksecure.data.entity.EventLog
import com.example.hacksecure.data.entity.MonitoredApp
import com.example.hacksecure.data.mapper.EventLogUiMapper
import com.example.hacksecure.ui.models.AppLog

class EventLogRepository(
    private val eventLogDao: EventLogDao,
    private val appDao: AppDao
) {
    /**
     * Observes only the latest [limit] logs and maps them into the existing UI structure.
     * This intentionally avoids ever loading the full `event_logs` table.
     */
    fun observeAppWiseLogs(limit: Int): LiveData<List<AppLog>> {
        val logsLive = eventLogDao.observeLatestLogs(limit)
        val appsLive = appDao.observeAllApps()

        val result = MediatorLiveData<List<AppLog>>()

        fun recompute(logs: List<EventLog>?, apps: List<MonitoredApp>?) {
            val safeLogs = logs.orEmpty()
            val appNameById = apps.orEmpty().associate { it.id to it.appName }

            val grouped = safeLogs.groupBy { it.appId }
            val appLogs = grouped.map { (appId, appEvents) ->
                val appName = appNameById[appId]?.takeIf { it.isNotBlank() } ?: "Unknown App"
                val lastActive = appEvents.maxOfOrNull { it.timestamp } ?: 0L
                val connections = appEvents
                    .sortedByDescending { it.timestamp }
                    .take(50)
                    .map(EventLogUiMapper::toConnectionLog)

                AppLog(
                    appId = appId,
                    appName = appName,
                    totalDataMb = 0,
                    connections = connections,
                    lastActiveTimestamp = lastActive
                )
            }.sortedByDescending { it.lastActiveTimestamp }

            result.value = appLogs
        }

        result.addSource(logsLive) { recompute(it, appsLive.value) }
        result.addSource(appsLive) { recompute(logsLive.value, it) }
        return result
    }

    fun observeLogsForApp(appId: Int, limit: Int): LiveData<AppLog?> {
        val logsForApp = eventLogDao.observeLogsForApp(appId, limit)
        val appNameLive = appDao.observeAppById(appId)

        val combined = MediatorLiveData<AppLog?>()
        fun recompute(logs: List<EventLog>?, app: MonitoredApp?) {
            val safeLogs = logs.orEmpty()
            if (safeLogs.isEmpty()) {
                combined.value = null
                return
            }

            val appName = app?.appName?.takeIf { it.isNotBlank() } ?: "Unknown App"
            val lastActive = safeLogs.maxOfOrNull { it.timestamp } ?: 0L
            val connections = safeLogs
                .sortedByDescending { it.timestamp }
                .take(50)
                .map(EventLogUiMapper::toConnectionLog)

            combined.value = AppLog(
                appId = appId,
                appName = appName,
                totalDataMb = 0,
                connections = connections,
                lastActiveTimestamp = lastActive
            )
        }

        combined.addSource(logsForApp) { recompute(it, appNameLive.value) }
        combined.addSource(appNameLive) { recompute(logsForApp.value, it) }
        return combined
    }
}

