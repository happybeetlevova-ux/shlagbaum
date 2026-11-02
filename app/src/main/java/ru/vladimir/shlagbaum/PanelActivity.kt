package ru.vladimir.shlagbaum

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** Экран с крупными кнопками — для магнитолы и для тех, кто не ставит виджет. */
class PanelActivity : Activity() {

    private fun dp(v: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    override fun onResume() {
        super.onResume()
        build() // перерисовать после возврата из настроек
    }

    override fun onConfigurationChanged(c: android.content.res.Configuration) {
        super.onConfigurationChanged(c)
        build() // поворот экрана
    }

    private fun build() {
        val count = Prefs.count(this)
        val mode = Prefs.mode(this)
        val landscape = resources.displayMetrics.widthPixels > resources.displayMetrics.heightPixels
        // В альбомной ориентации — до 3 кнопок в ряд, в портретной — по 1–2
        val perRow = if (landscape) minOf(count, 3) else if (count >= 4) 2 else 1

        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF121212.toInt())
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(TextView(this).apply {
            text = getString(R.string.app_name); textSize = 22f
            setTextColor(Color.WHITE); setTypeface(typeface, Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        top.addView(TextView(this).apply {
            text = "Настройки"; textSize = 16f; setTextColor(0xFFB0B0B0.toInt())
            setPadding(dp(16), dp(10), dp(8), dp(10))
            setOnClickListener {
                startActivity(Intent(this@PanelActivity, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_FROM_PANEL, true))
            }
        })
        screen.addView(top)

        var row: LinearLayout? = null
        for (i in 0 until count) {
            if (i % perRow == 0) {
                row = LinearLayout(this)
                screen.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
            }
            row!!.addView(bigButton(i, mode), LinearLayout.LayoutParams(0, MATCH_PARENT, 1f))
        }
        // Неполный последний ряд дополняем пустым местом, чтобы кнопки были одного размера
        val rest = (perRow - count % perRow) % perRow
        repeat(rest) { row!!.addView(View(this), LinearLayout.LayoutParams(0, MATCH_PARENT, 1f)) }

        setContentView(screen)
    }

    private fun bigButton(i: Int, mode: Int): View {
        val ic = Prefs.icon(this, i)
        val showIcon = ic > 0 && mode != Prefs.MODE_TEXT
        val showText = !showIcon || mode == Prefs.MODE_BOTH

        val b = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(Prefs.COLORS[Prefs.color(this@PanelActivity, i)])
            }
            contentDescription = Prefs.name(this@PanelActivity, i)
            setOnClickListener {
                startActivity(Intent(this@PanelActivity, CallActivity::class.java)
                    .putExtra(CallActivity.EXTRA_INDEX, i))
            }
        }
        if (showIcon) b.addView(ImageView(this).apply {
            setImageResource(Prefs.ICONS[ic]); setColorFilter(Color.WHITE)
        }, LinearLayout.LayoutParams(dp(64), dp(64)))
        if (showText) b.addView(TextView(this).apply {
            text = Prefs.name(this@PanelActivity, i)
            setTextColor(Color.WHITE); setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER; maxLines = 2
            setAutoSizeTextTypeUniformWithConfiguration(14, 40, 1, TypedValue.COMPLEX_UNIT_SP)
        }, LinearLayout.LayoutParams(MATCH_PARENT, dp(if (showIcon) 56 else 110)))

        return LinearLayout(this).apply {
            setPadding(dp(6), dp(6), dp(6), dp(6))
            addView(b, LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }
    }
}
