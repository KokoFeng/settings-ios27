package com.ios27.settings

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/** Warna gaya iOS (terang / gelap mengikuti sistem). */
object Theme {
    var dark = false
    val bg: Int get() = if (dark) 0xFF000000.toInt() else 0xFFF2F2F7.toInt()
    val card: Int get() = if (dark) 0xFF1C1C1E.toInt() else 0xFFFFFFFF.toInt()
    val text: Int get() = if (dark) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
    val sub: Int get() = 0xFF8E8E93.toInt()
    val chev: Int get() = if (dark) 0xFF5A5A5E.toInt() else 0xFFC4C4C7.toInt()
    val sep: Int get() = if (dark) 0xFF38383A.toInt() else 0xFFD1D1D6.toInt()
    val blue: Int get() = 0xFF0A84FF.toInt()
}

fun Context.dp(v: Float): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
fun Context.dpf(v: Float): Float = v * resources.displayMetrics.density

fun Context.statusBarH(): Int {
    val id = resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (id > 0) resources.getDimensionPixelSize(id) else dp(24f)
}

fun Context.ripple(): RippleDrawable =
    RippleDrawable(ColorStateList.valueOf(0x33808080), null, ColorDrawable(0xFFFFFFFF.toInt()))

/** Ubin ikon berwarna (squircle) dengan glif putih. */
class Tile(val color: Int, val glyph: String? = null, val kind: String? = null)

fun drawWifi(c: Canvas, p: Paint, cx: Float, base: Float, r: Float, lit: Int, on: Int, off: Int, sw: Float) {
    p.style = Paint.Style.STROKE; p.strokeWidth = sw; p.strokeCap = Paint.Cap.ROUND
    for (i in 1..3) {
        val rr = r * i / 3f
        p.color = if (i <= lit) on else off
        c.drawArc(RectF(cx - rr, base - rr, cx + rr, base + rr), 225f, 90f, false, p)
    }
    p.style = Paint.Style.FILL; p.color = if (lit >= 1) on else off
    c.drawCircle(cx, base, sw * 0.75f, p)
}

class TileView(ctx: Context, private val tile: Tile, private val sizeDp: Float = 30f) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onMeasure(w: Int, h: Int) {
        val s = context.dp(sizeDp); setMeasuredDimension(s, s)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        p.style = Paint.Style.FILL; p.color = tile.color
        c.drawRoundRect(0f, 0f, w, w, w * 0.23f, w * 0.23f, p)
        val white = Color.WHITE
        when (tile.kind) {
            "wifi" -> drawWifi(c, p, w / 2f, w * 0.72f, w * 0.40f, 3, white, white, w * 0.085f)
            "bt" -> {
                p.style = Paint.Style.STROKE; p.strokeWidth = w * 0.075f; p.color = white
                p.strokeJoin = Paint.Join.ROUND; p.strokeCap = Paint.Cap.ROUND
                val path = Path()
                path.moveTo(w * 0.32f, w * 0.32f); path.lineTo(w * 0.68f, w * 0.68f); path.lineTo(w * 0.5f, w * 0.84f)
                path.lineTo(w * 0.5f, w * 0.16f); path.lineTo(w * 0.68f, w * 0.32f); path.lineTo(w * 0.32f, w * 0.68f)
                c.drawPath(path, p)
            }
            "cell" -> {
                p.style = Paint.Style.FILL; p.color = white
                for (i in 0..3) {
                    val l = w * (0.2f + i * 0.155f); val h = w * (0.16f + i * 0.14f)
                    c.drawRoundRect(l, w * 0.78f - h, l + w * 0.1f, w * 0.78f, w * 0.03f, w * 0.03f, p)
                }
            }
            "battery" -> {
                p.style = Paint.Style.STROKE; p.strokeWidth = w * 0.06f; p.color = white
                c.drawRoundRect(w * 0.16f, w * 0.34f, w * 0.78f, w * 0.66f, w * 0.07f, w * 0.07f, p)
                p.style = Paint.Style.FILL
                c.drawRoundRect(w * 0.21f, w * 0.39f, w * 0.62f, w * 0.61f, w * 0.03f, w * 0.03f, p)
                c.drawRoundRect(w * 0.80f, w * 0.44f, w * 0.85f, w * 0.56f, w * 0.02f, w * 0.02f, p)
            }
            "bell" -> {
                p.style = Paint.Style.FILL; p.color = white
                val path = Path()
                path.moveTo(w * 0.26f, w * 0.64f); path.quadTo(w * 0.26f, w * 0.2f, w * 0.5f, w * 0.2f)
                path.quadTo(w * 0.74f, w * 0.2f, w * 0.74f, w * 0.64f); path.lineTo(w * 0.8f, w * 0.7f)
                path.lineTo(w * 0.2f, w * 0.7f); path.close()
                c.drawPath(path, p)
                c.drawCircle(w * 0.5f, w * 0.78f, w * 0.06f, p)
            }
            "speaker" -> {
                p.style = Paint.Style.FILL; p.color = white
                val path = Path()
                path.moveTo(w * 0.22f, w * 0.42f); path.lineTo(w * 0.36f, w * 0.42f); path.lineTo(w * 0.52f, w * 0.28f)
                path.lineTo(w * 0.52f, w * 0.72f); path.lineTo(w * 0.36f, w * 0.58f); path.lineTo(w * 0.22f, w * 0.58f); path.close()
                c.drawPath(path, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = w * 0.06f; p.strokeCap = Paint.Cap.ROUND
                c.drawArc(RectF(w * 0.42f, w * 0.34f, w * 0.7f, w * 0.66f), -45f, 90f, false, p)
            }
            "lock" -> {
                p.style = Paint.Style.FILL; p.color = white
                c.drawRoundRect(w * 0.28f, w * 0.46f, w * 0.72f, w * 0.78f, w * 0.06f, w * 0.06f, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = w * 0.07f
                c.drawArc(RectF(w * 0.34f, w * 0.2f, w * 0.66f, w * 0.6f), 180f, 180f, false, p)
            }
            else -> {
                p.style = Paint.Style.FILL; p.color = white; p.textAlign = Paint.Align.CENTER
                p.textSize = w * 0.62f; p.typeface = Typeface.DEFAULT_BOLD
                val g = tile.glyph ?: "•"
                c.drawText(g, w / 2f, w / 2f - (p.ascent() + p.descent()) / 2f, p)
            }
        }
    }
}

/** Saklar ala iOS. Mengubah tampilan hanya lewat [checked]; [onToggle] dipanggil saat diketuk. */
class IosSwitch(ctx: Context) : View(ctx) {
    var checked = false
        set(v) { field = v; invalidate() }
    var onToggle: ((Boolean) -> Unit)? = null
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        setOnClickListener { onToggle?.invoke(!checked) }
    }

