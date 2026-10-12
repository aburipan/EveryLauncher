package com.example.everylauncher

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import javax.net.ssl.HttpsURLConnection

private const val TM_IDLE = 0
private const val TM_RUNNING = 1
private const val TM_PAUSED = 2
private const val TM_DONE = 3

class MainActivity : Activity() {
    private val accent = Color.rgb(138, 180, 248)
    private val glass = Color.argb(38, 255, 255, 255)
    private val glassStroke = Color.argb(51, 255, 255, 255)
    private val softWhite = Color.argb(190, 255, 255, 255)
    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private lateinit var root: FrameLayout
    private lateinit var greeting: TextView
    private lateinit var wIcon: TextView
    private lateinit var wTemp: TextView
    private lateinit var wDesc: TextView
    private lateinit var wRange: TextView
    private lateinit var nextText: TextView
    private lateinit var eventList: LinearLayout
    private lateinit var calScroll: ScrollView
    private lateinit var pager: ViewPager2
    private val dots = mutableListOf<View>()
    private val handler = Handler(Looper.getMainLooper())
    private var tick = 0
    private val ticker = object : Runnable {
        override fun run() {
            applyTheme()
            tick++
            loadCalendar()                          // calendar: every 30 s
            if (tick % 30 == 0) loadWeather()       // weather: every 15 min
            handler.postDelayed(this, 30_000)
        }
    }

    // ----- stopwatch / timer state -----
    private class Panel(val view: View, val update: () -> Unit)
    private val swUpdaters = mutableListOf<() -> Unit>()
    private val tmUpdaters = mutableListOf<() -> Unit>()
    private var swRunning = false
    private var swStart = 0L
    private var swAcc = 0L
    private var tmTotal = 5 * 60_000L
    private var tmRemaining = 5 * 60_000L
    private var tmState = TM_IDLE
    private var tmEnd = 0L
    private var player: MediaPlayer? = null
    private var toneGen: ToneGenerator? = null
    private lateinit var weatherView: WeatherView
    private class HourFc(val hour: Int, val temp: Int, val code: Int, val pop: Int, val day: Boolean, val tag: String)
    private class DayFc(val label: String, val code: Int, val hi: Int, val lo: Int, val pop: Int)
    private var hourlyFc: List<HourFc> = emptyList()
    private var dailyFc: List<DayFc> = emptyList()
    private lateinit var hourRow: LinearLayout
    private lateinit var hourHsv: HorizontalScrollView
    private var overlay: View? = null
    private var overlayShrink = 0.94f
    private lateinit var dotRow: LinearLayout
    private var overlayUpdater: (() -> Unit)? = null
    private var overlayList: MutableList<() -> Unit>? = null
    private var themeColors = intArrayOf(Color.rgb(8, 12, 28), Color.rgb(30, 38, 78))
    private var fastRunning = false
    private val alarmStop = Runnable { stopAlarm() }
    private val fastTick = object : Runnable {
        override fun run() {
            timerTick()
            refreshAll()
            if (swRunning || tmState == TM_RUNNING) {
                handler.postDelayed(this, 100)
            } else {
                fastRunning = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        root = FrameLayout(this)
        weatherView = WeatherView(this)
        root.addView(weatherView, FrameLayout.LayoutParams(MATCH, MATCH))
        pager = ViewPager2(this)
        pager.adapter = PageAdapter(listOf(hourlyPage(), homePage(), calendarPage(), toolsPage()))
        pager.setCurrentItem(1, false)
        pager.setPageTransformer(ViewPager2.PageTransformer { page, pos ->
            val a = Math.abs(pos)
            page.alpha = 1f - a * 0.7f
            val s = 1f - a * 0.1f
            page.scaleX = s
            page.scaleY = s
        })
        root.addView(pager, FrameLayout.LayoutParams(MATCH, MATCH))

        dotRow = LinearLayout(this)
        dotRow.gravity = Gravity.CENTER
        for (i in 0 until 4) {
            val d = View(this)
            dots.add(d)
            val p = LinearLayout.LayoutParams(dp(8), dp(8))
            p.setMargins(dp(4), 0, dp(4), 0)
            dotRow.addView(d, p)
        }
        val dp0 = FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        dp0.bottomMargin = dp(14)
        root.addView(dotRow, dp0)
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) { updateDots(position) }
        })
        updateDots(1)

