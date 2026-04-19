package com.paul.simplesprint.core.repositories

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.paul.simplesprint.core.models.LastRunResult
import com.paul.simplesprint.core.models.SavedRunResult
import com.paul.simplesprint.features.motion_detection.MotionDetectionConfig
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "sprint_sync_store")

class LocalRepository(
    private val context: Context,
) {
    companion object {
        private const val AUTO_RESET_DELAY_MIN_SECONDS = 1
        private const val AUTO_RESET_DELAY_MAX_SECONDS = 5
        private const val AUTO_RESET_DELAY_DEFAULT_SECONDS = 3
        private const val TABLET_TIMER_SCALE_MIN_PERCENT = 28
        private const val TABLET_TIMER_SCALE_MAX_PERCENT = 52
        private const val TABLET_TIMER_SCALE_DEFAULT_PERCENT = 36
        private val MOTION_CONFIG_KEY = stringPreferencesKey("motion_detection_config_v2")
        private val LAST_RUN_KEY = stringPreferencesKey("last_run_result_v2_nanos")
        private val SAVED_RUN_RESULTS_KEY = stringPreferencesKey("saved_run_results_v1")
        private val AUTO_RESET_ENABLED_KEY = booleanPreferencesKey("auto_reset_enabled_v1")
        private val AUTO_RESET_DELAY_SECONDS_KEY = intPreferencesKey("auto_reset_delay_seconds_v1")
        private val TABLET_TIMER_SCALE_PERCENT_KEY = intPreferencesKey("tablet_timer_scale_percent_v1")
    }

    suspend fun loadMotionConfig(): MotionDetectionConfig {
        val snapshot = context.dataStore.data.first()
        val encoded = snapshot[MOTION_CONFIG_KEY] ?: return MotionDetectionConfig.defaults()
        return MotionDetectionConfig.fromJsonString(encoded)
    }

    suspend fun saveMotionConfig(config: MotionDetectionConfig) {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs[MOTION_CONFIG_KEY] = config.toJsonString()
        }
    }

    suspend fun loadLastRun(): LastRunResult? {
        val snapshot = context.dataStore.data.first()
        val encoded = snapshot[LAST_RUN_KEY] ?: return null
        return LastRunResult.fromJsonString(encoded)
    }

    suspend fun saveLastRun(run: LastRunResult) {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs[LAST_RUN_KEY] = run.toJsonString()
        }
    }

    suspend fun clearLastRun() {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs.remove(LAST_RUN_KEY)
        }
    }

    suspend fun loadSavedRunResults(): List<SavedRunResult> {
        val snapshot = context.dataStore.data.first()
        val encoded = snapshot[SAVED_RUN_RESULTS_KEY] ?: return emptyList()
        return SavedRunResult.listFromJsonString(encoded)
    }

    suspend fun saveSavedRunResults(results: List<SavedRunResult>) {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs[SAVED_RUN_RESULTS_KEY] = SavedRunResult.listToJsonString(results)
        }
    }

    suspend fun addSavedRunResult(result: SavedRunResult) {
        val existing = loadSavedRunResults()
        saveSavedRunResults(existing + result)
    }

    suspend fun deleteSavedRunResult(id: String) {
        val existing = loadSavedRunResults()
        saveSavedRunResults(existing.filterNot { it.id == id })
    }

    suspend fun loadAutoResetEnabled(): Boolean {
        val snapshot = context.dataStore.data.first()
        return snapshot[AUTO_RESET_ENABLED_KEY] ?: false
    }

    suspend fun saveAutoResetEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs[AUTO_RESET_ENABLED_KEY] = enabled
        }
    }

    suspend fun loadAutoResetDelaySeconds(): Int {
        val snapshot = context.dataStore.data.first()
        val persisted = snapshot[AUTO_RESET_DELAY_SECONDS_KEY] ?: AUTO_RESET_DELAY_DEFAULT_SECONDS
        return persisted.coerceIn(AUTO_RESET_DELAY_MIN_SECONDS, AUTO_RESET_DELAY_MAX_SECONDS)
    }

    suspend fun saveAutoResetDelaySeconds(delaySeconds: Int) {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs[AUTO_RESET_DELAY_SECONDS_KEY] = delaySeconds.coerceIn(
                AUTO_RESET_DELAY_MIN_SECONDS,
                AUTO_RESET_DELAY_MAX_SECONDS,
            )
        }
    }

    suspend fun loadTabletTimerScalePercent(): Int {
        val snapshot = context.dataStore.data.first()
        val persisted = snapshot[TABLET_TIMER_SCALE_PERCENT_KEY] ?: TABLET_TIMER_SCALE_DEFAULT_PERCENT
        return persisted.coerceIn(TABLET_TIMER_SCALE_MIN_PERCENT, TABLET_TIMER_SCALE_MAX_PERCENT)
    }

    suspend fun saveTabletTimerScalePercent(scalePercent: Int) {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs[TABLET_TIMER_SCALE_PERCENT_KEY] = scalePercent.coerceIn(
                TABLET_TIMER_SCALE_MIN_PERCENT,
                TABLET_TIMER_SCALE_MAX_PERCENT,
            )
        }
    }
}
