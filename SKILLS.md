# Task log

## Fix: assembleRelease failure (compileReleaseKotlin)
- Cause: platform declaration clash in AppSettings.kt. Properties appearance, keySounds, keyHaptics with private set generate JVM setters that collide with the manual setAppearance, setKeySounds, setKeyHaptics functions.
- Fix: added @JvmName (applyAppearance, applyKeySounds, applyKeyHaptics) on the manual functions. Kotlin callers in SettingsScreen.kt unchanged.
- Pattern: when a property has a custom setter-named function, either rename it or use @JvmName to avoid the JVM signature clash.