        setContentView(root)
        applyTheme()
        hideSystemUi()
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_CALENDAR), 1
        )
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
        handler.removeCallbacks(ticker)
        handler.postDelayed(ticker, 30_000)
        loadWeather(); loadCalendar()
        weatherView.refreshImages()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        stopAlarm()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    override fun onRequestPermissionsResult(c: Int, p: Array<out String>, r: IntArray) {
        loadWeather(); loadCalendar()
    }

    @Deprecated("Home key: stay on launcher")
    override fun onBackPressed() {
        if (overlay != null) closeExpanded()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUi() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    }

    // ---------- UI helpers ----------
    private fun granted(p: String) = checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun lp(w: Int, h: Int, weight: Float = 0f) = LinearLayout.LayoutParams(w, h, weight)

    private fun rounded(color: Int, radiusDp: Int, stroke: Int? = null): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(radiusDp).toFloat()
        if (stroke != null) g.setStroke(dp(1), stroke)
        return g
    }

    private fun label(
        text: String, sp: Float, color: Int = Color.WHITE,
        font: String = "sans-serif-light"
    ): TextView {
        val t = TextView(this)
        t.text = text
        t.textSize = sp
        t.setTextColor(color)
        t.typeface = Typeface.create(font, Typeface.NORMAL)
        return t
    }

    private fun shade(t: TextView) {
        t.setShadowLayer(dp(6).toFloat(), 0f, dp(1).toFloat(), Color.argb(120, 0, 0, 0))
    }

    // mode: 0=normal, 1=big, 2=compact
    private fun pill(text: String, filled: Boolean = false, mode: Int = 0, onClick: () -> Unit): TextView {
        val t = TextView(this)
        t.text = text
        t.textSize = when (mode) { 1 -> 22f; 2 -> 13f; else -> 16f }
        t.gravity = Gravity.CENTER
        t.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        t.setTextColor(if (filled) Color.rgb(16, 24, 48) else Color.WHITE)
        val ph = when (mode) { 1 -> 36; 2 -> 12; else -> 24 }
        val pv = when (mode) { 1 -> 18; 2 -> 8; else -> 12 }
        t.setPadding(dp(ph), dp(pv), dp(ph), dp(pv))
        val r = if (mode == 1) 40 else 28
        val bg = if (filled) rounded(accent, r) else rounded(glass, r, glassStroke)
        t.background = RippleDrawable(ColorStateList.valueOf(Color.argb(64, 255, 255, 255)), bg, null)
        t.isClickable = true
        t.setOnClickListener { onClick() }
        return t
    }

    private fun glassCard(): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.background = rounded(glass, 28, glassStroke)
        c.setPadding(dp(28), dp(24), dp(28), dp(24))
        return c
    }

    private fun updateDots(sel: Int) {
        dots.forEachIndexed { i, d ->
            d.background = rounded(if (i == sel) Color.WHITE else Color.argb(90, 255, 255, 255), 4)
            val p = d.layoutParams as LinearLayout.LayoutParams
            p.width = dp(if (i == sel) 22 else 8)
            d.layoutParams = p
        }
    }

    private fun applyTheme() {
        val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val colors = when (h) {
            in 5..9 -> intArrayOf(Color.rgb(44, 50, 96), Color.rgb(176, 100, 112))
            in 10..16 -> intArrayOf(Color.rgb(20, 60, 124), Color.rgb(64, 130, 190))
            in 17..18 -> intArrayOf(Color.rgb(50, 38, 92), Color.rgb(196, 98, 88))
            else -> intArrayOf(Color.rgb(8, 12, 28), Color.rgb(30, 38, 78))
        }
        themeColors = colors
        root.background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors)
        greeting.text = when (h) {
            in 5..10 -> "おはようございます"
            in 11..17 -> "こんにちは"
            else -> "こんばんは"
        }
    }

    // ---------- Page 1: Home (clock + weather) ----------
    private fun homePage(): View {
        greeting = label("", 20f, softWhite)

        val clock = TextClock(this)
        clock.format12Hour = "H:mm"
        clock.format24Hour = "H:mm"
        clock.textSize = 120f
        clock.setTextColor(Color.WHITE)
        clock.typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
        clock.includeFontPadding = false

        val date = TextClock(this)
        date.format12Hour = "M月d日 (E)"
        date.format24Hour = "M月d日 (E)"
        date.textSize = 26f
        date.setTextColor(softWhite)
        date.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)

        shade(greeting); shade(clock); shade(date)
        val left = LinearLayout(this)
        left.orientation = LinearLayout.VERTICAL
        left.gravity = Gravity.CENTER_VERTICAL
        left.translationY = -dp(48).toFloat()
        val clockBox = LinearLayout(this)
        clockBox.orientation = LinearLayout.VERTICAL
        clockBox.addView(greeting)
        clockBox.addView(clock)
        clockBox.addView(date)
        clockBox.isClickable = true
        clockBox.setOnClickListener { openClock(it) }
        left.addView(clockBox)
        val appsBtn = pill("アプリ") { showApps() }
        val bp = lp(WRAP, WRAP)
        bp.topMargin = dp(28)
        left.addView(appsBtn, bp)

        // weather card
        wIcon = label("", 56f)
        wTemp = label("--°", 64f, Color.WHITE, "sans-serif-thin")
        wDesc = label("天気を取得中…", 20f)
        wRange = label("", 16f, softWhite)
        val topRow = LinearLayout(this)
        topRow.gravity = Gravity.CENTER_VERTICAL
        topRow.addView(wIcon)
        val tp = lp(WRAP, WRAP)
        tp.leftMargin = dp(16)
        topRow.addView(wTemp, tp)
        val weatherCard = glassCard()
        weatherCard.addView(topRow)
        weatherCard.addView(wDesc)
        weatherCard.addView(wRange)
        val whp = lp(WRAP, WRAP)
        whp.topMargin = dp(8)
        weatherCard.addView(label("タップで7日間の予報", 12f, Color.argb(130, 255, 255, 255)), whp)
        weatherCard.isClickable = true
        weatherCard.setOnClickListener { openForecast(it) }

        // next event card
        nextText = label("…", 18f)
        val nextCard = glassCard()
        nextCard.addView(label("次の予定", 14f, accent, "sans-serif-medium"))
        nextCard.addView(nextText)
        val nhp = lp(WRAP, WRAP)
        nhp.topMargin = dp(8)
        nextCard.addView(label("タップで7日間の予定", 12f, Color.argb(130, 255, 255, 255)), nhp)
        nextCard.isClickable = true
        nextCard.setOnClickListener { pager.setCurrentItem(2, true) }
        val np = lp(MATCH, WRAP)
        np.topMargin = dp(16)

        val right = LinearLayout(this)
        right.orientation = LinearLayout.VERTICAL
        right.gravity = Gravity.CENTER_VERTICAL
        right.translationY = dp(12).toFloat()
        right.addView(weatherCard, lp(MATCH, WRAP))
        right.addView(nextCard, np)

        val page = LinearLayout(this)
        page.setPadding(dp(64), dp(24), dp(64), dp(48))
        page.addView(left, lp(0, MATCH, 1.2f))
        val rp = lp(0, MATCH, 1f)
        rp.leftMargin = dp(32)
        page.addView(right, rp)
        return page
    }

    // ---------- Page 0: next 24 hours (horizontal band, 6 tiles per screen) ----------
    private val hourSideDp = 64

    private fun hourlyPage(): View {
        hourRow = LinearLayout(this)
        hourRow.orientation = LinearLayout.HORIZONTAL
        hourHsv = HorizontalScrollView(this)
        hourHsv.isHorizontalScrollBarEnabled = false
        hourHsv.overScrollMode = View.OVER_SCROLL_NEVER
        hourHsv.addView(hourRow)
        // the band owns horizontal drags; empty space around it still swipes the pages
        val host = HorizontalHost(this)
        host.addView(hourHsv, FrameLayout.LayoutParams(MATCH, WRAP))

        val side = dp(hourSideDp)
        val page = LinearLayout(this)
        page.orientation = LinearLayout.VERTICAL
        // title at the top, band's lower edge at about 2/3 of the screen height (2nd divider of 3)
        val screenH = Math.min(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
        page.setPadding(0, dp(32), 0, screenH / 3)
        val tp = lp(WRAP, WRAP)
        tp.leftMargin = dp(64)
        page.addView(label("今後24時間の天気", 28f, Color.WHITE, "sans-serif-light"), tp)
        page.addView(View(this), lp(MATCH, 0, 1f))      // flexible gap between title and band
        // side margins: tiles are clipped at the band's edges and never scroll into the margins
        val hostLp = lp(MATCH, WRAP)
        hostLp.setMargins(side, 0, side, 0)
        page.addView(host, hostLp)
        buildHourly()
        return page
    }

    private fun buildHourly() {
        val sx = hourHsv.scrollX
        hourRow.removeAllViews()
        val list = hourlyFc
        if (list.isEmpty()) {
            hourRow.addView(label("予報データを取得中…", 20f, softWhite))
            return
        }
        val screenW = Math.max(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
        val gap = dp(8)
        val avail = screenW - 2 * dp(hourSideDp)
        val tileW = (avail - 5 * gap) / 6        // 6 tiles fill the band
        for (i in list.indices) {
            val tp = LinearLayout.LayoutParams(tileW, WRAP)
            tp.setMargins(0, 0, gap, 0)
            hourRow.addView(hourTile(list[i], i == 0), tp)
        }
        hourHsv.post { hourHsv.scrollTo(sx, 0) }
    }

    // shrink tiles a little on short screens so title + gap + band fit above the 2/3 line
    private fun hourScale(): Float {
        val dm = resources.displayMetrics
        val screenHdp = Math.min(dm.widthPixels, dm.heightPixels) / dm.density
        val room = screenHdp * 2f / 3f - 32f - 38f - 16f
        return Math.max(0.7f, Math.min(1f, room / 204f))
    }

    private fun hourTile(h: HourFc, isNow: Boolean): View {
        val sc = hourScale()
        val icon = weatherLabel(h.code, h.day).first
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER
        col.setPadding(dp(4), dp((12 * sc).toInt()), dp(4), dp((12 * sc).toInt()))
        col.background = if (isNow) rounded(Color.argb(70, 138, 180, 248), 24, accent)
        else rounded(glass, 24, glassStroke)
        col.addView(label(if (h.tag.isEmpty()) "\u00A0" else h.tag, 12f * sc, accent, "sans-serif-medium"))
        col.addView(label(if (isNow) "現在" else "${h.hour}時", 19f * sc, if (isNow) accent else Color.WHITE, "sans-serif-medium"))
        val ip = lp(WRAP, WRAP)
        ip.topMargin = dp((8 * sc).toInt())
        col.addView(label(icon, 50f * sc), ip)
        val tp = lp(WRAP, WRAP)
        tp.topMargin = dp((8 * sc).toInt())
        col.addView(label("${h.temp}°", 32f * sc), tp)
        col.addView(label("💧${h.pop}%", 14f * sc, softWhite))
        return col
    }

    // ---------- expanded views (overlay) ----------
    private fun showOverlay(content: View, origin: View? = null) {
        val box = FrameLayout(this)
        // no opaque background: the shared weather background behind the pager shows through
        box.setBackgroundColor(Color.argb(40, 0, 0, 0))
        box.isClickable = true
        box.addView(content, FrameLayout.LayoutParams(MATCH, MATCH))
        val close = pill("閉じる") { closeExpanded() }
        val cp = FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP or Gravity.END)
        cp.setMargins(0, dp(24), dp(32), 0)
        box.addView(close, cp)
        root.addView(box, FrameLayout.LayoutParams(MATCH, MATCH))
        overlay = box
        playOpen(box, origin)
    }

    // short zoom-in from the tapped card; the pages underneath fade out
    private fun playOpen(box: View, origin: View?) {
        if (origin != null) {
            val loc = IntArray(2)
            origin.getLocationInWindow(loc)
            box.pivotX = loc[0] + origin.width / 2f
            box.pivotY = loc[1] + origin.height / 2f
            overlayShrink = 0.3f
        } else {
            overlayShrink = 0.94f
        }
        box.alpha = 0f
        box.scaleX = overlayShrink
        box.scaleY = overlayShrink
        box.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(260).setInterpolator(DecelerateInterpolator(1.6f)).start()
        pager.animate().cancel()
        dotRow.animate().cancel()
        pager.animate().alpha(0f).setDuration(220)
            .withEndAction { if (overlay != null) pager.visibility = View.INVISIBLE }.start()
        dotRow.animate().alpha(0f).setDuration(220).start()
    }

    private fun openClock(origin: View? = null) {
        dismissOverlayNow()
        val clock = TextClock(this)
        clock.format12Hour = "H:mm:ss"
        clock.format24Hour = "H:mm:ss"
        clock.textSize = 140f
        clock.setTextColor(Color.WHITE)
        clock.typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
        clock.gravity = Gravity.CENTER
        clock.includeFontPadding = false
        val date = TextClock(this)
        date.format12Hour = "yyyy年M月d日 EEEE"
        date.format24Hour = "yyyy年M月d日 EEEE"
        date.textSize = 28f
        date.setTextColor(softWhite)
        date.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        date.gravity = Gravity.CENTER
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER
        col.addView(clock, lp(MATCH, WRAP))
        val dp0 = lp(MATCH, WRAP)
        dp0.topMargin = dp(12)
        col.addView(date, dp0)
        showOverlay(col, origin)
    }

    private fun openForecast(origin: View? = null) {
        dismissOverlayNow()
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER
        col.setPadding(dp(32), dp(16), dp(32), dp(16))
        col.addView(label("7日間の天気予報", 24f, accent, "sans-serif-medium"))
        val days = dailyFc
        if (days.isEmpty()) {
            col.addView(label("予報データを取得中…", 20f, softWhite))
        } else {
            val row = LinearLayout(this)
            for (i in days.indices) {
                val tp = lp(0, WRAP, 1f)
                tp.setMargins(dp(4), 0, dp(4), 0)
                row.addView(dayTile(days[i], i == 0), tp)
            }
            val rp = lp(MATCH, WRAP)
            rp.topMargin = dp(24)
            col.addView(row, rp)
        }
        showOverlay(col, origin)
    }

    private fun dayTile(d: DayFc, isToday: Boolean): View {
        val wl = weatherLabel(d.code, true)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER
        col.setPadding(dp(6), dp(20), dp(6), dp(20))
        col.background = if (isToday) rounded(Color.argb(70, 138, 180, 248), 24, accent)
        else rounded(glass, 24, glassStroke)
        col.addView(label(d.label, 17f, if (isToday) accent else Color.WHITE, "sans-serif-medium"))
        val ip = lp(WRAP, WRAP)
        ip.topMargin = dp(10)
        col.addView(label(wl.first, 38f), ip)
        col.addView(label(wl.second, 13f, softWhite))
        val tp = lp(WRAP, WRAP)
        tp.topMargin = dp(10)
        col.addView(label("${d.hi}°", 28f), tp)
        col.addView(label("${d.lo}°", 20f, softWhite))
        val pp = lp(WRAP, WRAP)
        pp.topMargin = dp(8)
        col.addView(label("💧${d.pop}%", 13f, softWhite), pp)
        return col
    }

    // ---------- Page 2: Calendar ----------
    private fun calendarPage(): View {
        eventList = LinearLayout(this)
        eventList.orientation = LinearLayout.VERTICAL
        val scroll = ScrollView(this)
        scroll.isVerticalScrollBarEnabled = false
        scroll.addView(eventList)
        calScroll = scroll

        val page = LinearLayout(this)
        page.orientation = LinearLayout.VERTICAL
        page.setPadding(dp(64), dp(32), dp(64), dp(48))
        val header = label("今後7日間の予定", 28f, Color.WHITE, "sans-serif-light")
        val hp = lp(WRAP, WRAP)
        hp.bottomMargin = dp(16)
        page.addView(header, hp)
        page.addView(scroll, lp(MATCH, 0, 1f))
        return page
    }

    private fun eventRow(whenText: String, title: String): View {
        val t = label(whenText, 15f, accent, "sans-serif-medium")
        t.minWidth = dp(170)
        val n = label(title, 20f)
        val row = LinearLayout(this)
        row.gravity = Gravity.CENTER_VERTICAL
        row.background = rounded(glass, 20, glassStroke)
        row.setPadding(dp(24), dp(16), dp(24), dp(16))
        row.addView(t)
        row.addView(n)
        val p = lp(MATCH, WRAP)
        p.bottomMargin = dp(10)
        row.layoutParams = p
        return row
    }

    // ---------- Page 3: Tools ----------
    private fun toolsPage(): View {
        val sw = stopwatchPanel(false)
        swUpdaters.add(sw.update)
        val swCard = toolCard("ストップウォッチ", sw.view) { openExpanded(true, it) }
        val tm = timerPanel(false)
        tmUpdaters.add(tm.update)
        val tmCard = toolCard("タイマー", tm.view) { openExpanded(false, it) }

        val cards = LinearLayout(this)
        cards.addView(swCard, lp(0, MATCH, 1f))
        val tcp = lp(0, MATCH, 1f)
        tcp.leftMargin = dp(20)
        cards.addView(tmCard, tcp)

        val alarm = pill("アラームを開く", true) { startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS)) }
        val ap = lp(WRAP, WRAP)
        ap.topMargin = dp(24)

        val page = LinearLayout(this)
        page.orientation = LinearLayout.VERTICAL
        page.gravity = Gravity.CENTER
        page.setPadding(dp(64), dp(24), dp(64), dp(48))
        page.addView(cards, lp(MATCH, WRAP))
        page.addView(alarm, ap)
        return page
    }

    private fun toolCard(title: String, content: View, onClick: (View) -> Unit): LinearLayout {
        val card = glassCard()
        card.gravity = Gravity.CENTER_HORIZONTAL
        card.addView(label(title, 14f, accent, "sans-serif-medium"))
        card.addView(content, lp(MATCH, 0, 1f))      // content centred below the title
        val hp = lp(WRAP, WRAP)
        hp.topMargin = dp(12)
        card.addView(label("タップで拡大", 12f, Color.argb(130, 255, 255, 255)), hp)
        card.isClickable = true
        card.setOnClickListener { onClick(it) }
        return card
    }

    // ---------- Stopwatch ----------
    private fun swElapsed(): Long =
        swAcc + (if (swRunning) SystemClock.elapsedRealtime() - swStart else 0L)

    private fun swToggle() {
        if (swRunning) {
            swAcc = swElapsed()
            swRunning = false
        } else {
            swStart = SystemClock.elapsedRealtime()
            swRunning = true
        }
        refreshAll()
        ensureTick()
    }

    private fun swReset() {
        swRunning = false
        swAcc = 0L
        refreshAll()
    }

    private fun stopwatchPanel(big: Boolean): Panel {
        val mode = if (big) 1 else 0
        val time = label("0:00", if (big) 120f else 56f, Color.WHITE, "sans-serif-thin")
        time.gravity = Gravity.CENTER
        time.includeFontPadding = false
        val primary = pill("開始", true, mode) { swToggle() }
        primary.minWidth = dp(if (big) 200 else 110)
        val reset = pill("リセット", false, mode) { swReset() }
        val row = LinearLayout(this)
        row.gravity = Gravity.CENTER
        row.addView(primary)
        row.addView(reset, spaced())
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER
        col.addView(time, lp(MATCH, WRAP))
        val rp = lp(WRAP, WRAP)
        rp.topMargin = dp(if (big) 28 else 16)
        col.addView(row, rp)
        val upd: () -> Unit = {
            time.text = fmtSw(swElapsed(), big)
            primary.text = if (swRunning) "停止" else if (swAcc > 0L) "再開" else "開始"
        }
        upd()
        return Panel(col, upd)
    }

    // ---------- Timer ----------
    private fun tmPrimary() {
        when (tmState) {
            TM_RUNNING -> {
                tmRemaining = Math.max(0L, tmEnd - SystemClock.elapsedRealtime())
                tmState = TM_PAUSED
            }
            TM_PAUSED -> {
                tmEnd = SystemClock.elapsedRealtime() + tmRemaining
                tmState = TM_RUNNING
            }
            else -> {
                stopAlarm()
                if (tmTotal > 0L) {
                    tmRemaining = tmTotal
                    tmEnd = SystemClock.elapsedRealtime() + tmRemaining
                    tmState = TM_RUNNING
                }
            }
        }
        refreshAll()
        ensureTick()
    }

    private fun tmStop() {
        stopAlarm()
        tmState = TM_IDLE
        tmRemaining = tmTotal
        refreshAll()
    }

    private fun setTimerTotal(ms: Long, start: Boolean) {
        stopAlarm()
        tmTotal = ms
        tmRemaining = ms
        tmState = TM_IDLE
        if (start) tmPrimary() else refreshAll()
    }

    private fun timerTick() {
        if (tmState == TM_RUNNING) {
            val left = tmEnd - SystemClock.elapsedRealtime()
            if (left <= 0L) {
                tmRemaining = 0L
                tmState = TM_DONE
                startAlarm()
            } else {
                tmRemaining = left
            }
        }
    }

    private fun timerPanel(big: Boolean): Panel {
        val mode = if (big) 1 else 0
        val time = label("5:00", if (big) 120f else 56f, Color.WHITE, "sans-serif-thin")
        time.gravity = Gravity.CENTER
        time.includeFontPadding = false
        val primary = pill("開始", true, mode) { tmPrimary() }
        primary.minWidth = dp(if (big) 200 else 120)
        val stop = pill("停止", false, mode) { tmStop() }
        val row = LinearLayout(this)
        row.gravity = Gravity.CENTER
        row.addView(primary)
        row.addView(stop, spaced())

        val presets = LinearLayout(this)
        presets.gravity = Gravity.CENTER
        val pmode = if (big) 0 else 2
        val mins = if (big) listOf(1, 3, 5, 10) else listOf(1, 5, 10)
        for (m in mins) {
            presets.addView(pill("${m}分", false, pmode) { setTimerTotal(m * 60_000L, true) }, spaced())
        }
        presets.addView(pill(if (big) "時間を設定" else "設定", false, pmode) { showTimerPicker() }, spaced())

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER
        col.addView(time, lp(MATCH, WRAP))
        val rp = lp(WRAP, WRAP)
        rp.topMargin = dp(if (big) 28 else 16)
        col.addView(row, rp)
        val pp = lp(WRAP, WRAP)
        pp.topMargin = dp(if (big) 24 else 12)
        col.addView(presets, pp)

        val upd: () -> Unit = {
            time.text = fmtHms((tmRemaining + 999) / 1000)
            time.setTextColor(if (tmState == TM_DONE) Color.rgb(255, 138, 128) else Color.WHITE)
            primary.text = when (tmState) {
                TM_RUNNING -> "一時停止"
                TM_PAUSED -> "再開"
                else -> "開始"
            }
        }
        upd()
        return Panel(col, upd)
    }

    private fun showTimerPicker() {
        fun picker(max: Int, v: Int): NumberPicker {
            val np = NumberPicker(this)
            np.minValue = 0
            np.maxValue = max
            np.value = v
            return np
        }
        val total = tmTotal / 1000
        val h = picker(23, (total / 3600).toInt())
        val m = picker(59, (total / 60 % 60).toInt())
        val s = picker(59, (total % 60).toInt())
        fun unit(t: String): TextView {
            val u = label(t, 16f)
            u.setPadding(dp(6), 0, dp(14), 0)
            return u
        }
        val row = LinearLayout(this)
        row.gravity = Gravity.CENTER
        row.setPadding(dp(16), dp(16), dp(16), dp(8))
        row.addView(h); row.addView(unit("時間"))
        row.addView(m); row.addView(unit("分"))
        row.addView(s); row.addView(unit("秒"))
        fun ms() = (h.value * 3600L + m.value * 60L + s.value) * 1000L
        AlertDialog.Builder(this)
            .setTitle("タイマーの時間")
            .setView(row)
            .setPositiveButton("開始") { _, _ -> if (ms() > 0L) setTimerTotal(ms(), true) }
            .setNeutralButton("セット") { _, _ -> if (ms() > 0L) setTimerTotal(ms(), false) }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun startAlarm() {
        stopAlarm()
        // make sure the alarm stream is audible
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (am.getStreamVolume(AudioManager.STREAM_ALARM) == 0) {
                am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM) / 2, 0)
            }
        } catch (e: Exception) { }

        val uris = listOf(
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_NOTIFICATION),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        )
        var ok = false
        for (u in uris) {
            if (u == null) continue
            var mp: MediaPlayer? = null
            try {
                mp = MediaPlayer()
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                mp.setDataSource(this, u)
                mp.isLooping = true
                mp.prepare()
                mp.start()
                player = mp
                ok = true
                break
            } catch (e: Exception) {
                try { mp?.release() } catch (e2: Exception) { }
            }
        }
        if (!ok) {
            // last resort: built-in beep
            try {
                val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 5000)
                toneGen = tg
            } catch (e: Exception) { }
        }
        handler.removeCallbacks(alarmStop)
        handler.postDelayed(alarmStop, 60_000)
    }

    private fun stopAlarm() {
        handler.removeCallbacks(alarmStop)
        try { player?.stop() } catch (e: Exception) { }
        try { player?.release() } catch (e: Exception) { }
        player = null
        try { toneGen?.release() } catch (e: Exception) { }
        toneGen = null
    }

    // ---------- shared ticking / formatting ----------
    private fun refreshAll() {
        swUpdaters.forEach { it() }
        tmUpdaters.forEach { it() }
    }

    private fun ensureTick() {
        if (!fastRunning) {
            fastRunning = true
            handler.post(fastTick)
        }
    }

    private fun fmtHms(totalSec: Long): String {
        val h = totalSec / 3600
        val m = totalSec / 60 % 60
        val s = totalSec % 60
        return if (h > 0L) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
    }

    private fun fmtSw(ms: Long, tenths: Boolean): String {
        val base = fmtHms(ms / 1000)
        return if (tenths) base + "." + (ms / 100 % 10) else base
    }

    // ---------- expanded (fullscreen) view ----------
    private fun openExpanded(isStopwatch: Boolean, origin: View? = null) {
        dismissOverlayNow()
        val panel = if (isStopwatch) stopwatchPanel(true) else timerPanel(true)
        val list = if (isStopwatch) swUpdaters else tmUpdaters
        list.add(panel.update)
        overlayUpdater = panel.update
        overlayList = list

        val center = LinearLayout(this)
        center.orientation = LinearLayout.VERTICAL
        center.gravity = Gravity.CENTER
        center.setPadding(dp(32), dp(16), dp(32), dp(16))
        center.addView(label(if (isStopwatch) "ストップウォッチ" else "タイマー", 20f, accent, "sans-serif-medium"))
        val pp = lp(MATCH, WRAP)
        pp.topMargin = dp(8)
        center.addView(panel.view, pp)
        showOverlay(center, origin)
    }

    private fun detachOverlayState() {
        overlayUpdater?.let { u -> overlayList?.remove(u) }
        overlayUpdater = null
        overlayList = null
    }

    // remove an open overlay immediately (no animation)
    private fun dismissOverlayNow() {
        overlay?.let { root.removeView(it) }
        overlay = null
        detachOverlayState()
    }

    // short zoom-out back to the card; the pages fade back in
    private fun closeExpanded() {
        val box = overlay ?: return
        overlay = null
        detachOverlayState()
        box.animate().alpha(0f).scaleX(overlayShrink).scaleY(overlayShrink)
            .setDuration(200).setInterpolator(AccelerateInterpolator(1.4f))
            .withEndAction { root.removeView(box) }.start()
        pager.animate().cancel()
        dotRow.animate().cancel()
        pager.visibility = View.VISIBLE
        dotRow.visibility = View.VISIBLE
        pager.animate().alpha(1f).setDuration(220).start()
        dotRow.animate().alpha(1f).setDuration(220).start()
    }

    private fun spaced(): LinearLayout.LayoutParams {
        val p = lp(WRAP, WRAP)
        p.leftMargin = dp(10)
        return p
    }

    // ---------- Weather ----------
    private fun weatherMessage(msg: String) {
        wIcon.text = "📍"
        wTemp.text = "--°"
        wDesc.text = msg
        wRange.text = ""
    }

    private fun loadWeather() {
        if (!granted(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            weatherMessage("位置情報の許可が必要です"); return
        }
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = lm.getProviders(true)
        val loc = providers.mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
        if (loc != null) {
            fetchWeather(loc.latitude, loc.longitude)
        } else if (providers.isNotEmpty()) {
            weatherMessage("現在地を取得中…")
            lm.requestSingleUpdate(providers[0], object : LocationListener {
                override fun onLocationChanged(l: Location) { fetchWeather(l.latitude, l.longitude) }
                override fun onProviderEnabled(p: String) {}
                override fun onProviderDisabled(p: String) {}
                override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
            }, Looper.getMainLooper())
        } else {
            weatherMessage("位置情報がオフです")
        }
    }

    private fun fetchWeather(lat: Double, lon: Double) {
        Thread {
            try {
                val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                        "&current=temperature_2m,weather_code,is_day" +
                        "&hourly=temperature_2m,weather_code,precipitation_probability,is_day" +
                        "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                        "&timezone=auto&forecast_days=7"
                val con = URL(url).openConnection() as HttpsURLConnection
                val json = JSONObject(con.inputStream.bufferedReader().readText())
                val cur = json.getJSONObject("current")
                val daily = json.getJSONObject("daily")
                val t = Math.round(cur.getDouble("temperature_2m"))
                val hi = Math.round(daily.getJSONArray("temperature_2m_max").getDouble(0))
                val lo = Math.round(daily.getJSONArray("temperature_2m_min").getDouble(0))
                val code = cur.getInt("weather_code")
                val day = cur.optInt("is_day", 1) == 1
                // next 24 hours, starting from the current hour (location time)
                val hj = json.getJSONObject("hourly")
                val hTimes = hj.getJSONArray("time")
                val hTemps = hj.getJSONArray("temperature_2m")
                val hCodes = hj.getJSONArray("weather_code")
                val hPops = hj.getJSONArray("precipitation_probability")
                val hDays = hj.getJSONArray("is_day")
                val dTimes = daily.getJSONArray("time")
                val nowKey = cur.getString("time").substring(0, 13)
                var startIdx = 0
                for (k in 0 until hTimes.length()) {
                    if (hTimes.getString(k).startsWith(nowKey)) { startIdx = k; break }
                }
                val dayFmt = SimpleDateFormat("M/d(E)", Locale.JAPAN)
                val dayParse = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val hours = ArrayList<HourFc>()
                var idx = startIdx
                while (idx < hTimes.length() && hours.size < 24) {
                    val tm = hTimes.getString(idx)
                    val hr = tm.substring(11, 13).toInt()
                    val tag = if (hours.isEmpty() || hr == 0) dayFmt.format(dayParse.parse(tm.substring(0, 10))!!) else ""
                    hours.add(
                        HourFc(
                            hr,
                            Math.round(hTemps.optDouble(idx, 0.0)).toInt(),
                            hCodes.optInt(idx, 0), hPops.optInt(idx, 0), hDays.optInt(idx, 1) == 1, tag
                        )
                    )
                    idx++
                }
                // 7-day forecast
                val dCodes = daily.getJSONArray("weather_code")
                val dHi = daily.getJSONArray("temperature_2m_max")
                val dLo = daily.getJSONArray("temperature_2m_min")
                val dPop = daily.getJSONArray("precipitation_probability_max")
                val parseFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val outFmt = SimpleDateFormat("M/d(E)", Locale.JAPAN)
                val days = ArrayList<DayFc>()
                for (i in 0 until dTimes.length()) {
                    val label = when (i) {
                        0 -> "今日"
                        1 -> "明日"
                        else -> outFmt.format(parseFmt.parse(dTimes.getString(i))!!)
                    }
                    days.add(
                        DayFc(
                            label, dCodes.optInt(i, 0),
                            Math.round(dHi.optDouble(i, 0.0)).toInt(),
                            Math.round(dLo.optDouble(i, 0.0)).toInt(),
                            dPop.optInt(i, 0)
                        )
                    )
                }
                val (icon, desc) = weatherLabel(code, day)
                runOnUiThread {
                    wIcon.text = icon
                    wTemp.text = "$t°"
                    wDesc.text = desc
                    wRange.text = "最高 $hi°  /  最低 $lo°"
                    weatherView.setWeather(code, day)
                    hourlyFc = hours
                    dailyFc = days
                    buildHourly()
                }
            } catch (e: Exception) {
                runOnUiThread { weatherMessage("天気を取得できません") }
            }
        }.start()
    }

    private fun weatherLabel(c: Int, day: Boolean): Pair<String, String> = when (c) {
        0 -> if (day) Pair("☀️", "快晴") else Pair("🌙", "快晴")
        1, 2 -> if (day) Pair("🌤", "晴れ") else Pair("🌙", "晴れ")
        3 -> Pair("☁️", "くもり")
        45, 48 -> Pair("🌫", "霧")
        in 51..57 -> Pair("🌦", "霧雨")
        in 61..67 -> Pair("🌧", "雨")
        in 71..77 -> Pair("❄️", "雪")
        in 80..82 -> Pair("🌦", "にわか雨")
        in 85..86 -> Pair("🌨", "にわか雪")
        in 95..99 -> Pair("⛈", "雷雨")
        else -> Pair("🌡", "—")
    }

    // ---------- Calendar ----------
    // all-day instances are stored as UTC midnight; convert to the same date at local midnight
    private fun allDayToLocal(utcMs: Long): Long {
        val u = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        u.timeInMillis = utcMs
        val l = java.util.Calendar.getInstance()
        l.clear()
        l.set(
            u.get(java.util.Calendar.YEAR), u.get(java.util.Calendar.MONTH),
            u.get(java.util.Calendar.DAY_OF_MONTH), 0, 0, 0
        )
        return l.timeInMillis
    }

    private fun loadCalendar() {
        val sy = calScroll.scrollY
        eventList.removeAllViews()
        if (!granted(Manifest.permission.READ_CALENDAR)) {
            eventList.addView(label("カレンダーの許可が必要です", 20f, softWhite))
            nextText.text = "許可が必要です"
            return
        }
        val now = System.currentTimeMillis()
        val b = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(b, now - 86_400_000L)      // look back a day (all-day events are stored in UTC), then drop what is over
        ContentUris.appendId(b, now + 7L * 86_400_000)
        val timeFmt = SimpleDateFormat("M/d(E) HH:mm", Locale.JAPAN)
        val allDayFmt = SimpleDateFormat("M/d(E)", Locale.JAPAN)
        allDayFmt.timeZone = TimeZone.getTimeZone("UTC")
        var first: String? = null
        var count = 0
        try {
            contentResolver.query(
                b.build(),
                arrayOf(
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.ALL_DAY,
                    CalendarContract.Instances.END
                ),
                null, null, "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { c ->
                while (c.moveToNext() && count < 20) {
                    val title = c.getString(0) ?: "(タイトルなし)"
                    val begin = c.getLong(1)
                    val allDay = c.getInt(2) == 1
                    // events that are already over disappear (all-day events: when their local day is over)
                    val endMs = if (allDay) allDayToLocal(c.getLong(3)) else c.getLong(3)
                    if (endMs <= now) continue
                    val whenText = if (allDay) allDayFmt.format(Date(begin)) + " 終日"
                    else timeFmt.format(Date(begin))
                    eventList.addView(eventRow(whenText, title))
                    if (first == null) first = whenText + "\n" + title
                    count++
                }
            }
        } catch (e: Exception) { }
        if (count == 0) eventList.addView(label("今後7日間の予定はありません", 20f, softWhite))
        nextText.text = first ?: "予定はありません"
        calScroll.post { calScroll.scrollTo(0, sy) }
    }

    // ---------- App drawer ----------
    private fun showApps() {
        val pm = packageManager
        val apps = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        ).filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val grid = GridView(this)
        grid.numColumns = 6
        grid.verticalSpacing = dp(12)
        grid.horizontalSpacing = dp(8)
        grid.setPadding(dp(24), dp(24), dp(24), dp(24))
        grid.selector = ColorDrawable(Color.TRANSPARENT)
        grid.background = rounded(Color.argb(245, 18, 22, 40), 32)
        grid.adapter = object : BaseAdapter() {
            override fun getCount() = apps.size
            override fun getItem(p: Int): Any = apps[p]
            override fun getItemId(p: Int) = p.toLong()
            override fun getView(p: Int, v: View?, parent: ViewGroup?): View {
                val ri = apps[p]
                val cell = LinearLayout(this@MainActivity)
                cell.orientation = LinearLayout.VERTICAL
                cell.gravity = Gravity.CENTER_HORIZONTAL
                cell.setPadding(0, dp(8), 0, dp(8))
                val icon = ImageView(this@MainActivity)
                icon.setImageDrawable(ri.loadIcon(pm))
                cell.addView(icon, lp(dp(56), dp(56)))
                val name = label(ri.loadLabel(pm).toString(), 13f)
                name.maxLines = 1
                name.ellipsize = TextUtils.TruncateAt.END
                name.gravity = Gravity.CENTER_HORIZONTAL
                val npp = lp(MATCH, WRAP)
                npp.topMargin = dp(6)
                cell.addView(name, npp)
                return cell
            }
        }
        grid.setOnItemClickListener { _, _, pos, _ ->
            pm.getLaunchIntentForPackage(apps[pos].activityInfo.packageName)?.let {
                startActivity(it)
                dialog.dismiss()
            }
        }
        dialog.setContentView(grid)
        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
    }

    // ---------- Pager ----------
    private class PageAdapter(val pages: List<View>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount() = pages.size
        override fun getItemViewType(position: Int) = position
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val v = pages[viewType]
            (v.parent as? ViewGroup)?.removeView(v)
            v.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            return object : RecyclerView.ViewHolder(v) {}
        }
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
    }
}


