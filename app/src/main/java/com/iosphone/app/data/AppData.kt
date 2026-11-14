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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val lock = Mutex()

    var contacts by mutableStateOf<List<Contact>>(emptyList())
        private set
    var calls by mutableStateOf<List<CallEntry>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set
    val missedUnread: Int
        get() = calls.count { it.isUnreadMissed }
    var version by mutableIntStateOf(0)
        private set
    var callsVersion by mutableIntStateOf(0)
        private set
    var resumeCount by mutableIntStateOf(0)
        private set

    /** Filter requested from outside (e.g. the missed-call notification); CallsScreen consumes it. */
    var pendingFilter by mutableIntStateOf(-1)

    private var index: Map<String, Contact> = emptyMap()
    private var idIndex: Map<Long, Contact> = emptyMap()
    private var lastContactsRefresh = 0L
    private var lastRefreshStart = 0L
    private var grantMask = -1

    /** Contacts and call log both changed (permissions, first load). */
    fun invalidate() {
        version++
    }

    /** Only the call log changed: cheap reload, contacts untouched. */
    fun invalidateCalls() {
        callsVersion++
    }

    fun onResume(context: Context) {
        resumeCount++
        val mask = grantMask(context)
        if (grantMask == -1) {
            grantMask = mask
            return
        }
        // The call log is small and changes while the app is in the background; always re-read it.
        callsVersion++
        val staleContacts = SystemClock.elapsedRealtime() - lastContactsRefresh > 600_000L
        if (mask != grantMask || staleContacts) version++
    }

    /** Reloads the call log only. A request made while another refresh was already running is coalesced. */
    suspend fun refreshCalls(context: Context) {
        val requested = SystemClock.elapsedRealtime()
        lock.withLock {
            if (loaded && lastRefreshStart >= requested) return
            val l = if (granted(context, Manifest.permission.READ_CALL_LOG)) {
                runCatching { CallLogRepository.load(context) }.getOrDefault(calls)
            } else {
                emptyList()
            }
            if (withContext(Dispatchers.Default) { l != calls }) calls = l
        }
    }

    suspend fun refresh(context: Context) {
        val requested = SystemClock.elapsedRealtime()
        lock.withLock {
            // A refresh that started after this request was made already covers it.
            if (loaded && lastRefreshStart >= requested) return
            lastRefreshStart = SystemClock.elapsedRealtime()
            grantMask = grantMask(context)
            val canContacts = granted(context, Manifest.permission.READ_CONTACTS)
            val canCalls = granted(context, Manifest.permission.READ_CALL_LOG)
            val (c, l) = coroutineScope {
                val cd = async {
                    if (canContacts) {
                        runCatching { ContactsRepository.load(context) }.getOrDefault(contacts)
                    } else {
                        emptyList()
                    }
                }
                val ld = async {
                    if (canCalls) runCatching { CallLogRepository.load(context) }.getOrDefault(calls) else emptyList()
                }
                cd.await() to ld.await()
            }
            val contactsChanged = withContext(Dispatchers.Default) { c != contacts }
            val callsChanged = withContext(Dispatchers.Default) { l != calls }
            if (contactsChanged) {
                val newIndex = withContext(Dispatchers.Default) {
                    val map = HashMap<String, Contact>()
                    for (ct in c) for (p in ct.phones) map.putIfAbsent(PhoneFormat.key(p.number), ct)
                    map
                }
                val newIdIndex = withContext(Dispatchers.Default) { c.associateBy { it.id } }
                index = newIndex
                idIndex = newIdIndex
                contacts = c
            }
            if (callsChanged) calls = l
            lastContactsRefresh = SystemClock.elapsedRealtime()
            loaded = true
        }
    }

    companion object {
        val shared = AppData()
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        fun warm(context: Context) {
            val app = context.applicationContext
            if (!shared.loaded) scope.launch { runCatching { shared.refresh(app) } }
        }
    }

    fun contactFor(number: String): Contact? = if (number.isEmpty()) null else index[PhoneFormat.key(number)]

    fun contactById(id: Long): Contact? = idIndex[id]

    private fun granted(context: Context, permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun grantMask(context: Context): Int {
        var mask = 0
        arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
        ).forEachIndexed { i, p -> if (granted(context, p)) mask = mask or (1 shl i) }
        return mask
    }
}

val LocalAppData = staticCompositionLocalOf<AppData> { error("AppData missing") }

@Composable
fun rememberAppData(): AppData {
    val context = LocalContext.current
    val data = AppData.shared
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
        val pendingContacts = Runnable { data.invalidate() }
        val pendingCalls = Runnable { data.invalidateCalls() }
        val contactsObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                handler.removeCallbacks(pendingContacts)
                handler.postDelayed(pendingContacts, 700)
            }
        }
        val callsObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                handler.removeCallbacks(pendingCalls)
                handler.postDelayed(pendingCalls, 250)
            }
        }
        val resolver = context.contentResolver
        runCatching { resolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contactsObserver) }
        runCatching { resolver.registerContentObserver(ContactsContract.Data.CONTENT_URI, false, contactsObserver) }
        runCatching { resolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, callsObserver) }
        onDispose {
            handler.removeCallbacks(pendingContacts)
            handler.removeCallbacks(pendingCalls)
            runCatching { resolver.unregisterContentObserver(contactsObserver) }
            runCatching { resolver.unregisterContentObserver(callsObserver) }
        }
    }
    LaunchedEffect(data.version) { data.refresh(context) }
    LaunchedEffect(data.callsVersion) { if (data.loaded) data.refreshCalls(context) }
    return data
}
