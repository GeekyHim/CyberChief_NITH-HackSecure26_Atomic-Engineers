package com.example.hacksecure

import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.hacksecure.data.database.DatabaseClient
import com.example.hacksecure.data.entity.EventLog
import com.example.hacksecure.data.entity.MonitoredApp
import com.example.hacksecure.logging.LoggingEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.IOException

class LocalVpnService : VpnService() {

    companion object {
        @Volatile
        var isRunning: Boolean = false
            internal set

        internal fun markStopped() {
            isRunning = false
        }
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var vpnInterface: ParcelFileDescriptor? = null
    private val loggingEngine: LoggingEngine by lazy { LoggingEngine(applicationContext) }
    private val cachedSelfAppId: Int by lazy { ensureAppIdForPackage(applicationContext.packageName) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("VPN_ENGINE", "LocalVpnService onStartCommand")

        if (vpnInterface == null) {
            val builder = Builder()
                .setSession("SafeSignalEngine")
                .setMtu(1500)
                .addAddress("10.0.0.2", 24)
                .addRoute("0.0.0.0", 0)

            try {
                builder.addDisallowedApplication("com.android.chrome")
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e("VPN_ENGINE", "Chrome not installed on this device, skipping bypass.", e)
            }

            val iface = builder.establish()
            if (iface == null) {
                Log.e("VPN_ENGINE", "Failed to establish VPN interface")
                stopSelf()
                isRunning = false
                return START_NOT_STICKY
            }

            vpnInterface = iface
            isRunning = true
            startEngine(iface)
        }

        return START_STICKY
    }

    private fun startEngine(tunInterface: ParcelFileDescriptor) {
        val inputStream = FileInputStream(tunInterface.fileDescriptor)

        serviceScope.launch {
            val buffer = ByteArray(32767)

            while (isActive) {
                try {
                    val length = inputStream.read(buffer)
                    if (length <= 0) continue

                    processPacket(buffer, length)
                } catch (e: IOException) {
                    Log.e("VPN_ENGINE", "Error reading from VPN interface", e)
                    break
                }
            }
        }
    }

    private fun processPacket(packet: ByteArray, length: Int) {
        // Minimum IPv4 header length is 20 bytes
        if (length < 20) return

        val version = (packet[0].toInt() shr 4) and 0x0F
        if (version != 4) {
            // Not IPv4, ignore
            return
        }

        // IHL (Internet Header Length) is the lower 4 bits of the first byte, in 32‑bit words
        val ihl = packet[0].toInt() and 0x0F
        val ipHeaderLength = ihl * 4
        if (length < ipHeaderLength + 4) {
            // Not enough data for transport header ports
            return
        }

        val protocolNumber = packet[9].toInt() and 0xFF
        val protocol = when (protocolNumber) {
            6 -> "TCP"
            17 -> "UDP"
            else -> "Other"
        }

        val srcIp = "${packet[12].toInt() and 0xFF}." +
                "${packet[13].toInt() and 0xFF}." +
                "${packet[14].toInt() and 0xFF}." +
                "${packet[15].toInt() and 0xFF}"

        val destIp = "${packet[16].toInt() and 0xFF}." +
                "${packet[17].toInt() and 0xFF}." +
                "${packet[18].toInt() and 0xFF}." +
                "${packet[19].toInt() and 0xFF}"

        var srcPort: Int? = null
        var destPort: Int? = null

        // Only TCP/UDP have the usual 16‑bit source/destination ports at the start
        if (protocol == "TCP" || protocol == "UDP") {
            val transportHeaderStart = ipHeaderLength

            // Make sure we don't read past the packet
            if (length >= transportHeaderStart + 4) {
                srcPort = ((packet[transportHeaderStart].toInt() and 0xFF) shl 8) or
                        (packet[transportHeaderStart + 1].toInt() and 0xFF)
                destPort = ((packet[transportHeaderStart + 2].toInt() and 0xFF) shl 8) or
                        (packet[transportHeaderStart + 3].toInt() and 0xFF)
            }
        }

        if (srcPort != null && destPort != null) {
            Log.d(
                "VPN_ENGINE",
                "Protocol: $protocol | srcIp=$srcIp | srcPort=$srcPort | dstIp=$destIp | dstPort=$destPort"
            )
        } else {
            Log.d(
                "VPN_ENGINE",
                "Protocol: $protocol | Source: $srcIp | Dest: $destIp"
            )
        }

        // Minimal backend->DB wiring: persist a bounded subset of metadata so the UI updates in real time.
        val now = System.currentTimeMillis()
        val metadata = buildString {
            append("srcPort=").append(srcPort ?: -1)
            append(",dstPort=").append(destPort ?: -1)
        }
        val appId = cachedSelfAppId
        val event = EventLog(
            /* appId */ appId,
            /* eventType */ "NETWORK_CONNECTION",
            /* timestamp */ now,
            /* severity */ 0,
            /* metadata */ metadata,
            /* sourceIp */ srcIp,
            /* destinationIp */ destIp,
            /* protocol */ protocol,
            /* port */ destPort ?: 0
        )
        loggingEngine.logEvent(event)
    }

    private fun ensureAppIdForPackage(packageName: String): Int {
        val db = DatabaseClient.getInstance(applicationContext).database
        val appDao = db.appDao()

        val existing = appDao.getAppByPackageName(packageName)
        if (existing != null) {
            return existing.id
        }

        val appName = try {
            val label = packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            )
            label?.toString()?.takeIf { it.isNotBlank() } ?: "Unknown App"
        } catch (_: PackageManager.NameNotFoundException) {
            "Unknown App"
        }

        val insertedId = appDao.insertApp(
            MonitoredApp(
                packageName,
                appName,
                true
            )
        )

        return insertedId.toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        vpnInterface?.close()
        vpnInterface = null
        loggingEngine.shutdown()
        markStopped()
        Log.d("VPN_ENGINE", "VPN ENGINE destroyed")
    }
}
