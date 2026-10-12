package com.dtox.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Everything the completion screen needs after an exercise. */
data class Completion(
    val gemsAwarded: Int,
    val breakdown: List<Pair<String, Int>>,
    val streak: Int,
    val plantStageBefore: Int,
    val plantStageAfter: Int,
)

/**
 * All practice state: gems, streaks, sessions, shop unlocks, theme, Zen mode.
 * Lives in the same local SharedPreferences file as the gate — nothing leaves
 * the phone.
 */
class PracticeStore(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences("dtox", Context.MODE_PRIVATE)

    // ---- gems ----------------------------------------------------------

    val gems: Int get() = sp.getInt(KEY_GEMS, 0)

    fun addGems(count: Int) {
        if (count <= 0) return
        sp.edit { putInt(KEY_GEMS, gems + count) }
    }

    fun spendGems(count: Int): Boolean {
        if (count > gems) return false
        sp.edit { putInt(KEY_GEMS, gems - count) }
        return true
    }

    // ---- Zen mode ------------------------------------------------------

    var zenMode: Boolean
        get() = sp.getBoolean(KEY_ZEN, false)
        set(value) = sp.edit { putBoolean(KEY_ZEN, value) }

    // ---- theme ---------------------------------------------------------

    var themeId: String
        get() = sp.getString(KEY_THEME, "forest") ?: "forest"
        set(value) = sp.edit { putString(KEY_THEME, value) }

    // ---- shop unlocks --------------------------------------------------

    val unlocked: Set<String>
        get() = sp.getStringSet(KEY_UNLOCKED, emptySet()) ?: emptySet()

    fun isUnlocked(itemId: String): Boolean = unlocked.contains(itemId)

    /** Buys [itemId]; returns false when gems are missing. */
    fun unlock(itemId: String): Boolean {
        val item = ShopItem.byId(itemId) ?: return false
        if (isUnlocked(itemId)) return true
        if (!spendGems(item.cost)) return false
        sp.edit { putStringSet(KEY_UNLOCKED, unlocked + itemId) }
        return true
    }

    fun patternAvailable(pattern: BreathPattern): Boolean =
        pattern.unlockCost == null || isUnlocked("pattern:" + pattern.id)

    // ---- practice history ----------------------------------------------

    val totalSessions: Int get() = sp.getInt(KEY_SESSIONS, 0)

    val streak: Int get() = sp.getInt(KEY_STREAK, 0)

    fun sessionsFor(exerciseId: String): Int = sp.getInt("sessions:" + exerciseId, 0)

    private val lastSessionDay: String? get() = sp.getString(KEY_LAST_DAY, null)

    /** True when no practice session has happened yet today. */
    fun isFirstToday(): Boolean = lastSessionDay != LocalDate.now().toString()

    /** Days since the last session, or null when there has never been one. */
    fun daysSinceLastSession(): Long? {
        val last = lastSessionDay ?: return null
        return runCatching {
            ChronoUnit.DAYS.between(LocalDate.parse(last), LocalDate.now())
        }.getOrNull()
    }

    /**
     * Record a completed exercise and calculate the reward.
     * In Zen mode nothing is awarded, but history is still recorded so the
     * plant keeps growing when the user switches back.
     */
    fun recordCompletion(exercise: Exercise): Completion {
        val today = LocalDate.now()
        val todayStr = today.toString()
        val lastStr = lastSessionDay

        val firstToday = lastStr != todayStr
        val isNew = sessionsFor(exercise.id) == 0
        val streakBefore = streak

        val newStreak = when {
            lastStr == null -> 1
            else -> {
                val diff = runCatching {
                    ChronoUnit.DAYS.between(LocalDate.parse(lastStr), today)
                }.getOrDefault(999L)
                when {
                    diff <= 0L -> streakBefore
                    diff == 1L -> {
                        sp.edit { putString(KEY_GRACE, "") } // grace refreshes after a real day
                        streakBefore + 1
                    }
                    diff == 2L && sp.getString(KEY_GRACE, "") != lastStr -> {
                        sp.edit { putString(KEY_GRACE, lastStr) } // one grace day
                        streakBefore + 1
                    }
                    else -> 1
                }
            }
        }

        val stageBefore = plantStage(totalSessions)

        sp.edit {
            putInt(KEY_SESSIONS, totalSessions + 1)
            putInt("sessions:" + exercise.id, sessionsFor(exercise.id) + 1)
            putString(KEY_LAST_DAY, todayStr)
            putInt(KEY_STREAK, newStreak)
        }

        val stageAfter = plantStage(totalSessions)

        if (zenMode) {
            return Completion(0, emptyList(), newStreak, stageBefore, stageAfter)
        }

        val breakdown = mutableListOf<Pair<String, Int>>()
        breakdown += "Exercise" to exercise.baseGems
        if (firstToday) breakdown += "First today" to 2
        if (isNew) breakdown += "New exercise" to 5
        if (newStreak != streakBefore && newStreak % 7 == 0) {
            breakdown += "Streak day $newStreak" to 10
        }
        if (newStreak == 30) breakdown += "30-day streak" to 50

        val total = breakdown.sumOf { it.second }
        addGems(total)

        return Completion(total, breakdown, newStreak, stageBefore, stageAfter)
    }

    /** +1 bonus gem when a mood check-in improves. */
    fun awardMoodBonus(): Int {
        if (zenMode) return 0
        addGems(1)
        return 1
    }

    companion object {
        /** Session-count thresholds for the plant's growth stages. */
        private val PLANT_STAGES = intArrayOf(0, 1, 5, 15, 30, 60, 100, 200)

        val PLANT_STAGE_NAMES = listOf(
            "Seed",
            "Sprout",
            "Small stem",
            "Leaves",
            "Bud",
            "Flower",
            "Full bloom",
            "Ancient tree",
        )

        fun plantStage(sessions: Int): Int {
            var stage = 0
            PLANT_STAGES.forEachIndexed { i, threshold ->
                if (sessions >= threshold) stage = i
            }
            return stage
        }

        fun plantStageName(stage: Int): String =
            PLANT_STAGE_NAMES[stage.coerceIn(0, PLANT_STAGE_NAMES.lastIndex)]

        private const val KEY_GEMS = "gems"
        private const val KEY_ZEN = "zen_mode"
        private const val KEY_THEME = "theme"
        private const val KEY_UNLOCKED = "unlocked"
        private const val KEY_SESSIONS = "total_sessions"
        private const val KEY_STREAK = "streak"
        private const val KEY_LAST_DAY = "last_session_day"
        private const val KEY_GRACE = "grace_for_day"
    }
}