// =====================================================================
//  Animated weather background (page 1)
// =====================================================================
class WeatherView(context: Context) : View(context) {
    companion object {
        const val NONE = 0
        const val CLEAR = 1
        const val PARTLY = 2
        const val CLOUDY = 3
        const val FOG = 4
        const val RAIN = 5
        const val SNOW = 6
        const val STORM = 7
    }

    private class Cloud(var x: Float, val y: Float, val scale: Float, val speed: Float, val w: Float, val puffs: FloatArray)
    private class Fog(var x: Float, val y: Float, val sx: Float, val sy: Float, val speed: Float)

    private val dp = resources.displayMetrics.density
    private val rnd = java.util.Random()
    private var type = NONE
    private var day = true
    private var intensity = 0.6f
    private var time = 0f
    private var fade = 1f
    private var last = 0L

    private var skyFrom: IntArray? = null
    private var skyTo: IntArray? = null
    private var shaderFrom: Shader? = null
    private var shaderTo: Shader? = null

    private val skyPaint = Paint()
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fogPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val solid = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private val sunGlow = RadialGradient(
        0f, 0f, 100f,
        intArrayOf(Color.argb(210, 255, 250, 215), Color.argb(90, 255, 215, 120), Color.argb(0, 255, 200, 100)),
        floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP
    )
    private val moonGlow = RadialGradient(
        0f, 0f, 100f,
        intArrayOf(Color.argb(150, 210, 225, 255), Color.argb(50, 170, 195, 255), Color.argb(0, 150, 180, 255)),
        floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP
    )

