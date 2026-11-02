package ru.vladimir.shlagbaum

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*

/**
 * Экран настроек. Всё сохраняется сразу при изменении — кнопка «Сохранить» не обязательна.
 */
class MainActivity : Activity() {

    private val GREEN = 0xFF2E7D32.toInt()
    private val REQ_SIM = 2
    companion object { const val EXTRA_FROM_PANEL = "from_panel" }

    private val colorViews = Array(Prefs.MAX) { mutableListOf<View>() }
    private val iconViews = Array(Prefs.MAX) { mutableListOf<View>() }
    private val blocks = mutableListOf<View>()
    private val countViews = mutableListOf<TextView>()

    private fun dp(v: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // По значку приложения сразу открываем крупные кнопки, если так настроено
        if (Prefs.startPanel(this) && !intent.getBooleanExtra(EXTRA_FROM_PANEL, false)) {
            startActivity(Intent(this, PanelActivity::class.java))
            finish(); return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(24))
        }
        root.addView(appIconBlock())
        root.addView(countBlock())

        // Режим отображения
        root.addView(header("Что показывать на кнопках"))
        val modes = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        listOf("Названия", "Значки", "Значок и название").forEachIndexed { m, t ->
            modes.addView(RadioButton(this).apply {
                id = 1000 + m; text = t; textSize = 15f
                isChecked = Prefs.mode(this@MainActivity) == m
            })
        }
        modes.setOnCheckedChangeListener { _, id -> Prefs.setMode(this, id - 1000); changed() }
        root.addView(modes)
        root.addView(hint("Если у кнопки не выбран значок, на ней будет название."))

        for (i in 0 until Prefs.MAX) root.addView(buttonBlock(i).also { blocks += it })
        showBlocks()

        root.addView(Button(this).apply {
            text = "Добавить виджет: в ряд"
            setOnClickListener { pin(BarrierWidget::class.java) }
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(24) })
        root.addView(Button(this).apply {
            text = "Добавить виджет: столбиком"
            setOnClickListener { pin(BarrierWidgetColumn::class.java) }
        })
        root.addView(hint("Изменения сохраняются автоматически и сразу появляются на виджете."))
        root.addView(carBlock())

        // Зелёная шапка + прокручиваемые настройки. Отступы под системные панели
        // считаем сами: на Android 15 приложение рисуется на весь экран.
        val header = TextView(this).apply {
            text = getString(R.string.app_name); textSize = 22f
            setTextColor(Color.WHITE); setBackgroundColor(GREEN)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val scroll = ScrollView(this).apply { clipToPadding = false; addView(root) }
        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFFAFAFA.toInt())
            addView(header, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        }
        setContentView(screen)
        setupSystemBars(screen, header, scroll)

