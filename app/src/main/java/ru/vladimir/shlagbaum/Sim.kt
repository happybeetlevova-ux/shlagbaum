package ru.vladimir.shlagbaum

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager

/** Работа с SIM-картами: список карт и выбор, с какой звонить. */
object Sim {

    /** slot — номер слота с нуля (SIM 1 = 0). */
    data class Card(val slot: Int, val subId: Int, val label: String)

    fun hasPermission(ctx: Context) =
        ctx.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun cards(ctx: Context): List<Card> {
        if (!hasPermission(ctx)) return emptyList()
        val sm = ctx.getSystemService(SubscriptionManager::class.java) ?: return emptyList()
        return try {
            (sm.activeSubscriptionInfoList ?: emptyList())
                .sortedBy { it.simSlotIndex }
                .map {
                    val op = (it.carrierName ?: it.displayName ?: "").toString().trim()
                    Card(it.simSlotIndex, it.subscriptionId,
                        "SIM ${it.simSlotIndex + 1}" + if (op.isNotEmpty()) " · $op" else "")
                }
        } catch (e: SecurityException) { emptyList() }
    }

    /** Найти «телефонный аккаунт» Android для SIM в нужном слоте. */
    @SuppressLint("MissingPermission")
    fun handleFor(ctx: Context, slot: Int): PhoneAccountHandle? {
        val all = cards(ctx)
        val card = all.firstOrNull { it.slot == slot } ?: return null
        val tm = ctx.getSystemService(TelecomManager::class.java) ?: return null
        val handles = try { tm.callCapablePhoneAccounts } catch (e: SecurityException) { return null }
        if (handles.isNullOrEmpty()) return null

        if (Build.VERSION.SDK_INT >= 30) {
            val tel = ctx.getSystemService(TelephonyManager::class.java)
            handles.firstOrNull { h ->
                try { tel?.getSubscriptionId(h) == card.subId } catch (e: Exception) { false }
            }?.let { return it }
        }
        // На многих телефонах id аккаунта совпадает с номером подписки
        handles.firstOrNull { it.id == card.subId.toString() }?.let { return it }
        // Запасной вариант: по порядку карт
        return if (handles.size == all.size) handles[all.indexOf(card)] else null
    }
}
