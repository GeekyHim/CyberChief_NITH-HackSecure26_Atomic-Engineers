package com.example.hacksecure.data.uid

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.example.hacksecure.data.database.DatabaseClient
import com.example.hacksecure.data.entity.MonitoredApp

/**
 * Resolves a network UID into a stable appId in the `apps` table:
 *
 * uid -> packageName -> appName -> apps.id
 *
 * This keeps the logging / buffering / scoring layers decoupled from
 * Android-specific PackageManager lookups and database writes.
 */
object UidAppResolver {

    /**
     * Resolves [uid] to an `apps.id` primary key, inserting a row into
     * the `apps` table when needed.
     *
     * If the UID cannot be resolved to any package, this returns -1
     * so callers can display "Unknown App" in the UI.
     */
    fun resolveAppIdForUid(context: Context, uid: Int): Int {
        val pm = context.packageManager

        val packageName = try {
            val packages = pm.getPackagesForUid(uid)
            packages?.firstOrNull()?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e("VPN_ENGINE", "Failed to resolve packages for uid=$uid", e)
            null
        }

        if (packageName == null) {
            return -1
        }

        val db = DatabaseClient.getInstance(context.applicationContext).database
        val appDao = db.appDao()

        val existing = appDao.getAppByPackageName(packageName)
        if (existing != null) {
            return existing.id
        }

        val appName = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val label = pm.getApplicationLabel(appInfo)
            label?.toString()?.takeIf { it.isNotBlank() } ?: "Unknown App"
        } catch (e: PackageManager.NameNotFoundException) {
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
}

