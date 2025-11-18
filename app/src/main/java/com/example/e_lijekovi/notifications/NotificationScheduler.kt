package com.example.e_lijekovi.notifications

import android.content.Context
import android.util.Log

/**
 * Placeholder NotificationScheduler to be implemented in later modules.
 * Currently provides a no-op updateAll so repository can call it safely.
 */
object NotificationScheduler {
    fun updateAll(context: Context) {
        // Minimal action to avoid unused-parameter warnings and to help debugging
        try {
            val pkg = context.packageName
            Log.d("NotificationScheduler", "updateAll called for $pkg")
        } catch (t: Throwable) {
            // ignore
        }
    }
}
