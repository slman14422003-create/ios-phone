package com.iosphone.app.telecom

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract

/**
 * Hand-off to the standalone iOS Contacts app. This app keeps no contacts UI of its own: every create, edit,
 * view and browse action is sent to that app (explicit package when installed, system contacts otherwise).
 */
object ContactsLink {
    const val PACKAGE = "com.iosphone.contacts"

    fun installed(context: Context): Boolean = runCatching {
        context.packageManager.getPackageInfo(PACKAGE, 0)
        true
    }.getOrDefault(false)

    private fun launch(context: Context, intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (installed(context)) intent.setPackage(PACKAGE)
        if (runCatching { context.startActivity(intent) }.isFailure && intent.`package` != null) {
            intent.setPackage(null)
            runCatching { context.startActivity(intent) }
        }
    }

    fun openList(context: Context) {
        val own = context.packageManager.getLaunchIntentForPackage(PACKAGE)
        if (own != null && runCatching { context.startActivity(own) }.isSuccess) return
        val generic = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CONTACTS)
        launch(context, generic)
    }

    fun openContact(context: Context, id: Long) {
        val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id)
        launch(context, Intent(Intent.ACTION_VIEW).setDataAndType(uri, ContactsContract.Contacts.CONTENT_ITEM_TYPE))
    }

    fun create(context: Context, number: String?) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE
            if (!number.isNullOrBlank()) putExtra(ContactsContract.Intents.Insert.PHONE, number)
        }
        launch(context, intent)
    }

    fun addToExisting(context: Context, number: String) {
        val intent = Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
            type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
            putExtra(ContactsContract.Intents.Insert.PHONE, number)
        }
        launch(context, intent)
    }
}
