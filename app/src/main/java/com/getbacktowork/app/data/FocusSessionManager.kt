package com.getbacktowork.app.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.telecom.TelecomManager
import com.getbacktowork.app.service.FocusTimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class FocusSessionManager internal constructor(
    private val context: Context?,
    private val prefs: SharedPreferences,
    private val clock: () -> Long,
    private val startTimerService: (Int) -> Unit,
    private val stopTimerService: () -> Unit
) {

    constructor(context: Context) : this(
        context = context,
        prefs = context.getSharedPreferences("get_back_to_work_prefs", Context.MODE_PRIVATE),
        clock = { System.currentTimeMillis() },
        startTimerService = { durationMinutes ->
            val serviceIntent = Intent(context, FocusTimerService::class.java).apply {
                action = FocusTimerService.ACTION_START_TIMER
                putExtra(FocusTimerService.EXTRA_DURATION_MINUTES, durationMinutes)
            }
            context.startForegroundService(serviceIntent)
        },
        stopTimerService = {
            context.stopService(Intent(context, FocusTimerService::class.java))
        }
    )

    private val selfPackageName: String = context?.packageName ?: "com.getbacktowork.focus"

    data class SessionState(
        val isActive: Boolean = false,
        val remainingMillis: Long = 0L,
        val totalDurationMinutes: Int = 25,
        val isStrictMode: Boolean = true,
        val allowedAppsCount: Int = 0,
        val isEmergencyPassActive: Boolean = false,
        val emergencyPassRemainingMillis: Long = 0L,
        val remainingPassesToday: Int = 2
    )

    data class HolidayState(
        val isMasterEnabled: Boolean = false,
        val isHolidayActiveToday: Boolean = false,
        val isManualHolidayEnabled: Boolean = false,
        val isAutoScheduleEnabled: Boolean = false,
        val scheduledDays: Set<Int> = setOf(java.util.Calendar.SUNDAY, java.util.Calendar.SATURDAY),
        val limitType: String = LIMIT_TYPE_REELS, // "REELS" or "MINUTES"
        val baseLimitValue: Int = 100,
        val limitValue: Int = 100,
        val reelsScrolledToday: Int = 0,
        val minutesUsedToday: Int = 0,
        val isLimitReached: Boolean = false,
        val isSnoozed: Boolean = false
    )

    data class TravelState(
        val isTravelActive: Boolean = false,
        val remainingMillis: Long = 0L,
        val durationHours: Int = 2
    )

    data class UltraFocusState(
        val isEnabled: Boolean = false,
        val startTime: Long = 0L,
        val durationHours: Int = 24,
        val canQuitNormally: Boolean = false,
        val remainingUntil24hMillis: Long = 0L,
        val isEmergency30mActive: Boolean = false,
        val emergency30mRemainingMillis: Long = 0L,
        val hasUsedEmergency30mToday: Boolean = false,
        val isTransitPassActive: Boolean = false,
        val transitPassRemainingMillis: Long = 0L
    )

    data class DailyDiaryEntry(
        val date: String,
        val focusedMinutes: Int = 0,
        val coldTurkeyMinutes: Int = 0,
        val screenMinutes: Int = 0,
        val distractingMinutes: Int = 0,
        val reelsWatched: Int = 0,
        val note: String = ""
    )

    enum class AdviceLevel { GOOD, CAUTION, LOCK_RECOMMENDED }

    data class CoachAdvice(
        val headline: String,
        val detail: String,
        val level: AdviceLevel,
        val screenUsedMinutes: Int,
        val screenGoalMinutes: Int,
        val focusedMinutes: Int,
        val streakDays: Int
    )

    data class ReflectionEntry(
        val timestamp: Long,
        val date: String,
        val reason: String
    )

    data class WhitelistGroup(
        val name: String,
        val packages: Set<String>,
        val isCustom: Boolean = true
    )

    data class YearProgressInfo(
        val year: Int,
        val dayOfYear: Int,
        val totalDays: Int,
        val daysRemaining: Int,
        val elapsedPercent: Int
    )

    data class FocusScheduleConfig(
        val workEnabled: Boolean = false,
        val workStartMinute: Int = 9 * 60,
        val workEndMinute: Int = 17 * 60,
        val workDays: Set<Int> = setOf(
            java.util.Calendar.MONDAY,
            java.util.Calendar.TUESDAY,
            java.util.Calendar.WEDNESDAY,
            java.util.Calendar.THURSDAY,
            java.util.Calendar.FRIDAY
        ),
        val sleepEnabled: Boolean = false,
        val sleepStartMinute: Int = 23 * 60,
        val sleepEndMinute: Int = 7 * 60,
        val isWorkActiveNow: Boolean = false,
        val isSleepActiveNow: Boolean = false
    )

    data class BlockedNotificationItem(
        val id: String,
        val packageName: String,
        val appName: String,
        val title: String,
        val preview: String,
        val timestamp: Long
    )

    data class AppDailyCap(
        val packageName: String,
        val maxMinutesPerDay: Int = 0,
        val maxOpensPerDay: Int = 0
    )

    data class AppTodayUsage(
        val packageName: String,
        val date: String,
        val opens: Int = 0,
        val minutes: Int = 0
    )

    data class ShieldDiagnosticEvent(
        val timestamp: Long,
        val category: String,
        val packageName: String,
        val detail: String
    )

    private val _sessionState = MutableStateFlow(getCurrentState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _allowedPackagesFlow = MutableStateFlow(getAllowedPackages())
    val allowedPackagesFlow: StateFlow<Set<String>> = _allowedPackagesFlow.asStateFlow()

    private val _savedGroupsFlow = MutableStateFlow(getSavedGroupsInternal())
    val savedGroupsFlow: StateFlow<List<WhitelistGroup>> = _savedGroupsFlow.asStateFlow()

    private val _adultFilterFlow = MutableStateFlow(isAdultFilterEnabled())
    val adultFilterFlow: StateFlow<Boolean> = _adultFilterFlow.asStateFlow()

    private val _blockIncognitoFlow = MutableStateFlow(isBlockIncognitoEnabled())
    val blockIncognitoFlow: StateFlow<Boolean> = _blockIncognitoFlow.asStateFlow()

    private val _holidayStateFlow = MutableStateFlow(getHolidayStateInternal())
    val holidayStateFlow: StateFlow<HolidayState> = _holidayStateFlow.asStateFlow()

    private val _travelStateFlow = MutableStateFlow(getTravelStateInternal())
    val travelStateFlow: StateFlow<TravelState> = _travelStateFlow.asStateFlow()

    private val _ultraFocusStateFlow = MutableStateFlow(getUltraFocusStateInternal())
    val ultraFocusStateFlow: StateFlow<UltraFocusState> = _ultraFocusStateFlow.asStateFlow()

    private val _diaryEntriesFlow = MutableStateFlow(loadDiaryEntriesInternal())
    val diaryEntriesFlow: StateFlow<List<DailyDiaryEntry>> = _diaryEntriesFlow.asStateFlow()

    private val _dailyScreenGoalFlow = MutableStateFlow(getDailyScreenGoalMinutes())
    val dailyScreenGoalFlow: StateFlow<Int> = _dailyScreenGoalFlow.asStateFlow()

    private val _reelsOverlayEnabledFlow = MutableStateFlow(isReelsOverlayEnabled())
    val reelsOverlayEnabledFlow: StateFlow<Boolean> = _reelsOverlayEnabledFlow.asStateFlow()

    private val _dailyGoalReminderFlow = MutableStateFlow(isDailyGoalReminderEnabled())
    val dailyGoalReminderFlow: StateFlow<Boolean> = _dailyGoalReminderFlow.asStateFlow()

    private val _homePinnedPackagesFlow = MutableStateFlow(getHomePinnedPackages())
    val homePinnedPackagesFlow: StateFlow<Set<String>> = _homePinnedPackagesFlow.asStateFlow()

    private val _homePinnedOrderFlow = MutableStateFlow(getHomePinnedOrder())
    val homePinnedOrderFlow: StateFlow<List<String>> = _homePinnedOrderFlow.asStateFlow()

    private val _homePinnedCustomizedFlow = MutableStateFlow(isHomePinnedCustomized())
    val homePinnedCustomizedFlow: StateFlow<Boolean> = _homePinnedCustomizedFlow.asStateFlow()

    private val _lifeGoalsFlow = MutableStateFlow(getLifeGoals())
    val lifeGoalsFlow: StateFlow<List<String>> = _lifeGoalsFlow.asStateFlow()

    private val _scheduleConfigFlow = MutableStateFlow(getScheduleConfigInternal())
    val scheduleConfigFlow: StateFlow<FocusScheduleConfig> = _scheduleConfigFlow.asStateFlow()

    private val _blockShortsInFocusFlow = MutableStateFlow(isBlockShortsInFocus())
    val blockShortsInFocusFlow: StateFlow<Boolean> = _blockShortsInFocusFlow.asStateFlow()

    private val _blockYtHomeFeedFlow = MutableStateFlow(isBlockYtHomeFeed())
    val blockYtHomeFeedFlow: StateFlow<Boolean> = _blockYtHomeFeedFlow.asStateFlow()

    private val _blockIgExploreFlow = MutableStateFlow(isBlockIgExplore())
    val blockIgExploreFlow: StateFlow<Boolean> = _blockIgExploreFlow.asStateFlow()

    private val _blockSocialFeedsFlow = MutableStateFlow(isBlockSocialAlgoFeeds())
    val blockSocialFeedsFlow: StateFlow<Boolean> = _blockSocialFeedsFlow.asStateFlow()

    private val _blockedDomainsFlow = MutableStateFlow(getBlockedDomains())
    val blockedDomainsFlow: StateFlow<Set<String>> = _blockedDomainsFlow.asStateFlow()

    private val _alwaysBlockDomainsFlow = MutableStateFlow(isAlwaysBlockDomainsEnabled())
    val alwaysBlockDomainsFlow: StateFlow<Boolean> = _alwaysBlockDomainsFlow.asStateFlow()

    private val _cooldownSecondsFlow = MutableStateFlow(getCooldownSeconds())
    val cooldownSecondsFlow: StateFlow<Int> = _cooldownSecondsFlow.asStateFlow()

    private val _cooldownPackagesFlow = MutableStateFlow(getCooldownPackages())
    val cooldownPackagesFlow: StateFlow<Set<String>> = _cooldownPackagesFlow.asStateFlow()

    private val _customAppNamesFlow = MutableStateFlow(getCustomAppNames())
    val customAppNamesFlow: StateFlow<Map<String, String>> = _customAppNamesFlow.asStateFlow()

    private val _earnedScrollUntilFlow = MutableStateFlow(getEarnedScrollEndTime())
    val earnedScrollUntilFlow: StateFlow<Long> = _earnedScrollUntilFlow.asStateFlow()

    private val _blockedNotificationsFlow = MutableStateFlow(getBlockedNotifications())
    val blockedNotificationsFlow: StateFlow<List<BlockedNotificationItem>> = _blockedNotificationsFlow.asStateFlow()

    private val _appDailyCapsFlow = MutableStateFlow(getAppDailyCaps())
    val appDailyCapsFlow: StateFlow<Map<String, AppDailyCap>> = _appDailyCapsFlow.asStateFlow()

    private val _appTodayUsageFlow = MutableStateFlow(getAppTodayUsageMap())
    val appTodayUsageFlow: StateFlow<Map<String, AppTodayUsage>> = _appTodayUsageFlow.asStateFlow()

    private val _shieldDiagnosticsFlow = MutableStateFlow(getShieldDiagnostics())
    val shieldDiagnosticsFlow: StateFlow<List<ShieldDiagnosticEvent>> = _shieldDiagnosticsFlow.asStateFlow()

    companion object {
        const val LIMIT_TYPE_REELS = "REELS"
        const val LIMIT_TYPE_MINUTES = "MINUTES"

        private const val KEY_IS_ACTIVE = "key_is_active"
        private const val KEY_END_TIME = "key_end_time"
        private const val KEY_DURATION_MINUTES = "key_duration_minutes"
        private const val KEY_STRICT_MODE = "key_strict_mode"
        private const val KEY_ALLOWED_PACKAGES = "key_allowed_packages"
        private const val KEY_SAVED_GROUPS = "key_saved_groups"
        private const val KEY_ADULT_FILTER_ENABLED = "key_adult_filter_enabled"
        private const val KEY_BLOCK_INCOGNITO_ENABLED = "key_block_incognito_enabled"
        private const val KEY_TRAVEL_END_TIME = "key_travel_end_time"
        private const val KEY_TRAVEL_DURATION_HOURS = "key_travel_duration_hours"
        private const val MAX_DAILY_EMERGENCY_PASSES = 2
        private const val KEY_EMERGENCY_PASS_END_TIME = "key_emergency_pass_end_time"
        private const val KEY_EMERGENCY_PASS_DATE = "key_emergency_pass_date"
        private const val KEY_EMERGENCY_PASSES_USED = "key_emergency_passes_used"
        private const val KEY_REFLECTIONS_JOURNAL = "key_reflections_journal"

        private const val KEY_HOLIDAY_MASTER_ENABLED = "key_holiday_master_enabled"
        private const val KEY_HOLIDAY_MANUAL_ENABLED = "key_holiday_manual_enabled"
        private const val KEY_HOLIDAY_AUTO_SCHEDULE = "key_holiday_auto_schedule"
        private const val KEY_HOLIDAY_SCHEDULED_DAYS = "key_holiday_scheduled_days"
        private const val KEY_HOLIDAY_LIMIT_TYPE = "key_holiday_limit_type"
        private const val KEY_HOLIDAY_REELS_LIMIT_VALUE = "key_holiday_reels_limit_value"
        private const val KEY_HOLIDAY_MINUTES_LIMIT_VALUE = "key_holiday_minutes_limit_value"
        private const val KEY_HOLIDAY_DATE = "key_holiday_date"
        private const val KEY_HOLIDAY_REELS_COUNT = "key_holiday_reels_count"
        private const val KEY_HOLIDAY_MINUTES_COUNT = "key_holiday_minutes_count"
        private const val KEY_HOLIDAY_REELS_EXTENSION = "key_holiday_reels_extension"
        private const val KEY_HOLIDAY_MINUTES_EXTENSION = "key_holiday_minutes_extension"
        private const val KEY_HOLIDAY_SNOOZE_END_TIME = "key_holiday_snooze_end_time"

        private const val KEY_ULTRA_FOCUS_ENABLED = "key_ultra_focus_enabled"
        private const val KEY_ULTRA_FOCUS_START_TIME = "key_ultra_focus_start_time"
        private const val KEY_ULTRA_FOCUS_DURATION_HOURS = "key_ultra_focus_duration_hours"
        private const val KEY_ULTRA_FOCUS_EMERGENCY_DATE = "key_ultra_focus_emergency_date"
        private const val KEY_ULTRA_TRANSIT_END_TIME = "key_ultra_transit_end_time"
        private const val KEY_ULTRA_CREDIT_LAST_TIME = "key_ultra_credit_last_time"

        private const val KEY_SESSION_START_TIME = "key_session_start_time"
        private const val KEY_SESSION_CREDITED_MINUTES = "key_session_credited_minutes"

        private const val KEY_DAILY_LOGS_DATA = "key_daily_logs_data"
        private const val KEY_DAILY_SCREEN_GOAL_MINUTES = "key_daily_screen_goal_minutes"
        private const val KEY_REELS_OVERLAY_ENABLED = "key_reels_overlay_enabled"
        private const val KEY_DAILY_GOAL_REMINDER_ENABLED = "key_daily_goal_reminder_enabled"
        private const val KEY_TRAVEL_REELS_COUNT = "key_travel_reels_count"
        private const val KEY_TRAVEL_SCREEN_MINUTES = "key_travel_screen_minutes"
        private const val KEY_TRAVEL_USAGE_DATE = "key_travel_usage_date"
        private const val KEY_LONG_VIDEO_MINUTES_COUNT = "key_long_video_minutes_count"
        private const val KEY_LONG_VIDEO_MINUTES_DATE = "key_long_video_minutes_date"
        private const val KEY_HOME_PINNED_PACKAGES = "key_home_pinned_packages"
        private const val KEY_HOME_PINNED_ORDER = "key_home_pinned_order"
        private const val KEY_HOME_PINNED_CUSTOMIZED = "key_home_pinned_customized"

        private const val KEY_LIFE_GOALS = "key_life_goals"
        private const val KEY_WORK_SCHED_ENABLED = "key_work_sched_enabled"
        private const val KEY_WORK_SCHED_START = "key_work_sched_start"
        private const val KEY_WORK_SCHED_END = "key_work_sched_end"
        private const val KEY_WORK_SCHED_DAYS = "key_work_sched_days"
        private const val KEY_SLEEP_SCHED_ENABLED = "key_sleep_sched_enabled"
        private const val KEY_SLEEP_SCHED_START = "key_sleep_sched_start"
        private const val KEY_SLEEP_SCHED_END = "key_sleep_sched_end"
        private const val KEY_BLOCK_SHORTS_IN_FOCUS = "key_block_shorts_in_focus"
        private const val KEY_BLOCK_YT_HOME = "key_block_yt_home"
        private const val KEY_BLOCK_IG_EXPLORE = "key_block_ig_explore"
        private const val KEY_BLOCK_SOCIAL_ALGO = "key_block_social_algo"
        private const val KEY_BLOCKED_DOMAINS = "key_blocked_domains"
        private const val KEY_ALWAYS_BLOCK_DOMAINS = "key_always_block_domains"
        private const val KEY_COOLDOWN_SECONDS = "key_cooldown_seconds"
        private const val KEY_COOLDOWN_PACKAGES = "key_cooldown_packages"
        private const val KEY_COOLDOWN_PASSES = "key_cooldown_passes"
        private const val KEY_CUSTOM_APP_NAMES = "key_custom_app_names"
        private const val KEY_EARNED_SCROLL_END_TIME = "key_earned_scroll_end_time"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_BLOCKED_NOTIFICATIONS = "key_blocked_notifications"
        private const val KEY_APP_DAILY_CAPS = "key_app_daily_caps"
        private const val KEY_APP_TODAY_USAGE = "key_app_today_usage"
        private const val KEY_APP_USAGE_DATE = "key_app_usage_date"
        private const val KEY_SHIELD_DIAGNOSTICS = "key_shield_diagnostics"

        val REALITY_CHECK_QUOTES = listOf(
            "Cold Turkey means zero retreat. You committed for 24 hours. No quitting, no excuses, no surrendering to cheap dopamine.",
            "Every urge to exit is your addiction begging to keep you weak. Defeat it. Live today, build your life, and refuse to surrender.",
            "You are trading your only youth, your highest potential, and your wildest dreams for cheap pixels on a piece of glass that will never remember your name.",
            "Nobody is coming to execute for you. Every single hour you spend numbing yourself on this phone is an hour of your future silently bleeding out into regret.",
            "Look at what you promised yourself. Are you going to be someone who achieved it, or someone who made excuses while swiping a glowing screen?",
            "The world does not reward intentions or consumption. It only rewards relentless, focused execution. Put the phone down and earn your self-respect.",
            "Your competition thanks you every time you unlock this phone. While you entertain yourself to escape boredom, they are building the life you dream about.",
            "You will blink, and five years will be gone. When you look back, you won't remember a single video you scrolled today—only the dream you abandoned to watch them.",
            "The pain of discipline weighs ounces. The pain of regret weighs tons. Choose which pain you want to carry before you unlock this phone.",
            "You don't need a break; you need results. Put this rectangle face down, look yourself in the mirror, and do the work you promised you would do."
        )

        @Volatile
        private var INSTANCE: FocusSessionManager? = null

        fun getInstance(context: Context): FocusSessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FocusSessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        // Initialize default allowed apps (e.g. Phone/Dialer, Messages) if first launch
        val existing = prefs.getStringSet(KEY_ALLOWED_PACKAGES, null)
        if (existing == null) {
            val defaults = getDefaultAllowedPackages()
            prefs.edit().putStringSet(KEY_ALLOWED_PACKAGES, defaults).apply()
        }
        refreshState()
    }

    fun startSession(durationMinutes: Int, strictMode: Boolean = true) {
        val now = clock()
        val endTime = now + (durationMinutes * 60 * 1000L)

        prefs.edit()
            .putBoolean(KEY_IS_ACTIVE, true)
            .putLong(KEY_END_TIME, endTime)
            .putLong(KEY_SESSION_START_TIME, now)
            .putInt(KEY_SESSION_CREDITED_MINUTES, 0)
            .putInt(KEY_DURATION_MINUTES, durationMinutes)
            .putBoolean(KEY_STRICT_MODE, strictMode)
            .commit()

        refreshState()

        // Start background Foreground Timer Service
        startTimerService(durationMinutes)
        notifyWidgets()
    }

    fun stopSession(force: Boolean = false) {
        if (!force && isStrictMode() && isSessionActive()) {
            // Cannot stop if strict mode is active and timer has not elapsed
            return
        }

        syncSessionFocusCredit()

        prefs.edit()
            .putBoolean(KEY_IS_ACTIVE, false)
            .putLong(KEY_END_TIME, 0L)
            .putLong(KEY_SESSION_START_TIME, 0L)
            .putInt(KEY_SESSION_CREDITED_MINUTES, 0)
            .commit()

        refreshState()

        stopTimerService()
        notifyWidgets()
    }

    fun isSessionActive(): Boolean {
        val active = prefs.getBoolean(KEY_IS_ACTIVE, false)
        if (!active) return false

        syncSessionFocusCredit()

        val endTime = prefs.getLong(KEY_END_TIME, 0L)
        val now = clock()
        if (now >= endTime) {
            // Session elapsed cleanly
            prefs.edit()
                .putBoolean(KEY_IS_ACTIVE, false)
                .putLong(KEY_END_TIME, 0L)
                .putLong(KEY_SESSION_START_TIME, 0L)
                .putInt(KEY_SESSION_CREDITED_MINUTES, 0)
                .commit()
            return false
        }
        return true
    }

    private fun syncSessionFocusCredit() {
        val startTime = prefs.getLong(KEY_SESSION_START_TIME, 0L)
        val endTime = prefs.getLong(KEY_END_TIME, 0L)
        if (startTime <= 0L || endTime <= startTime) return

        val effectiveEnd = minOf(clock(), endTime)
        val totalElapsedMins = ((effectiveEnd - startTime) / 60_000L).toInt().coerceAtLeast(0)
        val alreadyCredited = prefs.getInt(KEY_SESSION_CREDITED_MINUTES, 0)
        val delta = totalElapsedMins - alreadyCredited
        if (delta > 0) {
            prefs.edit().putInt(KEY_SESSION_CREDITED_MINUTES, totalElapsedMins).commit()
            addFocusedMinutesToDiary(delta, isColdTurkey = false)
        }
    }

    /**
     * Determines whether unapproved apps should be actively blocked right now.
     * Returns true if a session is running and the user is NOT within an emergency pass cooldown,
     * OR if Ultra Focus Mode is enabled and user is not inside an active 30m emergency window.
     * Note: Travel Mode NEVER bypasses an active Strict Focus session.
     */
    fun shouldBlockApps(): Boolean {
        if (isSessionActive()) {
            return !isEmergencyPassActive()
        }
        if (isUltraFocusActive()) {
            if (isTravelModeActive()) return false
            return true
        }
        if (isScheduledFocusActiveNow()) {
            if (isEmergencyPassActive() || isTravelModeActive() || isEarnedScrollActive()) return false
            return true
        }
        return false
    }

    private fun getTodayDateString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(clock()))
    }

    fun isEmergencyPassActive(): Boolean {
        val passEndTime = prefs.getLong(KEY_EMERGENCY_PASS_END_TIME, 0L)
        val now = clock()
        return passEndTime > now
    }

    fun getRemainingEmergencyPasses(): Int {
        val today = getTodayDateString()
        val savedDate = prefs.getString(KEY_EMERGENCY_PASS_DATE, "")
        if (savedDate != today) {
            prefs.edit().putString(KEY_EMERGENCY_PASS_DATE, today).putInt(KEY_EMERGENCY_PASSES_USED, 0).commit()
            return MAX_DAILY_EMERGENCY_PASSES
        }
        val used = prefs.getInt(KEY_EMERGENCY_PASSES_USED, 0)
        return (MAX_DAILY_EMERGENCY_PASSES - used).coerceAtLeast(0)
    }

    fun activateEmergencyPass(durationMinutes: Int = 5): Boolean {
        val remaining = getRemainingEmergencyPasses()
        if (remaining <= 0) return false

        val used = prefs.getInt(KEY_EMERGENCY_PASSES_USED, 0)
        val now = clock()
        val passEndTime = now + (durationMinutes * 60 * 1000L)

        prefs.edit()
            .putInt(KEY_EMERGENCY_PASSES_USED, used + 1)
            .putLong(KEY_EMERGENCY_PASS_END_TIME, passEndTime)
            .commit()

        refreshState()
        return true
    }

    fun endEmergencyPassEarly() {
        prefs.edit().putLong(KEY_EMERGENCY_PASS_END_TIME, 0L).commit()
        refreshState()
    }

    fun getEmergencyPassRemainingMillis(): Long {
        val passEndTime = prefs.getLong(KEY_EMERGENCY_PASS_END_TIME, 0L)
        val now = clock()
        val diff = passEndTime - now
        return if (diff > 0) diff else 0L
    }

    fun saveReflection(reason: String) {
        try {
            val existingJson = prefs.getString(KEY_REFLECTIONS_JOURNAL, "[]") ?: "[]"
            val array = org.json.JSONArray(existingJson)
            val obj = org.json.JSONObject().apply {
                put("timestamp", clock())
                put("date", getTodayDateString())
                put("reason", reason.trim())
            }
            array.put(obj)
            prefs.edit().putString(KEY_REFLECTIONS_JOURNAL, array.toString()).commit()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun getReflections(): List<ReflectionEntry> {
        val list = mutableListOf<ReflectionEntry>()
        val rawJson = prefs.getString(KEY_REFLECTIONS_JOURNAL, "[]") ?: "[]"
        try {
            val arr = org.json.JSONArray(rawJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ReflectionEntry(
                        timestamp = obj.optLong("timestamp", 0L),
                        date = obj.optString("date", ""),
                        reason = obj.optString("reason", "")
                    )
                )
            }
        } catch (e: Exception) {
            // ignore
        }
        return list.reversed()
    }

    fun getRemainingMillis(): Long {
        if (!isSessionActive()) return 0L
        val endTime = prefs.getLong(KEY_END_TIME, 0L)
        val diff = endTime - clock()
        return if (diff > 0) diff else 0L
    }

    fun isStrictMode(): Boolean {
        return prefs.getBoolean(KEY_STRICT_MODE, true)
    }

    fun getAllowedPackages(): Set<String> {
        val set = prefs.getStringSet(KEY_ALLOWED_PACKAGES, emptySet()) ?: emptySet()
        val result = mutableSetOf<String>()
        // Filter out any legacy fallback packages so only real user apps show
        for (pkg in set) {
            if (pkg != selfPackageName &&
                pkg != "com.google.android.dialer" &&
                pkg != "com.android.dialer" &&
                pkg != "com.android.phone"
            ) {
                result.add(pkg)
            }
        }
        getDefaultDialerPackage()?.let { result.add(it) }
        return result
    }

    fun isWhitelistLocked(): Boolean {
        return (isSessionActive() && isStrictMode()) || isUltraFocusActive()
    }

    fun setAllowedPackages(packages: Set<String>): Boolean {
        if (isWhitelistLocked()) {
            return false
        }
        val cleaned = packages.filter {
            it != selfPackageName &&
            it != "com.google.android.dialer" &&
            it != "com.android.dialer" &&
            it != "com.android.phone"
        }.toSet()
        prefs.edit().putStringSet(KEY_ALLOWED_PACKAGES, HashSet(cleaned)).commit()
        _allowedPackagesFlow.value = getAllowedPackages()
        refreshState()
        return true
    }

    fun toggleAppAllowed(packageName: String): Boolean {
        if (isWhitelistLocked()) {
            return false
        }
        val current = (prefs.getStringSet(KEY_ALLOWED_PACKAGES, emptySet()) ?: emptySet()).toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        return setAllowedPackages(current)
    }

    fun isAppAllowed(packageName: String): Boolean {
        // Our app is always allowed
        if (packageName == selfPackageName) return true

        // Essential emergency/phone services are always allowed
        if (isDialerOrEmergencyPackage(packageName)) return true

        // System framework, biometrics, credentials, autofill, keyboards & OS utilities
        if (isSystemFrameworkOrInputService(packageName)) return true

        // Transit Lifeline inside Cold Turkey: allow maps, ride-hailing, rail/airlines & weather only
        if (isUltraFocusActive() && isUltraFocusTransitActive() && isTransitEssentialApp(packageName)) {
            return true
        }

        val allowed = getAllowedPackages()
        return allowed.contains(packageName)
    }

    fun isTransitEssentialApp(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return lower == "com.google.android.apps.maps" ||
            lower == "com.waze" ||
            lower == "net.osmand" ||
            lower == "com.ubercab" ||
            lower == "com.olacabs.customer" ||
            lower == "me.lyft.android" ||
            lower == "com.rapido.passenger" ||
            lower == "com.indrive.passenger" ||
            lower == "in.redbus.android" ||
            lower == "cris.org.in.prs.ima" ||
            lower == "com.ixigo.train.ixitrain" ||
            lower == "com.makemytrip" ||
            lower == "com.goibibo" ||
            lower == "com.indigo.android" ||
            lower == "com.moovit" ||
            lower == "com.citymapper.app.release" ||
            lower == "com.sec.android.daemonapp" ||
            lower == "com.google.android.apps.weather" ||
            lower == "com.google.android.apps.nbu.paisa.user" ||
            lower == "com.phonepe.app" ||
            lower == "net.one97.paytm"
    }

    fun isSystemFrameworkOrInputService(pkg: String): Boolean {
        val lower = pkg.lowercase()

        // 1. Google Play Services & Play Core
        if (lower == "com.google.android.gms" || lower == "com.google.android.gsf") return true

        // 2. Android Credential Manager & Autofill
        if (lower == "com.android.credentialmanager" || lower.contains("credentialmanager")) return true

        // 3. Biometrics, Fingerprint & Face Authentication
        if (lower.contains("biometric") || lower.contains("fingerprint")) return true

        // 4. Android Core & Permission Controllers
        if (lower == "android" ||
            lower == "com.android.permissioncontroller" ||
            lower == "com.google.android.permissioncontroller" ||
            lower == "com.samsung.android.permissioncontroller" ||
            lower.endsWith(".permissioncontroller")
        ) return true

        // 5. System File Pickers & Intent Resolvers
        if (lower == "com.android.documentsui" ||
            lower == "com.google.android.documentsui" ||
            lower == "com.android.intentresolver" ||
            lower == "com.sec.android.app.myfiles"
        ) return true

        // 6. WebViews & VPN Dialogs
        if (lower == "com.android.webview" ||
            lower == "com.google.android.webview" ||
            lower == "com.android.vpndialogs"
        ) return true

        // 7. Keyboards / Input Method Editors & Autofill (e.g. Samsung Pass)
        if (lower.contains("inputmethod") ||
            lower.contains("honeyboard") ||
            lower.contains("swiftkey") ||
            lower.contains("keyboard") ||
            lower.contains("samsungpass") ||
            lower.contains("authfw") ||
            lower.contains("autofill")
        ) return true

        // 8. OEM Assistant / Edge Services (Samsung)
        if (lower == "com.samsung.android.app.cocktailbarservice" ||
            lower.startsWith("com.samsung.android.game.")
        ) return true

        // 9. Essential Non-Addictive OS Utilities, QR Scanners, 2FA & Connectivity
        if (isEssentialSystemUtility(lower)) return true

        return false
    }

    /**
     * Permits essential on-device productivity & safety tools (QR scanners/camera, clock/alarms,
     * calculator, contacts, voice notes, 2FA authenticators, USB/MTP, NFC wallets, screenshot/share,
     * and lockscreen/AOD services) so Cold Turkey never breaks real-world tasks or notification actions.
     */
    fun isEssentialSystemUtility(pkgLower: String): Boolean {
        val lower = pkgLower.lowercase()

        // Built-in Camera & QR Code Scanners (e.g. Samsung Quick Settings QR tile launches sec.android.app.camera)
        if (lower == "com.sec.android.app.camera" ||
            lower == "com.google.android.googlecamera" ||
            lower == "com.android.camera" ||
            lower == "com.android.camera2" ||
            lower == "com.oneplus.camera" ||
            lower == "com.oplus.camera" ||
            lower == "com.miui.camera" ||
            lower == "com.huawei.camera" ||
            lower.contains("qrscanner") ||
            lower.contains("qrcode") ||
            lower.contains("barcodescanner")
        ) return true

        // Clocks, Alarms & Timers
        if (lower == "com.sec.android.app.clockpackage" ||
            lower == "com.google.android.deskclock" ||
            lower == "com.android.deskclock"
        ) return true

        // Calculator, Contacts & Voice Recorder
        if (lower == "com.sec.android.app.popupcalculator" ||
            lower == "com.google.android.calculator" ||
            lower == "com.android.calculator2" ||
            lower == "com.samsung.android.app.contacts" ||
            lower == "com.google.android.contacts" ||
            lower == "com.android.contacts" ||
            lower == "com.sec.android.app.voicenote" ||
            lower == "com.google.android.apps.recorder" ||
            lower == "com.android.soundrecorder"
        ) return true

        // Two-Factor Authenticators (2FA / MFA prompts from notifications)
        if (lower == "com.google.android.apps.authenticator2" ||
            lower == "com.azure.authenticator" ||
            lower == "com.duosecurity.duomobile" ||
            lower == "com.okta.android.auth" ||
            lower == "com.authy.authy" ||
            lower == "com.yubico.yubioath" ||
            lower == "com.bitwarden.authenticator"
        ) return true

        // USB Transfer, Quick Share, Cast, Connectivity & Captures
        if (lower == "com.samsung.android.mtp" ||
            lower == "com.samsung.android.mtpapplication" ||
            lower == "com.android.mtp" ||
            lower == "com.android.bluetooth" ||
            lower == "com.google.android.bluetooth" ||
            lower == "com.android.wifi.dialog" ||
            lower == "com.android.networkstack.tethering" ||
            lower == "com.samsung.android.app.sharelive" ||
            lower == "com.samsung.android.mdx.quickboard" ||
            lower == "com.samsung.android.app.smartcapture" ||
            lower == "com.samsung.android.app.screenrecorder" ||
            lower == "com.samsung.android.app.chooser" ||
            lower == "com.android.printspooler" ||
            lower == "com.google.android.apps.pdfviewer" ||
            lower == "com.samsung.android.app.clipboarduiservice"
        ) return true

        // Wallets / Tap-to-Pay NFC
        if (lower == "com.samsung.android.spay" ||
            lower == "com.samsung.android.spaymini" ||
            lower == "com.google.android.apps.walletnfcrel"
        ) return true

        // Lockscreen / AOD / Emergency & Safety (never launch blocker over AOD or SOS)
        if (lower == "com.samsung.android.app.aodservice" ||
            lower == "com.android.keyguard" ||
            lower == "com.samsung.android.keyguard" ||
            lower == "com.android.emergency" ||
            lower == "com.google.android.apps.safetyhub" ||
            lower == "com.samsung.android.app.sos"
        ) return true

        return false
    }

    private fun isDialerOrEmergencyPackage(pkg: String): Boolean {
        val lower = pkg.lowercase()
        if (lower.contains("dialer") ||
            lower.contains("incall") ||
            lower.contains("telecom") ||
            lower.contains("telephony") ||
            lower.contains("phone") ||
            lower.contains("emergency")
        ) {
            return true
        }

        val defaultDialer = getDefaultDialerPackage()
        if (pkg == defaultDialer) return true

        return false
    }

    fun getSavedGroups(): List<WhitelistGroup> {
        return getSavedGroupsInternal()
    }

    private fun getSavedGroupsInternal(): List<WhitelistGroup> {
        val list = mutableListOf<WhitelistGroup>()

        // 1. Built-in "Calls Only"
        val dialerSet = mutableSetOf<String>()
        getDefaultDialerPackage()?.let { dialerSet.add(it) }
        list.add(WhitelistGroup("Calls Only", dialerSet, isCustom = false))

        // 2. Built-in "Work Essentials"
        val workSet = getWorkEssentialsPackageSet()
        list.add(WhitelistGroup("Work Essentials", workSet, isCustom = false))

        // 3. User Custom Groups
        val rawJson = prefs.getString(KEY_SAVED_GROUPS, null)
        if (!rawJson.isNullOrEmpty()) {
            try {
                val jsonArray = JSONArray(rawJson)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val name = obj.getString("name")
                    val pkgsArray = obj.getJSONArray("packages")
                    val pkgs = mutableSetOf<String>()
                    for (j in 0 until pkgsArray.length()) {
                        pkgs.add(pkgsArray.getString(j))
                    }
                    list.add(WhitelistGroup(name, pkgs, isCustom = true))
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
        return list
    }

    fun saveCustomGroup(name: String, packages: Set<String>) {
        val existing = getSavedGroupsInternal().filter { it.isCustom && it.name != name }.toMutableList()
        existing.add(WhitelistGroup(name, packages, isCustom = true))

        val jsonArray = JSONArray()
        for (g in existing) {
            val obj = JSONObject()
            obj.put("name", g.name)
            val pkgsArray = JSONArray()
            for (p in g.packages) {
                pkgsArray.put(p)
            }
            obj.put("packages", pkgsArray)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_SAVED_GROUPS, jsonArray.toString()).commit()
        _savedGroupsFlow.value = getSavedGroupsInternal()
    }

    fun deleteCustomGroup(name: String) {
        val existing = getSavedGroupsInternal().filter { it.isCustom && it.name != name }
        val jsonArray = JSONArray()
        for (g in existing) {
            val obj = JSONObject()
            obj.put("name", g.name)
            val pkgsArray = JSONArray()
            for (p in g.packages) {
                pkgsArray.put(p)
            }
            obj.put("packages", pkgsArray)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_SAVED_GROUPS, jsonArray.toString()).commit()
        _savedGroupsFlow.value = getSavedGroupsInternal()
    }

    fun isAdultFilterEnabled(): Boolean {
        return prefs.getBoolean(KEY_ADULT_FILTER_ENABLED, true)
    }

    fun setAdultFilterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ADULT_FILTER_ENABLED, enabled).commit()
        _adultFilterFlow.value = enabled
    }

    fun isBlockIncognitoEnabled(): Boolean {
        return prefs.getBoolean(KEY_BLOCK_INCOGNITO_ENABLED, true)
    }

    fun setBlockIncognitoEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_INCOGNITO_ENABLED, enabled).commit()
        _blockIncognitoFlow.value = enabled
    }

    private fun getWorkEssentialsPackageSet(): Set<String> {
        val knownWork = setOf(
            "com.whatsapp",
            "com.slack",
            "com.microsoft.teams",
            "com.microsoft.office.outlook",
            "notion.id",
            "com.google.android.gm",
            "com.google.android.calendar",
            "com.google.android.apps.docs",
            "com.google.android.apps.tasks",
            "com.sec.android.app.popupcalculator",
            "com.sec.android.app.notes",
            "com.samsung.android.app.notes",
            "com.google.android.keep"
        )
        val result = mutableSetOf<String>()
        getDefaultDialerPackage()?.let { result.add(it) }
        val pm = context?.packageManager ?: return result
        for (pkg in knownWork) {
            try {
                pm.getPackageInfo(pkg, 0)
                result.add(pkg)
            } catch (e: Exception) {
                // Not installed
            }
        }
        return result
    }

    fun getDefaultDialerPackage(): String? {
        val ctx = context ?: return null
        return try {
            val telecom = ctx.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            telecom?.defaultDialerPackage
        } catch (e: Exception) {
            null
        }
    }

    private fun getDefaultAllowedPackages(): Set<String> {
        val defaults = mutableSetOf<String>()
        getDefaultDialerPackage()?.let { defaults.add(it) }
        return defaults
    }

    // -------------------------------------------------------------
    // HOLIDAY MODE (Conscious Leisure on Weekends / Days Off)
    // -------------------------------------------------------------

    fun isHolidayMasterEnabled(): Boolean {
        return prefs.getBoolean(KEY_HOLIDAY_MASTER_ENABLED, false)
    }

    fun setHolidayMasterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HOLIDAY_MASTER_ENABLED, enabled).commit()
        refreshHolidayState()
    }

    fun isHolidayAutoScheduleEnabled(): Boolean {
        return prefs.getBoolean(KEY_HOLIDAY_AUTO_SCHEDULE, false)
    }

    fun setHolidayAutoScheduleEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HOLIDAY_AUTO_SCHEDULE, enabled).commit()
        refreshHolidayState()
    }

    fun isHolidayToday(): Boolean {
        // If Travel Mode or Ultra Focus is active, Holiday Mode is suspended
        if (isTravelModeActive() || isUltraFocusActive()) return false

        // If master switch is OFF, Holiday Mode NEVER runs!
        if (!isHolidayMasterEnabled()) return false

        // Manual today override
        if (prefs.getBoolean(KEY_HOLIDAY_MANUAL_ENABLED, false)) return true

        // Auto schedule on selected days
        if (isHolidayAutoScheduleEnabled()) {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = clock() }
            val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
            return getHolidayScheduledDays().contains(dayOfWeek)
        }

        return false
    }

    fun setHolidayManualEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HOLIDAY_MANUAL_ENABLED, enabled).commit()
        refreshHolidayState()
    }

    fun isHolidaySnoozed(): Boolean {
        val snoozeEnd = prefs.getLong(KEY_HOLIDAY_SNOOZE_END_TIME, 0L)
        return clock() < snoozeEnd
    }

    fun snoozeHolidayReminder(minutes: Int = 30) {
        val snoozeEnd = clock() + (minutes * 60 * 1000L)
        prefs.edit().putLong(KEY_HOLIDAY_SNOOZE_END_TIME, snoozeEnd).commit()
        refreshHolidayState()
    }

    fun clearHolidaySnooze() {
        prefs.edit().remove(KEY_HOLIDAY_SNOOZE_END_TIME).commit()
        refreshHolidayState()
    }

    fun getHolidayScheduledDays(): Set<Int> {
        val defaultDays = setOf(
            java.util.Calendar.SUNDAY.toString(),
            java.util.Calendar.SATURDAY.toString()
        )
        val set = prefs.getStringSet(KEY_HOLIDAY_SCHEDULED_DAYS, defaultDays) ?: defaultDays
        return set.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun setHolidayScheduledDays(days: Set<Int>) {
        val strSet = days.map { it.toString() }.toSet()
        prefs.edit().putStringSet(KEY_HOLIDAY_SCHEDULED_DAYS, strSet).commit()
        refreshHolidayState()
    }

    fun toggleHolidayScheduledDay(day: Int) {
        val current = getHolidayScheduledDays().toMutableSet()
        if (current.contains(day)) {
            current.remove(day)
        } else {
            current.add(day)
        }
        setHolidayScheduledDays(current)
    }

    fun getHolidayLimitType(): String {
        return prefs.getString(KEY_HOLIDAY_LIMIT_TYPE, LIMIT_TYPE_REELS) ?: LIMIT_TYPE_REELS
    }

    fun setHolidayLimitType(type: String) {
        prefs.edit().putString(KEY_HOLIDAY_LIMIT_TYPE, type).commit()
        refreshHolidayState()
    }

    fun getHolidayBaseLimitValue(): Int {
        val isReels = getHolidayLimitType() == LIMIT_TYPE_REELS
        val key = if (isReels) KEY_HOLIDAY_REELS_LIMIT_VALUE else KEY_HOLIDAY_MINUTES_LIMIT_VALUE
        val defaultVal = if (isReels) 100 else 20
        return prefs.getInt(key, defaultVal)
    }

    fun getHolidayLimitValue(): Int {
        val type = getHolidayLimitType()
        return getHolidayBaseLimitValue() + getDailyHolidayExtension(type)
    }

    fun setHolidayLimitValue(value: Int) {
        val isReels = getHolidayLimitType() == LIMIT_TYPE_REELS
        val key = if (isReels) KEY_HOLIDAY_REELS_LIMIT_VALUE else KEY_HOLIDAY_MINUTES_LIMIT_VALUE
        prefs.edit().putInt(key, value).commit()
        refreshHolidayState()
    }

    fun getHolidayReelsScrolled(): Int {
        checkAndResetDailyHolidayCounters()
        return prefs.getInt(KEY_HOLIDAY_REELS_COUNT, 0)
    }

    fun incrementHolidayReelsScrolled() {
        checkAndResetDailyHolidayCounters()
        val current = prefs.getInt(KEY_HOLIDAY_REELS_COUNT, 0)
        prefs.edit().putInt(KEY_HOLIDAY_REELS_COUNT, current + 1).commit()
        recordReelsToDiary(1)
        refreshHolidayState()
    }

    fun getHolidayMinutesUsed(): Int {
        checkAndResetDailyHolidayCounters()
        return prefs.getInt(KEY_HOLIDAY_MINUTES_COUNT, 0)
    }

    fun addHolidayMinutesUsed(minutes: Int = 1) {
        checkAndResetDailyHolidayCounters()
        val current = prefs.getInt(KEY_HOLIDAY_MINUTES_COUNT, 0)
        prefs.edit().putInt(KEY_HOLIDAY_MINUTES_COUNT, current + minutes).commit()
        refreshHolidayState()
    }

    private fun getDailyHolidayExtension(limitType: String = getHolidayLimitType()): Int {
        checkAndResetDailyHolidayCounters()
        val key = if (limitType == LIMIT_TYPE_REELS) {
            KEY_HOLIDAY_REELS_EXTENSION
        } else {
            KEY_HOLIDAY_MINUTES_EXTENSION
        }
        return prefs.getInt(key, 0)
    }

    fun grantHolidayExtension(amount: Int) {
        checkAndResetDailyHolidayCounters()
        val key = if (getHolidayLimitType() == LIMIT_TYPE_REELS) {
            KEY_HOLIDAY_REELS_EXTENSION
        } else {
            KEY_HOLIDAY_MINUTES_EXTENSION
        }
        val current = prefs.getInt(key, 0)
        prefs.edit().putInt(key, current + amount).commit()
        snoozeHolidayReminder(30)
        refreshHolidayState()
    }

    fun resetHolidayCounters() {
        prefs.edit()
            .putInt(KEY_HOLIDAY_REELS_COUNT, 0)
            .putInt(KEY_HOLIDAY_MINUTES_COUNT, 0)
            .putInt(KEY_HOLIDAY_REELS_EXTENSION, 0)
            .putInt(KEY_HOLIDAY_MINUTES_EXTENSION, 0)
            .remove(KEY_HOLIDAY_SNOOZE_END_TIME)
            .commit()
        refreshHolidayState()
    }

    fun isHolidayLimitReached(): Boolean {
        if (!isHolidayToday()) return false
        if (isHolidaySnoozed()) return false
        val limit = getHolidayLimitValue()
        return if (getHolidayLimitType() == LIMIT_TYPE_REELS) {
            getHolidayReelsScrolled() >= limit
        } else {
            getHolidayMinutesUsed() >= limit
        }
    }

    private fun checkAndResetDailyHolidayCounters() {
        val today = getTodayDateString()
        val savedDate = prefs.getString(KEY_HOLIDAY_DATE, "")
        if (savedDate != today) {
            prefs.edit()
                .putString(KEY_HOLIDAY_DATE, today)
                .putInt(KEY_HOLIDAY_REELS_COUNT, 0)
                .putInt(KEY_HOLIDAY_MINUTES_COUNT, 0)
                .putInt(KEY_HOLIDAY_REELS_EXTENSION, 0)
                .putInt(KEY_HOLIDAY_MINUTES_EXTENSION, 0)
                .remove(KEY_HOLIDAY_SNOOZE_END_TIME)
                .commit()
        }
    }

    fun refreshHolidayState() {
        _holidayStateFlow.value = getHolidayStateInternal()
    }

    private fun getHolidayStateInternal(): HolidayState {
        checkAndResetDailyHolidayCounters()
        val master = isHolidayMasterEnabled()
        val active = isHolidayToday()
        val manual = prefs.getBoolean(KEY_HOLIDAY_MANUAL_ENABLED, false)
        val autoSched = isHolidayAutoScheduleEnabled()
        val days = getHolidayScheduledDays()
        val type = getHolidayLimitType()
        val baseLimit = getHolidayBaseLimitValue()
        val limit = baseLimit + getDailyHolidayExtension(type)
        val reels = prefs.getInt(KEY_HOLIDAY_REELS_COUNT, 0)
        val minutes = prefs.getInt(KEY_HOLIDAY_MINUTES_COUNT, 0)
        val snoozed = isHolidaySnoozed()
        val reached = if (type == LIMIT_TYPE_REELS) reels >= limit else minutes >= limit
        return HolidayState(
            isMasterEnabled = master,
            isHolidayActiveToday = active,
            isManualHolidayEnabled = manual,
            isAutoScheduleEnabled = autoSched,
            scheduledDays = days,
            limitType = type,
            baseLimitValue = baseLimit,
            limitValue = limit,
            reelsScrolledToday = reels,
            minutesUsedToday = minutes,
            isLimitReached = reached && active && !snoozed,
            isSnoozed = snoozed
        )
    }

    fun refreshState() {
        _sessionState.value = getCurrentState()
        refreshHolidayState()
        refreshTravelState()
        refreshUltraFocusState()
        refreshHomePinnedState()
        _blockedNotificationsFlow.value = getBlockedNotifications()
        _appDailyCapsFlow.value = getAppDailyCaps()
        _appTodayUsageFlow.value = getAppTodayUsageMap()
        _shieldDiagnosticsFlow.value = getShieldDiagnostics()
    }

    private fun getCurrentState(): SessionState {
        val active = isSessionActive()
        val remaining = getRemainingMillis()
        val duration = prefs.getInt(KEY_DURATION_MINUTES, 25)
        val strict = prefs.getBoolean(KEY_STRICT_MODE, true)
        val allowedCount = getAllowedPackages().size
        val isPassActive = isEmergencyPassActive()
        val passRemaining = getEmergencyPassRemainingMillis()
        val passesLeft = getRemainingEmergencyPasses()
        return SessionState(
            isActive = active,
            remainingMillis = remaining,
            totalDurationMinutes = duration,
            isStrictMode = strict,
            allowedAppsCount = allowedCount,
            isEmergencyPassActive = isPassActive,
            emergencyPassRemainingMillis = passRemaining,
            remainingPassesToday = passesLeft
        )
    }

    // -------------------------------------------------------------
    // TRAVEL MODE (Conscious Leisure Window for Flights & Transit)
    // -------------------------------------------------------------
    fun isTravelModeActive(): Boolean {
        val endTime = prefs.getLong(KEY_TRAVEL_END_TIME, 0L)
        val now = clock()
        if (now >= endTime) {
            if (endTime != 0L) {
                prefs.edit().remove(KEY_TRAVEL_END_TIME).commit()
            }
            return false
        }
        return true
    }

    fun getTravelRemainingMillis(): Long {
        val endTime = prefs.getLong(KEY_TRAVEL_END_TIME, 0L)
        val diff = endTime - clock()
        return if (diff > 0) diff else 0L
    }

    fun activateTravelMode(durationHours: Int = 2): Boolean {
        // Anti-bypass: Never allow activating Travel Mode during Strict Focus OR Ultra Focus!
        if ((isSessionActive() && isStrictMode()) || isUltraFocusActive()) {
            return false
        }
        return activateTravelWindowInternal(durationHours * 60 * 60 * 1000L, durationHours)
    }

    fun activateTravelModeMinutes(durationMinutes: Int = 30): Boolean {
        // Anti-bypass: Never allow activating Travel Mode during Strict Focus OR Ultra Focus!
        if ((isSessionActive() && isStrictMode()) || isUltraFocusActive()) {
            return false
        }
        return activateTravelWindowInternal(durationMinutes * 60 * 1000L, 0)
    }

    private fun activateTravelWindowInternal(durationMillis: Long, durationHoursMeta: Int): Boolean {
        val now = clock()
        val endTime = now + durationMillis
        prefs.edit()
            .putLong(KEY_TRAVEL_END_TIME, endTime)
            .putInt(KEY_TRAVEL_DURATION_HOURS, durationHoursMeta)
            .commit()
        refreshTravelState()
        refreshUltraFocusState()
        return true
    }

    fun endTravelMode() {
        prefs.edit().remove(KEY_TRAVEL_END_TIME).commit()
        refreshTravelState()
        refreshUltraFocusState()
    }

    private fun ensureTravelCountersToday() {
        val today = getTodayDateString()
        val savedDate = prefs.getString(KEY_TRAVEL_USAGE_DATE, "")
        if (savedDate != today) {
            prefs.edit()
                .putString(KEY_TRAVEL_USAGE_DATE, today)
                .putInt(KEY_TRAVEL_REELS_COUNT, 0)
                .putInt(KEY_TRAVEL_SCREEN_MINUTES, 0)
                .commit()
        }
    }

    fun incrementTravelReelsScrolled() {
        ensureTravelCountersToday()
        val current = prefs.getInt(KEY_TRAVEL_REELS_COUNT, 0)
        prefs.edit().putInt(KEY_TRAVEL_REELS_COUNT, current + 1).commit()
        recordReelsToDiary(1)
    }

    fun getTravelReelsScrolled(): Int {
        ensureTravelCountersToday()
        return prefs.getInt(KEY_TRAVEL_REELS_COUNT, 0)
    }

    fun addTravelScreenMinutesUsed(minutes: Int = 1) {
        if (minutes <= 0) return
        ensureTravelCountersToday()
        val current = prefs.getInt(KEY_TRAVEL_SCREEN_MINUTES, 0)
        prefs.edit().putInt(KEY_TRAVEL_SCREEN_MINUTES, current + minutes).commit()
    }

    fun getTravelScreenMinutesUsed(): Int {
        ensureTravelCountersToday()
        return prefs.getInt(KEY_TRAVEL_SCREEN_MINUTES, 0)
    }

    private fun ensureLongVideoCountersToday() {
        val today = getTodayDateString()
        val savedDate = prefs.getString(KEY_LONG_VIDEO_MINUTES_DATE, "")
        if (savedDate != today) {
            prefs.edit()
                .putString(KEY_LONG_VIDEO_MINUTES_DATE, today)
                .putInt(KEY_LONG_VIDEO_MINUTES_COUNT, 0)
                .commit()
        }
    }

    fun addLongVideoMinutesUsed(minutes: Int = 1) {
        if (minutes <= 0) return
        ensureLongVideoCountersToday()
        val current = prefs.getInt(KEY_LONG_VIDEO_MINUTES_COUNT, 0)
        prefs.edit().putInt(KEY_LONG_VIDEO_MINUTES_COUNT, current + minutes).commit()
    }

    fun getLongVideoMinutesToday(): Int {
        ensureLongVideoCountersToday()
        return prefs.getInt(KEY_LONG_VIDEO_MINUTES_COUNT, 0)
    }

    fun isTravelLifeReminderDue(): Boolean {
        if (!isTravelModeActive() || isUltraFocusActive() || isHolidaySnoozed()) return false
        return getTravelScreenMinutesUsed() >= 30 || getTravelReelsScrolled() >= 25
    }

    fun getActiveReelsOverlayCount(): Int {
        return when {
            isHolidayToday() -> getHolidayReelsScrolled()
            isTravelModeActive() -> getTravelReelsScrolled()
            else -> getTodayReelsWatched()
        }
    }

    fun refreshTravelState() {
        _travelStateFlow.value = getTravelStateInternal()
    }

    private fun getTravelStateInternal(): TravelState {
        val active = isTravelModeActive()
        val remaining = getTravelRemainingMillis()
        val hours = prefs.getInt(KEY_TRAVEL_DURATION_HOURS, 2)
        return TravelState(
            isTravelActive = active,
            remainingMillis = remaining,
            durationHours = hours
        )
    }

    // -------------------------------------------------------------
    // ULTRA FOCUS MODE (Cold Turkey Whitelist Lockdown)
    // -------------------------------------------------------------
    fun isUltraFocusActive(): Boolean {
        return prefs.getBoolean(KEY_ULTRA_FOCUS_ENABLED, false)
    }

    fun startUltraFocusNow(durationHours: Int = 24) {
        val now = clock()
        val committedHours = durationHours.coerceIn(1, 72)
        prefs.edit()
            .putBoolean(KEY_ULTRA_FOCUS_ENABLED, true)
            .putLong(KEY_ULTRA_FOCUS_START_TIME, now)
            .putInt(KEY_ULTRA_FOCUS_DURATION_HOURS, committedHours)
            .putLong(KEY_ULTRA_CREDIT_LAST_TIME, now)
            .commit()
        refreshUltraFocusState()
    }

    fun hasUsedEmergency30mToday(): Boolean {
        val today = getTodayDateString()
        val saved = prefs.getString(KEY_ULTRA_FOCUS_EMERGENCY_DATE, "")
        return saved == today
    }

    fun activateEmergency30mUsage(): Boolean {
        if (!isUltraFocusActive() ||
            hasUsedEmergency30mToday() ||
            isTravelModeActive() ||
            (isSessionActive() && isStrictMode())
        ) {
            return false
        }
        val started = activateTravelWindowInternal(30 * 60 * 1000L, 0)
        if (started) {
            val today = getTodayDateString()
            prefs.edit().putString(KEY_ULTRA_FOCUS_EMERGENCY_DATE, today).commit()
            refreshUltraFocusState()
        }
        return started
    }

    /**
     * Cold Turkey Transit Lifeline:
     * When user must travel unexpectedly during Cold Turkey, temporarily permits navigation,
     * ride-hailing, rail/air transit, weather & NFC/UPI payment tools WITHOUT unblocking social/video/browsers!
     */
    fun activateUltraFocusTransitMode(durationMinutes: Int = 120): Boolean {
        if (!isUltraFocusActive()) return false
        val endTime = clock() + (durationMinutes * 60 * 1000L)
        prefs.edit().putLong(KEY_ULTRA_TRANSIT_END_TIME, endTime).commit()
        refreshUltraFocusState()
        return true
    }

    fun endUltraFocusTransitMode() {
        prefs.edit().remove(KEY_ULTRA_TRANSIT_END_TIME).commit()
        refreshUltraFocusState()
    }

    fun isUltraFocusTransitActive(): Boolean {
        if (!isUltraFocusActive()) return false
        val endTime = prefs.getLong(KEY_ULTRA_TRANSIT_END_TIME, 0L)
        val now = clock()
        if (now >= endTime) {
            if (endTime != 0L) {
                prefs.edit().remove(KEY_ULTRA_TRANSIT_END_TIME).commit()
            }
            return false
        }
        return true
    }

    fun getUltraFocusTransitRemainingMillis(): Long {
        if (!isUltraFocusTransitActive()) return 0L
        val diff = prefs.getLong(KEY_ULTRA_TRANSIT_END_TIME, 0L) - clock()
        return if (diff > 0L) diff else 0L
    }

    fun getUltraFocusDurationHours(): Int {
        return prefs.getInt(KEY_ULTRA_FOCUS_DURATION_HOURS, 24)
    }

    fun getUltraFocus24hRemainingMillis(now: Long = clock()): Long {
        if (!isUltraFocusActive()) return 0L
        val startTime = prefs.getLong(KEY_ULTRA_FOCUS_START_TIME, 0L)
        if (startTime == 0L) return 0L
        val elapsed = now - startTime
        val lockDuration = getUltraFocusDurationHours() * 60L * 60L * 1000L
        val remaining = lockDuration - elapsed
        return if (remaining > 0L) remaining else 0L
    }

    fun canQuitUltraFocusNormally(): Boolean {
        if (!isUltraFocusActive()) return true
        return getUltraFocus24hRemainingMillis() == 0L
    }

    fun canQuitNormally(): Boolean = canQuitUltraFocusNormally()

    fun setUltraFocusEnabled(enabled: Boolean): Boolean {
        if (!enabled) {
            if (!canQuitNormally()) {
                return false
            }
            syncUltraFocusCredits()
            prefs.edit()
                .putBoolean(KEY_ULTRA_FOCUS_ENABLED, false)
                .putLong(KEY_ULTRA_FOCUS_START_TIME, 0L)
                .putLong(KEY_ULTRA_CREDIT_LAST_TIME, 0L)
                .remove(KEY_ULTRA_TRANSIT_END_TIME)
                .commit()
        } else {
            startUltraFocusNow()
        }
        refreshUltraFocusState()
        return true
    }

    private fun syncUltraFocusCredits() {
        if (!isUltraFocusActive()) return
        val now = clock()
        val last = prefs.getLong(KEY_ULTRA_CREDIT_LAST_TIME, 0L)
        val start = prefs.getLong(KEY_ULTRA_FOCUS_START_TIME, 0L)
        val base = if (last > 0L) last else start
        if (base <= 0L) return

        val elapsedMillis = (now - base).coerceAtLeast(0L)
        val elapsedMins = (elapsedMillis / 60_000L).toInt()
        if (elapsedMins > 0) {
            val remainderMillis = elapsedMillis % 60_000L
            prefs.edit().putLong(KEY_ULTRA_CREDIT_LAST_TIME, now - remainderMillis).commit()
            if (!isTravelModeActive()) {
                addFocusedMinutesToDiary(elapsedMins, isColdTurkey = true)
            }
        }
    }

    fun refreshUltraFocusState() {
        syncUltraFocusCredits()
        _ultraFocusStateFlow.value = getUltraFocusStateInternal()
    }

    private fun getUltraFocusStateInternal(): UltraFocusState {
        val enabled = isUltraFocusActive()
        val startTime = prefs.getLong(KEY_ULTRA_FOCUS_START_TIME, 0L)
        val durationHrs = getUltraFocusDurationHours()
        val canQuit = canQuitNormally()
        val remaining24h = getUltraFocus24hRemainingMillis()
        val isTravelActive = isTravelModeActive()
        val travelRemaining = getTravelRemainingMillis()
        val usedEmergency = hasUsedEmergency30mToday()
        val isTransitActive = isUltraFocusTransitActive()
        val transitRemaining = getUltraFocusTransitRemainingMillis()

        return UltraFocusState(
            isEnabled = enabled,
            startTime = startTime,
            durationHours = durationHrs,
            canQuitNormally = canQuit,
            remainingUntil24hMillis = remaining24h,
            isEmergency30mActive = isTravelActive && enabled,
            emergency30mRemainingMillis = if (enabled) travelRemaining else 0L,
            hasUsedEmergency30mToday = usedEmergency,
            isTransitPassActive = isTransitActive,
            transitPassRemainingMillis = transitRemaining
        )
    }

    // -------------------------------------------------------------
    // DAILY SCREEN BUDGET, COACH ADVISOR & FOCUS STREAK DIARY
    // -------------------------------------------------------------
    fun getDailyScreenGoalMinutes(): Int {
        return prefs.getInt(KEY_DAILY_SCREEN_GOAL_MINUTES, 60)
    }

    fun setDailyScreenGoalMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(15, 240)
        prefs.edit().putInt(KEY_DAILY_SCREEN_GOAL_MINUTES, clamped).commit()
        _dailyScreenGoalFlow.value = clamped
    }

    fun isReelsOverlayEnabled(): Boolean {
        return prefs.getBoolean(KEY_REELS_OVERLAY_ENABLED, true)
    }

    fun setReelsOverlayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REELS_OVERLAY_ENABLED, enabled).commit()
        _reelsOverlayEnabledFlow.value = enabled
    }

    fun isDailyGoalReminderEnabled(): Boolean {
        return prefs.getBoolean(KEY_DAILY_GOAL_REMINDER_ENABLED, true)
    }

    fun setDailyGoalReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DAILY_GOAL_REMINDER_ENABLED, enabled).commit()
        _dailyGoalReminderFlow.value = enabled
    }

    fun isHomePinnedCustomized(): Boolean {
        return prefs.getBoolean(KEY_HOME_PINNED_CUSTOMIZED, getHomePinnedPackages().isNotEmpty())
    }

    fun getHomePinnedPackages(): Set<String> {
        return prefs.getStringSet(KEY_HOME_PINNED_PACKAGES, emptySet())?.toSet() ?: emptySet()
    }

    /** Returns pinned launcher packages in the user's preferred display order. */
    fun getHomePinnedOrder(): List<String> {
        val pinnedSet = getHomePinnedPackages()
        if (pinnedSet.isEmpty()) return emptyList()
        val rawOrder = prefs.getString(KEY_HOME_PINNED_ORDER, "").orEmpty()
        val ordered = if (rawOrder.isBlank()) {
            emptyList()
        } else {
            rawOrder.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && pinnedSet.contains(it) }
                .distinct()
                .toList()
        }
        val missing = pinnedSet.filterNot { ordered.contains(it) }.sorted()
        return ordered + missing
    }

    fun setHomePinnedPackages(orderedPackages: List<String>) {
        val cleaned = orderedPackages.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val pkgSet = cleaned.toSet()
        prefs.edit()
            .putStringSet(KEY_HOME_PINNED_PACKAGES, HashSet(pkgSet))
            .putString(KEY_HOME_PINNED_ORDER, cleaned.joinToString("\n"))
            .putBoolean(KEY_HOME_PINNED_CUSTOMIZED, true)
            .commit()
        _homePinnedPackagesFlow.value = pkgSet
        _homePinnedOrderFlow.value = cleaned
        _homePinnedCustomizedFlow.value = true
    }

    fun toggleHomePinnedPackage(
        packageName: String,
        initialFallbackPackages: List<String> = emptyList()
    ): Boolean {
        val base = if (!isHomePinnedCustomized() && initialFallbackPackages.isNotEmpty()) {
            initialFallbackPackages.toMutableList()
        } else {
            getHomePinnedOrder().toMutableList()
        }
        val nowPinned = if (base.contains(packageName)) {
            base.removeAll { it == packageName }
            false
        } else {
            base.add(packageName)
            true
        }
        setHomePinnedPackages(base)
        return nowPinned
    }

    fun resetHomePinnedToAuto() {
        prefs.edit()
            .remove(KEY_HOME_PINNED_PACKAGES)
            .remove(KEY_HOME_PINNED_ORDER)
            .putBoolean(KEY_HOME_PINNED_CUSTOMIZED, false)
            .commit()
        refreshHomePinnedState()
    }

    private fun refreshHomePinnedState() {
        _homePinnedPackagesFlow.value = getHomePinnedPackages()
        _homePinnedOrderFlow.value = getHomePinnedOrder()
        _homePinnedCustomizedFlow.value = isHomePinnedCustomized()
    }

    fun addScreenMinutesUsed(minutes: Int = 1, isDistracting: Boolean = false) {
        if (minutes <= 0) return
        val today = getTodayDateString()
        updateDiaryEntry(today) { existing ->
            existing.copy(
                screenMinutes = existing.screenMinutes + minutes,
                distractingMinutes = existing.distractingMinutes + if (isDistracting) minutes else 0
            )
        }
    }

    fun addFocusedMinutesToDiary(minutes: Int, isColdTurkey: Boolean = false) {
        if (minutes <= 0) return
        val today = getTodayDateString()
        updateDiaryEntry(today) { existing ->
            existing.copy(
                focusedMinutes = existing.focusedMinutes + minutes,
                coldTurkeyMinutes = existing.coldTurkeyMinutes + if (isColdTurkey) minutes else 0
            )
        }
    }

    private fun recordReelsToDiary(count: Int) {
        if (count <= 0) return
        val today = getTodayDateString()
        updateDiaryEntry(today) { existing ->
            existing.copy(reelsWatched = existing.reelsWatched + count)
        }
    }

    fun recordEverydayReelScrolled() {
        recordReelsToDiary(1)
    }

    fun saveDiaryNote(date: String = getTodayDateString(), note: String) {
        updateDiaryEntry(date) { existing ->
            existing.copy(note = note.trim())
        }
    }

    fun getTodayDiaryEntry(): DailyDiaryEntry {
        val today = getTodayDateString()
        return loadDiaryEntriesInternal().firstOrNull { it.date == today }
            ?: DailyDiaryEntry(date = today)
    }

    fun getTodayReelsWatched(): Int = getTodayDiaryEntry().reelsWatched

    fun isDailyScreenGoalExceeded(): Boolean {
        return getTodayDiaryEntry().screenMinutes >= getDailyScreenGoalMinutes()
    }

    fun isDailyGoalCheckInDue(): Boolean {
        if (!isDailyGoalReminderEnabled()) return false
        if (shouldBlockApps() || isHolidayToday() || isTravelModeActive() || isHolidaySnoozed()) return false
        return isDailyScreenGoalExceeded()
    }

    fun getDiaryEntries(): List<DailyDiaryEntry> {
        syncSessionFocusCredit()
        syncUltraFocusCredits()
        return loadDiaryEntriesInternal()
    }

    fun getCurrentStreakDays(): Int {
        val map = loadDiaryEntriesInternal().associateBy { it.date }
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = clock() }
        val todayStr = sdf.format(cal.time)
        val todayEntry = map[todayStr]
        val qualifyingToday = (todayEntry?.focusedMinutes ?: 0) >= 15 || isUltraFocusActive() || isSessionActive()

        var streak = 0
        if (!qualifyingToday) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        for (i in 0 until 365) {
            val dateStr = sdf.format(cal.time)
            val entry = map[dateStr]
            val ok = (entry?.focusedMinutes ?: 0) >= 15 || (dateStr == todayStr && qualifyingToday)
            if (ok) {
                streak++
                cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    fun getTotalFocusedMinutes(): Int {
        return loadDiaryEntriesInternal().sumOf { it.focusedMinutes }
    }

    fun getDailyCoachAdvice(): CoachAdvice {
        val today = getTodayDiaryEntry()
        val goal = getDailyScreenGoalMinutes()
        val used = today.screenMinutes
        val focused = today.focusedMinutes
        val streak = getCurrentStreakDays()

        val remainingBudget = (goal - used).coerceAtLeast(0)
        return when {
            isUltraFocusActive() -> CoachAdvice(
                headline = "Cold Turkey locked in",
                detail = "Stay execution-first. ${focused}m focused today · ${streak}d streak.",
                level = AdviceLevel.GOOD,
                screenUsedMinutes = used,
                screenGoalMinutes = goal,
                focusedMinutes = focused,
                streakDays = streak
            )
            used >= goal -> CoachAdvice(
                headline = "Screen budget exceeded (${used}m / ${goal}m)",
                detail = "Keep daily phone time under ${goal}m. Lock in a Focus block or put the phone down.",
                level = AdviceLevel.LOCK_RECOMMENDED,
                screenUsedMinutes = used,
                screenGoalMinutes = goal,
                focusedMinutes = focused,
                streakDays = streak
            )
            used >= (goal * 0.7f).toInt() -> CoachAdvice(
                headline = "${remainingBudget}m left under your ${goal}m cap",
                detail = "Stay below 1 hr on the screen and finish your top priority work.",
                level = AdviceLevel.CAUTION,
                screenUsedMinutes = used,
                screenGoalMinutes = goal,
                focusedMinutes = focused,
                streakDays = streak
            )
            focused == 0 -> CoachAdvice(
                headline = "Keep phone time under ${goal}m today",
                detail = "No deep work logged yet. Start a 25–45m Focus session before scrolling.",
                level = AdviceLevel.CAUTION,
                screenUsedMinutes = used,
                screenGoalMinutes = goal,
                focusedMinutes = focused,
                streakDays = streak
            )
            else -> CoachAdvice(
                headline = "On track · ${remainingBudget}m budget left",
                detail = "${focused}m focused today with only ${used}m screen use. Keep executing.",
                level = AdviceLevel.GOOD,
                screenUsedMinutes = used,
                screenGoalMinutes = goal,
                focusedMinutes = focused,
                streakDays = streak
            )
        }
    }

    private fun updateDiaryEntry(date: String, transform: (DailyDiaryEntry) -> DailyDiaryEntry) {
        val entries = loadDiaryEntriesInternal().associateBy { it.date }.toMutableMap()
        val current = entries[date] ?: DailyDiaryEntry(date = date)
        entries[date] = transform(current)
        val sorted = entries.values.sortedByDescending { it.date }.take(60)
        saveDiaryEntriesInternal(sorted)
    }

    private fun saveDiaryEntriesInternal(entries: List<DailyDiaryEntry>) {
        val encoded = entries.joinToString("\n") { e ->
            listOf(
                e.date,
                e.focusedMinutes.toString(),
                e.coldTurkeyMinutes.toString(),
                e.screenMinutes.toString(),
                e.distractingMinutes.toString(),
                e.reelsWatched.toString(),
                escapeField(e.note)
            ).joinToString("|")
        }
        prefs.edit().putString(KEY_DAILY_LOGS_DATA, encoded).commit()
        _diaryEntriesFlow.value = entries
        notifyWidgets()
    }

    private fun notifyWidgets() {
        val ctx = context ?: return
        try {
            com.getbacktowork.app.ui.widget.FocusWidgetProvider.refreshAllWidgets(ctx)
        } catch (_: Throwable) {
        }
    }

    private fun loadDiaryEntriesInternal(): List<DailyDiaryEntry> {
        val raw = prefs.getString(KEY_DAILY_LOGS_DATA, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size < 6) return@mapNotNull null
                DailyDiaryEntry(
                    date = parts[0],
                    focusedMinutes = parts[1].toIntOrNull() ?: 0,
                    coldTurkeyMinutes = parts[2].toIntOrNull() ?: 0,
                    screenMinutes = parts[3].toIntOrNull() ?: 0,
                    distractingMinutes = parts[4].toIntOrNull() ?: 0,
                    reelsWatched = parts[5].toIntOrNull() ?: 0,
                    note = if (parts.size > 6) unescapeField(parts[6]) else ""
                )
            }
            .sortedByDescending { it.date }
            .toList()
    }

    private fun escapeField(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("|", "\\p")
            .replace("\n", "\\n")
            .replace("\r", "")
    }

    private fun unescapeField(value: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < value.length) {
            val ch = value[i]
            if (ch == '\\' && i + 1 < value.length) {
                when (value[i + 1]) {
                    '\\' -> { sb.append('\\'); i += 2 }
                    'p' -> { sb.append('|'); i += 2 }
                    'n' -> { sb.append('\n'); i += 2 }
                    else -> { sb.append(ch); i++ }
                }
            } else {
                sb.append(ch)
                i++
            }
        }
        return sb.toString()
    }

    // -------------------------------------------------------------
    // PHASE 2-4 FEATURES: GOALS, SCHEDULES, GRANULAR FEEDS, COOLDOWN, ALIASES, WIDGET
    // -------------------------------------------------------------

    fun getYearProgressInfo(): YearProgressInfo {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = clock() }
        val year = cal.get(java.util.Calendar.YEAR)
        val dayOfYear = cal.get(java.util.Calendar.DAY_OF_YEAR)
        val totalDays = cal.getActualMaximum(java.util.Calendar.DAY_OF_YEAR).coerceAtLeast(365)
        val remaining = (totalDays - dayOfYear).coerceAtLeast(0)
        val pct = ((dayOfYear.toFloat() / totalDays.toFloat()) * 100f).toInt().coerceIn(0, 100)
        return YearProgressInfo(
            year = year,
            dayOfYear = dayOfYear,
            totalDays = totalDays,
            daysRemaining = remaining,
            elapsedPercent = pct
        )
    }

    fun getLifeGoals(): List<String> {
        val raw = prefs.getString(KEY_LIFE_GOALS, "").orEmpty()
        if (raw.isBlank()) {
            return listOf(
                "Protect deep work before touching social feeds",
                "Keep phone screen use under 1 hour daily",
                "Invest spare hours into health & real-world goals"
            )
        }
        return raw.lineSequence()
            .filter { it.isNotBlank() }
            .map { unescapeField(it) }
            .take(3)
            .toList()
    }

    fun setLifeGoals(goals: List<String>) {
        val cleaned = goals.map { it.trim() }.filter { it.isNotEmpty() }.take(3)
        val encoded = cleaned.joinToString("\n") { escapeField(it) }
        prefs.edit().putString(KEY_LIFE_GOALS, encoded).commit()
        _lifeGoalsFlow.value = getLifeGoals()
    }

    fun getScheduleConfig(): FocusScheduleConfig = getScheduleConfigInternal()

    fun refreshScheduleState() {
        _scheduleConfigFlow.value = getScheduleConfigInternal()
    }

    fun setWorkSchedule(
        enabled: Boolean,
        startMinute: Int = _scheduleConfigFlow.value.workStartMinute,
        endMinute: Int = _scheduleConfigFlow.value.workEndMinute,
        days: Set<Int> = _scheduleConfigFlow.value.workDays
    ) {
        val dayStr = days.joinToString(",")
        prefs.edit()
            .putBoolean(KEY_WORK_SCHED_ENABLED, enabled)
            .putInt(KEY_WORK_SCHED_START, startMinute.coerceIn(0, 1439))
            .putInt(KEY_WORK_SCHED_END, endMinute.coerceIn(0, 1439))
            .putString(KEY_WORK_SCHED_DAYS, dayStr)
            .commit()
        refreshScheduleState()
    }

    fun setSleepSchedule(
        enabled: Boolean,
        startMinute: Int = _scheduleConfigFlow.value.sleepStartMinute,
        endMinute: Int = _scheduleConfigFlow.value.sleepEndMinute
    ) {
        prefs.edit()
            .putBoolean(KEY_SLEEP_SCHED_ENABLED, enabled)
            .putInt(KEY_SLEEP_SCHED_START, startMinute.coerceIn(0, 1439))
            .putInt(KEY_SLEEP_SCHED_END, endMinute.coerceIn(0, 1439))
            .commit()
        refreshScheduleState()
    }

    fun isScheduledFocusActiveNow(): Boolean {
        val cfg = getScheduleConfigInternal()
        return cfg.isWorkActiveNow || cfg.isSleepActiveNow
    }

    private fun getScheduleConfigInternal(): FocusScheduleConfig {
        val workEnabled = prefs.getBoolean(KEY_WORK_SCHED_ENABLED, false)
        val workStart = prefs.getInt(KEY_WORK_SCHED_START, 9 * 60)
        val workEnd = prefs.getInt(KEY_WORK_SCHED_END, 17 * 60)
        val defaultWorkDays = setOf(
            java.util.Calendar.MONDAY,
            java.util.Calendar.TUESDAY,
            java.util.Calendar.WEDNESDAY,
            java.util.Calendar.THURSDAY,
            java.util.Calendar.FRIDAY
        )
        val rawDays = prefs.getString(KEY_WORK_SCHED_DAYS, null)
        val workDays = if (rawDays == null) {
            defaultWorkDays
        } else {
            rawDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        }
        val sleepEnabled = prefs.getBoolean(KEY_SLEEP_SCHED_ENABLED, false)
        val sleepStart = prefs.getInt(KEY_SLEEP_SCHED_START, 23 * 60)
        val sleepEnd = prefs.getInt(KEY_SLEEP_SCHED_END, 7 * 60)

        val cal = java.util.Calendar.getInstance().apply { timeInMillis = clock() }
        val nowMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
        val prevDay = (cal.clone() as java.util.Calendar).apply {
            add(java.util.Calendar.DAY_OF_YEAR, -1)
        }.get(java.util.Calendar.DAY_OF_WEEK)

        val isWorkNow = workEnabled && isTimeInScheduleWindow(nowMin, workStart, workEnd, dayOfWeek, prevDay, workDays)
        val allDays = setOf(
            java.util.Calendar.SUNDAY, java.util.Calendar.MONDAY, java.util.Calendar.TUESDAY,
            java.util.Calendar.WEDNESDAY, java.util.Calendar.THURSDAY, java.util.Calendar.FRIDAY,
            java.util.Calendar.SATURDAY
        )
        val isSleepNow = sleepEnabled && isTimeInScheduleWindow(nowMin, sleepStart, sleepEnd, dayOfWeek, prevDay, allDays)

        return FocusScheduleConfig(
            workEnabled = workEnabled,
            workStartMinute = workStart,
            workEndMinute = workEnd,
            workDays = workDays,
            sleepEnabled = sleepEnabled,
            sleepStartMinute = sleepStart,
            sleepEndMinute = sleepEnd,
            isWorkActiveNow = isWorkNow,
            isSleepActiveNow = isSleepNow
        )
    }

    private fun isTimeInScheduleWindow(
        nowMin: Int,
        startMin: Int,
        endMin: Int,
        dayOfWeek: Int,
        prevDayOfWeek: Int,
        activeDays: Set<Int>
    ): Boolean {
        if (startMin == endMin) return false
        return if (startMin < endMin) {
            activeDays.contains(dayOfWeek) && nowMin in startMin until endMin
        } else {
            // Wraps past midnight (e.g. 23:00 -> 07:00)
            (nowMin >= startMin && activeDays.contains(dayOfWeek)) ||
                (nowMin < endMin && activeDays.contains(prevDayOfWeek))
        }
    }

    fun isBlockShortsInFocus(): Boolean = prefs.getBoolean(KEY_BLOCK_SHORTS_IN_FOCUS, true)
    fun setBlockShortsInFocus(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_SHORTS_IN_FOCUS, enabled).commit()
        _blockShortsInFocusFlow.value = enabled
    }

    fun isBlockYtHomeFeed(): Boolean = prefs.getBoolean(KEY_BLOCK_YT_HOME, false)
    fun setBlockYtHomeFeed(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_YT_HOME, enabled).commit()
        _blockYtHomeFeedFlow.value = enabled
    }

    fun isBlockIgExplore(): Boolean = prefs.getBoolean(KEY_BLOCK_IG_EXPLORE, true)
    fun setBlockIgExplore(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_IG_EXPLORE, enabled).commit()
        _blockIgExploreFlow.value = enabled
    }

    fun isBlockSocialAlgoFeeds(): Boolean = prefs.getBoolean(KEY_BLOCK_SOCIAL_ALGO, true)
    fun setBlockSocialAlgoFeeds(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_SOCIAL_ALGO, enabled).commit()
        _blockSocialFeedsFlow.value = enabled
    }

    fun getBlockedDomains(): Set<String> {
        return prefs.getStringSet(KEY_BLOCKED_DOMAINS, null)?.toSet()
            ?: setOf("reddit.com", "x.com", "instagram.com", "tiktok.com")
    }

    fun addBlockedDomain(rawInput: String): Boolean {
        val normalized = normalizeDomain(rawInput)
        if (normalized.length < 3 || !normalized.contains(".")) return false
        val updated = getBlockedDomains().toMutableSet().apply { add(normalized) }
        prefs.edit().putStringSet(KEY_BLOCKED_DOMAINS, HashSet(updated)).commit()
        _blockedDomainsFlow.value = updated
        return true
    }

    fun removeBlockedDomain(domain: String) {
        val updated = getBlockedDomains().toMutableSet().apply { remove(domain) }
        prefs.edit().putStringSet(KEY_BLOCKED_DOMAINS, HashSet(updated)).commit()
        _blockedDomainsFlow.value = updated
    }

    fun isAlwaysBlockDomainsEnabled(): Boolean = prefs.getBoolean(KEY_ALWAYS_BLOCK_DOMAINS, false)
    fun setAlwaysBlockDomainsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ALWAYS_BLOCK_DOMAINS, enabled).commit()
        _alwaysBlockDomainsFlow.value = enabled
    }

    private fun normalizeDomain(raw: String): String {
        return raw.trim()
            .lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore("/")
            .substringBefore("?")
            .trim()
    }

    fun getCooldownSeconds(): Int = prefs.getInt(KEY_COOLDOWN_SECONDS, 0)

    fun setCooldownSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(0, 60)
        prefs.edit().putInt(KEY_COOLDOWN_SECONDS, clamped).commit()
        _cooldownSecondsFlow.value = clamped
        if (clamped > 0 && getCooldownPackages().isEmpty()) {
            val defaultCooldownPkgs = setOf(
                "com.google.android.youtube",
                "com.instagram.android",
                "com.zhiliaoapp.musically",
                "com.ss.android.ugc.trill",
                "com.facebook.katana",
                "com.snapchat.android",
                "com.reddit.frontpage",
                "com.twitter.android",
                "com.android.chrome",
                "com.sec.android.app.sbrowser"
            )
            prefs.edit().putStringSet(KEY_COOLDOWN_PACKAGES, HashSet(defaultCooldownPkgs)).commit()
            _cooldownPackagesFlow.value = defaultCooldownPkgs
        }
    }

    fun getCooldownPackages(): Set<String> {
        return prefs.getStringSet(KEY_COOLDOWN_PACKAGES, emptySet())?.toSet() ?: emptySet()
    }

    fun toggleCooldownPackage(packageName: String): Boolean {
        val updated = getCooldownPackages().toMutableSet()
        val added = if (updated.contains(packageName)) {
            updated.remove(packageName)
            false
        } else {
            updated.add(packageName)
            true
        }
        prefs.edit().putStringSet(KEY_COOLDOWN_PACKAGES, HashSet(updated)).commit()
        _cooldownPackagesFlow.value = updated
        return added
    }

    fun grantCooldownWindow(packageName: String, minutes: Int = 10) {
        val now = clock()
        val expiry = now + (minutes.coerceAtLeast(1) * 60_000L)
        val map = loadCooldownPassesMap(now).toMutableMap()
        map[packageName] = expiry
        saveCooldownPassesMap(map)
    }

    fun isCooldownRequiredForPackage(packageName: String): Boolean {
        if (shouldBlockApps()) return false
        if (isTravelModeActive() || isEarnedScrollActive()) return false
        if (getCooldownSeconds() <= 0) return false
        if (!getCooldownPackages().contains(packageName)) return false
        val passes = loadCooldownPassesMap(clock())
        val validUntil = passes[packageName] ?: 0L
        return clock() >= validUntil
    }

    private fun loadCooldownPassesMap(now: Long): Map<String, Long> {
        val raw = prefs.getString(KEY_COOLDOWN_PASSES, "").orEmpty()
        if (raw.isBlank()) return emptyMap()
        val map = mutableMapOf<String, Long>()
        raw.lineSequence().filter { it.isNotBlank() }.forEach { line ->
            val parts = line.split("|")
            if (parts.size == 2) {
                val ts = parts[1].toLongOrNull() ?: 0L
                if (ts > now) map[parts[0]] = ts
            }
        }
        return map
    }

    private fun saveCooldownPassesMap(map: Map<String, Long>) {
        val encoded = map.entries.joinToString("\n") { "${it.key}|${it.value}" }
        prefs.edit().putString(KEY_COOLDOWN_PASSES, encoded).commit()
    }

    fun getCustomAppNames(): Map<String, String> {
        val raw = prefs.getString(KEY_CUSTOM_APP_NAMES, "").orEmpty()
        if (raw.isBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        raw.lineSequence().filter { it.isNotBlank() }.forEach { line ->
            val idx = line.indexOf('|')
            if (idx > 0 && idx < line.length - 1) {
                val pkg = line.substring(0, idx)
                val alias = unescapeField(line.substring(idx + 1))
                if (alias.isNotBlank()) map[pkg] = alias
            }
        }
        return map
    }

    fun setCustomAppName(packageName: String, customAlias: String) {
        val map = getCustomAppNames().toMutableMap()
        val clean = customAlias.trim()
        if (clean.isEmpty()) {
            map.remove(packageName)
        } else {
            map[packageName] = clean.take(28)
        }
        val encoded = map.entries.joinToString("\n") { "${it.key}|${escapeField(it.value)}" }
        prefs.edit().putString(KEY_CUSTOM_APP_NAMES, encoded).commit()
        _customAppNamesFlow.value = map
    }

    fun resolveAppName(packageName: String, originalName: String): String {
        return getCustomAppNames()[packageName]?.takeIf { it.isNotBlank() } ?: originalName
    }

    fun getEarnedScrollEndTime(): Long = prefs.getLong(KEY_EARNED_SCROLL_END_TIME, 0L)

    fun grantEarnedScrollMinutes(minutes: Int = 10) {
        val end = clock() + (minutes.coerceAtLeast(1) * 60_000L)
        prefs.edit().putLong(KEY_EARNED_SCROLL_END_TIME, end).commit()
        _earnedScrollUntilFlow.value = end
    }

    fun isEarnedScrollActive(): Boolean {
        val end = prefs.getLong(KEY_EARNED_SCROLL_END_TIME, 0L)
        if (end == 0L) return false
        if (clock() >= end) {
            prefs.edit().remove(KEY_EARNED_SCROLL_END_TIME).commit()
            _earnedScrollUntilFlow.value = 0L
            return false
        }
        return true
    }

    fun getEarnedScrollRemainingMillis(): Long {
        if (!isEarnedScrollActive()) return 0L
        return (prefs.getLong(KEY_EARNED_SCROLL_END_TIME, 0L) - clock()).coerceAtLeast(0L)
    }

    fun hasCompletedOnboarding(): Boolean = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun setOnboardingCompleted(completed: Boolean = true) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).commit()
    }

    fun shouldShowOnboarding(): Boolean {
        if (hasCompletedOnboarding()) return false
        if (isSessionActive() || isUltraFocusActive()) return false
        // If existing user already wrote diary entries or customized whitelist beyond default, treat silently as onboarded
        if (loadDiaryEntriesInternal().isNotEmpty()) {
            setOnboardingCompleted(true)
            return false
        }
        return true
    }

    fun buildDiaryExportText(): String {
        val sb = StringBuilder()
        val entries = getDiaryEntries()
        val yearInfo = getYearProgressInfo()
        sb.appendLine("=== GetBackToWork Focus Diary Export ===")
        sb.appendLine("Current Streak: ${getCurrentStreakDays()} days")
        sb.appendLine("Total Focused Time: ${getTotalFocusedMinutes()} mins")
        sb.appendLine("Daily Screen Cap: ${getDailyScreenGoalMinutes()} mins")
        sb.appendLine("Year Progress: ${yearInfo.daysRemaining} days left in ${yearInfo.year} (${yearInfo.elapsedPercent}% elapsed)")
        sb.appendLine()
        sb.appendLine("Top Personal Goals:")
        getLifeGoals().forEachIndexed { idx, goal ->
            sb.appendLine("${idx + 1}. $goal")
        }
        sb.appendLine()
        sb.appendLine("Daily Logs:")
        if (entries.isEmpty()) {
            sb.appendLine("(No logged entries yet)")
        } else {
            entries.forEach { e ->
                sb.append("- ${e.date}: Focus=${e.focusedMinutes}m (ColdTurkey=${e.coldTurkeyMinutes}m), Screen=${e.screenMinutes}m, Reels=${e.reelsWatched}")
                if (e.note.isNotBlank()) sb.append(" | Note: ${e.note}")
                sb.appendLine()
            }
        }
        return sb.toString().trim()
    }

    // -------------------------------------------------------------
    // FOCUS NOTIFICATION DIGEST (Missed Alerts Held While Blocking)
    // -------------------------------------------------------------

    fun recordBlockedNotification(
        packageName: String,
        appName: String,
        title: String,
        preview: String
    ) {
        val cleanPkg = packageName.trim()
        if (cleanPkg.isEmpty()) return
        val cleanTitle = title.trim().take(80)
        val cleanPreview = preview.trim().take(140)
        if (cleanTitle.isEmpty() && cleanPreview.isEmpty()) return

        val now = clock()
        val existing = getBlockedNotifications().toMutableList()
        val isRecentDuplicate = existing.any {
            it.packageName == cleanPkg &&
                it.title == cleanTitle &&
                it.preview == cleanPreview &&
                (now - it.timestamp) < 15_000L
        }
        if (isRecentDuplicate) return

        val item = BlockedNotificationItem(
            id = "${now}_${cleanPkg}_${existing.size}",
            packageName = cleanPkg,
            appName = appName.trim().ifEmpty { cleanPkg }.take(40),
            title = cleanTitle.ifEmpty { appName.trim().ifEmpty { cleanPkg } },
            preview = cleanPreview,
            timestamp = now
        )
        existing.add(0, item)
        saveBlockedNotificationsInternal(existing.take(60))
    }

    fun getBlockedNotifications(): List<BlockedNotificationItem> {
        val raw = prefs.getString(KEY_BLOCKED_NOTIFICATIONS, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size < 6) return@mapNotNull null
                BlockedNotificationItem(
                    id = unescapeField(parts[0]),
                    packageName = unescapeField(parts[1]),
                    appName = unescapeField(parts[2]),
                    title = unescapeField(parts[3]),
                    preview = unescapeField(parts[4]),
                    timestamp = parts[5].toLongOrNull() ?: 0L
                )
            }
            .sortedByDescending { it.timestamp }
            .toList()
    }

    fun dismissBlockedNotification(id: String) {
        val remaining = getBlockedNotifications().filterNot { it.id == id }
        saveBlockedNotificationsInternal(remaining)
    }

    fun clearBlockedNotifications() {
        prefs.edit().remove(KEY_BLOCKED_NOTIFICATIONS).commit()
        _blockedNotificationsFlow.value = emptyList()
    }

    private fun saveBlockedNotificationsInternal(items: List<BlockedNotificationItem>) {
        val encoded = items.joinToString("\n") { item ->
            listOf(
                escapeField(item.id),
                escapeField(item.packageName),
                escapeField(item.appName),
                escapeField(item.title),
                escapeField(item.preview),
                item.timestamp.toString()
            ).joinToString("|")
        }
        prefs.edit().putString(KEY_BLOCKED_NOTIFICATIONS, encoded).commit()
        _blockedNotificationsFlow.value = items
    }

    // -------------------------------------------------------------
    // PER-APP DAILY TIME & OPEN CAPS (Everyday "No Mode" Limits)
    // -------------------------------------------------------------

    fun setAppDailyCap(
        packageName: String,
        maxMinutesPerDay: Int,
        maxOpensPerDay: Int
    ) {
        val cleanPkg = packageName.trim()
        if (cleanPkg.isEmpty()) return
        val clampedMins = maxMinutesPerDay.coerceIn(0, 360)
        val clampedOpens = maxOpensPerDay.coerceIn(0, 100)
        val map = getAppDailyCaps().toMutableMap()
        if (clampedMins == 0 && clampedOpens == 0) {
            map.remove(cleanPkg)
        } else {
            map[cleanPkg] = AppDailyCap(cleanPkg, clampedMins, clampedOpens)
        }
        val encoded = map.values.joinToString("\n") { "${escapeField(it.packageName)}|${it.maxMinutesPerDay}|${it.maxOpensPerDay}" }
        prefs.edit().putString(KEY_APP_DAILY_CAPS, encoded).commit()
        _appDailyCapsFlow.value = map
    }

    fun getAppDailyCaps(): Map<String, AppDailyCap> {
        val raw = prefs.getString(KEY_APP_DAILY_CAPS, "").orEmpty()
        if (raw.isBlank()) return emptyMap()
        val map = mutableMapOf<String, AppDailyCap>()
        raw.lineSequence().filter { it.isNotBlank() }.forEach { line ->
            val parts = line.split("|")
            if (parts.size >= 3) {
                val pkg = unescapeField(parts[0])
                val mins = parts[1].toIntOrNull()?.coerceIn(0, 360) ?: 0
                val opens = parts[2].toIntOrNull()?.coerceIn(0, 100) ?: 0
                if (pkg.isNotEmpty() && (mins > 0 || opens > 0)) {
                    map[pkg] = AppDailyCap(pkg, mins, opens)
                }
            }
        }
        return map
    }

    fun getAppDailyCap(packageName: String): AppDailyCap? = getAppDailyCaps()[packageName]

    fun getAppTodayUsageMap(): Map<String, AppTodayUsage> {
        val today = getTodayDateString()
        val storedDate = prefs.getString(KEY_APP_USAGE_DATE, "")
        if (storedDate != today) {
            prefs.edit()
                .putString(KEY_APP_USAGE_DATE, today)
                .remove(KEY_APP_TODAY_USAGE)
                .commit()
            return emptyMap()
        }
        val raw = prefs.getString(KEY_APP_TODAY_USAGE, "").orEmpty()
        if (raw.isBlank()) return emptyMap()
        val map = mutableMapOf<String, AppTodayUsage>()
        raw.lineSequence().filter { it.isNotBlank() }.forEach { line ->
            val parts = line.split("|")
            if (parts.size >= 3) {
                val pkg = unescapeField(parts[0])
                val opens = parts[1].toIntOrNull()?.coerceAtLeast(0) ?: 0
                val mins = parts[2].toIntOrNull()?.coerceAtLeast(0) ?: 0
                if (pkg.isNotEmpty()) {
                    map[pkg] = AppTodayUsage(pkg, today, opens, mins)
                }
            }
        }
        return map
    }

    fun getAppTodayUsage(packageName: String): AppTodayUsage {
        val today = getTodayDateString()
        return getAppTodayUsageMap()[packageName] ?: AppTodayUsage(packageName, today, 0, 0)
    }

    fun recordAppOpen(packageName: String): Int {
        val cleanPkg = packageName.trim()
        if (cleanPkg.isEmpty()) return 0
        val today = getTodayDateString()
        val map = getAppTodayUsageMap().toMutableMap()
        val current = map[cleanPkg] ?: AppTodayUsage(cleanPkg, today, 0, 0)
        val updated = current.copy(opens = current.opens + 1)
        map[cleanPkg] = updated
        saveAppTodayUsageInternal(today, map)
        return updated.opens
    }

    fun addAppMinutesUsed(packageName: String, minutes: Int = 1): Int {
        val cleanPkg = packageName.trim()
        if (cleanPkg.isEmpty() || minutes <= 0) return getAppTodayUsage(cleanPkg).minutes
        val today = getTodayDateString()
        val map = getAppTodayUsageMap().toMutableMap()
        val current = map[cleanPkg] ?: AppTodayUsage(cleanPkg, today, 0, 0)
        val updated = current.copy(minutes = current.minutes + minutes)
        map[cleanPkg] = updated
        saveAppTodayUsageInternal(today, map)
        return updated.minutes
    }

    private fun saveAppTodayUsageInternal(today: String, map: Map<String, AppTodayUsage>) {
        val encoded = map.values.joinToString("\n") { "${escapeField(it.packageName)}|${it.opens}|${it.minutes}" }
        prefs.edit()
            .putString(KEY_APP_USAGE_DATE, today)
            .putString(KEY_APP_TODAY_USAGE, encoded)
            .commit()
        _appTodayUsageFlow.value = map
    }

    /**
     * Checks whether a specific app has exceeded its everyday daily cap.
     * Minute limits enforce both on entry and mid-use; open limits enforce on new entries
     * so users aren't kicked out immediately upon triggering their final permitted open.
     */
    fun getAppDailyCapBreachReason(packageName: String, isNewEntry: Boolean = true): String? {
        if (shouldBlockApps() || isTravelModeActive() || isEarnedScrollActive()) return null
        if (isAppAllowed(packageName) || isTransitEssentialApp(packageName)) return null
        val cap = getAppDailyCap(packageName) ?: return null
        val usage = getAppTodayUsage(packageName)
        if (cap.maxMinutesPerDay > 0 && usage.minutes >= cap.maxMinutesPerDay) {
            return "Daily time limit reached (${usage.minutes}m / ${cap.maxMinutesPerDay}m)"
        }
        if (isNewEntry && cap.maxOpensPerDay > 0 && usage.opens >= cap.maxOpensPerDay) {
            return "Daily open limit reached (${usage.opens} / ${cap.maxOpensPerDay} opens)"
        }
        return null
    }

    // -------------------------------------------------------------
    // ON-DEVICE SHIELD DIAGNOSTICS (Beta & OEM Debugging Log)
    // -------------------------------------------------------------

    fun recordShieldDiagnostic(category: String, packageName: String, detail: String) {
        val cleanCat = category.trim().take(32)
        val cleanPkg = packageName.trim().take(80)
        val cleanDetail = detail.trim().take(120)
        if (cleanCat.isEmpty()) return

        val now = clock()
        val existing = getShieldDiagnostics().toMutableList()
        val last = existing.firstOrNull()
        if (last != null &&
            last.category == cleanCat &&
            last.packageName == cleanPkg &&
            (now - last.timestamp) < 2000L
        ) {
            return
        }
        existing.add(0, ShieldDiagnosticEvent(now, cleanCat, cleanPkg, cleanDetail))
        val trimmed = existing.take(25)
        val encoded = trimmed.joinToString("\n") { e ->
            listOf(
                e.timestamp.toString(),
                escapeField(e.category),
                escapeField(e.packageName),
                escapeField(e.detail)
            ).joinToString("|")
        }
        prefs.edit().putString(KEY_SHIELD_DIAGNOSTICS, encoded).commit()
        _shieldDiagnosticsFlow.value = trimmed
    }

    fun getShieldDiagnostics(): List<ShieldDiagnosticEvent> {
        val raw = prefs.getString(KEY_SHIELD_DIAGNOSTICS, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size < 4) return@mapNotNull null
                ShieldDiagnosticEvent(
                    timestamp = parts[0].toLongOrNull() ?: 0L,
                    category = unescapeField(parts[1]),
                    packageName = unescapeField(parts[2]),
                    detail = unescapeField(parts[3])
                )
            }
            .sortedByDescending { it.timestamp }
            .toList()
    }

    fun clearShieldDiagnostics() {
        prefs.edit().remove(KEY_SHIELD_DIAGNOSTICS).commit()
        _shieldDiagnosticsFlow.value = emptyList()
    }

    fun buildShieldDiagnosticsText(): String {
        val sb = StringBuilder()
        sb.appendLine("=== GetBackToWork Shield Diagnostics ===")
        val events = getShieldDiagnostics()
        if (events.isEmpty()) {
            sb.appendLine("(No shield events recorded yet)")
        } else {
            val fmt = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            events.forEach { ev ->
                val timeStr = fmt.format(java.util.Date(ev.timestamp))
                sb.appendLine("[$timeStr] ${ev.category} | ${ev.packageName} | ${ev.detail}")
            }
        }
        return sb.toString().trim()
    }
}
