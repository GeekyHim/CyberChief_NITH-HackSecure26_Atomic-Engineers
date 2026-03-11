package com.example.hacksecure

import android.content.Context
import android.util.Log

/**
 * Independent reputation / blacklist helper.
 *
 * This does NOT modify LocalVpnService. You can wire it up later
 * from any component by calling ReputationFilter.isBlacklistedIp(...).
 */
object ReputationFilter {

    // Thread‑safe lazy init of the in‑memory blacklist.
    // Reads res/raw/blacklist_ips.txt once, then keeps it in a HashSet for O(1) lookups.
    @Volatile
    private var loaded = false
    private val blacklistedIps: HashSet<String> = HashSet()

    /**
     * Ensure blacklist is loaded into memory.
     * Safe to call many times; file is only read once.
     */
    private fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            try {
                val input = context.resources.openRawResource(R.raw.blacklist_ips)
                input.bufferedReader().useLines { lines ->
                    lines.map { it.trim() }
                        .filter { it.isNotEmpty() && !it.startsWith("#") }
                        .forEach { blacklistedIps.add(it) }
                }
                loaded = true
                Log.d("VPN_ENGINE", "ReputationFilter loaded ${blacklistedIps.size} IPs")
            } catch (e: Exception) {
                Log.e("VPN_ENGINE", "Failed to load blacklist_ips", e)
            }
        }
    }

    /**
     * Public API: check if an IP string is blacklisted.
     * Returns false if the list failed to load or IP is not present.
     */
    fun isBlacklistedIp(context: Context, ip: String): Boolean {
        ensureLoaded(context)
        return blacklistedIps.contains(ip)
    }
}