    override fun onMeasure(w: Int, h: Int) = setMeasuredDimension(context.dp(51f), context.dp(31f))

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        p.style = Paint.Style.FILL
        p.color = if (checked) 0xFF34C759.toInt() else if (Theme.dark) 0xFF39393D.toInt() else 0xFFE9E9EA.toInt()
        c.drawRoundRect(0f, 0f, w, h, h / 2f, h / 2f, p)
        val r = h / 2f - context.dpf(2f)
        val cx = if (checked) w - h / 2f else h / 2f
        p.color = Color.WHITE; p.setShadowLayer(context.dpf(2f), 0f, context.dpf(1f), 0x44000000)
        c.drawCircle(cx, h / 2f, r, p); p.clearShadowLayer()
    }
}

/** Sinyal Wi-Fi (0..4). */
class SignalView(ctx: Context, private val level: Int) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val lit = when (level) { 0, 1 -> 1; 2 -> 2; else -> 3 }
        val on = Theme.text
        val off = (Theme.text and 0x00FFFFFF) or 0x4D000000
        drawWifi(c, p, w / 2f, w * 0.82f, w * 0.8f, lit, on, off, w * 0.12f)
    }
}

class LockView(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        p.style = Paint.Style.FILL; p.color = Theme.text
        c.drawRoundRect(w * 0.2f, w * 0.46f, w * 0.8f, w * 0.92f, w * 0.1f, w * 0.1f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = w * 0.11f
        c.drawArc(RectF(w * 0.3f, w * 0.1f, w * 0.7f, w * 0.7f), 180f, 180f, false, p)
    }
}

/** Tombol (i) biru. */
class InfoButton(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        p.style = Paint.Style.STROKE; p.strokeWidth = w * 0.06f; p.color = Theme.blue
        c.drawCircle(w / 2f, w / 2f, w * 0.36f, p)
        p.style = Paint.Style.FILL; p.textAlign = Paint.Align.CENTER; p.textSize = w * 0.46f; p.typeface = Typeface.DEFAULT_BOLD
        c.drawText("i", w / 2f, w / 2f - (p.ascent() + p.descent()) / 2f, p)
    }
}

