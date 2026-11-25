package com.getbacktowork.app

import android.content.SharedPreferences
import com.getbacktowork.app.data.AppInfo
import com.getbacktowork.app.data.AppRepository
import com.getbacktowork.app.data.FocusSessionManager
import com.getbacktowork.app.service.AppBlockerAccessibilityService
import com.getbacktowork.app.ui.overlay.ClarityPuzzleBank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusLogicTest {

    private class InMemorySharedPreferences : SharedPreferences {
        private val data = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = data.toMutableMap()
        override fun getString(key: String?, defValue: String?): String? =
            (data[key] as? String) ?: defValue

        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (data[key] as? Set<String>)?.toMutableSet() ?: defValues

        override fun getInt(key: String?, defValue: Int): Int =
            (data[key] as? Int) ?: defValue

        override fun getLong(key: String?, defValue: Long): Long =
            (data[key] as? Long) ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float =
            (data[key] as? Float) ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            (data[key] as? Boolean) ?: defValue

        override fun contains(key: String?): Boolean = data.containsKey(key)

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val updates = mutableMapOf<String, Any?>()
            private val removals = mutableSetOf<String>()
            private var clearAll = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
                if (key != null) {
                    removals.remove(key)
                    updates[key] = value
                }
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply {
                if (key != null) {
                    removals.remove(key)
                    updates[key] = values?.toSet()
                }
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
                if (key != null) {
                    removals.remove(key)
                    updates[key] = value
                }
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
                if (key != null) {
                    removals.remove(key)
                    updates[key] = value
                }
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
                if (key != null) {
                    removals.remove(key)
                    updates[key] = value
                }
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
                if (key != null) {
                    removals.remove(key)
                    updates[key] = value
                }
            }

            override fun remove(key: String?): SharedPreferences.Editor = apply {
                if (key != null) {
                    updates.remove(key)
                    removals.add(key)
                }
            }

            override fun clear(): SharedPreferences.Editor = apply {
                updates.clear()
                removals.clear()
                clearAll = true
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clearAll) data.clear()
                for (k in removals) data.remove(k)
                for ((k, v) in updates) data[k] = v
            }
        }

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    }

    private fun createManager(
        initialTimeMillis: Long = 1_700_000_000_000L
    ): Triple<FocusSessionManager, () -> Long, (Long) -> Unit> {
        var now = initialTimeMillis
        val prefs = InMemorySharedPreferences()
        val manager = FocusSessionManager(
            context = null,
            prefs = prefs,
            clock = { now },
            startTimerService = {},
            stopTimerService = {}
        )
        return Triple(manager, { now }, { deltaMs -> now += deltaMs })
    }

    @Test
    fun testDialerAndEmergencyAppDetection() {
        val testPackages = listOf(
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.samsung.android.incallui",
            "com.samsung.android.app.telephonyui",
            "com.android.incallui",
            "com.android.phone",
            "com.android.server.telecom"
        )
        val (manager, _, _) = createManager()

        for (pkg in testPackages) {
            assertTrue("Expected $pkg to be recognized as phone/emergency", manager.isAppAllowed(pkg))
        }
    }

    @Test
    fun testSystemFrameworkCredentialsBiometricsAndKeyboardsAllowed() {
        val (manager, _, _) = createManager()
        val essentialSystem = listOf(
            "com.android.credentialmanager",
            "com.samsung.android.biometrics.app.setting",
            "com.google.android.gms",
            "com.samsung.android.honeyboard",
            "com.google.android.inputmethod.latin",
            "com.google.android.permissioncontroller"
        )
        for (pkg in essentialSystem) {
            assertTrue("Expected $pkg to be permitted during focus", manager.isAppAllowed(pkg))
        }
    }

    @Test
    fun testDistractingAppsBlockedWhenNotWhitelisted() {
        val (manager, _, _) = createManager()
        val distractingPackages = listOf(
            "com.instagram.android",
            "com.google.android.youtube",
            "com.twitter.android",
            "com.zhiliaoapp.musically",
            "com.facebook.katana",
            "com.netflix.mediaclient"
        )

        for (pkg in distractingPackages) {
            assertFalse("Expected $pkg NOT to be allowed when unwhitelisted", manager.isAppAllowed(pkg))
        }
    }

    @Test
    fun testWhitelistLockedDuringStrictFocusEmergencyPassAndUltraFocus() {
        val (manager, _, advanceClock) = createManager()

        // Initially unlocked
        assertFalse(manager.isWhitelistLocked())
        assertTrue(manager.toggleAppAllowed("com.slack"))

        // Strict Focus locks whitelist
        manager.startSession(durationMinutes = 25, strictMode = true)
        assertTrue(manager.isWhitelistLocked())
        assertFalse(manager.toggleAppAllowed("com.instagram.android"))

        // Emergency 5m Pass pauses blocking, MUST STILL lock whitelist
        assertTrue(manager.activateEmergencyPass(5))
        assertFalse("Blocking is paused during 5m pass", manager.shouldBlockApps())
        assertTrue("Whitelist must remain locked during 5m pass", manager.isWhitelistLocked())
        assertFalse(manager.toggleAppAllowed("com.instagram.android"))

        // Expire Strict Focus session
        advanceClock(26 * 60 * 1000L)
        assertFalse(manager.isSessionActive())
        assertFalse(manager.isWhitelistLocked())

        // Non-strict Focus does not lock whitelist
        manager.startSession(durationMinutes = 15, strictMode = false)
        assertFalse(manager.isWhitelistLocked())
        manager.stopSession()
        assertFalse(manager.isSessionActive())

        // Ultra Focus (Cold Turkey) locks whitelist persistently (even during 30m pass)
        manager.startUltraFocusNow()
        assertTrue(manager.isWhitelistLocked())
        assertFalse(manager.toggleAppAllowed("com.instagram.android"))

        assertTrue(manager.activateEmergency30mUsage())
        assertFalse(manager.shouldBlockApps())
        assertTrue("Whitelist must stay locked during Ultra Focus 30m pass", manager.isWhitelistLocked())
    }

    @Test
    fun testTravelModeBlockedDuringUltraFocusAndStrictFocus() {
        val (manager, _, _) = createManager()

        // Allowed when idle
        assertTrue(manager.activateTravelMode(2))
        assertTrue(manager.isTravelModeActive())
        manager.endTravelMode()
        assertFalse(manager.isTravelModeActive())

        // Blocked during Strict Focus
        manager.startSession(durationMinutes = 25, strictMode = true)
        assertFalse(manager.activateTravelMode(2))
        assertFalse(manager.activateTravelModeMinutes(30))
        manager.stopSession(force = true)

        // Blocked during Ultra Focus
        manager.startUltraFocusNow()
        assertFalse(manager.activateTravelMode(1))
        assertFalse(manager.activateTravelModeMinutes(30))
        assertFalse(manager.isTravelModeActive())
    }

    @Test
    fun testUltraFocusEmergencyPassOnePerDayAnd24hLock() {
        val (manager, _, advanceClock) = createManager()

        // Cannot activate before Ultra Focus is enabled
        assertFalse(manager.activateEmergency30mUsage())

        manager.startUltraFocusNow()
        assertTrue(manager.shouldBlockApps())
        assertFalse(manager.canQuitNormally())
        assertFalse(manager.setUltraFocusEnabled(false))

        // First 30m emergency pass succeeds
        assertTrue(manager.activateEmergency30mUsage())
        assertTrue(manager.hasUsedEmergency30mToday())
        assertFalse("Phone open during 30m emergency window", manager.shouldBlockApps())

        // Cannot activate again while active OR after 30m expires on the SAME calendar day
        assertFalse(manager.activateEmergency30mUsage())
        advanceClock(31 * 60 * 1000L)
        assertTrue("Cold Turkey lock resumes automatically after 30m", manager.shouldBlockApps())
        assertFalse("Second emergency pass same day must be rejected", manager.activateEmergency30mUsage())

        // Cannot exit Cold Turkey at 23h59m
        advanceClock(23 * 60 * 60 * 1000L)
        assertFalse(manager.canQuitNormally())
        assertFalse(manager.setUltraFocusEnabled(false))

        // Next day (+2h -> past 24h & rolled calendar day): emergency pass resets and 24h unlock allowed
        advanceClock(2 * 60 * 60 * 1000L)
        assertFalse(manager.hasUsedEmergency30mToday())
        assertTrue(manager.canQuitNormally())
        assertTrue(manager.setUltraFocusEnabled(false))
    }

    @Test
    fun testHolidayExtensionsAreSplitByModeAndPreserveBaseLimit() {
        val (manager, _, advanceClock) = createManager()

        manager.setHolidayMasterEnabled(true)
        manager.setHolidayManualEnabled(true)
        manager.setHolidayLimitType(FocusSessionManager.LIMIT_TYPE_REELS)
        manager.setHolidayLimitValue(200)

        manager.grantHolidayExtension(25)
        val reelsState = manager.holidayStateFlow.value
        assertEquals(200, reelsState.baseLimitValue)
        assertEquals(225, reelsState.limitValue)

        // Switching to MINUTES must keep its own uncorrupted base & 0 extension
        manager.setHolidayLimitType(FocusSessionManager.LIMIT_TYPE_MINUTES)
        val minsState = manager.holidayStateFlow.value
        assertEquals(20, minsState.baseLimitValue)
        assertEquals(20, minsState.limitValue)

        manager.grantHolidayExtension(15)
        assertEquals(35, manager.holidayStateFlow.value.limitValue)
        assertEquals(20, manager.holidayStateFlow.value.baseLimitValue)

        // Advancing 24 hours rolls the day and clears both daily extensions
        advanceClock(24 * 60 * 60 * 1000L)
        manager.refreshHolidayState()
        assertEquals(20, manager.holidayStateFlow.value.limitValue)
        manager.setHolidayLimitType(FocusSessionManager.LIMIT_TYPE_REELS)
        assertEquals(200, manager.holidayStateFlow.value.limitValue)
    }

    @Test
    fun testStopSessionUsesStopServiceCallbackSafely() {
        var stoppedCount = 0
        var now = 1_700_000_000_000L
        val prefs = InMemorySharedPreferences()
        val manager = FocusSessionManager(
            context = null,
            prefs = prefs,
            clock = { now },
            startTimerService = {},
            stopTimerService = { stoppedCount += 1 }
        )

        manager.startSession(durationMinutes = 25, strictMode = false)
        assertTrue(manager.isSessionActive())
        manager.stopSession()
        assertFalse(manager.isSessionActive())
        assertEquals(1, stoppedCount)
    }

    @Test
    fun testEssentialUtilitiesAllowedInColdTurkeyWithoutPermittingDistractions() {
        val (manager, _, _) = createManager()
        manager.startUltraFocusNow()

        val essentialTools = listOf(
            "com.sec.android.app.camera",               // Samsung Quick Settings QR scanner / camera
            "com.google.android.GoogleCamera",
            "com.sec.android.app.clockpackage",         // Alarms & timers
            "com.sec.android.app.popupcalculator",      // Calculator
            "com.samsung.android.app.contacts",         // Contacts
            "com.sec.android.app.voicenote",            // Voice memos
            "com.google.android.apps.authenticator2",   // 2FA OTPs
            "com.azure.authenticator",
            "com.duosecurity.duomobile",
            "com.samsung.android.mtp",                  // USB file transfer
            "com.samsung.android.app.sharelive",        // Quick Share
            "com.samsung.android.app.smartcapture",     // Screenshots
            "com.google.android.apps.walletnfcrel",     // Google Wallet
            "com.samsung.android.spay",                 // Samsung Wallet
            "com.samsung.android.app.aodservice"        // Always-On Display daemon
        )
        for (pkg in essentialTools) {
            assertTrue("Expected essential utility $pkg to be allowed", manager.isAppAllowed(pkg))
        }

        // Distracting apps & unwhitelisted browsers must remain blocked
        assertFalse(manager.isAppAllowed("com.instagram.android"))
        assertFalse(manager.isAppAllowed("com.zhiliaoapp.musically"))
        assertFalse(manager.isAppAllowed("com.google.android.youtube"))
        assertFalse(manager.isAppAllowed("com.android.chrome"))
    }

    @Test
    fun testSmartSettingsGuardAllowsUsbAndWifiWhileBlockingTamperScreens() {
        // Safe Settings screens must be permitted
        assertFalse(
            AppBlockerAccessibilityService.isTamperSettingsMatch(
                className = "com.android.settings.Settings",
                headerTitleText = "Settings",
                bodyNodeText = "Settings Connections Accessibility Battery"
            )
        )
        assertFalse(
            AppBlockerAccessibilityService.isTamperSettingsMatch(
                className = "com.android.settings.SubSettings",
                headerTitleText = "Developer options",
                bodyNodeText = "Developer options Memory Running services USB debugging"
            )
        )
        assertFalse(
            AppBlockerAccessibilityService.isTamperSettingsMatch(
                className = "com.android.settings.SubSettings",
                headerTitleText = "USB settings",
                bodyNodeText = "Transferring files USB tethering Charging phone only"
            )
        )
        assertFalse(
            AppBlockerAccessibilityService.isTamperSettingsMatch(
                className = "com.android.settings.SubSettings",
                headerTitleText = "Wi-Fi",
                bodyNodeText = "Office-5G Connected"
            )
        )

        // Tamper Settings screens must be blocked
        assertTrue(
            AppBlockerAccessibilityService.isTamperSettingsMatch(
                className = "com.android.settings.SubSettings",
                headerTitleText = "Accessibility",
                bodyNodeText = "Accessibility Installed apps"
            )
        )
        assertTrue(
            AppBlockerAccessibilityService.isTamperSettingsMatch(
                className = "com.android.settings.SubSettings",
                headerTitleText = "Device admin apps",
                bodyNodeText = "GetBackToWork Anti-Uninstall"
            )
        )
        assertTrue(
            AppBlockerAccessibilityService.isTamperSettingsMatch(
                className = "com.android.settings.SubSettings",
                headerTitleText = "App info",
                bodyNodeText = "GetBackToWork Uninstall Force stop"
            )
        )
        assertTrue(
            AppBlockerAccessibilityService.isTamperSettingsMatch(
                className = "com.android.settings.SubSettings",
                headerTitleText = "Running services",
                bodyNodeText = "Running services RAM"
            )
        )
        assertTrue(
            AppBlockerAccessibilityService.isTamperSettingsMatch(
                className = "com.android.settings.SubSettings",
                headerTitleText = "Date and time",
                bodyNodeText = "Automatic date and time Set date"
            )
        )
    }

    @Test
    fun testConfigurableColdTurkeyDurationAndTransitLifeline() {
        val (manager, _, advanceTime) = createManager()
        manager.startUltraFocusNow(durationHours = 4)

        assertTrue(manager.isUltraFocusActive())
        assertEquals(4, manager.getUltraFocusDurationHours())
        advanceTime(2L * 60L * 60L * 1000L)
        assertFalse("Cold Turkey cannot quit after 2h of a 4h lock", manager.setUltraFocusEnabled(false))

        // Activate Transit Lifeline while Cold Turkey is active:
        // Navigation, rides, rail, and payments must open while social media & browsers stay blocked!
        assertTrue(manager.activateUltraFocusTransitMode(120))
        assertTrue(manager.isUltraFocusTransitActive())
        assertTrue(manager.isAppAllowed("com.google.android.apps.maps"))
        assertTrue(manager.isAppAllowed("com.ubercab"))
        assertTrue(manager.isAppAllowed("cris.org.in.prs.ima"))
        assertTrue(manager.isAppAllowed("com.google.android.apps.nbu.paisa.user"))
        assertFalse(manager.isAppAllowed("com.instagram.android"))
        assertFalse(manager.isAppAllowed("com.google.android.youtube"))
        assertFalse(manager.isAppAllowed("com.zhiliaoapp.musically"))
        assertFalse(manager.isAppAllowed("com.android.chrome"))

        advanceTime(2L * 60L * 60L * 1000L + 1000L)
        manager.refreshUltraFocusState()
        assertTrue("4h lock should be complete", manager.canQuitUltraFocusNormally())
        assertTrue(manager.setUltraFocusEnabled(false))
        assertFalse(manager.isUltraFocusActive())
    }

    @Test
    fun testDiaryFocusLoggingStreakAndSmartAdvisorGoal() {
        val oneDayMs = 24L * 60L * 60L * 1000L
        val (manager, _, advanceTime) = createManager()

        // Default goal is <1h (60m)
        assertEquals(60, manager.getDailyScreenGoalMinutes())
        val initialAdvice = manager.getDailyCoachAdvice()
        assertEquals(FocusSessionManager.AdviceLevel.CAUTION, initialAdvice.level)

        // Day 1: Complete a 25m session -> logged to diary & streak starts (1d)
        manager.startSession(25, strictMode = true)
        advanceTime(25L * 60L * 1000L + 1000L)
        manager.refreshState()
        assertEquals(25, manager.getTodayDiaryEntry().focusedMinutes)
        assertEquals(1, manager.getCurrentStreakDays())
        manager.saveDiaryNote(note = "Shipped core architecture offline.")
        assertEquals("Shipped core architecture offline.", manager.getTodayDiaryEntry().note)

        // Day 2: Complete another session -> streak becomes 2d; exceeding 60m triggers LOCK_RECOMMENDED
        advanceTime(oneDayMs)
        manager.startSession(30, strictMode = true)
        advanceTime(30L * 60L * 1000L + 1000L)
        manager.refreshState()
        assertEquals(2, manager.getCurrentStreakDays())

        manager.addScreenMinutesUsed(45, isDistracting = true)
        val cautionAdvice = manager.getDailyCoachAdvice()
        assertEquals(FocusSessionManager.AdviceLevel.CAUTION, cautionAdvice.level)

        manager.addScreenMinutesUsed(20, isDistracting = true)
        val overLimitAdvice = manager.getDailyCoachAdvice()
        assertEquals(FocusSessionManager.AdviceLevel.LOCK_RECOMMENDED, overLimitAdvice.level)
        assertTrue(manager.isDailyGoalCheckInDue())
    }

    @Test
    fun testTravelModeLifeOutsidePhoneReminders() {
        val (manager, _, _) = createManager()
        assertTrue(manager.activateTravelMode(2))
        assertFalse(manager.isTravelLifeReminderDue())

        repeat(24) { manager.incrementTravelReelsScrolled() }
        assertFalse(manager.isTravelLifeReminderDue())

        manager.incrementTravelReelsScrolled()
        assertTrue("25 Travel Reels should trigger outdoor life check-in", manager.isTravelLifeReminderDue())
        assertEquals(25, manager.getActiveReelsOverlayCount())
    }

    @Test
    fun testShortsAndReelsDetectionCoversModernAndObfuscatedTrees() {
        // Legacy IDs
        assertTrue(
            AppBlockerAccessibilityService.isShortsOrReelsSignals(
                packageName = "com.google.android.youtube",
                viewIds = listOf("com.google.android.youtube:id/reel_recycler"),
                labels = emptyList()
            )
        )
        // Obfuscated Compose / 2026 YouTube Shorts fallback (Remix + Dislike + Comments, no Search/Fullscreen)
        assertTrue(
            AppBlockerAccessibilityService.isShortsOrReelsSignals(
                packageName = "com.google.android.youtube",
                viewIds = emptyList(),
                labels = listOf("@creator_handle", "Like this video", "Dislike this video", "Comments", "Remix")
            )
        )
        // Home feed must NEVER false-trigger on Shorts shelf
        assertFalse(
            AppBlockerAccessibilityService.isShortsOrReelsSignals(
                packageName = "com.google.android.youtube",
                viewIds = listOf("com.google.android.youtube:id/pivot_bar"),
                labels = listOf("Search YouTube", "Home", "Shorts", "Subscriptions")
            )
        )
        // Regular horizontal Watch video must NOT trigger Reels counter
        assertFalse(
            AppBlockerAccessibilityService.isShortsOrReelsSignals(
                packageName = "com.google.android.youtube",
                viewIds = listOf("com.google.android.youtube:id/player_view"),
                labels = listOf("Like this video", "Comments", "Remix", "Enter fullscreen")
            )
        )
        // Instagram Reels, Facebook Reels, Snapchat Spotlight, TikTok & Browser URLs
        assertTrue(
            AppBlockerAccessibilityService.isShortsOrReelsSignals(
                packageName = "com.instagram.android",
                viewIds = emptyList(),
                labels = listOf("Original audio", "Like", "Comment", "Send")
            )
        )
        assertTrue(
            AppBlockerAccessibilityService.isShortsOrReelsSignals(
                packageName = "com.facebook.katana",
                viewIds = emptyList(),
                labels = listOf("Reels", "Remix this reel", "Add a comment")
            )
        )
        assertTrue(
            AppBlockerAccessibilityService.isShortsOrReelsSignals(
                packageName = "com.snapchat.android",
                viewIds = emptyList(),
                labels = listOf("Spotlight", "Share", "Comment")
            )
        )
        assertTrue(
            AppBlockerAccessibilityService.isShortsOrReelsSignals(
                packageName = "com.android.chrome",
                viewIds = emptyList(),
                labels = emptyList(),
                url = "https://m.youtube.com/shorts/abc123"
            )
        )
    }

    @Test
    fun testHomePinnedPackagesToggleForMinimalistNoModeLauncher() {
        val (manager, _, _) = createManager()
        assertTrue(manager.getHomePinnedPackages().isEmpty())
        assertTrue(manager.toggleHomePinnedPackage("com.whatsapp"))
        assertTrue(manager.getHomePinnedPackages().contains("com.whatsapp"))
        assertFalse(manager.toggleHomePinnedPackage("com.whatsapp"))
        assertFalse(manager.getHomePinnedPackages().contains("com.whatsapp"))
    }

    @Test
    fun testLongFormVideoDetectionAndMinutesCounter() {
        val (manager, _, advanceTime) = createManager()

        // YouTube horizontal/long-form video -> true; Home/Miniplayer/Shorts -> false
        assertTrue(
            AppBlockerAccessibilityService.isLongFormVideoSignals(
                packageName = "com.google.android.youtube",
                viewIds = listOf("com.google.android.youtube:id/watch_player"),
                labels = listOf("Like this video", "Enter fullscreen")
            )
        )
        assertFalse(
            AppBlockerAccessibilityService.isLongFormVideoSignals(
                packageName = "com.google.android.youtube",
                viewIds = listOf("com.google.android.youtube:id/pivot_bar"),
                labels = listOf("Search YouTube", "Home", "Shorts")
            )
        )
        assertFalse(
            AppBlockerAccessibilityService.isLongFormVideoSignals(
                packageName = "com.google.android.youtube",
                viewIds = listOf("com.google.android.youtube:id/reel_recycler"),
                labels = listOf("Remix", "Dislike this video", "Comments")
            )
        )
        // Streaming apps & browser watch URLs
        assertTrue(
            AppBlockerAccessibilityService.isLongFormVideoSignals(
                packageName = "com.netflix.mediaclient",
                viewIds = emptyList(),
                labels = emptyList()
            )
        )
        assertTrue(
            AppBlockerAccessibilityService.isLongFormVideoSignals(
                packageName = "com.android.chrome",
                viewIds = emptyList(),
                labels = emptyList(),
                url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
            )
        )

        // Daily long-form minutes tracking rolls over cleanly by day
        assertEquals(0, manager.getLongVideoMinutesToday())
        manager.addLongVideoMinutesUsed(12)
        assertEquals(12, manager.getLongVideoMinutesToday())
        advanceTime(24L * 60L * 60L * 1000L)
        assertEquals(0, manager.getLongVideoMinutesToday())
    }

    @Test
    fun testYearProgressLifeGoalsAndDiaryExport() {
        val (manager, _, _) = createManager()
        val yearInfo = manager.getYearProgressInfo()
        assertTrue(yearInfo.totalDays in 365..366)
        assertEquals(yearInfo.totalDays - yearInfo.dayOfYear, yearInfo.daysRemaining)
        assertTrue(yearInfo.elapsedPercent in 0..100)

        manager.setLifeGoals(listOf("Launch v1", "Stay under 1h screen", "Read nightly"))
        assertEquals(listOf("Launch v1", "Stay under 1h screen", "Read nightly"), manager.getLifeGoals())

        manager.addFocusedMinutesToDiary(45)
        val exported = manager.buildDiaryExportText()
        assertTrue(exported.contains("Launch v1"))
        assertTrue(exported.contains("Focus=45m"))
        assertTrue(exported.contains("Year Progress:"))
    }

    @Test
    fun testAutoFocusSchedulesWithMidnightWrapAndEarnedScroll() {
        // Choose a deterministic local calendar point: 2026-11-25 14:30 (Wednesday)
        val cal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 25, 14, 30, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val (manager, _, advanceTime) = createManager(initialTimeMillis = cal.timeInMillis)

        assertFalse(manager.shouldBlockApps())

        // Workday schedule 09:00-17:00 Wed -> active now at 14:30
        manager.setWorkSchedule(
            enabled = true,
            startMinute = 9 * 60,
            endMinute = 17 * 60,
            days = setOf(java.util.Calendar.WEDNESDAY)
        )
        assertTrue(manager.isScheduledFocusActiveNow())
        assertTrue(manager.shouldBlockApps())

        // Earned Scroll 10m temporarily waives scheduled block
        manager.grantEarnedScrollMinutes(10)
        assertTrue(manager.isEarnedScrollActive())
        assertFalse(manager.shouldBlockApps())

        // Advance 11 minutes -> Earned Scroll expires and schedule resumes
        advanceTime(11L * 60L * 1000L)
        assertFalse(manager.isEarnedScrollActive())
        assertTrue(manager.shouldBlockApps())

        // Disable work, enable Sleep Wind-Down 23:00 -> 07:00, advance to 01:30 next morning
        manager.setWorkSchedule(enabled = false)
        manager.setSleepSchedule(enabled = true, startMinute = 23 * 60, endMinute = 7 * 60)
        assertFalse(manager.isScheduledFocusActiveNow())
        advanceTime(10L * 60L * 60L * 1000L + 50L * 60L * 1000L) // ~01:31
        assertTrue("Overnight sleep schedule must hold across midnight", manager.isScheduledFocusActiveNow())
        assertTrue(manager.shouldBlockApps())
    }

    @Test
    fun testGranularSurgicalFeedAndDomainBlockingSignals() {
        // YouTube Home feed blocked while Search & Subscriptions stay allowed
        assertTrue(
            AppBlockerAccessibilityService.isYouTubeHomeFeedSignals(
                packageName = "com.google.android.youtube",
                viewIds = emptyList(),
                labels = listOf("Home", "Shorts", "Subscriptions"),
                selectedLabels = listOf("Home")
            )
        )
        assertFalse(
            AppBlockerAccessibilityService.isYouTubeHomeFeedSignals(
                packageName = "com.google.android.youtube",
                viewIds = listOf("com.google.android.youtube:id/search_results"),
                labels = listOf("Search"),
                selectedLabels = listOf("Home")
            )
        )

        // Instagram Explore blocked while Direct Inbox stays allowed
        assertTrue(
            AppBlockerAccessibilityService.isInstagramExploreSignals(
                packageName = "com.instagram.android",
                viewIds = listOf("com.instagram.android:id/explore_grid"),
                labels = emptyList(),
                selectedLabels = listOf("Search and explore")
            )
        )
        assertFalse(
            AppBlockerAccessibilityService.isInstagramExploreSignals(
                packageName = "com.instagram.android",
                viewIds = listOf("com.instagram.android:id/direct_inbox"),
                labels = listOf("Messages"),
                selectedLabels = emptyList()
            )
        )

        // X/Twitter 'For you' & LinkedIn Home cleaned while Messaging/Following preserved
        assertTrue(
            AppBlockerAccessibilityService.isSocialAlgorithmicFeedSignals(
                packageName = "com.twitter.android",
                viewIds = emptyList(),
                labels = emptyList(),
                selectedLabels = listOf("For you")
            )
        )
        assertFalse(
            AppBlockerAccessibilityService.isSocialAlgorithmicFeedSignals(
                packageName = "com.twitter.android",
                viewIds = emptyList(),
                labels = emptyList(),
                selectedLabels = listOf("Following")
            )
        )
        assertTrue(
            AppBlockerAccessibilityService.isSocialAlgorithmicFeedSignals(
                packageName = "com.linkedin.android",
                viewIds = emptyList(),
                labels = emptyList(),
                selectedLabels = listOf("Home")
            )
        )
        assertFalse(
            AppBlockerAccessibilityService.isSocialAlgorithmicFeedSignals(
                packageName = "com.linkedin.android",
                viewIds = emptyList(),
                labels = emptyList(),
                selectedLabels = listOf("Messaging")
            )
        )

        // Custom domain normalization & matching
        val (manager, _, _) = createManager()
        assertTrue(manager.addBlockedDomain("https://www.News.YCombinator.com/news"))
        val match = AppBlockerAccessibilityService.matchesBlockedDomain(
            url = "https://news.ycombinator.com/item?id=1",
            blockedDomains = manager.getBlockedDomains(),
            blockShortUrls = true
        )
        assertEquals("news.ycombinator.com", match)
        assertNull(
            AppBlockerAccessibilityService.matchesBlockedDomain(
                url = "https://kotlinlang.org/docs",
                blockedDomains = manager.getBlockedDomains(),
                blockShortUrls = true
            )
        )
    }

    @Test
    fun testScreenZenCooldownCustomAppNamesAndPuzzleBank() {
        val (manager, _, advanceTime) = createManager()

        // Custom App Renaming
        manager.setCustomAppName("com.google.android.youtube", "Lectures")
        assertEquals("Lectures", manager.resolveAppName("com.google.android.youtube", "YouTube"))
        manager.setCustomAppName("com.google.android.youtube", "")
        assertEquals("YouTube", manager.resolveAppName("com.google.android.youtube", "YouTube"))

        // ScreenZen Cooldown window
        manager.setCooldownSeconds(15)
        assertTrue(manager.isCooldownRequiredForPackage("com.google.android.youtube"))
        manager.grantCooldownWindow("com.google.android.youtube", minutes = 10)
        assertFalse(manager.isCooldownRequiredForPackage("com.google.android.youtube"))

        advanceTime(10L * 60L * 1000L + 1000L)
        assertTrue("10m cooldown pass must expire cleanly", manager.isCooldownRequiredForPackage("com.google.android.youtube"))

        // Verify all Tactical Chess Mate-in-1 puzzles have valid square answers
        assertTrue(ClarityPuzzleBank.chessMates.size >= 4)
        for (p in ClarityPuzzleBank.chessMates) {
            assertNotNull(p.winningSquare)
            assertEquals(2, p.winningSquare.length)
            assertTrue(p.winningNotation.endsWith("#"))
        }
    }

    @Test
    fun testHomePinnedEssentialsEditableOrderAndAutoReset() {
        val (manager, _, _) = createManager()

        // Fresh state defaults to not customized (empty order & set)
        assertFalse(manager.isHomePinnedCustomized())
        assertTrue(manager.getHomePinnedPackages().isEmpty())
        assertTrue(manager.getHomePinnedOrder().isEmpty())

        // Toggling from default seeds fallback apps before appending new pin
        val nowPinned = manager.toggleHomePinnedPackage(
            packageName = "com.spotify.music",
            initialFallbackPackages = listOf("com.slack", "com.notes")
        )
        assertTrue(nowPinned)
        assertTrue(manager.isHomePinnedCustomized())
        assertEquals(listOf("com.slack", "com.notes", "com.spotify.music"), manager.getHomePinnedOrder())
        assertEquals(setOf("com.slack", "com.notes", "com.spotify.music"), manager.homePinnedPackagesFlow.value)

        // Setting explicit custom order preserves ordering and deduplicates
        manager.setHomePinnedPackages(listOf("com.obsidian.md", "com.slack", "com.obsidian.md", " "))
        assertEquals(listOf("com.obsidian.md", "com.slack"), manager.getHomePinnedOrder())
        assertEquals(listOf("com.obsidian.md", "com.slack"), manager.homePinnedOrderFlow.value)
        assertTrue(manager.homePinnedCustomizedFlow.value)

        // Intentionally clearing all pinned essentials preserves customized flag so home stays clean
        manager.setHomePinnedPackages(emptyList())
        assertTrue(manager.isHomePinnedCustomized())
        assertTrue(manager.getHomePinnedOrder().isEmpty())
        assertTrue(manager.getHomePinnedPackages().isEmpty())

        // Resetting returns to auto-following Allowed Tools defaults
        manager.resetHomePinnedToAuto()
        assertFalse(manager.isHomePinnedCustomized())
        assertTrue(manager.getHomePinnedOrder().isEmpty())
        assertFalse(manager.homePinnedCustomizedFlow.value)

        // Legacy migration: existing key_home_pinned_packages without customized flag treats as customized
        val legacyPrefs = InMemorySharedPreferences().apply {
            edit().putStringSet("key_home_pinned_packages", mutableSetOf("com.b", "com.a")).commit()
        }
        val legacyManager = FocusSessionManager(
            context = null,
            prefs = legacyPrefs,
            clock = { 1_700_000_000_000L },
            startTimerService = {},
            stopTimerService = {}
        )
        assertTrue(legacyManager.isHomePinnedCustomized())
        assertEquals(listOf("com.a", "com.b"), legacyManager.getHomePinnedOrder())
    }

    @Test
    fun testNotificationDigestRecordingDedupAndDismissal() {
        val (manager, _, advanceTime) = createManager()

        assertTrue(manager.getBlockedNotifications().isEmpty())

        // Captures preview cleanly and throttles rapid identical alerts (<15s)
        manager.recordBlockedNotification(
            packageName = "com.instagram.android",
            appName = "Instagram",
            title = "Alex",
            preview = "Sent a reel"
        )
        manager.recordBlockedNotification(
            packageName = "com.instagram.android",
            appName = "Instagram",
            title = "Alex",
            preview = "Sent a reel"
        )
        assertEquals(1, manager.getBlockedNotifications().size)

        // Advance past dedup window & add second alert (newest first)
        advanceTime(20_000L)
        manager.recordBlockedNotification(
            packageName = "com.slack",
            appName = "Slack",
            title = "#builds",
            preview = "Release APK green"
        )
        val items = manager.getBlockedNotifications()
        assertEquals(2, items.size)
        assertEquals("Slack", items[0].appName)
        assertEquals("Instagram", items[1].appName)

        // Dismiss one, then clear remaining
        manager.dismissBlockedNotification(items[0].id)
        assertEquals(1, manager.getBlockedNotifications().size)
        assertEquals("Instagram", manager.getBlockedNotifications().first().appName)

        manager.clearBlockedNotifications()
        assertTrue(manager.getBlockedNotifications().isEmpty())
    }

    @Test
    fun testPerAppDailyCapsOpenLimitTimeLimitEarnedUnlockAndMidnightReset() {
        val (manager, _, advanceTime) = createManager(initialTimeMillis = 1_700_000_000_000L)

        val pkg = "com.instagram.android"
        manager.setAppDailyCap(pkg, maxMinutesPerDay = 15, maxOpensPerDay = 3)
        assertEquals(15, manager.getAppDailyCap(pkg)?.maxMinutesPerDay)
        assertEquals(3, manager.getAppDailyCap(pkg)?.maxOpensPerDay)

        // Opens 1..3 permitted; attempt 4 blocked on new entry while mid-session stays unblocked until cap/exit
        assertNull(manager.getAppDailyCapBreachReason(pkg, isNewEntry = true))
        assertEquals(1, manager.recordAppOpen(pkg))
        assertEquals(2, manager.recordAppOpen(pkg))
        assertNull(manager.getAppDailyCapBreachReason(pkg, isNewEntry = true))
        assertEquals(3, manager.recordAppOpen(pkg))

        // During open #3 (isNewEntry = false), opens cap alone does NOT kick mid-open; on open #4 (isNewEntry = true), it blocks!
        assertNull(manager.getAppDailyCapBreachReason(pkg, isNewEntry = false))
        assertNotNull(manager.getAppDailyCapBreachReason(pkg, isNewEntry = true))

        // Time limit blocks BOTH mid-use and on entry
        manager.addAppMinutesUsed(pkg, 15)
        assertNotNull(manager.getAppDailyCapBreachReason(pkg, isNewEntry = false))

        // Earned Scroll 5m unlock temporarily waives daily cap, then expires
        manager.grantEarnedScrollMinutes(5)
        assertNull(manager.getAppDailyCapBreachReason(pkg, isNewEntry = true))
        advanceTime(6L * 60_000L)
        assertNotNull(manager.getAppDailyCapBreachReason(pkg, isNewEntry = true))

        // Allowed apps remain exempt
        assertTrue(manager.toggleAppAllowed(pkg))
        assertTrue(manager.isAppAllowed(pkg))
        assertNull(manager.getAppDailyCapBreachReason(pkg, isNewEntry = true))
        assertTrue(manager.toggleAppAllowed(pkg))
        assertFalse(manager.isAppAllowed(pkg))

        // Midnight rollover resets per-app usage cleanly while preserving rule
        advanceTime(24L * 60L * 60L * 1000L)
        val rolledUsage = manager.getAppTodayUsage(pkg)
        assertEquals(0, rolledUsage.opens)
        assertEquals(0, rolledUsage.minutes)
        assertNull(manager.getAppDailyCapBreachReason(pkg, isNewEntry = true))
    }

    @Test
    fun testMultiWindowPipBypassDetectionAndShieldDiagnosticsLog() {
        // Detects disallowed PiP / split-screen app while allowed app is open in primary window
        val detected = AppBlockerAccessibilityService.findDisallowedVisibleWindowPackage(
            visibleAppWindowPackages = listOf(
                "com.getbacktowork.focus",
                "com.android.systemui",
                "com.slack",
                "com.google.android.youtube"
            ),
            selfPackageName = "com.getbacktowork.focus",
            isLauncherCheck = { it == "com.sec.android.app.launcher" },
            isAllowedCheck = { it == "com.slack" }
        )
        assertEquals("com.google.android.youtube", detected)

        val safeWindows = AppBlockerAccessibilityService.findDisallowedVisibleWindowPackage(
            visibleAppWindowPackages = listOf("com.slack", "com.sec.android.app.launcher"),
            selfPackageName = "com.getbacktowork.focus",
            isLauncherCheck = { it == "com.sec.android.app.launcher" },
            isAllowedCheck = { it == "com.slack" }
        )
        assertNull(safeWindows)

        // Shield diagnostic logging + throttled dedup + clear
        val (manager, _, advanceTime) = createManager()
        manager.recordShieldDiagnostic("PIP_MULTIWINDOW_BLOCK", "com.google.android.youtube", "Blocked PiP window")
        manager.recordShieldDiagnostic("PIP_MULTIWINDOW_BLOCK", "com.google.android.youtube", "Duplicate within 2s")
        assertEquals(1, manager.getShieldDiagnostics().size)

        advanceTime(3000L)
        manager.recordShieldDiagnostic("SHORTS_REELS_BLOCK", "com.instagram.android", "Exited Reels")
        assertEquals(2, manager.getShieldDiagnostics().size)
        assertTrue(manager.buildShieldDiagnosticsText().contains("SHORTS_REELS_BLOCK"))

        manager.clearShieldDiagnostics()
        assertTrue(manager.getShieldDiagnostics().isEmpty())
    }

    @Test
    fun testWorkProfileKeysPartitioningAndPinnedResolution() {
        val personalSlack = AppInfo(
            packageName = "com.slack",
            appName = "Slack",
            isWorkProfile = false,
            profileSerial = 0L
        )
        val workSlack = AppInfo(
            packageName = "com.slack",
            appName = "Slack",
            isWorkProfile = true,
            profileSerial = 10L,
            componentClassName = "com.slack.ui.HomeActivity"
        )
        val workTeams = AppInfo(
            packageName = "com.microsoft.teams",
            appName = "Teams",
            isWorkProfile = true,
            profileSerial = 10L
        )

        assertEquals("com.slack", personalSlack.uniqueKey)
        assertEquals("work:10:com.slack", workSlack.uniqueKey)
        assertTrue(personalSlack.uniqueKey != workSlack.uniqueKey)

        val (personal, work) = AppRepository.partitionPersonalAndWorkApps(
            listOf(personalSlack, workSlack, workTeams)
        )
        assertEquals(listOf(personalSlack), personal)
        assertEquals(listOf(workSlack, workTeams), work)

        val resolved = AppRepository.resolvePinnedSelection(
            apps = listOf(personalSlack, workSlack, workTeams),
            orderedKeys = listOf("work:10:com.slack", "com.slack")
        )
        assertEquals(listOf(workSlack, personalSlack), resolved)
    }

    @Test
    fun testWorkProfileTopOrderingAndPinningIsolation() {
        val personalOutlook = AppInfo(
            packageName = "com.microsoft.office.outlook",
            appName = "Outlook",
            isWorkProfile = false,
            profileSerial = 0L
        )
        val workOutlook = AppInfo(
            packageName = "com.microsoft.office.outlook",
            appName = "Outlook",
            isWorkProfile = true,
            profileSerial = 10L
        )
        val allApps = listOf(personalOutlook, workOutlook)
        val (personal, work) = AppRepository.partitionPersonalAndWorkApps(allApps)

        // Work section rendered at the top before personal section
        val topOrdered = work + personal
        assertEquals(listOf(workOutlook, personalOutlook), topOrdered)

        // Pinning only the work app does not pin the personal app with the same package
        val workOnlyPinned = AppRepository.resolvePinnedSelection(
            apps = allApps,
            orderedKeys = listOf(workOutlook.uniqueKey)
        )
        assertEquals(listOf(workOutlook), workOnlyPinned)

        // Pinning both preserves order and keeps them distinct
        val bothPinned = AppRepository.resolvePinnedSelection(
            apps = allApps,
            orderedKeys = listOf(workOutlook.uniqueKey, personalOutlook.uniqueKey)
        )
        assertEquals(listOf(workOutlook, personalOutlook), bothPinned)
    }
}