    private val clouds = ArrayList<Cloud>()
    private val fogs = ArrayList<Fog>()
    private var cloudUnit = 100f

    private var starX = FloatArray(0)
    private var starY = FloatArray(0)
    private var starR = FloatArray(0)
    private var starP = FloatArray(0)
    private var shootT = -1f
    private var shootTimer = 5f
    private var shootX = 0f
    private var shootY = 0f
    private val moonPath = Path()
    private val moonCut = Path()
    private var moonR = 40f

    private var rainN = 0
    private var rx = FloatArray(0)
    private var ry = FloatArray(0)
    private var rsp = FloatArray(0)
    private var rlen = FloatArray(0)
    private var rwid = FloatArray(0)
    private var ral = FloatArray(0)
    private val ripX = FloatArray(20)
    private val ripY = FloatArray(20)
    private val ripAge = FloatArray(20) { 1f }
    private var ripTimer = 0f

    private var snowN = 0
    private var fX = FloatArray(0)
    private var fY = FloatArray(0)
    private var fSp = FloatArray(0)
    private var fR = FloatArray(0)
    private var fPh = FloatArray(0)
    private var fAl = FloatArray(0)

    private var bolt = FloatArray(0)
    private var flashT = -1f
    private var flashTimer = 2.5f

    // ---- photo backgrounds ----
    private var photo: Bitmap? = null
    private var photoFrom: Bitmap? = null
    private var photoDark = false
    private var photoFromDark = false
    private var photoFade = 1f
    private var photoStamp = ""
    private var loadToken = 0
    private val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val scrimPaint = Paint()
    private val photoMatrix = Matrix()
    private val nightFilter = LightingColorFilter(Color.rgb(70, 84, 132), Color.rgb(0, 0, 6))
    private var scrimV: Shader? = null
    private var scrimH: Shader? = null
    private val exts = listOf("jpg", "jpeg", "png", "webp")


