package com.example.hacksecure.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.hacksecure.data.database.DatabaseClient
import com.example.hacksecure.data.repository.EventLogRepository
import com.example.hacksecure.ui.models.AppLog

class AppWiseLogsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EventLogRepository by lazy {
        val db = DatabaseClient.getInstance(application).database
        EventLogRepository(
            eventLogDao = db.eventLogDao(),
            appDao = db.appDao()
        )
    }

    fun observeAppWiseLogs(limit: Int = 500): LiveData<List<AppLog>> {
        return repository.observeAppWiseLogs(limit)
    }

    fun observeLogsForApp(appId: Int, limit: Int = 200): LiveData<AppLog?> {
        return repository.observeLogsForApp(appId, limit)
    }
}

