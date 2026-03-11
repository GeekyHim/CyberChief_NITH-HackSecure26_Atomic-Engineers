package com.example.hacksecure.ui.models

data class ConnectionLog(
    val sourceIp: String,
    val destinationIp: String,
    val protocol: String,
    val safetyScore: String,
    val timestamp: String
)

data class AppLog(
    val appId: Int,
    val appName: String,
    val totalDataMb: Int,
    val connections: List<ConnectionLog>,
    val lastActiveTimestamp: Long
) {
    val connectionCount: Int get() = connections.size
}

