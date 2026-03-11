package com.example.hacksecure.debug

import android.content.Context
import android.util.Log
import com.example.hacksecure.data.database.DatabaseClient
import com.example.hacksecure.data.entity.EventLog
import com.example.hacksecure.data.entity.MonitoredApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

object DebugDataGenerator {

    private data class DemoApp(
        val packageName: String,
        val appName: String,
        val eventsToGenerate: Int
    )

    private val demoApps = listOf(
        DemoApp("com.android.chrome", "Chrome", eventsToGenerate = 15),
        DemoApp("com.instagram.android", "Instagram", eventsToGenerate = 12),
        DemoApp("com.whatsapp", "WhatsApp", eventsToGenerate = 20),
        DemoApp("com.google.android.gm", "Gmail", eventsToGenerate = 10),
        DemoApp("com.google.android.youtube", "YouTube", eventsToGenerate = 10),
        DemoApp("com.facebook.katana", "Facebook", eventsToGenerate = 12),
        DemoApp("com.twitter.android", "Twitter", eventsToGenerate = 10),
        DemoApp("com.google.android.apps.maps", "Google Maps", eventsToGenerate = 8),
        DemoApp("com.spotify.music", "Spotify", eventsToGenerate = 10),
        DemoApp("com.snapchat.android", "Snapchat", eventsToGenerate = 8)
    )

    fun generateDemoData(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = DatabaseClient.getInstance(context.applicationContext).database
                val appDao = db.appDao()
                val eventLogDao = db.eventLogDao()

                val appIdByPackage = mutableMapOf<String, Int>()

                // Ensure apps are present
                for (demoApp in demoApps) {
                    val existing = appDao.getAppByPackageName(demoApp.packageName)
                    val appId = if (existing != null) {
                        existing.id
                    } else {
                        val insertedId = appDao.insertApp(
                            MonitoredApp(
                                demoApp.packageName,
                                demoApp.appName,
                                true
                            )
                        )
                        insertedId.toInt()
                    }
                    appIdByPackage[demoApp.packageName] = appId
                }

                val now = System.currentTimeMillis()
                val threeHoursMs = 3L * 60L * 60L * 1000L

                val events = mutableListOf<EventLog>()

                for (demoApp in demoApps) {
                    val appId = appIdByPackage[demoApp.packageName] ?: continue
                    repeat(demoApp.eventsToGenerate) {
                        val timestampOffset = Random.nextLong(0L, threeHoursMs)
                        val timestamp = now - timestampOffset

                        val (eventType, severity, destIp, port) = generateNetworkPatternForApp(demoApp.packageName)

                        val sourceIp = "192.168.1.${Random.nextInt(2, 250)}"
                        val protocol = if (port == 53 || port == 3478) "UDP" else "TCP"

                        val metadata = "demo=true; app=${demoApp.appName}; dest=$destIp:$port"

                        events += EventLog(
                            appId,
                            eventType,
                            timestamp,
                            severity,
                            metadata,
                            sourceIp,
                            destIp,
                            protocol,
                            port
                        )
                    }
                }

                if (events.isNotEmpty()) {
                    eventLogDao.insertLogs(events)
                }
            } catch (t: Throwable) {
                Log.e("DEBUG_DATA", "Failed to generate demo data", t)
            }
        }
    }

    private fun generateNetworkPatternForApp(packageName: String): Quad {
        return when (packageName) {
            "com.android.chrome" -> Quad(
                eventType = "HTTPS_REQUEST",
                severity = 1,
                destIp = randomFrom(
                    "142.250.${Random.nextInt(160, 191)}.${Random.nextInt(1, 254)}",
                    "172.217.${Random.nextInt(0, 31)}.${Random.nextInt(1, 254)}",
                    "1.1.1.${Random.nextInt(1, 254)}"
                ),
                port = 443
            )

            "com.instagram.android",
            "com.facebook.katana" -> Quad(
                eventType = "API_CALL",
                severity = 2,
                destIp = "157.240.${Random.nextInt(0, 64)}.${Random.nextInt(1, 254)}",
                port = 443
            )

            "com.whatsapp" -> Quad(
                eventType = "MESSAGE_TRAFFIC",
                severity = 1,
                destIp = "157.240.${Random.nextInt(0, 64)}.${Random.nextInt(1, 254)}",
                port = 5228
            )

            "com.google.android.youtube" -> Quad(
                eventType = "STREAMING",
                severity = 2,
                destIp = "142.250.${Random.nextInt(160, 191)}.${Random.nextInt(1, 254)}",
                port = 443
            )

            "com.google.android.apps.maps" -> Quad(
                eventType = "LOCATION_API",
                severity = 2,
                destIp = "172.217.${Random.nextInt(0, 31)}.${Random.nextInt(1, 254)}",
                port = 443
            )

            "com.spotify.music" -> Quad(
                eventType = "AUDIO_STREAM",
                severity = 1,
                destIp = "35.${Random.nextInt(160, 191)}.${Random.nextInt(0, 255)}.${Random.nextInt(1, 254)}",
                port = 443
            )

            "com.snapchat.android" -> Quad(
                eventType = "MEDIA_UPLOAD",
                severity = 3,
                destIp = "8.8.8.8",
                port = 443
            )

            "com.google.android.gm" -> Quad(
                eventType = "EMAIL_SYNC",
                severity = 1,
                destIp = "172.217.${Random.nextInt(0, 31)}.${Random.nextInt(1, 254)}",
                port = 443
            )

            "com.twitter.android" -> Quad(
                eventType = "SOCIAL_FEED",
                severity = 2,
                destIp = "104.244.${Random.nextInt(40, 55)}.${Random.nextInt(1, 254)}",
                port = 443
            )

            else -> Quad(
                eventType = "GENERIC_TRAFFIC",
                severity = 1,
                destIp = randomFrom("8.8.8.8", "1.1.1.1"),
                port = randomFrom(80, 443, 53, 5228, 3478)
            )
        }
    }

    private data class Quad(
        val eventType: String,
        val severity: Int,
        val destIp: String,
        val port: Int
    )

    private fun <T> randomFrom(vararg values: T): T {
        return values[Random.nextInt(values.size)]
    }
}

