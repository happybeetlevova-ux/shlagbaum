package ru.vladimir.shlagbaum

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews

/** Виджет: кнопки в ряд. */
open class BarrierWidget : AppWidgetProvider() {

    open val layoutRes: Int get() = R.layout.widget

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { mgr.updateAppWidget(it, buildViews(ctx, layoutRes)) }
    }

    companion object {
        private val btn = intArrayOf(R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6)
        private val bg = intArrayOf(R.id.bg1, R.id.bg2, R.id.bg3, R.id.bg4, R.id.bg5, R.id.bg6)
        private val icon = intArrayOf(R.id.icon1, R.id.icon2, R.id.icon3, R.id.icon4, R.id.icon5, R.id.icon6)
        private val label = intArrayOf(R.id.label1, R.id.label2, R.id.label3, R.id.label4, R.id.label5, R.id.label6)

        fun buildViews(ctx: Context, layoutRes: Int): RemoteViews {
            val v = RemoteViews(ctx.packageName, layoutRes)
            val mode = Prefs.mode(ctx)
            val count = Prefs.count(ctx)
            for (i in 0 until Prefs.MAX) {
                // Лишние кнопки прячем, оставшиеся растягиваются на весь виджет
                v.setViewVisibility(btn[i], if (i < count) View.VISIBLE else View.GONE)
                if (i >= count) continue
                v.setInt(bg[i], "setColorFilter", Prefs.COLORS[Prefs.color(ctx, i)])

                val ic = Prefs.icon(ctx, i)
                // Без выбранного значка всегда показываем название
                val showIcon = ic > 0 && mode != Prefs.MODE_TEXT
                val showText = !showIcon || mode == Prefs.MODE_BOTH

                if (showIcon) v.setImageViewResource(icon[i], Prefs.ICONS[ic])
                v.setViewVisibility(icon[i], if (showIcon) View.VISIBLE else View.GONE)
                v.setTextViewText(label[i], Prefs.name(ctx, i))
                v.setViewVisibility(label[i], if (showText) View.VISIBLE else View.GONE)
                v.setContentDescription(btn[i], Prefs.name(ctx, i))

                val intent = Intent(ctx, CallActivity::class.java)
                    .putExtra(CallActivity.EXTRA_INDEX, i)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val pi = PendingIntent.getActivity(ctx, i, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                v.setOnClickPendingIntent(btn[i], pi)
            }
            return v
        }

        /** Перерисовать все виджеты обоих видов. */
        fun refreshAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            listOf(
                BarrierWidget::class.java to R.layout.widget,
                BarrierWidgetColumn::class.java to R.layout.widget_column
            ).forEach { (cls, layout) ->
                mgr.getAppWidgetIds(ComponentName(ctx, cls)).forEach {
                    mgr.updateAppWidget(it, buildViews(ctx, layout))
                }
            }
        }
    }
}

/** Виджет: кнопки столбиком. */
class BarrierWidgetColumn : BarrierWidget() {
    override val layoutRes: Int get() = R.layout.widget_column
}