        if (CallApps.hasTelephony(this) &&
            checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 1)
        }
    }

    private fun buttonBlock(i: Int): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(header("Кнопка ${i + 1}"), LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            .apply { topMargin = dp(20) })

        box.addView(label("Название"))
        box.addView(EditText(this).apply {
            setText(Prefs.rawName(this@MainActivity, i))
            hint = "Например: Двор"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            filters = arrayOf(InputFilter.LengthFilter(30))
            isSingleLine = true
            addTextChangedListener(watcher { Prefs.setName(this@MainActivity, i, it.trim()) })
        })

        box.addView(label("Номер для звонка"))
        box.addView(EditText(this).apply {
            setText(Prefs.phone(this@MainActivity, i))
            hint = "+79001234567"
            inputType = InputType.TYPE_CLASS_PHONE
            isSingleLine = true
            addTextChangedListener(watcher { Prefs.setPhone(this@MainActivity, i, it.trim()) })
        })

        if (CallApps.hasTelephony(this)) {
            box.addView(label("Звонить с SIM-карты"))
            box.addView(simChooser(i))
        }

        box.addView(label("Цвет"))
        val colors = LinearLayout(this)
        Prefs.COLORS.forEachIndexed { c, col ->
            val sw = View(this).apply {
                setOnClickListener { Prefs.setColor(this@MainActivity, i, c); markColors(i); changed() }
            }
            colorViews[i] += sw
            colors.addView(sw, LinearLayout.LayoutParams(dp(36), dp(36)).apply { rightMargin = dp(10) })
        }
        box.addView(colors)
        markColors(i)

        box.addView(label("Значок"))
        var row: LinearLayout? = null
        Prefs.ICONS.forEachIndexed { k, res ->
            if (k % 3 == 0) {
                row = LinearLayout(this).also { box.addView(it, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)) }
            }
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(4), dp(6), dp(4), dp(4))
                setOnClickListener { Prefs.setIcon(this@MainActivity, i, k); markIcons(i); changed() }
            }
            val img = ImageView(this).apply {
                setImageResource(if (res != 0) res else R.drawable.ic_none)
                setColorFilter(Color.WHITE)
                setPadding(dp(10), dp(10), dp(10), dp(10))
            }
            cell.addView(img, LinearLayout.LayoutParams(dp(52), dp(52)))
            cell.addView(TextView(this).apply {
                text = Prefs.ICON_NAMES[k]; textSize = 13f; gravity = Gravity.CENTER; maxLines = 1
            }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            iconViews[i] += cell
            row!!.addView(cell, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        }
        markIcons(i)
        return box
    }

    /** Обвести выбранный цвет. */
    private fun markColors(i: Int) {
        val sel = Prefs.color(this, i)
        colorViews[i].forEachIndexed { c, v ->
            v.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Prefs.COLORS[c])
                if (c == sel) setStroke(dp(4), 0xFF212121.toInt())
            }
        }
    }

    /** Подсветить выбранный значок цветом кнопки. */
    private fun markIcons(i: Int) {
        val sel = Prefs.icon(this, i)
        val col = Prefs.COLORS[Prefs.color(this, i)]
        iconViews[i].forEachIndexed { k, cell ->
            val img = (cell as LinearLayout).getChildAt(0)
            img.background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(if (k == sel) col else 0xFFB0B0B0.toInt())
            }
        }
    }

    private fun changed() {
        for (i in 0 until Prefs.MAX) markIcons(i)
        BarrierWidget.refreshAll(this)
    }

    /** Настройки для магнитолы: экран с кнопками и приложение для звонков. */
    private fun carBlock(): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(header("Магнитола и экран с кнопками"), LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            .apply { topMargin = dp(28) })

        box.addView(Switch(this).apply {
            text = "При запуске открывать крупные кнопки"; textSize = 15f
            isChecked = Prefs.startPanel(this@MainActivity)
            setOnCheckedChangeListener { _, on -> Prefs.setStartPanel(this@MainActivity, on) }
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(8) })
        box.addView(Button(this).apply {
            text = "Открыть экран с кнопками"
            setOnClickListener { startActivity(Intent(this@MainActivity, PanelActivity::class.java)) }
        })

        box.addView(label("Через какое приложение звонить"))
        val apps = CallApps.list(this)
        val saved = Prefs.callApp(this)
        val options = mutableListOf("" to "Автоматически")
        apps.forEach { options += it.pkg to "${it.label}  (${it.pkg})" }
        if (saved.isNotEmpty() && apps.none { it.pkg == saved }) options += saved to "$saved — не найдено"
        val group = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        options.forEachIndexed { k, (pkg, t) ->
            group.addView(RadioButton(this).apply {
                id = 5000 + k; text = t; textSize = 14f; isChecked = pkg == saved
            })
        }
        group.setOnCheckedChangeListener { _, id -> Prefs.setCallApp(this, options[id - 5000].first) }
        box.addView(group)
        box.addView(hint(if (CallApps.hasTelephony(this))
            "На телефоне оставьте «Автоматически». На магнитоле выберите её Bluetooth-«Телефон»."
        else
            "Это устройство звонит через телефон по Bluetooth. Выберите приложение «Телефон» / «Bluetooth» " +
                "магнитолы. Если номер только открывается, нажмите в нём кнопку вызова."))
        return box
    }

    /** Выбор SIM-карты для кнопки. */
    private fun simChooser(i: Int): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val saved = Prefs.sim(this, i)
        val cards = Sim.cards(this)

        val options = mutableListOf(-1 to "Как в телефоне")
        cards.forEach { options += it.slot to it.label }
        if (saved >= 0 && cards.none { it.slot == saved }) options += saved to "SIM ${saved + 1} — сейчас не найдена"

        val group = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        options.forEach { (value, text) ->
            group.addView(RadioButton(this).apply {
                id = 3000 + i * 10 + value + 1
                this.text = text; textSize = 15f
                isChecked = value == saved
            })
        }
        group.setOnCheckedChangeListener { _, id -> Prefs.setSim(this, i, (id - 3000) % 10 - 1) }
        box.addView(group)

        when {
            !Sim.hasPermission(this) -> box.addView(TextView(this).apply {
                text = "Разрешить выбор SIM-карты"
                textSize = 15f; setTextColor(GREEN)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, dp(6), 0, dp(6))
                setOnClickListener {
                    requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), REQ_SIM)
                }
            })
            cards.size <= 1 -> box.addView(hint("В телефоне одна SIM-карта — выбирать не нужно."))
        }
        return box
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, res: IntArray) {
        super.onRequestPermissionsResult(code, perms, res)
        // После разрешения перестраиваем экран, чтобы появились SIM-карты
        if (code == REQ_SIM && res.firstOrNull() == PackageManager.PERMISSION_GRANTED) recreate()
    }

    /** Выбор количества кнопок: ряд «1 2 3 4 5 6». */
    private fun countBlock(): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(header("Количество кнопок"), LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            .apply { topMargin = dp(16) })
        val row = LinearLayout(this)
        for (n in 1..Prefs.MAX) {
            val tv = TextView(this).apply {
                text = n.toString(); textSize = 18f; gravity = Gravity.CENTER
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setOnClickListener {
                    Prefs.setCount(this@MainActivity, n); markCount(); showBlocks(); changed()
                }
            }
            countViews += tv
            row.addView(tv, LinearLayout.LayoutParams(0, dp(46), 1f).apply { rightMargin = dp(6) })
        }
        box.addView(row)
        box.addView(hint("Для 4–6 кнопок виджет удобнее растянуть: зажмите его и потяните за край."))
        markCount()
        return box
    }

    private fun markCount() {
        val sel = Prefs.count(this)
        countViews.forEachIndexed { k, tv ->
            val on = k + 1 == sel
            tv.setTextColor(if (on) Color.WHITE else 0xFF424242.toInt())
            tv.background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                if (on) setColor(GREEN) else { setColor(Color.TRANSPARENT); setStroke(dp(1), 0xFFBDBDBD.toInt()) }
            }
        }
    }

    /** Показать настройки только для включённых кнопок. */
    private fun showBlocks() {
        val n = Prefs.count(this)
        blocks.forEachIndexed { i, b -> b.visibility = if (i < n) View.VISIBLE else View.GONE }
    }

    /** Выбор значка приложения: включаем один ярлык и выключаем другой. */
    private val iconChoices = mutableListOf<View>()
    private val aliases = arrayOf(".IconGreen", ".IconCall")

    private fun appIconBlock(): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(header("Значок приложения"))
        val row = LinearLayout(this)
        listOf(R.mipmap.ic_launcher to "Шлагбаум", R.mipmap.ic_launcher_call to "Звонок")
            .forEachIndexed { k, (res, t) ->
                val cell = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(dp(8), dp(8), dp(8), dp(6))
                    setOnClickListener { setAppIcon(k) }
                }
                cell.addView(ImageView(this).apply { setImageResource(res) },
                    LinearLayout.LayoutParams(dp(64), dp(64)))
                cell.addView(TextView(this).apply { text = t; textSize = 13f; gravity = Gravity.CENTER })
                iconChoices += cell
                row.addView(cell, LinearLayout.LayoutParams(dp(110), WRAP_CONTENT).apply { rightMargin = dp(12) })
            }
        box.addView(row)
        box.addView(hint("После смены значка ярлык на рабочем столе может пропасть на несколько секунд — " +
            "если не появится, найдите «Шлагбаум» в списке приложений."))
        markAppIcon()
        return box
    }

    private fun currentAppIcon(): Int {
        val st = packageManager.getComponentEnabledSetting(ComponentName(this, packageName + aliases[1]))
        return if (st == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) 1 else 0
    }

    private fun setAppIcon(k: Int) {
        if (k == currentAppIcon()) return
        aliases.forEachIndexed { j, a ->
            packageManager.setComponentEnabledSetting(
                ComponentName(this, packageName + a),
                if (j == k) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP)
        }
        markAppIcon()
        Toast.makeText(this, "Значок приложения изменён", Toast.LENGTH_SHORT).show()
    }

    private fun markAppIcon() {
        val sel = currentAppIcon()
        iconChoices.forEachIndexed { k, v ->
            v.background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                if (k == sel) { setColor(0xFFE8F5E9.toInt()); setStroke(dp(2), GREEN) }
                else setColor(Color.TRANSPARENT)
            }
        }
    }

    /** Отступы под строку состояния, панель навигации и клавиатуру. */
    private fun setupSystemBars(screen: View, header: TextView, scroll: View) {
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
            // Белые значки строки состояния на зелёной шапке, тёмные — внизу
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
        } else {
            window.statusBarColor = GREEN
        }
        screen.setOnApplyWindowInsetsListener { _, ins ->
            var top = 0; var bottom = 0; var left = 0; var right = 0
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = ins.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                val ime = ins.getInsets(WindowInsets.Type.ime())
                top = bars.top; left = bars.left; right = bars.right
                bottom = maxOf(bars.bottom, ime.bottom)
            }
            header.setPadding(dp(20) + left, top + dp(14), dp(20) + right, dp(14))
            scroll.setPadding(left, 0, right, bottom)
            ins
        }
        screen.requestApplyInsets()
    }

    private fun pin(cls: Class<*>) {
        val mgr = AppWidgetManager.getInstance(this)
        if (mgr.isRequestPinAppWidgetSupported) {
            mgr.requestPinAppWidget(ComponentName(this, cls), null, null)
        } else {
            Toast.makeText(this,
                "Добавьте вручную: долгое нажатие на рабочий стол → Виджеты → Шлагбаум",
                Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        BarrierWidget.refreshAll(this)
    }

    private fun watcher(onChange: (String) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun afterTextChanged(s: Editable?) { onChange(s?.toString() ?: ""); BarrierWidget.refreshAll(this@MainActivity) }
    }

    private fun header(t: String) = TextView(this).apply {
        text = t; textSize = 18f; setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, dp(8), 0, dp(4))
    }
    private fun label(t: String) = TextView(this).apply {
        text = t; textSize = 13f; setPadding(0, dp(10), 0, dp(2)); setTextColor(0xFF616161.toInt())
    }
    private fun hint(t: String) = TextView(this).apply {
        text = t; textSize = 12f; setTextColor(0xFF757575.toInt()); setPadding(0, dp(4), 0, 0)
    }
}
