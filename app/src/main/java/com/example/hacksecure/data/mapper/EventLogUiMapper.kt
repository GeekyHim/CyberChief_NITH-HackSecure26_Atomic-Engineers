package com.example.hacksecure.data.mapper

import com.example.hacksecure.data.entity.EventLog
import com.example.hacksecure.ui.models.ConnectionLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EventLogUiMapper {
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun toConnectionLog(entity: EventLog): ConnectionLog {
        val sourceIp = entity.sourceIp?.takeIf { it.isNotBlank() } ?: "Unknown"
        val destinationIp = entity.destinationIp?.takeIf { it.isNotBlank() } ?: "Unknown"
        val protocol = entity.protocol?.takeIf { it.isNotBlank() } ?: "Unknown"
        val safetyScore = severityLabel(entity.severity)
        val timestamp = timeFormat.format(Date(entity.timestamp))

        return ConnectionLog(
            sourceIp = sourceIp,
            destinationIp = destinationIp,
            protocol = protocol,
            safetyScore = safetyScore,
            timestamp = timestamp
        )
    }

    private fun severityLabel(severity: Int): String {
        return when {
            severity >= 80 -> "High Risk"
            severity >= 40 -> "Suspicious"
            severity > 0 -> "Low Risk"
            else -> "Safe"
        }
    }
}

