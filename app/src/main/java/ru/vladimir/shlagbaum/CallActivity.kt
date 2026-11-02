package ru.vladimir.shlagbaum

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.widget.Toast

/** Получает номер шлагбаума и сразу звонит, без открытия интерфейса. */
class CallActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val i = intent.getIntExtra(EXTRA_INDEX, -1)
        val phone = if (i in 0 until Prefs.MAX) Prefs.phone(this, i) else ""

        val callApp = Prefs.callApp(this)
        when {
            phone.isBlank() -> {
                Toast.makeText(this, "Номер не задан — откройте настройки", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivity::class.java))
            }
            // Магнитола или выбрано отдельное приложение для звонков (Bluetooth-«Телефон»)
            callApp.isNotEmpty() || !CallApps.hasTelephony(this) -> viaApp(i, phone, callApp)
            checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED -> {
                Toast.makeText(this, "Разрешите звонки в приложении «Шлагбаум»", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivity::class.java))
            }
            else -> call(i, phone)
        }
        finish()
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun call(i: Int, phone: String) {
        val uri = Uri.parse("tel:" + Uri.encode(phone))
        val slot = Prefs.sim(this, i)
        val name = Prefs.name(this, i)

        if (slot >= 0) {
            val handle = Sim.handleFor(this, slot)
            if (handle != null) {
                try {
                    val extras = Bundle().apply {
                        putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                    }
                    Toast.makeText(this, "Открываю: $name (SIM ${slot + 1})", Toast.LENGTH_SHORT).show()
                    getSystemService(TelecomManager::class.java).placeCall(uri, extras)
                    return
                } catch (e: SecurityException) { /* ниже — обычный звонок */ }
            } else {
                Toast.makeText(this, "SIM ${slot + 1} не найдена — звоню как в телефоне",
                    Toast.LENGTH_LONG).show()
            }
        }
        if (slot < 0) Toast.makeText(this, "Открываю: $name", Toast.LENGTH_SHORT).show()
        startActivity(Intent(Intent.ACTION_CALL, uri))
    }

    private fun viaApp(i: Int, phone: String, pkg: String) {
        val uri = Uri.parse("tel:" + Uri.encode(phone))
        val ok = (pkg.isNotEmpty() && CallApps.launch(this, pkg, uri)) || CallApps.launch(this, null, uri)
        if (ok) {
            Toast.makeText(this, "Открываю: ${Prefs.name(this, i)}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this,
                "Не найдено приложение для звонков. Подключите телефон по Bluetooth " +
                    "и выберите приложение в настройках «Шлагбаума».", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    companion object { const val EXTRA_INDEX = "index" }
}
