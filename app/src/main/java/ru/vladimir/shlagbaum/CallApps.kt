package ru.vladimir.shlagbaum

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Приложения, которые умеют звонить по номеру. На магнитоле это обычно
 * Bluetooth-«Телефон», который звонит через подключённый смартфон.
 */
object CallApps {

    data class App(val pkg: String, val label: String)

    fun hasTelephony(ctx: Context) =
        ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

    fun list(ctx: Context): List<App> {
        val pm = ctx.packageManager
        val uri = Uri.parse("tel:100")
        val found = linkedMapOf<String, String>()
        for (action in arrayOf(Intent.ACTION_CALL, Intent.ACTION_DIAL)) {
            pm.queryIntentActivities(Intent(action, uri), 0).forEach {
                val pkg = it.activityInfo.packageName
                if (pkg != ctx.packageName && pkg !in found)
                    found[pkg] = it.loadLabel(pm).toString()
            }
        }
        return found.map { App(it.key, it.value) }
    }

    /** Отправить номер в конкретное приложение: сначала «позвонить», потом «набрать». */
    fun launch(ctx: Context, pkg: String?, uri: Uri): Boolean {
        for (action in arrayOf(Intent.ACTION_CALL, Intent.ACTION_DIAL)) {
            val intent = Intent(action, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (pkg != null) intent.setPackage(pkg)
            if (intent.resolveActivity(ctx.packageManager) == null) continue
            try { ctx.startActivity(intent); return true } catch (e: Exception) { /* следующий способ */ }
        }
        return false
    }
}
