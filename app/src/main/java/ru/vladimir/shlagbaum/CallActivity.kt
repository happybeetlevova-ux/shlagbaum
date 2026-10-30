package ru.vladimir.shlagbaum

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast

/** Получает номер шлагбаума и сразу звонит, без открытия интерфейса. */
class CallActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val i = intent.getIntExtra(EXTRA_INDEX, -1)
        val phone = if (i in 0 until Prefs.MAX) Prefs.phone(this, i) else ""

        when {
            phone.isBlank() -> {
                Toast.makeText(this, "Номер не задан — откройте настройки", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivity::class.java))
            }
            checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED -> {
                Toast.makeText(this, "Разрешите звонки в приложении «Шлагбаум»", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivity::class.java))
            }
            else -> {
                Toast.makeText(this, "Открываю: ${Prefs.name(this, i)}", Toast.LENGTH_SHORT).show()
                startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(phone))))
            }
        }
        finish()
    }

    companion object { const val EXTRA_INDEX = "index" }
}
