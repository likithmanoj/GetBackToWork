# 🛡️ GetBackToWork

**GetBackToWork** is a **100% offline, zero-account, zero-telemetry** digital detox, surgical feed blocker, and minimalist launcher for Android — designed to replace subscription apps like *Regain*, *Opal*, *Freedom*, and *ScreenZen*.

> **Privacy Verifiable by Construction:** Inspect [`app/src/main/AndroidManifest.xml`](./app/src/main/AndroidManifest.xml) — the app **does not even declare `android.permission.INTERNET`**. Everything stays strictly on your device.

---

## ✨ Key Features

- **Minimalist Phone Homescreen (`CATEGORY_HOME`):** Optional typography-first default launcher even when no Focus timer is running, with **Editable & Reorderable Pinned Essentials**, custom app renaming (e.g. `YouTube` → `Lectures`), and a `<1h/day` live budget bar.
- **Cold Turkey & Deep Focus + Transit Lifeline:** Unbreakable focus locks (`15m–120m` or `4h–24h` Cold Turkey) with smart **Transit Pass** for maps, transit, rideshare, and UPI payments, plus **Calls Only / Work Essentials / custom profiles**.
- **Surgical In-App Feed & Short-Form Blockers:** Blocks **YouTube Shorts** + optional **YouTube Home feed**, **Instagram Reels + Explore**, and **X (`For you`) / LinkedIn Home**, while preserving **Search, Subscriptions, Lectures, and Direct Messages**.
- **Per-App Daily Time & Open Caps + Mindful Cooldown (`CooldownPromptActivity`):** Set everyday `No Mode` daily minute limits and daily open limits per app; choose **`2m Reply` / `5m Task` / `10m Browse`** windows paired with **Open #X today** awareness, your top 3 personal goals, and the live **Days Left in the Year** countdown.
- **Focus Notification Digest ("Missed During Focus"):** Silences distracting alerts during Focus without losing them forever — saves local previews into a calm Digest card inside **Focus Diary** and posts a summary when timed blocks finish.
- **Anti-Panic Unlock Friction ("Earn Your Scroll"):** 60-second breathing cooldown, 50-word written Intention Gate logged locally, **15 Deep Squats** (accelerometer verified), **3-step Mental Math**, and **8×8 Tactical Chess Mate-in-1** puzzles.
- **Holiday & Travel Modes + Floating Counters:** Conscious weekend limits (Reels count or Minutes cap) with a floating mini pill on Reels/Shorts and long-form video minutes (`YouTube / Netflix / Hotstar`), plus **PiP & Split-Screen anti-bypass** checks, **Work/Sleep Auto-Schedules**, **Home Screen Widget**, **Quick Settings Tile**, and an **OEM Survival + Shield Event Debugger**.

---

## 📱 Project Architecture

```text
app/src/main/java/com/getbacktowork/app/
├── data/
│   ├── AppInfo.kt
│   ├── AppRepository.kt
│   └── FocusSessionManager.kt          # Central state, caps, schedules, digest, diagnostics
├── service/
│   ├── AppBlockerAccessibilityService.kt # Real-time app/feed/URL/PiP/settings blocker
│   ├── NotificationFilterService.kt      # Distracting alert silencer + local Digest capture
│   ├── FocusTimerService.kt              # Foreground focus countdown + post-session digest
│   ├── FocusQuickTileService.kt          # Quick Settings 1-tap toggle
│   └── AdminReceiver.kt                  # Optional Anti-Uninstall shield
├── receiver/
│   └── BootReceiver.kt                   # Restores active locks after reboot
└── ui/
    ├── MainActivity.kt
    ├── components/AppRuleConfigDialog.kt # Shared per-app caps, cooldown & alias editor
    ├── launcher/
    │   ├── MinimalistHomeActivity.kt     # Everyday & Focus distraction-free launcher
    │   └── PinnedEssentialsEditorDialog.kt
    ├── overlay/
    │   ├── BlockingOverlayActivity.kt    # Fullscreen minimal shield + emergency gates
    │   ├── CooldownPromptActivity.kt     # Mindful open pause & 2m/5m/10m window picker
    │   ├── EmergencyPassSheet.kt         # Breathing, 50-word journal, Squats, Chess & Math
    │   ├── HolidayInterrupterActivity.kt # Holiday, Travel, Daily <1h & App Cap prompts
    │   └── ReelsFloatingCounterOverlay.kt# Floating Reels count & long-form video minutes pill
    ├── screens/
    │   ├── TimerScreen.kt                # 4-mode dashboard, schedules & launcher controls
    │   ├── WhitelistScreen.kt            # Allowed apps + per-app rules picker
    │   ├── DiaryScreen.kt                # Digest, app breakdown, 7-day balance & goals
    │   └── PermissionsScreen.kt          # System Shield, surgical blockers, OEM guide & logs
    └── widget/
        └── FocusWidgetProvider.kt        # Glanceable home widget + 1-tap 25m focus
```

---

## 🚀 Installation (Latest APK or Build from Source)

### Option 1: Download Prebuilt APK (Releases)
1. Download **`GetBackToWork.apk`** from [GitHub Releases](../../releases).
2. Install on Android.
3. **Android 13+ Sideload Note:** When enabling Accessibility Service for a sideloaded APK, Android may show *"Restricted setting"*. Open **Settings → Apps → GetBackToWork → ⋮ (top right) → Allow restricted settings**, then enable Accessibility in **System Shield**.

### Option 2: Build & Run Locally
```bash
./gradlew test assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔒 Permissions Explained

- **Accessibility Service (`App Blocker & Anti-Tamper`):** Reads foreground app/browser URLs and Reels/Shorts nodes strictly on-device to block distractions during active locks and enforce per-app rules.
- **Display Over Other Apps:** Shows the floating Reels/video-minutes pill and mindful pause prompts.
- **Notification Access (`Notification Silencer`):** Hides distracting notifications while locked and saves previews to your local Focus Digest (phone calls and allowed work tools always come through).
- **Device Admin (`Optional Anti-Uninstall Shield`):** [`device_admin.xml`](./app/src/main/res/xml/device_admin.xml) declares **zero `<uses-policies>`** — it is used solely so Android requires turning off Admin before impulsive uninstallation mid-session. Leave it off while testing.

---

## 🗺️ Roadmap

See [ROADMAP.md](./ROADMAP.md) for the full design rationale and phased roadmap.
