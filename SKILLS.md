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
