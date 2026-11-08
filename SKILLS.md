# Task log

## Fix: assembleRelease failure (compileReleaseKotlin)
- Cause: platform declaration clash in AppSettings.kt. Properties appearance, keySounds, keyHaptics with private set generate JVM setters that collide with the manual setAppearance, setKeySounds, setKeyHaptics functions.
- Fix: added @JvmName (applyAppearance, applyKeySounds, applyKeyHaptics) on the manual functions. Kotlin callers in SettingsScreen.kt unchanged.
- Pattern: when a property has a custom setter-named function, either rename it or use @JvmName to avoid the JVM signature clash.

## UI pass: contacts, tab bar, editor
- Glass: BlurView scale 2f, radius 32, stronger tint in IosTheme for blurTint.
- GlassMenu: opaque menuSurface, scale-in animation, ScreenBlur counter makes PhoneContent blur and dim the whole screen while a menu is open.
- Contacts: Arabic letter normalization and Arabic alphabet ordering, SideIndex rewritten (fit-to-height slots, wave magnification, haptic ticks, full-screen blur plus large letter while pressed, requestScrollToItem), hairlines stop before the index.
- Tab pill: draggable capsule (Animatable x, snap to nearest tab on release, grow while dragging, haptic on crossing).
- Keypad: add-contact is now a GlassIconButton.
- Editor: fixed header with scroll hairline, floating labels, clear button, IME next, phone label picker (PhoneLabels in ContactsWriter.kt), animated add/remove rows, autofocus on new rows.
- Perf: contentType on lazy items, index scroll without coroutine launch.
- Not verified by a local build: run the GitHub Actions workflow.

## Call recording and FaceTime removal
- FaceTime placeholder buttons removed (contact detail, in-call controls and the unused string).
- Recording backend: telecom/CallRecorder.kt (MediaRecorder, tries VOICE_CALL, then VOICE_RECOGNITION, then MIC; AAC m4a), telecom/RecordingService.kt (microphone foreground service), hooks in CallManager state callbacks (start on ACTIVE, stop on DISCONNECTED or removal), AppSettings.autoRecord toggle (default on) in Settings.
- Storage: filesDir/recordings, name pattern time_in-or-out_number.m4a, parsed by data/Recordings.kt.
- UI: Route.Recordings(contactId), ui/recordings/RecordingsScreen.kt (play, pause, seek, delete). Contact detail button opens that contact's recordings; the Calls filter menu opens all recordings; in-call button toggles recording manually.
- Limitation: Android blocks call audio for normal apps, so remote voice is only captured on devices that allow VOICE_CALL or when speaker is on.

## Dual SIM, animations, lock-screen reliability
- SIM: telecom/SimManager.kt (call-capable SIM accounts with slot, label, number), AppSettings.defaultSim (empty means ask every time), ui/common/SimPicker.kt (iOS action sheet in a Dialog window with spring slide, hosted by PhoneShell and ContactsShell), Caller.kt dispatchCall picks handle (override, default, or sheet), keypad SIM chip with menu, Settings section for default SIM.
- Animations: iosRow press highlight (Glass.kt), animateItem on contact and call rows, spring sheet, predictive back enabled in manifest.
- Lock screen: new permissions (READ_PHONE_NUMBERS, BLUETOOTH_CONNECT, WAKE_LOCK, VIBRATE, MODIFY_AUDIO_SETTINGS, DISABLE_KEYGUARD, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, voicemail), ProximityLock wake lock during earpiece calls, public notification visibility, Settings rows for full-screen intent, battery optimization and notifications with live status.
- Stability: PhoneApp crash logger (filesDir/crash.log), lint no longer aborts release builds, packaging excludes, profileinstaller, lifecycle-process, startup, LeakCanary in debug only, extra proguard keep rules.
- Not verified by a local build: run the GitHub Actions workflow.