    private fun rf(a: Float, b: Float) = a + rnd.nextFloat() * (b - a)

    fun setWeather(code: Int, isDay: Boolean) {
        val nt = when (code) {
            0, 1 -> CLEAR
            2 -> PARTLY
            3 -> CLOUDY
            45, 48 -> FOG
            in 51..67, in 80..82 -> RAIN
            in 71..77, in 85..86 -> SNOW
            in 95..99 -> STORM
            else -> CLOUDY
        }
        val ni = when (code) {
            51, 56, 71, 85 -> 0.3f
            53, 61, 66, 73, 80 -> 0.55f
            55, 57, 63, 77, 81, 95 -> 0.8f
            else -> 1f
        }
        if (nt == type && isDay == day && ni == intensity) return
        skyFrom = skyTo
        type = nt
        day = isDay
        intensity = ni
        skyTo = skyFor(nt, isDay)
        fade = 0f
        flashT = -1f
        flashTimer = rf(1.5f, 4f)
        buildScene()
        startLoad()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        buildScene()
    }

    private fun skyFor(t: Int, d: Boolean): IntArray {
        fun c(a: Int, b: Int, na: Int, nb: Int) = if (d) intArrayOf(a, b) else intArrayOf(na, nb)
        return when (t) {
            CLEAR -> c(Color.rgb(28, 96, 196), Color.rgb(120, 190, 242), Color.rgb(5, 8, 24), Color.rgb(24, 36, 86))
            PARTLY -> c(Color.rgb(46, 100, 176), Color.rgb(140, 188, 224), Color.rgb(10, 16, 38), Color.rgb(36, 50, 94))
            CLOUDY -> c(Color.rgb(78, 92, 112), Color.rgb(142, 154, 168), Color.rgb(22, 26, 36), Color.rgb(52, 58, 74))
            FOG -> c(Color.rgb(100, 110, 120), Color.rgb(160, 168, 174), Color.rgb(26, 30, 36), Color.rgb(66, 72, 80))
            RAIN -> c(Color.rgb(40, 56, 80), Color.rgb(84, 104, 130), Color.rgb(10, 16, 28), Color.rgb(32, 44, 64))
            SNOW -> c(Color.rgb(84, 102, 132), Color.rgb(150, 166, 190), Color.rgb(26, 36, 60), Color.rgb(80, 96, 124))
            else -> c(Color.rgb(26, 22, 48), Color.rgb(64, 56, 92), Color.rgb(8, 6, 20), Color.rgb(36, 30, 60))
        }
    }

