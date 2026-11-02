package ru.vladimir.shlagbaum

import android.content.Context

/** Настройки кнопок (до 6): название, номер, цвет, значок + количество и режим отображения. */
object Prefs {
    /** Сколько кнопок можно включить максимум. */
    const val MAX = 6
    private const val FILE = "barriers"

    /** Палитра цветов кнопок. */
    val COLORS = intArrayOf(
        0xFF2E7D32.toInt(), // зелёный
        0xFF1565C0.toInt(), // синий
        0xFFEF6C00.toInt(), // оранжевый
        0xFFC62828.toInt(), // красный
        0xFF6A1B9A.toInt(), // фиолетовый
        0xFF455A64.toInt()  // серый
    )

    /** Значки: ресурс и подпись. Индекс 0 — «без значка». */
    val ICONS = intArrayOf(0, R.drawable.ic_yard, R.drawable.ic_parking, R.drawable.ic_school,
        R.drawable.ic_work, R.drawable.ic_barrier)
    val ICON_NAMES = arrayOf("Нет", "Двор", "Парковка", "Школа", "Работа", "Шлагбаум")

    const val MODE_TEXT = 0
    const val MODE_ICON = 1
    const val MODE_BOTH = 2

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Название, введённое пользователем (может быть пустым). */
    fun rawName(ctx: Context, i: Int): String {
        val v = sp(ctx).getString("name_$i", "") ?: ""
        // Старые версии сохраняли подстановку «Шлагбаум N» — считаем её пустой
        return if (v == "Шлагбаум ${i + 1}") "" else v
    }

    /** Подпись на кнопке: своё название → название значка → «Шлагбаум N». */
    fun name(ctx: Context, i: Int): String {
        val n = rawName(ctx, i)
        if (n.isNotBlank()) return n
        val ic = icon(ctx, i)
        return if (ic > 0) ICON_NAMES[ic] else "Шлагбаум ${i + 1}"
    }

    fun phone(ctx: Context, i: Int): String = sp(ctx).getString("phone_$i", "") ?: ""
    fun color(ctx: Context, i: Int): Int = sp(ctx).getInt("color_$i", i % COLORS.size)
    fun icon(ctx: Context, i: Int): Int = sp(ctx).getInt("icon_$i", 0)
    /** С какой SIM звонить: -1 — как в телефоне, иначе номер слота с нуля. */
    fun sim(ctx: Context, i: Int): Int = sp(ctx).getInt("sim_$i", -1)
    fun setSim(ctx: Context, i: Int, v: Int) = sp(ctx).edit().putInt("sim_$i", v).apply()

    /** Приложение для звонков ("" — автоматически). Нужно для магнитол. */
    fun callApp(ctx: Context): String = sp(ctx).getString("call_app", "") ?: ""
    fun setCallApp(ctx: Context, v: String) = sp(ctx).edit().putString("call_app", v).apply()

    /** Открывать экран с кнопками при запуске. На магнитоле — по умолчанию да. */
    fun startPanel(ctx: Context): Boolean =
        sp(ctx).getBoolean("start_panel", !CallApps.hasTelephony(ctx))
    fun setStartPanel(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("start_panel", v).apply()

    fun mode(ctx: Context): Int = sp(ctx).getInt("mode", MODE_TEXT)
    /** Сколько кнопок показывать на виджете (1–6, по умолчанию 3). */
    fun count(ctx: Context): Int = sp(ctx).getInt("count", 3).coerceIn(1, MAX)

    fun setName(ctx: Context, i: Int, v: String) = sp(ctx).edit().putString("name_$i", v).apply()
    fun setPhone(ctx: Context, i: Int, v: String) = sp(ctx).edit().putString("phone_$i", v).apply()
    fun setColor(ctx: Context, i: Int, v: Int) = sp(ctx).edit().putInt("color_$i", v).apply()
    fun setIcon(ctx: Context, i: Int, v: Int) = sp(ctx).edit().putInt("icon_$i", v).apply()
    fun setMode(ctx: Context, v: Int) = sp(ctx).edit().putInt("mode", v).apply()
    fun setCount(ctx: Context, v: Int) = sp(ctx).edit().putInt("count", v.coerceIn(1, MAX)).apply()
}
