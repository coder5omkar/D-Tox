package com.dtox.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.time.LocalDate

/**
 * All of D-Tox's storage. Plain SharedPreferences — no database, no
 * serialization libraries, nothing leaves the device.
 */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences("dtox", Context.MODE_PRIVATE)

    /** Packages the user chose to gate. */
    var gatedApps: Set<String>
        get() = sp.getStringSet(KEY_GATED, emptySet()) ?: emptySet()
        set(value) = sp.edit { putStringSet(KEY_GATED, value) }

    /** Length of the breathing pause in seconds. */
    var breathSeconds: Int
        get() = sp.getInt(KEY_SECONDS, 30)
        set(value) = sp.edit { putInt(KEY_SECONDS, value) }

    /** Id of the selected [BreathPattern]. */
    var patternId: String
        get() = sp.getString(KEY_PATTERN, BreathPattern.BOX.id) ?: BreathPattern.BOX.id
        set(value) = sp.edit { putString(KEY_PATTERN, value) }

    /** Gentle haptic tick on every phase change. */
    var haptics: Boolean
        get() = sp.getBoolean(KEY_HAPTICS, true)
        set(value) = sp.edit { putBoolean(KEY_HAPTICS, value) }

    /** Allow opening the app before the breath has finished (not recommended). */
    var allowSkip: Boolean
        get() = sp.getBoolean(KEY_SKIP, false)
        set(value) = sp.edit { putBoolean(KEY_SKIP, value) }

    fun pausesToday(): Int {
        rolloverIfNeeded()
        return sp.getInt(KEY_TODAY, 0)
    }

    val totalPauses: Int
        get() = sp.getInt(KEY_TOTAL, 0)

    val totalSeconds: Long
        get() = sp.getLong(KEY_TOTAL_SECONDS, 0L)

    fun recordPause(seconds: Int) {
        rolloverIfNeeded()
        sp.edit {
            putInt(KEY_TODAY, sp.getInt(KEY_TODAY, 0) + 1)
            putInt(KEY_TOTAL, sp.getInt(KEY_TOTAL, 0) + 1)
            putLong(KEY_TOTAL_SECONDS, sp.getLong(KEY_TOTAL_SECONDS, 0L) + seconds)
        }
    }

    private fun rolloverIfNeeded() {
        val today = LocalDate.now().toString()
        if (sp.getString(KEY_DATE, "") != today) {
            sp.edit {
                putString(KEY_DATE, today)
                putInt(KEY_TODAY, 0)
            }
        }
    }

    private companion object {
        const val KEY_GATED = "gated_apps"
        const val KEY_SECONDS = "breath_seconds"
        const val KEY_PATTERN = "breath_pattern"
        const val KEY_HAPTICS = "haptics"
        const val KEY_SKIP = "allow_skip"
        const val KEY_DATE = "stats_date"
        const val KEY_TODAY = "pauses_today"
        const val KEY_TOTAL = "pauses_total"
        const val KEY_TOTAL_SECONDS = "seconds_total"
    }
}