    private fun cloudTint(): IntArray = when (type) {
        CLEAR, PARTLY -> if (day) intArrayOf(255, 255, 255, 140) else intArrayOf(150, 165, 205, 70)
        CLOUDY -> if (day) intArrayOf(236, 241, 246, 190) else intArrayOf(112, 122, 148, 130)
        RAIN -> if (day) intArrayOf(108, 120, 136, 215) else intArrayOf(58, 66, 86, 195)
        STORM -> if (day) intArrayOf(66, 68, 90, 230) else intArrayOf(34, 36, 56, 215)
        SNOW -> if (day) intArrayOf(224, 231, 241, 190) else intArrayOf(118, 130, 158, 140)
        else -> intArrayOf(226, 230, 235, 110)
    }

    private fun buildScene() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f || type == NONE) return

        skyTo?.let { shaderTo = LinearGradient(0f, 0f, 0f, h, it[0], it[1], Shader.TileMode.CLAMP) }
        scrimV = LinearGradient(0f, 0f, 0f, h,
            intArrayOf(Color.argb(80, 0, 0, 0), Color.argb(20, 0, 0, 0), Color.argb(120, 0, 0, 0)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        scrimH = LinearGradient(0f, 0f, w * 0.65f, 0f,
            intArrayOf(Color.argb(95, 0, 0, 0), Color.argb(0, 0, 0, 0)), null, Shader.TileMode.CLAMP)
        shaderFrom = skyFrom?.let { LinearGradient(0f, 0f, 0f, h, it[0], it[1], Shader.TileMode.CLAMP) }

        // cloud + fog shaders
        val t = cloudTint()
        cloudPaint.shader = RadialGradient(
            0f, 0f, 100f,
            intArrayOf(Color.argb(t[3], t[0], t[1], t[2]), Color.argb(t[3] * 3 / 4, t[0], t[1], t[2]), Color.argb(0, t[0], t[1], t[2])),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        fogPaint.shader = RadialGradient(
            0f, 0f, 100f,
            intArrayOf(Color.argb(150, 226, 230, 235), Color.argb(80, 226, 230, 235), Color.argb(0, 226, 230, 235)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )

        // clouds
        clouds.clear()
        cloudUnit = min(w, h) * 0.16f
        val cfg = when (type) {
            CLEAR -> floatArrayOf(3f, 0.08f, 0.45f, 0.6f, 1.0f)
            PARTLY -> floatArrayOf(6f, 0.05f, 0.45f, 0.8f, 1.3f)
            CLOUDY -> floatArrayOf(9f, -0.02f, 0.4f, 1.1f, 1.8f)
            FOG -> floatArrayOf(3f, 0.05f, 0.4f, 1.0f, 1.6f)
            RAIN, STORM -> floatArrayOf(9f, -0.05f, 0.3f, 1.2f, 2.0f)
            SNOW -> floatArrayOf(7f, -0.03f, 0.3f, 1.1f, 1.7f)
            else -> floatArrayOf(0f, 0f, 0f, 1f, 1f)
        }
        for (i in 0 until cfg[0].toInt()) {
            val sc = rf(cfg[3], cfg[4])
            val k = 5 + rnd.nextInt(3)
            val mid = (k - 1) / 2f
            val puffs = FloatArray(k * 3)
            for (j in 0 until k) {
                val edge = Math.abs(j - mid) / mid
                puffs[j * 3] = (j - mid) * 0.55f + rf(-0.08f, 0.08f)
                puffs[j * 3 + 1] = -0.22f * (1f - edge) + rf(-0.06f, 0.06f)
                puffs[j * 3 + 2] = 0.55f + 0.4f * (1f - edge) + rf(-0.05f, 0.1f)
            }
            val cw = (k * 0.55f + 1f) * cloudUnit * sc
            clouds.add(Cloud(rf(-cw, w + cw), h * rf(cfg[1], cfg[2]), sc, rf(6f, 22f) * dp * sc, cw, puffs))
        }

        // fog
        fogs.clear()
        if (type == FOG) {
            for (i in 0 until 6) {
                fogs.add(Fog(rf(-w * 0.3f, w), h * rf(0.25f, 0.95f), w * rf(0.35f, 0.6f) / 100f, h * rf(0.08f, 0.16f) / 100f, rf(8f, 20f) * dp))
            }
        }

        // stars + moon
        starX = FloatArray(90) { rf(0f, w) }
        starY = FloatArray(90) { rf(0f, h * 0.7f) }
        starR = FloatArray(90) { rf(0.6f, 1.8f) * dp }
        starP = FloatArray(90) { rf(0f, 1f) }
        moonR = min(w, h) * 0.07f
        moonPath.reset()
        moonPath.addCircle(0f, 0f, moonR, Path.Direction.CW)
        moonCut.reset()
        moonCut.addCircle(moonR * 0.5f, -moonR * 0.18f, moonR * 0.88f, Path.Direction.CW)
        moonPath.op(moonCut, Path.Op.DIFFERENCE)
        shootT = -1f
        shootTimer = rf(3f, 8f)

        // rain
        rainN = if (type == RAIN || type == STORM) (50 + 200 * intensity).toInt() else 0
        val spd = 0.6f + 0.4f * intensity
        rx = FloatArray(rainN)
        ry = FloatArray(rainN)
        rsp = FloatArray(rainN)
        rlen = FloatArray(rainN)
        rwid = FloatArray(rainN)
        ral = FloatArray(rainN)
        for (i in 0 until rainN) {
            val dz = rnd.nextFloat()
            rsp[i] = (800f + 900f * dz) * dp * spd
            rlen[i] = (14f + 24f * dz) * dp * spd
            rwid[i] = (0.8f + 1.4f * dz) * dp
            ral[i] = 60f + 130f * dz
            rx[i] = rf(0f, w * 1.3f)
            ry[i] = rf(0f, h)
        }
        for (i in ripAge.indices) ripAge[i] = 1f

        // snow
        snowN = if (type == SNOW) (45 + 150 * intensity).toInt() else 0
        fX = FloatArray(snowN) { rf(0f, w) }
        fY = FloatArray(snowN) { rf(0f, h) }
        fR = FloatArray(snowN) { rf(1.2f, 3.8f) * dp }
        fSp = FloatArray(snowN) { (18f + 14f * (fR[it] / dp)) * dp }
        fPh = FloatArray(snowN) { rf(0f, 6.28f) }
        fAl = FloatArray(snowN) { rf(140f, 235f) }
    }

    override fun onDraw(canvas: Canvas) {
        if (type == NONE) return
        val now = System.nanoTime()
        var dt = if (last == 0L) 0.016f else (now - last) / 1_000_000_000f
        last = now
        if (dt > 0.05f) dt = 0.05f
        time += dt
        if (fade < 1f) fade = min(1f, fade + dt)
        if (photoFade < 1f) photoFade = min(1f, photoFade + dt / 1.2f)

        drawScene(canvas, dt)

        val animated = type == RAIN || type == SNOW || type == STORM || (type == CLEAR && !day)
        if (photo != null && photoFade >= 1f && !animated) postInvalidateDelayed(50) else postInvalidateOnAnimation()
    }

    private fun drawScene(canvas: Canvas, dt: Float) {
        val ph = photo
        val needProc = ph == null || (photoFade < 1f && photoFrom == null)
        if (needProc) drawProcedural(canvas, dt)
        if (ph != null) {
            if (photoFade < 1f) photoFrom?.let { drawPhoto(canvas, it, 255, photoFromDark) }
            drawPhoto(canvas, ph, (255 * photoFade).toInt(), photoDark)
            drawScrim(canvas, photoFade)
        }
        drawParticles(canvas, dt, ph != null)
    }

    // ---------- photo handling ----------
    private fun imageName(t: Int, d: Boolean): String = when (t) {
        CLEAR -> if (d) "clear_day" else "clear_night"
        PARTLY -> "partly"
        CLOUDY -> "cloudy"
        FOG -> "fog"
        RAIN -> "rain"
        SNOW -> "snow"
        else -> "storm"
    }

    // where an image for [name] lives: app external files dir first, then bundled assets
    private fun stampFor(name: String): String {
        val dir = context.getExternalFilesDir("weather")
        if (dir != null) {
            for (e in exts) {
                val f = File(dir, "$name.$e")
                if (f.exists()) return "ext:" + f.name + ":" + f.lastModified()
            }
        }
        try {
            val list = context.assets.list("weather")
            if (list != null) {
                for (e in exts) {
                    if (list.contains("$name.$e")) return "asset:$name.$e"
                }
            }
        } catch (ex: Exception) { }
        return ""
    }

    private fun decodeStamp(stamp: String): Bitmap? {
        if (stamp.isEmpty()) return null
        val target = max(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
        fun open(): java.io.InputStream? {
            return if (stamp.startsWith("ext:")) {
                val dir = context.getExternalFilesDir("weather")
                if (dir != null) java.io.FileInputStream(File(dir, stamp.split(":")[1])) else null
            } else {
                context.assets.open("weather/" + stamp.removePrefix("asset:"))
            }
        }
        return try {
            val bo = BitmapFactory.Options()
            bo.inJustDecodeBounds = true
            open()?.use { BitmapFactory.decodeStream(it, null, bo) }
            var ss = 1
            while (bo.outWidth / (ss * 2) >= target) ss *= 2
            val o = BitmapFactory.Options()
            o.inSampleSize = ss
            open()?.use { BitmapFactory.decodeStream(it, null, o) }
        } catch (e: Throwable) {
            null
        }
    }

    private fun startLoad() {
        val name = imageName(type, day)
        val stamp = stampFor(name)
        val dark = !day && name != "clear_night"
        if (stamp == photoStamp && photo != null) {
            photoDark = dark
            return
        }
        photoStamp = stamp
        val token = ++loadToken
        if (stamp.isEmpty()) {
            photo = null
            photoFrom = null
            return
        }
        Thread {
            val bmp = decodeStamp(stamp)
            post {
                if (token == loadToken) {
                    if (bmp != null) {
                        photoFrom = photo
                        photoFromDark = photoDark
                        photo = bmp
                        photoDark = dark
                        photoFade = 0f
                    } else {
                        photo = null
                        photoFrom = null
                        photoStamp = ""
                    }
                    invalidate()
                }
            }
        }.start()
    }

    // call when the app comes back: picks up images that were added/replaced with adb push
    fun refreshImages() {
        if (type == NONE) return
        if (stampFor(imageName(type, day)) != photoStamp) startLoad()
    }

    private fun drawPhoto(c: Canvas, bmp: Bitmap, alpha: Int, dark: Boolean) {
        val w = width.toFloat()
        val h = height.toFloat()
        val base = max(w / bmp.width, h / bmp.height)
        val sc = base * (1.12f + 0.06f * sin(time * 0.05f))      // slow zoom
        val dx = max(0f, bmp.width * sc - w)
        val dy = max(0f, bmp.height * sc - h)
        val px = 0.5f + 0.5f * sin(time * 0.04f)                  // slow pan
        val py = 0.5f + 0.5f * sin(time * 0.031f + 1f)
        photoMatrix.reset()
        photoMatrix.postScale(sc, sc)
        photoMatrix.postTranslate(-dx * px, -dy * py)
        photoPaint.alpha = alpha
        photoPaint.colorFilter = if (dark) nightFilter else null
        c.drawBitmap(bmp, photoMatrix, photoPaint)
    }

    // soft dark gradients so the white clock / text stays readable on bright photos
    private fun drawScrim(c: Canvas, a: Float) {
        val w = width.toFloat()
        val h = height.toFloat()
        scrimPaint.alpha = (255 * a).toInt()
        scrimPaint.shader = scrimV
        c.drawRect(0f, 0f, w, h, scrimPaint)
        scrimPaint.shader = scrimH
        scrimPaint.alpha = (255 * a).toInt()
        c.drawRect(0f, 0f, w, h, scrimPaint)
    }

    // ---------- procedural fallback (used when no photo is available) ----------
    private fun drawProcedural(canvas: Canvas, dt: Float) {
        val a = fade
        val w = width.toFloat()
        val h = height.toFloat()
        if (fade < 1f) {
            shaderFrom?.let {
                skyPaint.shader = it
                skyPaint.alpha = 255
                canvas.drawRect(0f, 0f, w, h, skyPaint)
            }
            skyPaint.shader = shaderTo
            skyPaint.alpha = (255 * a).toInt()
        } else {
            skyPaint.shader = shaderTo
            skyPaint.alpha = 255
        }
        canvas.drawRect(0f, 0f, w, h, skyPaint)

        when (type) {
            CLEAR -> {
                if (day) drawSun(canvas, a) else { drawStars(canvas, a); drawMoon(canvas, a) }
                drawClouds(canvas, dt, a)
            }
            PARTLY -> {
                if (day) drawSun(canvas, a * 0.8f) else { drawStars(canvas, a * 0.6f); drawMoon(canvas, a * 0.8f) }
                drawClouds(canvas, dt, a)
            }
            FOG -> { drawClouds(canvas, dt, a); drawFog(canvas, dt, a) }
            else -> drawClouds(canvas, dt, a)
        }
    }

    // ---------- moving particles (drawn over photo or procedural scene) ----------
    private fun drawParticles(c: Canvas, dt: Float, onPhoto: Boolean) {
        val a = if (onPhoto) 0.65f else fade
        when (type) {
            CLEAR -> if (!day) drawShooting(c, dt, if (onPhoto) 1f else fade)
            RAIN -> drawRain(c, dt, a, !onPhoto)
            SNOW -> drawSnow(c, dt, if (onPhoto) 0.9f else fade)
            STORM -> {
                drawRain(c, dt, a, !onPhoto)
                drawLightning(c, dt, if (onPhoto) 1f else fade, !onPhoto)
            }
        }
    }

    private fun drawClouds(c: Canvas, dt: Float, a: Float) {
        val w = width.toFloat()
        cloudPaint.alpha = (255 * a).toInt()
        for (cl in clouds) {
            cl.x += cl.speed * dt
            if (cl.x - cl.w > w) cl.x = -cl.w
            val k = cl.puffs.size / 3
            for (j in 0 until k) {
                val px = cl.x + cl.puffs[j * 3] * cloudUnit * cl.scale
                val py = cl.y + cl.puffs[j * 3 + 1] * cloudUnit * cl.scale
                val rr = cl.puffs[j * 3 + 2] * cloudUnit * cl.scale / 100f
                c.save()
                c.translate(px, py)
                c.scale(rr, rr * 0.75f)
                c.drawCircle(0f, 0f, 100f, cloudPaint)
                c.restore()
            }
        }
    }

    private fun drawSun(c: Canvas, a: Float) {
        val r = min(width, height) * 0.085f
        val cx = width * 0.8f
        val cy = height * 0.18f
        val pulse = 1f + 0.04f * sin(time * 0.9f)
        glowPaint.shader = sunGlow
        glowPaint.alpha = (255 * a).toInt()
        c.save()
        c.translate(cx, cy)
        val big = r * 6f * pulse / 100f
        c.scale(big, big)
        c.drawCircle(0f, 0f, 100f, glowPaint)
        c.restore()
        c.save()
        c.translate(cx, cy)
        val core = r * 2f / 100f
        c.scale(core, core)
        c.drawCircle(0f, 0f, 100f, glowPaint)
        c.restore()
    }

    private fun drawMoon(c: Canvas, a: Float) {
        val cx = width * 0.8f
        val cy = height * 0.2f
        glowPaint.shader = moonGlow
        glowPaint.alpha = (255 * a).toInt()
        c.save()
        c.translate(cx, cy)
        val gs = moonR * 5f / 100f
        c.scale(gs, gs)
        c.drawCircle(0f, 0f, 100f, glowPaint)
        c.restore()
        solid.style = Paint.Style.FILL
        solid.color = Color.argb((255 * a).toInt(), 250, 248, 230)
        c.save()
        c.translate(cx, cy)
        c.drawPath(moonPath, solid)
        c.restore()
    }

    private fun drawStars(c: Canvas, a: Float) {
        solid.style = Paint.Style.FILL
        for (i in starX.indices) {
            val tw = 0.55f + 0.45f * sin(time * (0.8f + starP[i] * 0.3f) + starP[i] * 6.28f)
            solid.color = Color.argb((220 * tw * a).toInt().coerceIn(0, 255), 255, 255, 255)
            c.drawCircle(starX[i], starY[i], starR[i], solid)
        }
    }

    private fun drawShooting(c: Canvas, dt: Float, a: Float) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (shootT >= 0f) {
            shootT += dt / 0.8f
            if (shootT > 1f) {
                shootT = -1f
                shootTimer = rf(6f, 14f)
            } else {
                val hx = shootX - shootT * 0.3f * w
                val hy = shootY + shootT * 0.18f * h
                line.style = Paint.Style.STROKE
                line.strokeCap = Paint.Cap.ROUND
                line.strokeWidth = 2f * dp
                line.color = Color.argb((230 * (1f - shootT) * a).toInt(), 255, 255, 255)
                c.drawLine(hx + 0.08f * w, hy - 0.048f * h, hx, hy, line)
            }
        } else {
            shootTimer -= dt
            if (shootTimer <= 0f) {
                shootT = 0f
                shootX = rf(0.3f, 0.9f) * w
                shootY = rf(0.02f, 0.3f) * h
            }
        }
    }

    private fun drawFog(c: Canvas, dt: Float, a: Float) {
        val w = width.toFloat()
        fogPaint.alpha = (255 * a).toInt()
        for (f in fogs) {
            f.x += f.speed * dt
            if (f.x - 100f * f.sx > w) f.x = -100f * f.sx
            c.save()
            c.translate(f.x, f.y)
            c.scale(f.sx, f.sy)
            c.drawCircle(0f, 0f, 100f, fogPaint)
            c.restore()
        }
    }

    private fun drawRain(c: Canvas, dt: Float, a: Float, ripples: Boolean) {
        val w = width.toFloat()
        val h = height.toFloat()
        val slant = 0.22f
        line.style = Paint.Style.STROKE
        line.strokeCap = Paint.Cap.ROUND
        for (i in 0 until rainN) {
            ry[i] += rsp[i] * dt
            rx[i] -= rsp[i] * slant * dt
            if (ry[i] - rlen[i] > h) {
                ry[i] = -rlen[i]
                rx[i] = rf(0f, w * 1.3f)
            }
            if (rx[i] < -60f) rx[i] += w * 1.3f
            line.strokeWidth = rwid[i]
            line.color = Color.argb((ral[i] * a).toInt(), 200, 220, 255)
            c.drawLine(rx[i] + rlen[i] * slant, ry[i] - rlen[i], rx[i], ry[i], line)
        }
        if (!ripples) return
        // ripples near the ground
        ripTimer -= dt
        if (ripTimer <= 0f) {
            ripTimer = rf(0.02f, 0.08f) / intensity
            for (i in ripAge.indices) {
                if (ripAge[i] >= 1f) {
                    ripAge[i] = 0f
                    ripX[i] = rf(0f, w)
                    ripY[i] = h * rf(0.86f, 0.98f)
                    break
                }
            }
        }
        line.strokeWidth = 1.2f * dp
        for (i in ripAge.indices) {
            if (ripAge[i] < 1f) {
                ripAge[i] += dt / 0.9f
                val rad = (6f + 34f * ripAge[i]) * dp
                line.color = Color.argb((90 * (1f - min(1f, ripAge[i])) * a).toInt(), 210, 225, 255)
                rect.set(ripX[i] - rad, ripY[i] - rad * 0.28f, ripX[i] + rad, ripY[i] + rad * 0.28f)
                c.drawOval(rect, line)
            }
        }
    }

    private fun drawSnow(c: Canvas, dt: Float, a: Float) {
        val w = width.toFloat()
        val h = height.toFloat()
        solid.style = Paint.Style.FILL
        for (i in 0 until snowN) {
            fY[i] += fSp[i] * dt
            fX[i] += (sin(time * 0.9f + fPh[i]) * 18f - 6f) * dp * dt
            if (fY[i] - fR[i] > h) {
                fY[i] = -fR[i]
                fX[i] = rf(0f, w)
            }
            if (fX[i] < -fR[i]) fX[i] += w
            if (fX[i] > w + fR[i]) fX[i] -= w
            solid.color = Color.argb((fAl[i] * a).toInt(), 255, 255, 255)
            c.drawCircle(fX[i], fY[i], fR[i], solid)
            if (fR[i] > 2.8f * dp) {
                solid.color = Color.argb((fAl[i] * 0.25f * a).toInt(), 255, 255, 255)
                c.drawCircle(fX[i], fY[i], fR[i] * 1.8f, solid)
            }
        }
    }

    private fun triggerBolt() {
        val w = width.toFloat()
        val h = height.toFloat()
        val pts = ArrayList<Float>()
        var x = rf(0.15f, 0.85f) * w
        var y = 0f
        val endY = h * rf(0.55f, 0.85f)
        pts.add(x)
        pts.add(y)
        val step = h / 9f
        while (y < endY) {
            y += step * rf(0.6f, 1.2f)
            x += rf(-0.06f, 0.06f) * w
            pts.add(x)
            pts.add(y)
        }
        bolt = pts.toFloatArray()
        flashT = 0f
    }

    private fun drawLightning(c: Canvas, dt: Float, a: Float, showBolt: Boolean) {
        if (flashT >= 0f) {
            flashT += dt
            if (flashT > 0.8f) flashT = -1f
        } else {
            flashTimer -= dt
            if (flashTimer <= 0f) {
                triggerBolt()
                flashTimer = rf(3f, 9f)
            }
        }
        val ft = flashT
        if (ft < 0f) return
        val inten = when {
            ft < 0.1f -> 1f
            ft < 0.18f -> 0.15f
            ft < 0.3f -> 0.8f
            else -> max(0f, 0.8f * (1f - (ft - 0.3f) / 0.5f))
        }
        solid.style = Paint.Style.FILL
        solid.color = Color.argb((90 * inten * a).toInt(), 235, 240, 255)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), solid)
        if (showBolt && ft < 0.32f && bolt.size >= 4) {
            line.style = Paint.Style.STROKE
            line.strokeCap = Paint.Cap.ROUND
            line.strokeWidth = 9f * dp
            line.color = Color.argb((90 * inten * a).toInt(), 200, 215, 255)
            var i = 0
            while (i + 3 < bolt.size) {
                c.drawLine(bolt[i], bolt[i + 1], bolt[i + 2], bolt[i + 3], line)
                i += 2
            }
            line.strokeWidth = 2.6f * dp
            line.color = Color.argb((255 * inten * a).toInt(), 255, 255, 255)
            i = 0
            while (i + 3 < bolt.size) {
                c.drawLine(bolt[i], bolt[i + 1], bolt[i + 2], bolt[i + 3], line)
                i += 2
            }
        }
    }
}


// Claims touches that start inside it, so a horizontal drag scrolls its content
// instead of being stolen by the ViewPager2 (touches outside still swipe pages).
class HorizontalHost(context: Context) : FrameLayout(context) {
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        return super.dispatchTouchEvent(ev)
    }
}