class StorageBar(ctx: Context, private val frac: Float) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        p.style = Paint.Style.FILL; p.color = if (Theme.dark) 0xFF2C2C2E.toInt() else 0xFFE5E5EA.toInt()
        c.drawRoundRect(0f, 0f, w, h, h / 2f, h / 2f, p)
        p.color = Theme.blue
        c.drawRoundRect(0f, 0f, maxOf(h, w * frac.coerceIn(0f, 1f)), h, h / 2f, h / 2f, p)
    }
}

// ---------------------------------------------------------------- komponen daftar
fun Context.card(): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    background = GradientDrawable().apply { setColor(Theme.card); cornerRadius = dpf(20f) }
    clipToOutline = true
    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(16f), dp(10f), dp(16f), dp(10f)) }
}

fun Context.sepView(inset: Int): View = View(this).apply {
    setBackgroundColor(Theme.sep)
    layoutParams = LinearLayout.LayoutParams(-1, 1).apply { leftMargin = inset }
}

class RowParts(val row: LinearLayout, val value: TextView)

fun Context.makeRow(tile: Tile?, title: String, chevron: Boolean, clickable: Boolean): RowParts {
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(52f); setPadding(dp(16f), dp(8f), dp(14f), dp(8f))
    }
    if (tile != null) row.addView(TileView(this, tile), LinearLayout.LayoutParams(dp(30f), dp(30f)).apply { rightMargin = dp(14f) })
    row.addView(TextView(this).apply { text = title; textSize = 17f; setTextColor(Theme.text) }, LinearLayout.LayoutParams(0, -2, 1f))
    val v = TextView(this).apply {
        textSize = 17f; setTextColor(Theme.sub); gravity = Gravity.END; maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END; maxWidth = dp(190f)
    }
    row.addView(v, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8f) })
    if (chevron) row.addView(TextView(this).apply { text = "›"; textSize = 24f; setTextColor(Theme.chev) },
        LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8f) })
    if (clickable) row.foreground = ripple()
    return RowParts(row, v)
}

fun Context.sectionLabel(text: String, spinner: View? = null): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
    setPadding(dp(32f), dp(14f), dp(16f), dp(4f))
    addView(TextView(this@sectionLabel).apply { this.text = text; textSize = 15f; setTextColor(Theme.sub); typeface = Typeface.DEFAULT_BOLD })
    if (spinner != null) addView(spinner, LinearLayout.LayoutParams(dp(18f), dp(18f)).apply { leftMargin = dp(8f) })
}

fun MainActivity.navBar(title: String?): FrameLayout {
    val bar = FrameLayout(this).apply { setPadding(dp(16f), statusBarH() + dp(6f), dp(16f), dp(6f)) }
    val back = TextView(this).apply {
        text = "‹"; textSize = 30f; gravity = Gravity.CENTER; setTextColor(Theme.text)
        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(if (Theme.dark) 0xFF2C2C2E.toInt() else 0xFFFFFFFF.toInt()) }
        setOnClickListener { pop() }
    }
    bar.addView(back, FrameLayout.LayoutParams(dp(42f), dp(42f)))
    if (title != null) bar.addView(TextView(this).apply {
        text = title; textSize = 17f; gravity = Gravity.CENTER; setTextColor(Theme.text); typeface = Typeface.DEFAULT_BOLD
    }, FrameLayout.LayoutParams(-2, dp(42f), Gravity.CENTER_HORIZONTAL))
    return bar
}

/** Kartu kepala layar: ikon besar + judul + keterangan (seperti layar Wi-Fi dan General di iOS). */
fun Context.headerCard(tile: Tile, title: String, desc: String, extra: View? = null): LinearLayout {
    val c = card()
    val top = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16f), dp(16f), dp(16f), dp(14f)) }
    top.addView(TileView(this, tile, 60f))
    top.addView(TextView(this).apply { text = title; textSize = 28f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Theme.text); setPadding(0, dp(8f), 0, dp(4f)) })
    top.addView(TextView(this).apply { text = desc; textSize = 15f; setTextColor(Theme.sub) })
    c.addView(top)
    if (extra != null) { c.addView(sepView(dp(16f))); c.addView(extra) }
    return c
}

fun Context.infoCard(message: String, button: String?, onClick: (() -> Unit)?): LinearLayout {
    val c = card()
    val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16f), dp(14f), dp(16f), dp(14f)) }
    box.addView(TextView(this).apply { text = message; textSize = 15f; setTextColor(Theme.text) })
    if (button != null) box.addView(TextView(this).apply {
        text = button; textSize = 17f; setTextColor(Theme.blue); typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(10f), 0, 0)
        setOnClickListener { onClick?.invoke() }
    })
    c.addView(box)
    return c
}
