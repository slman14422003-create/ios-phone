package com.iosphone.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.CallLog
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.iosphone.app.util.PhoneFormat

class AppData {
    var contacts by mutableStateOf<List<Contact>>(emptyList())
        private set
    var calls by mutableStateOf<List<CallEntry>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set
    var version by mutableIntStateOf(0)
        private set
    var resumeCount by mutableIntStateOf(0)
        private set
    private var index: Map<String, Contact> = emptyMap()
    private var lastRefresh = 0L
    private var grantMask = -1

    fun invalidate() {
        version++
    }

    fun onResume(context: Context) {
        resumeCount++
        val mask = grantMask(context)
        if (grantMask == -1) {
            grantMask = mask
            return
        }
        val stale = SystemClock.elapsedRealtime() - lastRefresh > 120_000L
        if (mask != grantMask || stale) version++
    }

    suspend fun refresh(context: Context) {
        grantMask = grantMask(context)
        val canContacts = granted(context, Manifest.permission.READ_CONTACTS)
        val canCalls = granted(context, Manifest.permission.READ_CALL_LOG)
        val (c, l) = coroutineScope {
            val cd = async {
                if (canContacts) {
                    runCatching { ContactsRepository.load(context) }.getOrDefault(emptyList())
                        .let { all -> if (AppSettings.hideNoNumber) all.filter { it.phones.isNotEmpty() } else all }
                } else {
                    emptyList()
                }
            }
            val ld = async {
                if (canCalls) runCatching { CallLogRepository.load(context) }.getOrDefault(emptyList()) else emptyList()
            }
            cd.await() to ld.await()
        }
        val contactsChanged = withContext(Dispatchers.Default) { c != contacts }
        val callsChanged = withContext(Dispatchers.Default) { l != calls }
        if (contactsChanged) {
            index = withContext(Dispatchers.Default) {
                val map = HashMap<String, Contact>()
                for (ct in c) for (p in ct.phones) map.putIfAbsent(PhoneFormat.key(p.number), ct)
                map
            }
            contacts = c
        }
        if (callsChanged) calls = l
        lastRefresh = SystemClock.elapsedRealtime()
        loaded = true
    }

    fun contactFor(number: String): Contact? = if (number.isEmpty()) null else index[PhoneFormat.key(number)]

    fun contactById(id: Long): Contact? = contacts.firstOrNull { it.id == id }

    private fun granted(context: Context, permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun grantMask(context: Context): Int {
        var mask = 0
        arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO,
        ).forEachIndexed { i, p -> if (granted(context, p)) mask = mask or (1 shl i) }
        return mask
    }
}

val LocalAppData = staticCompositionLocalOf<AppData> { error("AppData missing") }

@Composable
fun rememberAppData(): AppData {
    val context = LocalContext.current
    val data = remember { AppData() }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) data.onResume(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(context) {
        val handler = Handler(Looper.getMainLooper())
        val pending = Runnable { data.invalidate() }
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                handler.removeCallbacks(pending)
                handler.postDelayed(pending, 500)
            }
        }
        val resolver = context.contentResolver
        runCatching { resolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer) }
        runCatching { resolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, observer) }
        onDispose {
            handler.removeCallbacks(pending)
            runCatching { resolver.unregisterContentObserver(observer) }
        }
    }
    LaunchedEffect(data.version) { data.refresh(context) }
    return data
}
