package com.iosphone.app.telecom

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager

class SimInfo(
    val handle: PhoneAccountHandle,
    val slot: Int,
    val label: String,
    val number: String?,
    val subId: Int = -1,
) {
    /** Same per-slot colors as the Messages app, so a SIM looks identical in both apps. */
    val color: Long get() = PALETTE[slot.coerceAtLeast(0) % PALETTE.size]
    val id: String get() = handle.componentName.flattenToString() + "/" + handle.id
}

private val PALETTE = longArrayOf(0xFFFF9500, 0xFFAF52DE, 0xFF5AC8FA, 0xFFFF2D55, 0xFF5856D6, 0xFF8E8E93)

object SimManager {
    fun list(context: Context): List<SimInfo> {
        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        return runCatching {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val result = ArrayList<SimInfo>()
            tm.callCapablePhoneAccounts.forEachIndexed { index, handle ->
                val account = tm.getPhoneAccount(handle) ?: return@forEachIndexed
                if (!account.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)) return@forEachIndexed
                val subId = runCatching { telephony.getSubscriptionId(handle) }.getOrDefault(-1)
                val sub = runCatching { sm.getActiveSubscriptionInfo(subId) }.getOrNull()
                val slot = sub?.simSlotIndex?.takeIf { it >= 0 } ?: index
                val label = sub?.displayName?.toString()?.takeIf { it.isNotBlank() }
                    ?: account.label?.toString()?.takeIf { it.isNotBlank() }
                    ?: "SIM ${slot + 1}"
                val number = account.address?.schemeSpecificPart?.takeIf { it.isNotBlank() }
                    ?: sub?.number?.takeIf { it.isNotBlank() }
                result.add(SimInfo(handle, slot, label, number, subId))
            }
            result.sortedBy { it.slot }
        }.getOrDefault(emptyList())
    }

    fun find(sims: List<SimInfo>, id: String): SimInfo? = if (id.isEmpty()) null else sims.firstOrNull { it.id == id }
}
