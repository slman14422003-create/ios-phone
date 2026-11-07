package com.iosphone.app.telecom

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Hand-off to the companion iOS Messages app. When it is installed the conversation is opened in it directly
 * (even if another SMS app is the default); otherwise the normal system SMS chooser is used.
 */
object Messaging {
    const val PACKAGE = "app.ios.messages"

    fun installed(context: Context): Boolean = runCatching {
        context.packageManager.getPackageInfo(PACKAGE, 0)
        true
    }.getOrDefault(false)

    fun open(context: Context, number: String, body: String? = null) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!body.isNullOrEmpty()) intent.putExtra("sms_body", body)
        if (installed(context)) intent.setPackage(PACKAGE)
        if (runCatching { context.startActivity(intent) }.isFailure && intent.`package` != null) {
            intent.setPackage(null)
            runCatching { context.startActivity(intent) }
        }
    }
}