## SIM sheet position and screen edges
- Cause: the SIM sheet lived in a Dialog window where navigation bar insets were not applied, so it dropped below the screen.
- Fix: SimPickerHost is now a normal overlay (BackHandler, navigationBarsPadding). MainActivity adds a full-size transparent ComposeView above the glass bar to host it; ContactsShell hosts it inline.
- Edges: ui/common/EdgeScrims.kt adds fades under the status bar and the gesture area (used in PhoneContent and ContactsShell); navigation bar contrast enforcement turned off in both activities.
- Duplicate SIM subtitle hidden when the carrier name equals the slot label.

## Performance pass
- Biggest cost: each BlurView blurred a full-screen node every frame. RenderNodeBlurController now crops the node to the BlurView bounds when it has no scale or rotation (target drawn shifted into it). Glass blur uses scale 1 and radius 26, shadow elevation 8.
- Data: AppData no longer reloads on every resume. ContentObservers on contacts and call log trigger a debounced refresh, resume refreshes only after 2 minutes or a permission change, loads run in parallel, unchanged lists are not republished, contacts sorted with precomputed collation keys. resumeCount drives resume-time UI checks (settings statuses, default dialer, permission gate).
- Recomposition: index blur and dim, and menu blur and dim, are read inside graphicsLayer (no per-frame recomposition). Index letter changes no longer recompose the contact list. Tab switch is a short crossfade only. animateItem removed from contacts. iosRow no longer allocates animation state per row.
- Images: avatars decoded at display size.
- Not measured on a device: report which screen still lags.

## Round: IMEI code, silent recordings, transparent tab pill, glass SIM sheet, Messages hand-off
- *#06#: DialCodes.imei reads IMEI/MEID per slot (Android 10+ only allows this for privileged apps); if it cannot, DialCodes.openImeiScreen opens Settings "Status" (shows IMEI) before the generic About-phone page. Manifest <queries> added so those screens are visible to the app.
- Silent recordings: VOICE_CALL starts but records silence on many phones. CallRecorder now has a watchdog (reads maxAmplitude; silent for 4 s means restart on the next source: VOICE_CALL, VOICE_RECOGNITION, MIC) and starts recording from RecordingService.serviceReady() so the mic foreground service is already foreground. When only the MIC is available the other side is captured through the speaker.
- Tab pill: selected capsule is now a transparent glass lens (faint tint + hairline) instead of a grey fill.
- SIM sheet: iOS 26 glass sheet (GlassBox gets blurRadius/overlay params), whole screen behind blurs and dims via ScreenBlur (also in ContactsShell), bottom tab bar hides while open, rows show the dialed number, SIM tile colors match the Messages app (same per-slot palette in SimInfo.color). Popup menus (GlassMenu) are translucent so the blurred screen shows through.
- Messages hand-off: telecom/Messaging.kt opens the conversation in app.ios.messages (explicit package) from call detail, contact detail, in-call message button and the quick-reply fallback. The Messages app has PhoneLink.kt doing the same toward com.iosphone.app.
- Not verified by a local build: run the GitHub Actions workflow.

## Contacts removed from the Phone app, linked to the standalone Contacts app
- Removed: ContactsActivity (second launcher icon), ContactsShell, ui/contacts (list, detail, editor), ContactsWriter, contacts launcher icons, WRITE_CONTACTS permission, Route.ContactDetail and Route.Editor, the hide-contacts-without-numbers setting.
- Kept read-only: ContactsRepository and AppData contacts (caller names, search, dial suggestions). They read the shared system contacts provider, so edits made in the Contacts app show up here through the existing ContentObserver.
- telecom/ContactsLink.kt: explicit-package hand-off to com.iosphone.contacts (falls back to system contacts intents when it is not installed). openList (Contacts tab), openContact (info button in Recents, search results), create (keypad add button, call detail), addToExisting (call detail).
- Contacts tab in the pill is an external tab (TabSpec.external): tap or drag-release launches the Contacts app and the pill returns to the selected tab.
- Moved shared helpers to ui/common/Rows.kt (ActionRow, labelText) and data/BlockedNumbers.kt (block).
- Deep links for the Contacts app: MainActivity handles com.iosphone.app.action.RECORDINGS (extra contact_id) and com.iosphone.app.action.CALL_INFO (extra number). Manifest queries include com.iosphone.contacts.
- Pattern: when a feature moves to a sibling app, keep a read-only data path for lookups and route every write or edit UI through an explicit-package intent.
- Not verified by a local build: run the GitHub Actions workflow.

