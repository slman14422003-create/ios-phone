package com.iosphone.app.telecom

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

class RecordingAccessibility : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    companion object {
        fun enabled(context: Context): Boolean {
            val flat = runCatching {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            }.getOrNull().orEmpty()
            val me = ComponentName(context, RecordingAccessibility::class.java).flattenToString()
            return flat.split(':').any { it.equals(me, ignoreCase = true) }
        }
    }
}
