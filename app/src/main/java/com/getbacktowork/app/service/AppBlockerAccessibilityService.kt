package com.getbacktowork.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Toast
import com.getbacktowork.app.data.FocusSessionManager
import com.getbacktowork.app.ui.overlay.BlockingOverlayActivity
import com.getbacktowork.app.ui.overlay.CooldownPromptActivity
import com.getbacktowork.app.ui.overlay.HolidayInterrupterActivity
import com.getbacktowork.app.ui.overlay.ReelsFloatingCounterOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppBlockerAccessibilityService : AccessibilityService() {

    private lateinit var sessionManager: FocusSessionManager
    private val launcherPackages = mutableSetOf<String>()
    private var lastBlockedPackage: String? = null
    private var lastBlockedTimestamp: Long = 0L

    private var lastScrollTimestamp: Long = 0L
    private var lastContentScanTimestamp: Long = 0L
    private var wasInShortsPlayer: Boolean = false
    private var lastShortSignature: String = ""
    private var activeLongVideoPackage: String? = null
    private var lastLongVideoMinuteTimestamp: Long = 0L
    private var lastCooldownPromptPackage: String? = null
    private var lastCooldownPromptTimestamp: Long = 0L
    private var lastForegroundApp: String? = null
    private var lastForegroundTimestamp: Long = 0L
    private var lastTrackedIdleApp: String? = null
    private var lastCapInterruptPkg: String? = null
    private var lastCapInterruptTimestamp: Long = 0L
    private var lastScreenMinuteTimestamp: Long = 0L
    private var lastInterruptedTimestamp: Long = 0L
    private var lastSurgicalToastTimestamp: Long = 0L
    private var reelsOverlay: ReelsFloatingCounterOverlay? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var tickerJob: Job? = null
    private var settingsCheckJob: Job? = null

    companion object {
        private const val TAG = "GetBackToWork"

        private val ADULT_DOMAINS = listOf(
            "pornhub", "xvideos", "xnxx", "redtube", "youporn",
            "erome", "onlyfans", "chaturbate", "cam4", "hentai",
            "xhamster", "tube8", "beeg", "spankbang", "brazzers"
        )

        private val TAMPER_CLASS_KEYWORDS = listOf(
            "accessibility",
            "deviceadmin",
            "appinfodashboard",
            "installedappdetails",
            "applicationdetails",
            "manageapplications",
            "manageoverlaypermission",
            "notificationaccess",
            "usageaccess",
            "datetimesettings",
            "runningservices",
            "runningprocesses"
        )

        private val TAMPER_TITLE_KEYWORDS = listOf(
            "accessibility",
            "installed services",
            "downloaded apps",
            "downloaded services",
            "device admin",
            "phone administrators",
            "app admin",
            "app info",
            "appear on top",
            "display over other apps",
            "notification access",
            "usage data access",
            "running services",
            "date and time",
            "date & time",
            "set date",
            "set time",
            "getbacktowork",
            "get back to work"
        )

        private val TAMPER_BODY_KEYWORDS = listOf(
            "getbacktowork",
            "get back to work",
            "com.getbacktowork",
            "force stop",
            "deactivate this device admin",
            "deactivate and uninstall",
            "uninstall updates"
        )

        fun isTamperSettingsMatch(
            className: String,
            headerTitleText: String,
            bodyNodeText: String = ""
        ): Boolean {
            val lowerClass = className.lowercase()
            if (TAMPER_CLASS_KEYWORDS.any { lowerClass.contains(it) }) {
                return true
            }

            val lowerHeader = headerTitleText.lowercase()
            if (TAMPER_TITLE_KEYWORDS.any { lowerHeader.contains(it) }) {
                return true
            }

            val lowerBody = bodyNodeText.lowercase()
            if (lowerBody.isNotEmpty() && TAMPER_BODY_KEYWORDS.any { lowerBody.contains(it) }) {
                return true
            }

            return false
        }

        private val YT_PLAYER_IDS = listOf(
            "reel_recycler", "reel_player_page_container", "reel_player_overlay",
            "reel_watch_fragment_root", "reel_watch_player", "shorts_container",
            "shorts_player_view", "reel_progress_bar", "reel_scrubber", "reel_player"
        )

        private val IG_REELS_IDS = listOf(
            "clips_viewer_view_pager", "clips_video_container", "clips_viewer_root",
            "clips_viewer", "clips_ufi", "clips_author", "reels_viewer"
        )

        private val FB_REELS_IDS = listOf(
            "reels_viewer", "fb_shorts_viewer", "reels_player", "reel_player"
        )

        private val SNAP_SPOTLIGHT_IDS = listOf(
            "spotlight_view", "spotlight_player", "spotlight_chrome"
        )

        private val YT_WATCH_PLAYER_IDS = listOf(
            "next_gen_watch_layout", "watch_player", "watch_while_time_bar",
            "player_control_play_pause", "fullscreen_button", "player_fragment_container",
            "time_bar", "player_view"
        )

        private val STREAMING_APP_PACKAGES = listOf(
            "netflix", "hotstar", "jio.media.ondemand", "primevideo", "amazon.avod",
            "twitch", "sonyliv", "graymatrix.did", "crunchyroll", "dailymotion",
            "vimeo", "mxtech.videoplayer"
        )

        fun isLongFormVideoSignals(
            packageName: String,
            viewIds: Collection<String>,
            labels: Collection<String>,
            url: String? = null
        ): Boolean {
            if (isShortsOrReelsSignals(packageName, viewIds, labels, url)) {
                return false
            }
            val lowerPkg = packageName.lowercase()
            if (STREAMING_APP_PACKAGES.any { lowerPkg.contains(it) }) {
                return true
            }
            val lowerIds = viewIds.map { it.lowercase() }
            val lowerLabels = labels.map { it.lowercase() }
            return when (lowerPkg) {
                "com.google.android.youtube" -> {
                    val isMiniPlayer = lowerLabels.any {
                        it.contains("close miniplayer") || it.contains("expand miniplayer")
                    }
                    if (isMiniPlayer) return false
                    val hasWatchId = lowerIds.any { id -> YT_WATCH_PLAYER_IDS.any { target -> id.contains(target) } }
                    val hasWatchLabel = lowerLabels.any {
                        it.contains("enter fullscreen") ||
                            it.contains("exit fullscreen") ||
                            it.contains("pause video") ||
                            it.contains("play video") ||
                            it.contains("playback speed")
                    }
                    val isHomeSearch = lowerLabels.any { it.contains("search youtube") } && !hasWatchLabel
                    hasWatchLabel || (hasWatchId && !isHomeSearch)
                }
                else -> {
                    val lowerUrl = url?.lowercase().orEmpty()
                    lowerUrl.contains("youtube.com/watch") ||
                        lowerUrl.contains("youtu.be/") ||
                        lowerUrl.contains("netflix.com/watch") ||
                        lowerUrl.contains("hotstar.com/") ||
                        lowerUrl.contains("primevideo.com") ||
                        lowerUrl.contains("twitch.tv/")
                }
            }
        }

        fun isShortsOrReelsSignals(
            packageName: String,
            viewIds: Collection<String>,
            labels: Collection<String>,
            url: String? = null
        ): Boolean {
            val lowerPkg = packageName.lowercase()
            if (lowerPkg == "com.zhiliaoapp.musically" || lowerPkg == "com.ss.android.ugc.trill") {
                return true
            }

            val lowerIds = viewIds.map { it.lowercase() }
            val lowerLabels = labels.map { it.lowercase() }

            return when (lowerPkg) {
                "com.google.android.youtube" -> {
                    if (lowerIds.any { id -> YT_PLAYER_IDS.any { target -> id.contains(target) } }) {
                        return true
                    }
                    val hasRemix = lowerLabels.any { it == "remix" || it.contains("remix this") || it.contains("shorts remix") }
                    val hasReaction = lowerLabels.any {
                        it.contains("dislike this video") || it.contains("like this video") || it == "dislike"
                    }
                    val hasComments = lowerLabels.any { it == "comments" || it.contains("open comments") || it.contains("view comments") }
                    val hasWatchFullscreen = lowerLabels.any {
                        it.contains("enter fullscreen") || it.contains("exit fullscreen") || it.contains("minimize player")
                    }
                    (hasRemix && (hasReaction || hasComments) && !hasWatchFullscreen) ||
                        (hasReaction && hasComments && !hasWatchFullscreen && lowerLabels.none { it.contains("search youtube") })
                }
                "com.instagram.android", "com.instagram.lite" -> {
                    if (lowerIds.any { id -> IG_REELS_IDS.any { target -> id.contains(target) } }) {
                        return true
                    }
                    val hasAudioCue = lowerLabels.any {
                        it.contains("original audio") || it.contains("use audio") || it.contains("reels camera")
                    }
                    val hasReelsActions = lowerLabels.any { it.contains("like") } && lowerLabels.any { it.contains("comment") }
                    val notFeedOrDm = lowerLabels.none {
                        it.contains("your story") || it.contains("search and explore")
                    }
                    hasAudioCue && hasReelsActions && notFeedOrDm
                }
                "com.facebook.katana", "com.facebook.lite" -> {
                    if (lowerIds.any { id -> FB_REELS_IDS.any { target -> id.contains(target) } }) {
                        return true
                    }
                    val hasReelsCue = lowerLabels.any {
                        it == "reels" || it.contains("original audio") || it.contains("remix this reel")
                    }
                    val hasComment = lowerLabels.any { it.contains("comment") }
                    val notNewsFeed = lowerLabels.none {
                        it.contains("what's on your mind") || it.contains("create post")
                    }
                    hasReelsCue && hasComment && notNewsFeed
                }
                "com.snapchat.android" -> {
                    if (lowerIds.any { id -> SNAP_SPOTLIGHT_IDS.any { target -> id.contains(target) } }) {
                        return true
                    }
                    lowerLabels.any { it.contains("spotlight") } &&
                        lowerLabels.any { it.contains("remix") || it.contains("comment") || it.contains("share") }
                }
                else -> {
                    val lowerUrl = url?.lowercase().orEmpty()
                    lowerUrl.contains("/shorts") ||
                        lowerUrl.contains("/reel") ||
                        lowerUrl.contains("/reels") ||
                        lowerUrl.contains("tiktok.com") ||
                    lowerUrl.contains("snapchat.com/spotlight")
                }
            }
        }

        fun isYouTubeHomeFeedSignals(
            packageName: String,
            viewIds: Collection<String>,
            labels: Collection<String>,
            selectedLabels: Collection<String>
        ): Boolean {
            if (packageName.lowercase() != "com.google.android.youtube") return false
            if (isShortsOrReelsSignals(packageName, viewIds, labels, null)) return false
            if (isLongFormVideoSignals(packageName, viewIds, labels, null)) return false

            val lowerIds = viewIds.map { it.lowercase() }
            if (lowerIds.any { it.contains("search_edit_text") || it.contains("search_results") }) {
                return false
            }
            val lowerSel = selectedLabels.map { it.lowercase() }
            if (lowerSel.any { it == "subscriptions" || it == "you" || it == "library" }) {
                return false
            }
            return lowerSel.any { it == "home" }
        }

        fun isInstagramExploreSignals(
            packageName: String,
            viewIds: Collection<String>,
            labels: Collection<String>,
            selectedLabels: Collection<String>
        ): Boolean {
            val lowerPkg = packageName.lowercase()
            if (lowerPkg != "com.instagram.android" && lowerPkg != "com.instagram.lite") return false
            if (isShortsOrReelsSignals(packageName, viewIds, labels, null)) return false

            val lowerIds = viewIds.map { it.lowercase() }
            val lowerLabels = labels.map { it.lowercase() }
            val lowerSel = selectedLabels.map { it.lowercase() }

            if (lowerIds.any { it.contains("direct_inbox") || it.contains("direct_thread") }) {
                return false
            }
            return lowerSel.any { it.contains("search and explore") || it == "explore" } ||
                lowerIds.any {
                    it.contains("explore_grid") ||
                        it.contains("discovery_grid") ||
                        it.contains("search_explore_recycler")
                } ||
                (lowerLabels.any { it.contains("search and explore") } && lowerIds.any { it.contains("discovery") })
        }

        fun isSocialAlgorithmicFeedSignals(
            packageName: String,
            viewIds: Collection<String>,
            labels: Collection<String>,
            selectedLabels: Collection<String>
        ): Boolean {
            val lowerPkg = packageName.lowercase()
            val lowerIds = viewIds.map { it.lowercase() }
            val lowerSel = selectedLabels.map { it.lowercase() }

            return when (lowerPkg) {
                "com.twitter.android" -> lowerSel.any { it == "for you" }
                "com.linkedin.android" -> {
                    val onSafeTab = lowerSel.any {
                        it.contains("messaging") || it.contains("jobs") || it.contains("network") || it.contains("notifications")
                    }
                    !onSafeTab && (lowerSel.any { it == "home" } || lowerIds.any { it.contains("feed_recycler") || it.contains("feed_list") })
                }
                else -> false
            }
        }

        fun matchesBlockedDomain(
            url: String?,
            blockedDomains: Collection<String>,
            blockShortUrls: Boolean
        ): String? {
            val lowerUrl = url?.lowercase()?.trim().orEmpty()
            if (lowerUrl.isEmpty()) return null
            if (blockShortUrls && (
                lowerUrl.contains("/shorts") ||
                    lowerUrl.contains("/reel") ||
                    lowerUrl.contains("/reels") ||
                    lowerUrl.contains("tiktok.com") ||
                    lowerUrl.contains("snapchat.com/spotlight")
                )
            ) {
                return "short-form video"
            }
            for (rawDomain in blockedDomains) {
                val domain = rawDomain.lowercase().trim()
                if (domain.isNotEmpty() && lowerUrl.contains(domain)) {
                    return domain
                }
            }
            return null
        }

        /**
         * Scans all visible application window packages (including Split-Screen & Picture-in-Picture
         * floating windows) to detect disallowed apps running alongside allowed screens.
         */
        fun findDisallowedVisibleWindowPackage(
            visibleAppWindowPackages: Collection<String>,
            selfPackageName: String,
            isLauncherCheck: (String) -> Boolean,
            isAllowedCheck: (String) -> Boolean
        ): String? {
            for (rawPkg in visibleAppWindowPackages) {
                val pkg = rawPkg.trim()
                if (pkg.isEmpty() || pkg == selfPackageName || pkg == "com.android.systemui") continue
                if (isLauncherCheck(pkg)) continue
                if (!isAllowedCheck(pkg)) {
                    return pkg
                }
            }
            return null
        }
    }

    override fun onCreate() {
        super.onCreate()
        sessionManager = FocusSessionManager.getInstance(this)
        reelsOverlay = ReelsFloatingCounterOverlay(this)
        lastScreenMinuteTimestamp = System.currentTimeMillis()
        loadLauncherPackages()
        startHolidayTimeTicker()
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        settingsCheckJob?.cancel()
        serviceScope.cancel()
        reelsOverlay?.destroy()
        reelsOverlay = null
        super.onDestroy()
    }

    /**
     * Continuous background ticker:
     * 1. Tracks active screen-on time across idle, Travel, and Holiday modes (capped below daily goal).
     * 2. Eliminates leisure time-tracking undercount on full-length videos (YouTube / Netflix / Hotstar).
     * 3. Dismisses floating Reels pill immediately if the screen turns off.
     */
    private fun startHolidayTimeTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(20000L) // Check every 20 seconds
                val isInteractive = (getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isInteractive == true
                val now = System.currentTimeMillis()
                if (!isInteractive) {
                    lastForegroundTimestamp = now
                    lastScreenMinuteTimestamp = now
                    lastLongVideoMinuteTimestamp = now
                    activeLongVideoPackage = null
                    lastForegroundApp = null
                    lastTrackedIdleApp = null
                    reelsOverlay?.hide()
                } else {
                    if (sessionManager.shouldBlockApps()) {
                        checkMultiWindowViolationDuringFocus()
                    }
                    if (lastScreenMinuteTimestamp == 0L) {
                        lastScreenMinuteTimestamp = now
                    } else if (now - lastScreenMinuteTimestamp >= 60000L) {
                        val elapsedScreenMins = ((now - lastScreenMinuteTimestamp) / 60000L).toInt()
                        lastScreenMinuteTimestamp = now - ((now - lastScreenMinuteTimestamp) % 60000L)
                        val fgApp = lastForegroundApp
                        val inBlockingMode = sessionManager.shouldBlockApps()
                        val isSelf = fgApp == applicationContext.packageName
                        if (!inBlockingMode && !isSelf) {
                            val isDistracting = fgApp != null && (isLeisureApp(fgApp) || isBrowserApp(fgApp))
                            sessionManager.addScreenMinutesUsed(elapsedScreenMins, isDistracting = isDistracting)
                            if (fgApp != null && !sessionManager.isSystemFrameworkOrInputService(fgApp) && !isLauncher(fgApp)) {
                                sessionManager.addAppMinutesUsed(fgApp, elapsedScreenMins)
                            }
                            if (sessionManager.isTravelModeActive()) {
                                sessionManager.addTravelScreenMinutesUsed(elapsedScreenMins)
                                if (sessionManager.isTravelLifeReminderDue()) {
                                    triggerHolidayInterrupter(HolidayInterrupterActivity.MODE_TRAVEL)
                                }
                            } else if (sessionManager.isDailyGoalCheckInDue()) {
                                triggerHolidayInterrupter(HolidayInterrupterActivity.MODE_DAILY_LIMIT)
                            }
                        }
                    }

                    val activeIdlePkg = lastForegroundApp
                    if (!sessionManager.shouldBlockApps() &&
                        activeIdlePkg != null &&
                        activeIdlePkg != applicationContext.packageName &&
                        !BlockingOverlayActivity.isCooldownPromptVisible &&
                        isActiveRootPackage(activeIdlePkg)
                    ) {
                        val capBreach = sessionManager.getAppDailyCapBreachReason(activeIdlePkg, isNewEntry = false)
                        if (capBreach != null) {
                            sessionManager.recordShieldDiagnostic("APP_DAILY_CAP", activeIdlePkg, capBreach)
                            triggerAppCapInterrupter(activeIdlePkg, capBreach)
                        } else if (sessionManager.isCooldownRequiredForPackage(activeIdlePkg) &&
                            (now - lastCooldownPromptTimestamp > 8000L)
                        ) {
                            launchCooldownPrompt(
                                packageName = activeIdlePkg,
                                nowMs = now,
                                diagCategory = "COOLDOWN_EXPIRED",
                                diagDetail = "Chosen mindful window expired"
                            )
                        }
                    }

                    if (!sessionManager.shouldBlockApps() && activeLongVideoPackage != null) {
                        if (lastLongVideoMinuteTimestamp == 0L) {
                            lastLongVideoMinuteTimestamp = now
                        } else if (now - lastLongVideoMinuteTimestamp >= 60000L) {
                            val mins = ((now - lastLongVideoMinuteTimestamp) / 60000L).toInt()
                            lastLongVideoMinuteTimestamp = now - ((now - lastLongVideoMinuteTimestamp) % 60000L)
                            sessionManager.addLongVideoMinutesUsed(mins)
                        }
                        if (!wasInShortsPlayer) {
                            updateFloatingLongVideoOverlay(true)
                        }
                    }

                    if (sessionManager.isHolidayToday()) {
                        val fgApp = lastForegroundApp
                        if (fgApp != null && isLeisureApp(fgApp) && !sessionManager.isHolidaySnoozed()) {
                            val elapsed = now - lastForegroundTimestamp
                            if (elapsed >= 60000L) {
                                val mins = (elapsed / 60000L).toInt()
                                sessionManager.addHolidayMinutesUsed(mins)
                                lastForegroundTimestamp = now - (elapsed % 60000L)
                                if (activeLongVideoPackage != null && !wasInShortsPlayer) {
                                    updateFloatingLongVideoOverlay(true)
                                }
                                if (sessionManager.isHolidayLimitReached()) {
                                    triggerHolidayInterrupter(HolidayInterrupterActivity.MODE_HOLIDAY)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            eventType != AccessibilityEvent.TYPE_VIEW_CLICKED &&
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }

        if (eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            if (sessionManager.shouldBlockApps()) {
                if (checkMultiWindowViolationDuringFocus()) return
            } else {
                for (visiblePkg in getVisibleApplicationWindowPackages()) {
                    val capBreach = sessionManager.getAppDailyCapBreachReason(visiblePkg, isNewEntry = false)
                    if (capBreach != null) {
                        sessionManager.recordShieldDiagnostic("APP_DAILY_CAP", visiblePkg, "Floating/PiP cap: $capBreach")
                        triggerAppCapInterrupter(visiblePkg, capBreach)
                        return
                    }
                }
            }
        }

        val packageName = event.packageName?.toString() ?: return

        // 1. Ignore our own app & overlay
        if (packageName == applicationContext.packageName) return

        // 2. Always ignore System UI (status bar, volume dialog, notification shade)
        if (packageName == "com.android.systemui") return

        // -------------------------------------------------------------
        // 3. CHECK HIGH FOCUS & ULTRA FOCUS LOCKS
        // -------------------------------------------------------------
        if (sessionManager.shouldBlockApps()) {
            wasInShortsPlayer = false
            lastShortSignature = ""
            reelsOverlay?.hide()
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                lastForegroundApp = packageName
                lastForegroundTimestamp = System.currentTimeMillis()
            }
            // Smart Settings / Package Installer Guard (check both window transitions & Settings scroll)
            if (isAlwaysBlockedInstallerOrSecurity(packageName)) {
                if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                    (sessionManager.isStrictMode() || sessionManager.isUltraFocusActive())
                ) {
                    Log.d(TAG, "Blocking package installer / OEM cleaner during active focus: $packageName")
                    sessionManager.recordShieldDiagnostic("SETTINGS_TAMPER", packageName, "Blocked installer/cleaner")
                    blockApp(packageName, isSettingsAttempt = true)
                }
                return
            }

            if (isSystemSettingsPackage(packageName)) {
                if ((eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                        eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) &&
                    (sessionManager.isStrictMode() || sessionManager.isUltraFocusActive())
                ) {
                    evaluateSettingsTamper(packageName, event)
                }
                return
            }

            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (checkMultiWindowViolationDuringFocus()) return

                // If user presses Home or goes to Launcher during active focus
                if (isLauncher(packageName)) {
                    if (System.currentTimeMillis() - BlockingOverlayActivity.lastLaunchedAllowedTime < 3000L) {
                        return
                    }
                    Log.d(TAG, "Launcher active during focus - showing Minimalist Launcher")
                    blockApp(packageName, isSettingsAttempt = false)
                    return
                }

                // Check whitelist (user apps + essential OS utilities, QR scanner, 2FA, biometrics, etc.)
                val isAllowed = sessionManager.isAppAllowed(packageName)
                if (!isAllowed) {
                    Log.d(TAG, "Blocking unapproved app during focus: $packageName")
                    sessionManager.recordShieldDiagnostic("APP_BLOCK", packageName, "Blocked during active Focus")
                    blockApp(packageName, isSettingsAttempt = false)
                    return
                }
            }

            if (isShortFormCandidateApp(packageName) || isBrowserApp(packageName)) {
                val nowMs = System.currentTimeMillis()
                if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                    nowMs - lastContentScanTimestamp < 250L
                ) {
                    return
                }
                lastContentScanTimestamp = nowMs
                val treeInfo = inspectActiveAppTree(packageName, event)
                checkSurgicalShortsOrReels(packageName, treeInfo)
                checkBrowserGuardsWithTree(packageName, treeInfo, enforceFocusDomainBlock = true)
            }
            return
        }

        // Per-App Daily Caps & ScreenZen Mindful Open Cooldown in Everyday / No Mode
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val nowMs = System.currentTimeMillis()
            if (isLauncher(packageName)) {
                lastTrackedIdleApp = null
            } else if (!sessionManager.isSystemFrameworkOrInputService(packageName) &&
                !isSystemSettingsPackage(packageName) &&
                !isAlwaysBlockedInstallerOrSecurity(packageName)
            ) {
                val isNewEntry = packageName != lastTrackedIdleApp
                val breachReason = sessionManager.getAppDailyCapBreachReason(packageName, isNewEntry = isNewEntry)
                if (breachReason != null) {
                    sessionManager.recordShieldDiagnostic("APP_DAILY_CAP", packageName, breachReason)
                    triggerAppCapInterrupter(packageName, breachReason)
                    return
                }
                if (sessionManager.isCooldownRequiredForPackage(packageName) &&
                    (packageName != lastCooldownPromptPackage || nowMs - lastCooldownPromptTimestamp > 1200L)
                ) {
                    launchCooldownPrompt(
                        packageName = packageName,
                        nowMs = nowMs,
                        diagCategory = "COOLDOWN_GATE",
                        diagDetail = "Mindful open delay triggered"
                    )
                    return
                }
                if (isNewEntry) {
                    val recentlyCountedByCooldown =
                        BlockingOverlayActivity.lastCooldownGrantPackage == packageName &&
                            (nowMs - BlockingOverlayActivity.lastCooldownGrantTime) < 4000L
                    if (!recentlyCountedByCooldown) {
                        sessionManager.recordAppOpen(packageName)
                    }
                    lastTrackedIdleApp = packageName
                }
            }
        }

        // Non-blocking fast exit for background content churn in non-media apps
        val isHoliday = sessionManager.isHolidayToday()
        val isShortCandidate = isShortFormCandidateApp(packageName)
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && !isShortCandidate) {
            return
        }

        val now = System.currentTimeMillis()
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && now - lastContentScanTimestamp < 220L) {
            return
        }
        if (isShortCandidate) {
            lastContentScanTimestamp = now
        }

        val treeInfo = if (isShortCandidate) inspectActiveAppTree(packageName, event) else null
        if (treeInfo != null && isBrowserApp(packageName)) {
            checkBrowserGuardsWithTree(
                packageName = packageName,
                treeInfo = treeInfo,
                enforceFocusDomainBlock = sessionManager.isAlwaysBlockDomainsEnabled()
            )
        }

        val isShortsActive = isShortCandidate && isShortsOrReelsSignals(
            packageName = packageName,
            viewIds = treeInfo?.viewIds ?: emptySet(),
            labels = treeInfo?.labels ?: emptySet(),
            url = treeInfo?.urlBarText
        )

        val isLongVideoActive = isShortCandidate && !isShortsActive && isLongFormVideoSignals(
            packageName = packageName,
            viewIds = treeInfo?.viewIds ?: emptySet(),
            labels = treeInfo?.labels ?: emptySet(),
            url = treeInfo?.urlBarText
        )

        if (!isShortCandidate && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            wasInShortsPlayer = false
            lastShortSignature = ""
            activeLongVideoPackage = null
            reelsOverlay?.hide()
        }

        // -------------------------------------------------------------
        // 4. CHECK HOLIDAY MODE (Conscious Leisure on Days Off / Weekends)
        // -------------------------------------------------------------
        if (isHoliday) {
            // Essential apps (Phone, WhatsApp, Settings, Clock, Notes, Work) are NEVER blocked!
            if (!isLeisureApp(packageName) && !isShortsActive && !isLongVideoActive) {
                val prevApp = lastForegroundApp
                if (prevApp != null && isLeisureApp(prevApp)) {
                    val elapsed = now - lastForegroundTimestamp
                    if (elapsed >= 30000L) {
                        val mins = ((elapsed + 30000L) / 60000L).toInt().coerceAtLeast(1)
                        sessionManager.addHolidayMinutesUsed(mins)
                    }
                }
                if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    lastForegroundApp = packageName
                    lastForegroundTimestamp = now
                }
                wasInShortsPlayer = false
                lastShortSignature = ""
                activeLongVideoPackage = null
                reelsOverlay?.hide()
                return
            }

            if (isShortsActive) {
                activeLongVideoPackage = null
                val sig = treeInfo?.signature.orEmpty()
                val didAdvance = when {
                    eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && now - lastScrollTimestamp > 750L -> true
                    wasInShortsPlayer && sig.isNotEmpty() && sig != lastShortSignature && now - lastScrollTimestamp > 1200L -> true
                    else -> false
                }
                if (didAdvance) {
                    lastScrollTimestamp = now
                    lastShortSignature = sig
                    sessionManager.incrementHolidayReelsScrolled()
                    if (!sessionManager.isHolidaySnoozed() && sessionManager.isHolidayLimitReached()) {
                        triggerHolidayInterrupter(HolidayInterrupterActivity.MODE_HOLIDAY)
                    }
                } else if (!wasInShortsPlayer || (now - lastScrollTimestamp <= 1200L && sig.isNotEmpty())) {
                    lastShortSignature = sig
                }
                wasInShortsPlayer = true
                updateFloatingReelsOverlay(true)
            } else if (isLongVideoActive) {
                wasInShortsPlayer = false
                lastShortSignature = ""
                if (activeLongVideoPackage != packageName) {
                    activeLongVideoPackage = packageName
                    lastLongVideoMinuteTimestamp = now
                }
                updateFloatingLongVideoOverlay(true)
            } else {
                wasInShortsPlayer = false
                lastShortSignature = ""
                activeLongVideoPackage = null
                reelsOverlay?.hide()
            }

            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (lastForegroundApp == packageName) {
                    val elapsed = now - lastForegroundTimestamp
                    if (elapsed >= 60000L) {
                        val mins = (elapsed / 60000L).toInt()
                        if (!sessionManager.isHolidaySnoozed()) {
                            sessionManager.addHolidayMinutesUsed(mins)
                        }
                        lastForegroundTimestamp = now
                    }
                } else {
                    val prevApp = lastForegroundApp
                    if (prevApp != null && isLeisureApp(prevApp) && !sessionManager.isHolidaySnoozed()) {
                        val prevElapsed = now - lastForegroundTimestamp
                        if (prevElapsed >= 30000L) {
                            val mins = ((prevElapsed + 30000L) / 60000L).toInt().coerceAtLeast(1)
                            sessionManager.addHolidayMinutesUsed(mins)
                        }
                    }
                    lastForegroundApp = packageName
                    lastForegroundTimestamp = now
                }

                if (!sessionManager.isHolidaySnoozed() && sessionManager.isHolidayLimitReached()) {
                    triggerHolidayInterrupter(HolidayInterrupterActivity.MODE_HOLIDAY)
                }
            }
            return
        }

        // -------------------------------------------------------------
        // 5. TRAVEL & EVERYDAY MINDFULNESS TRACKING (Cute Reels & Video Minutes Counter)
        // -------------------------------------------------------------
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            lastForegroundApp = packageName
            lastForegroundTimestamp = now
        }

        if (isShortsActive) {
            activeLongVideoPackage = null
            val sig = treeInfo?.signature.orEmpty()
            val didAdvance = when {
                eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && now - lastScrollTimestamp > 750L -> true
                wasInShortsPlayer && sig.isNotEmpty() && sig != lastShortSignature && now - lastScrollTimestamp > 1200L -> true
                else -> false
            }
            if (didAdvance) {
                lastScrollTimestamp = now
                lastShortSignature = sig
                if (sessionManager.isTravelModeActive()) {
                    sessionManager.incrementTravelReelsScrolled()
                    if (sessionManager.isTravelLifeReminderDue()) {
                        triggerHolidayInterrupter(HolidayInterrupterActivity.MODE_TRAVEL)
                    }
                } else {
                    sessionManager.recordEverydayReelScrolled()
                }
            } else if (!wasInShortsPlayer || (now - lastScrollTimestamp <= 1200L && sig.isNotEmpty())) {
                lastShortSignature = sig
            }
            wasInShortsPlayer = true
            updateFloatingReelsOverlay(true)
        } else if (isLongVideoActive) {
            wasInShortsPlayer = false
            lastShortSignature = ""
            if (activeLongVideoPackage != packageName) {
                activeLongVideoPackage = packageName
                lastLongVideoMinuteTimestamp = now
            }
            updateFloatingLongVideoOverlay(true)
        } else {
            wasInShortsPlayer = false
            lastShortSignature = ""
            activeLongVideoPackage = null
            reelsOverlay?.hide()
        }
    }

    private data class ScreenTreeInfo(
        val viewIds: Set<String>,
        val labels: Set<String>,
        val selectedLabels: Set<String>,
        val signature: String,
        val urlBarText: String?,
        val hasIncognitoSignal: Boolean
    )

    private fun isShortFormCandidateApp(pkg: String): Boolean {
        val lower = pkg.lowercase()
        return lower == "com.google.android.youtube" ||
            lower == "com.instagram.android" ||
            lower == "com.instagram.lite" ||
            lower == "com.zhiliaoapp.musically" ||
            lower == "com.ss.android.ugc.trill" ||
            lower == "com.facebook.katana" ||
            lower == "com.facebook.lite" ||
            lower == "com.snapchat.android" ||
            lower == "com.twitter.android" ||
            lower == "com.linkedin.android" ||
            STREAMING_APP_PACKAGES.any { lower.contains(it) } ||
            isBrowserApp(lower)
    }

    private fun inspectActiveAppTree(packageName: String, event: AccessibilityEvent): ScreenTreeInfo? {
        val activeRoot = rootInActiveWindow
        val rootNode: AccessibilityNodeInfo? = when {
            activeRoot != null && activeRoot.packageName?.toString() == packageName -> activeRoot
            else -> {
                var windowRoot: AccessibilityNodeInfo? = null
                try {
                    for (win in windows) {
                        if (win.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                            val candidate = win.root
                            if (candidate?.packageName?.toString() == packageName) {
                                windowRoot = candidate
                                break
                            }
                        }
                    }
                } catch (_: Exception) {
                }
                windowRoot ?: event.source
            }
        }
        if (rootNode == null) return null

        val viewIds = mutableSetOf<String>()
        val labels = mutableSetOf<String>()
        val selectedLabels = mutableSetOf<String>()
        var signature = ""
        var urlBar: String? = null
        var hasIncognito = false
        var visitedCount = 0

        val ignoredSignatureLabels = setOf(
            "like", "dislike", "comment", "comments", "share", "remix", "subscribe",
            "subscribed", "follow", "following", "reels", "shorts", "search",
            "more", "save", "home", "subscriptions", "library", "you", "back"
        )

        fun visit(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || depth > 16 || visitedCount > 240) return
            visitedCount++

            val vid = node.viewIdResourceName?.lowercase().orEmpty()
            if (vid.isNotEmpty()) {
                viewIds.add(vid)
                if (vid.contains("incognito_toggle_button") || vid.contains("incognito_tab")) {
                    hasIncognito = true
                }
                if (urlBar == null &&
                    (vid.contains("url_bar") || vid.contains("location_bar") || vid.contains("search_box_text"))
                ) {
                    val t = node.text?.toString()
                    if (!t.isNullOrBlank()) urlBar = t
                }
            }

            val rawText = node.text?.toString()?.trim().orEmpty()
            val rawDesc = node.contentDescription?.toString()?.trim().orEmpty()
            val isNodeSelected = node.isSelected
            for (raw in listOf(rawText, rawDesc)) {
                if (raw.isNotEmpty() && raw.length <= 90) {
                    val lower = raw.lowercase()
                    labels.add(lower)
                    if (isNodeSelected) {
                        selectedLabels.add(lower)
                    }
                    if (lower.contains("you've gone incognito") ||
                        lower.contains("close incognito tabs") ||
                        lower.contains("secret mode") ||
                        lower.contains("private tab")
                    ) {
                        hasIncognito = true
                    }
                    val looksLikeHandle = raw.startsWith("@") && raw.length in 2..40
                    val looksLikeTitle = raw.length in 6..60 &&
                        !ignoredSignatureLabels.contains(lower) &&
                        !Regex("^[0-9:.,KMB+%\\s/-]+$").matches(raw)
                    if (signature.isEmpty() && looksLikeHandle) {
                        signature = lower
                    } else if (signature.isEmpty() && looksLikeTitle) {
                        signature = lower
                    }
                }
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                visit(child, depth + 1)
            }
        }

        visit(rootNode, 0)
        return ScreenTreeInfo(
            viewIds = viewIds,
            labels = labels,
            selectedLabels = selectedLabels,
            signature = signature,
            urlBarText = urlBar,
            hasIncognitoSignal = hasIncognito
        )
    }

    private fun checkSurgicalShortsOrReels(packageName: String, treeInfo: ScreenTreeInfo?) {
        val ids = treeInfo?.viewIds ?: emptySet()
        val labs = treeInfo?.labels ?: emptySet()
        val sel = treeInfo?.selectedLabels ?: emptySet()
        val url = treeInfo?.urlBarText

        if (sessionManager.isBlockShortsInFocus() &&
            isShortsOrReelsSignals(packageName = packageName, viewIds = ids, labels = labs, url = url)
        ) {
            sessionManager.recordShieldDiagnostic("SHORTS_REELS_BLOCK", packageName, "Surgical exit from short-form player")
            performGlobalAction(GLOBAL_ACTION_BACK)
            showToastThrottled("⚡ Short-form Reels / Shorts blocked during Focus.")
            return
        }

        if (sessionManager.isBlockYtHomeFeed() &&
            isYouTubeHomeFeedSignals(packageName = packageName, viewIds = ids, labels = labs, selectedLabels = sel)
        ) {
            sessionManager.recordShieldDiagnostic("YT_HOME_BLOCK", packageName, "Blocked YouTube Home recommendations")
            performGlobalAction(GLOBAL_ACTION_BACK)
            showToastThrottled("🎯 YouTube Home feed blocked. Use Search or Subscriptions for study.")
            return
        }

        if (sessionManager.isBlockIgExplore() &&
            isInstagramExploreSignals(packageName = packageName, viewIds = ids, labels = labs, selectedLabels = sel)
        ) {
            sessionManager.recordShieldDiagnostic("IG_EXPLORE_BLOCK", packageName, "Blocked Instagram Explore tab")
            performGlobalAction(GLOBAL_ACTION_BACK)
            showToastThrottled("🎯 Instagram Explore blocked. DMs and Profile only.")
            return
        }

        if (sessionManager.isBlockSocialAlgoFeeds() &&
            isSocialAlgorithmicFeedSignals(packageName = packageName, viewIds = ids, labels = labs, selectedLabels = sel)
        ) {
            sessionManager.recordShieldDiagnostic("ALGO_FEED_BLOCK", packageName, "Cleaned X/LinkedIn feed")
            performGlobalAction(GLOBAL_ACTION_BACK)
            showToastThrottled("🎯 Algorithmic feed cleaned. Search or message intentionally.")
        }
    }

    private fun checkBrowserGuardsWithTree(
        packageName: String,
        treeInfo: ScreenTreeInfo?,
        enforceFocusDomainBlock: Boolean = false
    ) {
        if (treeInfo == null || !isBrowserApp(packageName)) return
        if (sessionManager.isBlockIncognitoEnabled() && treeInfo.hasIncognitoSignal) {
            sessionManager.recordShieldDiagnostic("BROWSER_GUARD", packageName, "Closed Incognito/Private tab")
            performGlobalAction(GLOBAL_ACTION_BACK)
            showToastThrottled("🔒 Incognito / Private browsing is blocked by GetBackToWork.")
            return
        }
        val urlText = treeInfo.urlBarText?.lowercase() ?: return
        if (sessionManager.isAdultFilterEnabled()) {
            for (domain in ADULT_DOMAINS) {
                if (urlText.contains(domain)) {
                    sessionManager.recordShieldDiagnostic("BROWSER_GUARD", packageName, "Adult site filter: $domain")
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    showToastThrottled("🛡️ Explicit website blocked by Adult Content Shield.")
                    return
                }
            }
        }
        if (enforceFocusDomainBlock) {
            val matched = matchesBlockedDomain(
                url = urlText,
                blockedDomains = sessionManager.getBlockedDomains(),
                blockShortUrls = sessionManager.shouldBlockApps() && sessionManager.isBlockShortsInFocus()
            )
            if (matched != null) {
                sessionManager.recordShieldDiagnostic("BROWSER_GUARD", packageName, "Blocked domain: $matched")
                performGlobalAction(GLOBAL_ACTION_BACK)
                showToastThrottled("🌐 Blocked site ($matched) during Focus Shield.")
            }
        }
    }

    private fun updateFloatingReelsOverlay(isWatchingReels: Boolean) {
        if (!isWatchingReels || !sessionManager.isReelsOverlayEnabled() || sessionManager.shouldBlockApps()) {
            reelsOverlay?.hide()
            return
        }
        when {
            sessionManager.isHolidayToday() -> {
                val holiday = sessionManager.holidayStateFlow.value
                val limit = if (holiday.limitType == FocusSessionManager.LIMIT_TYPE_REELS) holiday.limitValue else 0
                reelsOverlay?.show(
                    count = sessionManager.getHolidayReelsScrolled(),
                    limit = limit,
                    modeLabel = "HOLIDAY"
                )
            }
            sessionManager.isTravelModeActive() -> {
                reelsOverlay?.show(
                    count = sessionManager.getTravelReelsScrolled(),
                    limit = 25,
                    modeLabel = "TRAVEL"
                )
            }
            else -> {
                reelsOverlay?.show(
                    count = sessionManager.getTodayReelsWatched(),
                    limit = 0,
                    modeLabel = "DAILY"
                )
            }
        }
    }

    private fun updateFloatingLongVideoOverlay(isWatchingVideo: Boolean) {
        if (!isWatchingVideo || !sessionManager.isReelsOverlayEnabled() || sessionManager.shouldBlockApps()) {
            reelsOverlay?.hide()
            return
        }
        when {
            sessionManager.isHolidayToday() -> {
                val holiday = sessionManager.holidayStateFlow.value
                val limit = if (holiday.limitType == FocusSessionManager.LIMIT_TYPE_MINUTES) holiday.limitValue else 0
                reelsOverlay?.showMinutes(
                    minutes = sessionManager.getHolidayMinutesUsed(),
                    limitMinutes = limit,
                    modeLabel = "HOLIDAY"
                )
            }
            sessionManager.isTravelModeActive() -> {
                reelsOverlay?.showMinutes(
                    minutes = sessionManager.getTravelScreenMinutesUsed(),
                    limitMinutes = 30,
                    modeLabel = "TRAVEL"
                )
            }
            else -> {
                reelsOverlay?.showMinutes(
                    minutes = sessionManager.getLongVideoMinutesToday(),
                    limitMinutes = sessionManager.getDailyScreenGoalMinutes(),
                    modeLabel = "VIDEO"
                )
            }
        }
    }

    /**
     * Inspects active browser windows for Incognito tabs or explicit adult content.
     */
    private fun checkBrowserGuards(packageName: String) {
        if (!isBrowserApp(packageName)) return

        val root = rootInActiveWindow ?: return
        try {
            // 1. Incognito / Private Tab Blocker
            if (sessionManager.isBlockIncognitoEnabled()) {
                val hasIncognito = findNodeByIdOrText(
                    root,
                    idMatches = listOf("incognito_toggle_button", "incognito_tab"),
                    textMatches = listOf("You've gone incognito", "Close incognito tabs", "Secret mode", "Private tab")
                )
                if (hasIncognito) {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    showToastThrottled("🔒 Incognito / Private browsing is blocked by GetBackToWork.")
                    return
                }
            }

            // 2. Explicit Adult Content Shield (On-Device URL / Title check)
            if (sessionManager.isAdultFilterEnabled()) {
                val urlText = extractUrlBarText(root)?.lowercase()
                if (urlText != null) {
                    for (domain in ADULT_DOMAINS) {
                        if (urlText.contains(domain)) {
                            performGlobalAction(GLOBAL_ACTION_BACK)
                            showToastThrottled("🛡️ Explicit website blocked by Adult Content Shield.")
                            return
                        }
                    }
                }
            }
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }
    }

    private fun isBrowserApp(pkg: String): Boolean {
        val lower = pkg.lowercase()
        return lower.contains("chrome") ||
               lower.contains("sbrowser") ||
               lower.contains("firefox") ||
               lower.contains("brave") ||
               lower.contains("opera") ||
               lower.contains("browser")
    }

    private fun findNodeByIdOrText(
        node: AccessibilityNodeInfo?,
        idMatches: List<String>,
        textMatches: List<String>
    ): Boolean {
        if (node == null) return false
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        for (target in idMatches) {
            if (viewId.contains(target.lowercase())) return true
        }

        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        for (target in textMatches) {
            val lowerTarget = target.lowercase()
            if (text.contains(lowerTarget) || desc.contains(lowerTarget)) {
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByIdOrText(child, idMatches, textMatches)
            @Suppress("DEPRECATION")
            child.recycle()
            if (found) return true
        }
        return false
    }

    private fun extractUrlBarText(node: AccessibilityNodeInfo?): String? {
        if (node == null) return null
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        if (viewId.contains("url_bar") || viewId.contains("location_bar") || viewId.contains("search_box_text")) {
            return node.text?.toString()
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = extractUrlBarText(child)
            @Suppress("DEPRECATION")
            child.recycle()
            if (result != null) return result
        }
        return null
    }

    private fun showToastThrottled(message: String) {
        val now = System.currentTimeMillis()
        if (now - lastSurgicalToastTimestamp > 3000L) {
            lastSurgicalToastTimestamp = now
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun triggerAppCapInterrupter(packageName: String, reason: String) {
        val now = System.currentTimeMillis()
        if (packageName == lastCapInterruptPkg && now - lastCapInterruptTimestamp < 3000L) return
        lastCapInterruptPkg = packageName
        lastCapInterruptTimestamp = now

        val rawLabel = try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
        }
        val appName = sessionManager.resolveAppName(packageName, rawLabel)

        val intent = Intent(this, HolidayInterrupterActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(HolidayInterrupterActivity.EXTRA_CHECKIN_MODE, HolidayInterrupterActivity.MODE_APP_CAP)
            putExtra(HolidayInterrupterActivity.EXTRA_CAP_APP_NAME, appName)
            putExtra(HolidayInterrupterActivity.EXTRA_CAP_REASON, reason)
        }
        startActivity(intent)
    }

    private fun launchCooldownPrompt(
        packageName: String,
        nowMs: Long,
        diagCategory: String,
        diagDetail: String
    ) {
        lastCooldownPromptPackage = packageName
        lastCooldownPromptTimestamp = nowMs
        sessionManager.recordShieldDiagnostic(diagCategory, packageName, diagDetail)
        val cooldownIntent = Intent(this, CooldownPromptActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(CooldownPromptActivity.EXTRA_TARGET_PACKAGE, packageName)
        }
        startActivity(cooldownIntent)
    }

    private fun isActiveRootPackage(targetPkg: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return try {
            root.packageName?.toString() == targetPkg
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }
    }

    private fun getVisibleApplicationWindowPackages(): List<String> {
        val result = mutableListOf<String>()
        try {
            val allWindows = windows ?: return emptyList()
            for (win in allWindows) {
                if (win.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                    val root = win.root
                    if (root != null) {
                        val pkg = root.packageName?.toString()
                        if (!pkg.isNullOrBlank()) result.add(pkg)
                        @Suppress("DEPRECATION")
                        root.recycle()
                    }
                }
            }
        } catch (_: Exception) {
        }
        return result
    }

    private fun checkMultiWindowViolationDuringFocus(): Boolean {
        val visiblePkgs = getVisibleApplicationWindowPackages()
        val violationPkg = findDisallowedVisibleWindowPackage(
            visibleAppWindowPackages = visiblePkgs,
            selfPackageName = applicationContext.packageName,
            isLauncherCheck = { pkg -> isLauncher(pkg) },
            isAllowedCheck = { pkg ->
                !isAlwaysBlockedInstallerOrSecurity(pkg) &&
                    !isSystemSettingsPackage(pkg) &&
                    sessionManager.isAppAllowed(pkg)
            }
        ) ?: return false

        val isSettingsOrInstaller = isAlwaysBlockedInstallerOrSecurity(violationPkg) || isSystemSettingsPackage(violationPkg)
        if (isSettingsOrInstaller && !(sessionManager.isStrictMode() || sessionManager.isUltraFocusActive())) {
            return false
        }
        val diagDetail = if (visiblePkgs.size > 1) {
            "Blocked Split-Screen / PiP window"
        } else {
            "Blocked window during Focus"
        }
        sessionManager.recordShieldDiagnostic("PIP_MULTIWINDOW_BLOCK", violationPkg, diagDetail)
        blockApp(violationPkg, isSettingsAttempt = isSettingsOrInstaller)
        return true
    }

    private fun triggerHolidayInterrupter(mode: String = HolidayInterrupterActivity.MODE_HOLIDAY) {
        val now = System.currentTimeMillis()
        if (now - lastInterruptedTimestamp < 5000L) return
        lastInterruptedTimestamp = now

        // Immediately snooze for 30 minutes so it CANNOT enter an auto-blocking loop!
        sessionManager.snoozeHolidayReminder(30)

        val intent = Intent(this, HolidayInterrupterActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(HolidayInterrupterActivity.EXTRA_CHECKIN_MODE, mode)
        }
        startActivity(intent)
    }

    private fun isLeisureApp(pkg: String): Boolean {
        val lower = pkg.lowercase()
        return lower == "com.zhiliaoapp.musically" ||
               lower == "com.ss.android.ugc.trill" ||
               lower.contains("instagram") ||
               lower.contains("youtube") ||
               lower.contains("netflix") ||
               lower.contains("hotstar") ||
               lower.contains("primevideo") ||
               lower.contains("amazon.avod") ||
               lower.contains("reddit") ||
               lower.contains("twitter") ||
               lower.contains("x.android") ||
               lower.contains("tiktok") ||
               lower.contains("facebook") ||
               lower.contains("snapchat") ||
               lower.contains("twitch") ||
               lower.contains("threads") ||
               lower.contains("pinterest")
    }

    private fun blockApp(blockedPackage: String, isSettingsAttempt: Boolean) {
        val now = System.currentTimeMillis()
        if (blockedPackage == lastBlockedPackage && (now - lastBlockedTimestamp) < 800L) {
            return
        }
        lastBlockedPackage = blockedPackage
        lastBlockedTimestamp = now

        val intent = Intent(this, BlockingOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(BlockingOverlayActivity.EXTRA_BLOCKED_PACKAGE, blockedPackage)
            putExtra(BlockingOverlayActivity.EXTRA_IS_SETTINGS, isSettingsAttempt)
        }
        startActivity(intent)
    }

    /**
     * Evaluates Settings screens selectively: permits Wi-Fi, Bluetooth, USB Preferences,
     * Developer Options (USB Debugging), Sound, Display, etc., while blocking tamper menus
     * (Accessibility, Device Admin, GetBackToWork App Info, Force Stop/Uninstall, Clock changes).
     */
    private fun evaluateSettingsTamper(packageName: String, event: AccessibilityEvent) {
        val className = event.className?.toString().orEmpty()
        val eventTitle = event.text.joinToString(" ") { it?.toString().orEmpty() }

        if (checkSettingsWindowForTamper(packageName, className, eventTitle)) {
            return
        }

        // Re-check shortly after transition once SubSettings RecyclerView/ActionBar populates
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            settingsCheckJob?.cancel()
            settingsCheckJob = serviceScope.launch {
                delay(180L)
                if (sessionManager.shouldBlockApps() &&
                    (sessionManager.isStrictMode() || sessionManager.isUltraFocusActive())
                ) {
                    val currentPkg = rootInActiveWindow?.packageName?.toString()
                    if (currentPkg == packageName) {
                        checkSettingsWindowForTamper(packageName, className, eventTitle)
                    }
                }
            }
        }
    }

    private fun checkSettingsWindowForTamper(
        packageName: String,
        className: String,
        eventTitle: String
    ): Boolean {
        val root = rootInActiveWindow
        val (headerSnippet, bodySnippet) = if (root != null) {
            try {
                collectSettingsHeaderAndBody(root, maxNodes = 90)
            } finally {
                @Suppress("DEPRECATION")
                root.recycle()
            }
        } else {
            "" to ""
        }

        if (isTamperSettingsMatch(className, "$eventTitle $headerSnippet", bodySnippet)) {
            Log.d(TAG, "Blocking tamper Settings screen ($className): $eventTitle")
            sessionManager.recordShieldDiagnostic("SETTINGS_TAMPER", packageName, "Tamper menu: $className")
            blockApp(packageName, isSettingsAttempt = true)
            return true
        }
        return false
    }

    private fun collectSettingsHeaderAndBody(
        root: AccessibilityNodeInfo,
        maxNodes: Int
    ): Pair<String, String> {
        val headerSb = StringBuilder()
        val bodySb = StringBuilder()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        var headerCount = 0
        while (queue.isNotEmpty() && visited < maxNodes) {
            val node = queue.removeFirst()
            visited++
            val viewId = node.viewIdResourceName?.lowercase().orEmpty()
            val isToolbarOrTitle = viewId.contains("collapsing") ||
                viewId.contains("action_bar") ||
                viewId.contains("toolbar")

            val t = node.text?.toString()
            if (!t.isNullOrBlank()) {
                bodySb.append(t).append(' ')
                if (isToolbarOrTitle || headerCount < 3) {
                    headerSb.append(t).append(' ')
                    headerCount++
                }
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                queue.add(child)
            }
            if (node != root) {
                @Suppress("DEPRECATION")
                node.recycle()
            }
        }
        while (queue.isNotEmpty()) {
            val leftover = queue.removeFirst()
            if (leftover != root) {
                @Suppress("DEPRECATION")
                leftover.recycle()
            }
        }
        return headerSb.toString() to bodySb.toString()
    }

    private fun isSystemSettingsPackage(pkg: String): Boolean {
        if (sessionManager.isSystemFrameworkOrInputService(pkg)) return false
        return pkg == "com.android.settings" ||
            pkg == "com.android.settings.intelligence" ||
            pkg.startsWith("com.android.settings.") ||
            pkg.startsWith("com.samsung.android.settings")
    }

    /**
     * Unconditional protection on Package Installers / OEM Cleaners against mid-session
     * app uninstallation or background process killing.
     */
    private fun isAlwaysBlockedInstallerOrSecurity(pkg: String): Boolean {
        if (sessionManager.isSystemFrameworkOrInputService(pkg)) return false
        val lower = pkg.lowercase()

        if (pkg == "com.google.android.packageinstaller" ||
            pkg == "com.android.packageinstaller" ||
            lower.contains("packageinstaller")
        ) return true

        if (pkg == "com.samsung.android.lool" ||
            pkg == "com.samsung.android.sm" ||
            pkg == "com.samsung.android.sm_cn"
        ) return true

        if (pkg == "com.miui.securitycenter" || pkg.startsWith("com.miui.cleanmaster")) return true
        if (pkg == "com.coloros.safecenter" || pkg == "com.oplus.safecenter") return true
        if (pkg == "com.iqoo.secure" || pkg == "com.vivo.permissionmanager") return true
        if (pkg == "com.huawei.systemmanager") return true

        return false
    }

    private fun isLauncher(pkg: String): Boolean {
        if (launcherPackages.contains(pkg)) return true

        if (launcherPackages.isEmpty()) {
            loadLauncherPackages()
        }
        return launcherPackages.contains(pkg)
    }

    private fun loadLauncherPackages() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val pm = packageManager
            val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (info in resolveInfos) {
                launcherPackages.add(info.activityInfo.packageName)
            }
        } catch (e: Exception) {
            launcherPackages.add("com.google.android.apps.nexuslauncher")
            launcherPackages.add("com.android.launcher3")
            launcherPackages.add("com.sec.android.app.launcher")
        }
    }

    override fun onInterrupt() {}
}