## Incoming call banner over an ongoing call, multi-call fixes
- Feature: ui/incall/CallBanner.kt (IncomingBanner, iOS-style pill at the top with avatar, name, decline and accept; tap expands to the full ringing screen). InCallScreen shows it when AppSettings.incomingDisplay == 0 (Automatic), a call is ringing and another call is active or on hold. Fullscreen mode (1) keeps the full ringing screen.
- InCallScreen: while the banner is up, the ongoing call stays on screen (primary = CallManager.ongoing()), the ringing call is hidden from the "on hold" chips, and mute, hold, record, keypad and end act on the shown call instead of CallManager.primary() (which returns the ringing call first).
- CallManager: new ringing() and ongoing() helpers, uiVisible flag set by InCallActivity onStart/onStop.
- CallService fixes: cancelIncoming no longer fires when another call is still ringing (new syncIncoming reposts the notification for the next ringing call); flash and auto-answer extras stop only when no call is ringing; heads-up notification is skipped while the in-app banner is visible, and posted again when the call screen goes to the background.
- CallActionReceiver and InCallActivity answer/decline target the ringing call, hangup targets the ongoing call.
- Pattern: any action meant for the ringing call must use CallManager.ringing(); actions for the live call must use the call currently shown, not primary().
- Not verified by a local build: run the GitHub Actions workflow.

## Silent call recordings, more audio sources and a diagnostics log
- CallRecorder source order is now VOICE_CALL, VOICE_COMMUNICATION, VOICE_RECOGNITION, MIC, UNPROCESSED, CAMCORDER; the watchdog moves to the next source when the stream is silent for 4 s.
- setPrivacySensitive(false) is requested on each recorder so the capture is not blocked by the call's own capture.
- filesDir/recording.log records which source opened, which one produced sound and every silent switch, to find the source that works on a given phone.
- stop() now also stops RecordingService when no recorder exists.
- Limitation: Android 10+ blocks call audio for normal apps; on phones that block every source the recording stays silent unless the speaker is on. Not tested on a device: read recording.log after a test call.
- Settings > Recording has a "Share recording log" row (NavRow) that sends the last 12000 chars of filesDir/recording.log through ACTION_SEND as text, since filesDir is not reachable without root.

## Recording still silent: accessibility service workaround
- recording.log showed every source (VOICE_COMMUNICATION, VOICE_RECOGNITION, MIC, UNPROCESSED, CAMCORDER) opening fine but silent, and VOICE_CALL failing, so Android silences capture during calls for this app; source switching cannot fix that.
- Added telecom/RecordingAccessibility.kt (empty AccessibilityService, no window content, res/xml/recording_accessibility.xml, manifest entry with BIND_ACCESSIBILITY_SERVICE). Apps with an enabled accessibility service are often allowed to capture the mic during calls. Settings > Recording has a status row "Call audio access" that opens Accessibility settings.
- User must enable it once under Accessibility > Downloaded/Installed apps. Not verified on a device: if recording.log still shows silence after enabling it, the phone blocks capture for all apps.

## Other person not recorded: speaker while recording
- After enabling the accessibility service the mic works but the remote voice is not captured (earpiece audio is not reachable by a normal app). New setting AppSettings.recordSpeaker (default on, Settings > Recording): CallRecorder.begin switches the route to speaker when it is neither speaker nor Bluetooth and sets forcedSpeaker; stop() restores the earpiece through restoreRoute() only if that call is still active.
- Limitation: with the speaker on, the mic captures the other person at speaker quality and some echo. Bluetooth routes are left alone.
