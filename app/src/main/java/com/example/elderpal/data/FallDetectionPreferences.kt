// Manages persistent user preferences for background fall detection using Jetpack DataStore.
package com.example.elderpal.data

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.fallPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "fall_detection_prefs")

class FallDetectionPreferences(private val context: Context) {

    private val fallDetectionKey = booleanPreferencesKey("fall_detection_enabled")

    // Reactive flow of whether fall detection is active
    val isFallDetectionEnabled: Flow<Boolean> = context.fallPrefsDataStore.data.map { preferences ->
        preferences[fallDetectionKey] ?: false
    }

    // Checks current preference synchronously
    suspend fun isEnabled(): Boolean {
        val preferences = context.fallPrefsDataStore.data.first()
        return preferences[fallDetectionKey] ?: false
    }

    // Persists the on/off preference and starts/stops the background service
    suspend fun setFallDetectionEnabled(enabled: Boolean) {
        context.fallPrefsDataStore.edit { preferences ->
            preferences[fallDetectionKey] = enabled
        }
        if (enabled) {
            startService(context)
        } else {
            stopService(context)
        }
    }

    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, FallDetectionService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Handle background service start restrictions
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FallDetectionService::class.java)
            context.stopService(intent)
        }
    }
}
