package com.iosphone.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
    private var index: Map<String, Contact> = emptyMap()

    fun invalidate() {
        version++
    }

    suspend fun refresh(context: Context) {
        val c = if (granted(context, Manifest.permission.READ_CONTACTS)) {
            runCatching { ContactsRepository.load(context) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        val l = if (granted(context, Manifest.permission.READ_CALL_LOG)) {
            runCatching { CallLogRepository.load(context) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        val map = HashMap<String, Contact>()
        for (ct in c) for (p in ct.phones) map.putIfAbsent(PhoneFormat.key(p.number), ct)
        index = map
        contacts = c
        calls = l
        loaded = true
    }

    fun contactFor(number: String): Contact? = if (number.isEmpty()) null else index[PhoneFormat.key(number)]

    fun contactById(id: Long): Contact? = contacts.firstOrNull { it.id == id }

    private fun granted(context: Context, permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}

val LocalAppData = staticCompositionLocalOf<AppData> { error("AppData missing") }

@Composable
fun rememberAppData(): AppData {
    val context = LocalContext.current
    val data = remember { AppData() }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) data.invalidate()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(data.version) { data.refresh(context) }
    return data
}
