---
name: ios-phone-progress
description: Progress log for the ios-phone Android project (com.iosphone.app, Kotlin + Compose, default dialer). Read before continuing any task in this project.
---

# Project
- Package com.iosphone.app, minSdk 31, Kotlin + Jetpack Compose, default dialer that mimics the iPhone Phone app.
- Rules: keep the glass/blur design, new strings go in values and values-ar, no new libraries, no README files.

# Done
## 1) Silence unknown callers
- AppSettings.silenceUnknown (prefs key silence_unknown, default false), toggle in SettingsScreen under the call settings card.
- CallService.handleRinging: on STATE_RINGING with the option on, requires READ_CONTACTS and READ_CALL_LOG, skips emergency calls, looks up the contact and past OUTGOING log entries on the io executor, then calls TelecomManager.silenceRinger() (retried at 350 ms and 1000 ms). Silenced calls skip startRingExtras (flash and auto answer). Hidden numbers count as unknown.

## 2) Swipe gestures on call log rows
- CallsScreen: SwipeRow wraps CallRow. Swipe toward the end edge reveals the green call action (fires past threshold), swipe the other way reveals Message and Delete buttons. RTL handled with LocalLayoutDirection sign. CLOCK_TICK haptic on zone change, one row open at a time, closes on scroll, disabled in editing mode.
- Delete calls CallLogRepository.delete then AppData.shared.invalidateCalls().

## 3) Missed calls badge
- CallEntry.isRead (read from CallLog IS_READ in CallLogRepository), CallEntry.isUnreadMissed, AppData.missedUnread.
- MissedCalls notification: setNumber(count), BADGE_ICON_SMALL, channel setShowBadge(true).
- MainActivity no longer clears missed calls on resume. They are cleared (MissedCalls.clear) when the Missed filter is open in CallsScreen, on notification swipe, or on call back.
- PhoneShell TabPill: red count badge on the Recents tab from AppData.missedUnread.

## 4) SIM chooser as dropdown menu
- SimPicker.kt: SimPickerHost now renders a GlassMenu (same style and position as the filter menu) with one item per SIM instead of the bottom sheet. Tab bar stays visible while it is open (PhoneShell).
- From CallsScreen rows (tap or swipe-call) the SIM menu is anchored under the tapped row (SimMenu + simChoices in SimPicker.kt, ClampedBelowProvider), narrower (220dp), with the contact name or number as header (GlassMenu header param). Other screens still use the top-end SimPickerHost.

## 5) Message icon
- All Material ChatBubble usages replaced with IosIcons.Message (in-call grid, swipe action, quick menu, keypad menu).

# Notes
- Launcher badge rendering depends on the device launcher.
- silenceRinger cannot stop a ringtone that has not started yet, hence the retries.
- Build was not run in the authoring environment (no Gradle). Run gradle assembleRelease and fix any compile error first.
