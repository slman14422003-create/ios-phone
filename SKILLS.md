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
