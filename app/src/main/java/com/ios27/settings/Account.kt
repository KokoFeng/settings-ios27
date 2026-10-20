package com.ios27.settings

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.os.Build
import android.text.format.Formatter
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File

/** Profil LOKAL (nama, email tampilan, foto). Tidak terhubung ke Apple/iCloud dan tidak dikirim ke mana pun. */
object Profile {
    private fun sp(c: Context) = c.getSharedPreferences("profile", Context.MODE_PRIVATE)
    fun name(c: Context) = sp(c).getString("name", "") ?: ""
    fun email(c: Context) = sp(c).getString("email", "") ?: ""
    fun has(c: Context) = name(c).isNotBlank()
    fun save(c: Context, name: String, email: String) { sp(c).edit().putString("name", name.trim()).putString("email", email.trim()).apply() }
    fun clear(c: Context) { sp(c).edit().clear().apply(); photoFile(c).delete() }
    fun photoFile(c: Context) = File(c.filesDir, "profile.jpg")
    fun flag(c: Context, key: String, def: Boolean) = sp(c).getBoolean(key, def)
    fun setFlag(c: Context, key: String, v: Boolean) { sp(c).edit().putBoolean(key, v).apply() }

    fun initials(c: Context): String = initialsOf(name(c))
    fun initialsOf(n: String): String {
        val parts = n.trim().split(" ").filter { it.isNotEmpty() }
        return parts.take(2).joinToString("") { it.take(1).uppercase() }
    }

    /** Foto dipotong persegi (tengah), diperkecil, lalu disimpan di penyimpanan aplikasi. */
    fun importPhoto(c: Context, uri: Uri): Boolean = try {
        val opt = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opt) }
        var sample = 1
        while (maxOf(opt.outWidth, opt.outHeight) / (sample * 2) >= 1024) sample *= 2
        val bmp = c.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
        if (bmp == null) false else {
            val side = minOf(bmp.width, bmp.height)
            val sq = Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
            val out = if (side > 512) Bitmap.createScaledBitmap(sq, 512, 512, true) else sq
            photoFile(c).outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            true
        }
    } catch (_: Throwable) { false }
}

/** Foto profil bulat; jika belum ada foto tampil inisial. */
class AvatarView(ctx: Context, private val sizeDp: Float) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var bmp: Bitmap? = null
    var initials = ""
        set(v) { field = v; invalidate() }

    fun load() {
        val f = Profile.photoFile(context)
        bmp = if (f.exists()) try { BitmapFactory.decodeFile(f.path) } catch (_: Throwable) { null } else null
        invalidate()
    }

    override fun onMeasure(w: Int, h: Int) { val s = context.dp(sizeDp); setMeasuredDimension(s, s) }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val b = bmp
        if (b != null) {
            val sh = BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val m = Matrix(); val s = maxOf(w / b.width, w / b.height)
            m.setScale(s, s); m.postTranslate((w - b.width * s) / 2f, (w - b.height * s) / 2f)
            sh.setLocalMatrix(m)
            p.shader = sh; c.drawCircle(w / 2f, w / 2f, w / 2f, p); p.shader = null
        } else {
            p.shader = LinearGradient(0f, 0f, 0f, w, 0xFFA5A5AB.toInt(), 0xFF7C7C82.toInt(), Shader.TileMode.CLAMP)
            c.drawCircle(w / 2f, w / 2f, w / 2f, p); p.shader = null
            p.color = Color.WHITE; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD; p.textSize = w * 0.38f
            val t = if (initials.isEmpty()) "☻" else initials
            c.drawText(t, w / 2f, w / 2f - (p.ascent() + p.descent()) / 2f, p)
        }
    }
}

/** Lingkaran titik-titik biru di layar masuk. */
class DottedView(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val cx = w / 2f
        p.style = Paint.Style.FILL
        for (ring in 0..1) {
            val r = w * (0.46f - ring * 0.06f); val n = 30 - ring * 4
            for (i in 0 until n) {
                val a = Math.PI * 2 * i / n + ring * 0.1
                val size = w * (0.016f + 0.018f * (0.5f + 0.5f * Math.sin(a * 2 + ring).toFloat()))
                p.color = if (ring == 0) 0xFF5AA6FF.toInt() else 0xFF8DBFFF.toInt()
                c.drawCircle(cx + (r * Math.cos(a)).toFloat(), cx + (r * Math.sin(a)).toFloat(), size, p)
            }
        }
        p.color = Theme.blue; p.textAlign = Paint.Align.CENTER; p.textSize = w * 0.36f
        c.drawText("☁", cx, cx - (p.ascent() + p.descent()) / 2f, p)
    }
}

object Demo {
    fun dialog(act: Activity, title: String) {
        IosDialog(act).title(title).message("This screen is a visual demo. It is not connected to any Apple or iCloud account.")
            .button("OK", 1).show()
    }
}

fun MainActivity.navBar2(backLabel: String, title: String): FrameLayout {
    val bar = FrameLayout(this).apply { setPadding(dp(8f), statusBarH() + dp(8f), dp(8f), dp(8f)) }
    bar.addView(TextView(this).apply {
        text = "‹ $backLabel"; textSize = 17f; setTextColor(Theme.blue); gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(8f), 0, dp(8f), 0); setOnClickListener { pop() }
    }, FrameLayout.LayoutParams(-2, dp(40f), Gravity.START))
    bar.addView(TextView(this).apply {
        text = title; textSize = 17f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Theme.text); gravity = Gravity.CENTER
    }, FrameLayout.LayoutParams(-2, dp(40f), Gravity.CENTER_HORIZONTAL))
    return bar
}

internal fun Context.smallLabel(text: String): TextView = TextView(this).apply {
    this.text = text; textSize = 13f; setTextColor(Theme.sub); setPadding(dp(32f), dp(18f), dp(16f), dp(4f))
}

private fun Context.pill(hint: String, value: String): EditText = EditText(this).apply {
    this.hint = hint; setHintTextColor(Theme.sub); setTextColor(Theme.text); setText(value); textSize = 17f
    background = GradientDrawable().apply { setColor(if (Theme.dark) 0xFF1C1C1E.toInt() else 0xFFEFEFF4.toInt()); cornerRadius = dpf(26f) }
    setPadding(dp(22f), dp(14f), dp(22f), dp(14f)); isSingleLine = true
}

private fun scrollOf(act: MainActivity, col: LinearLayout) = ScrollView(act).apply { addView(col); isVerticalScrollBarEnabled = false }

// ------------------------------------------------------------ layar masuk (profil lokal)
class AccountSetupScreen(act: MainActivity) : Screen(act) {
    private lateinit var avatar: AvatarView

    override fun build(): View {
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(act.dp(24f), 0, act.dp(24f), act.dp(40f)) }
        col.addView(act.navBar(null))
        col.addView(DottedView(act), LinearLayout.LayoutParams(act.dp(150f), act.dp(150f)).apply {
            gravity = Gravity.CENTER_HORIZONTAL; topMargin = act.dp(16f); bottomMargin = act.dp(16f)
        })
        col.addView(TextView(act).apply { text = "Sign In"; textSize = 24f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Theme.text) })
        col.addView(TextView(act).apply {
            text = "Type your name to set up a profile for iCloud, the App Store, and more. The profile stays on this phone only — nothing is sent to Apple or any server."
            textSize = 17f; setTextColor(Theme.sub); setPadding(0, act.dp(4f), 0, act.dp(16f))
        })
        val name = act.pill("Name", Profile.name(act))
        val email = act.pill("Email (display only, optional)", Profile.email(act))
        avatar = AvatarView(act, 72f).apply { load(); initials = Profile.initialsOf(Profile.name(act)); setOnClickListener { act.pickPhoto() } }
        val photoRow = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        photoRow.addView(avatar, LinearLayout.LayoutParams(act.dp(72f), act.dp(72f)))
        photoRow.addView(TextView(act).apply {
            text = "Add Photo"; textSize = 17f; setTextColor(Theme.blue); setPadding(act.dp(16f), 0, 0, 0); setOnClickListener { act.pickPhoto() }
        })
        col.addView(photoRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = act.dp(16f) })
        col.addView(name)
        col.addView(email, LinearLayout.LayoutParams(-1, -2).apply { topMargin = act.dp(12f) })
        name.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(e: android.text.Editable?) { avatar.initials = Profile.initialsOf(e?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        col.addView(TextView(act).apply {
            text = "Continue"; textSize = 17f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(Theme.blue); cornerRadius = act.dpf(26f) }
            setPadding(0, act.dp(15f), 0, act.dp(15f))
            setOnClickListener {
                if (name.text.isNullOrBlank()) { name.error = "Type a name"; return@setOnClickListener }
                Profile.save(act, name.text.toString(), email.text.toString()); act.pop()
            }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = act.dp(24f) })
        col.addView(TextView(act).apply {
            text = "ⓘ  This is a local, visual profile. It does not connect to a real Apple Account."
            textSize = 13f; setTextColor(Theme.sub); setPadding(0, act.dp(14f), 0, 0)
        })
        return scrollOf(act, col)
    }

    override fun onResult(req: Int, res: Int, data: Intent?) {
        if (req == 31 && res == Activity.RESULT_OK && data?.data != null) { Profile.importPhoto(act, data.data!!); avatar.load() }
    }
}

// ----------------------------------------------------------------- Apple ID
class AppleIdScreen(act: MainActivity) : Screen(act) {
    private lateinit var body: LinearLayout
    private val refreshers = ArrayList<() -> Unit>()

    override fun build(): View {
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, act.dp(40f)) }
        col.addView(act.navBar2("Settings", "Apple ID"))
        body = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        col.addView(body)
        return scrollOf(act, col)
    }

    private fun editDialog() {
        val d = IosDialog(act).title("Name, Phone Numbers, Email")
        val n = d.field("Name", Profile.name(act))
        val e = d.field("Email (display only)", Profile.email(act))
        d.button("Cancel").button("Save", 1) {
            if (!n.text.isNullOrBlank()) { Profile.save(act, n.text.toString(), e.text.toString()); onShow() }
        }
        d.show()
    }

    private fun demo(t: String): (MainActivity) -> Unit = { Demo.dialog(it, t) }

    private fun render() {
        body.removeAllViews(); refreshers.clear()
        val head = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(0, act.dp(8f), 0, act.dp(8f)) }
        val av = AvatarView(act, 100f).apply { load(); initials = Profile.initials(act); setOnClickListener { act.pickPhoto() } }
        head.addView(av, LinearLayout.LayoutParams(act.dp(100f), act.dp(100f)))
        head.addView(TextView(act).apply { text = "EDIT"; textSize = 10f; setTextColor(Theme.sub); gravity = Gravity.CENTER; setOnClickListener { act.pickPhoto() } })
        head.addView(TextView(act).apply {
            text = Profile.name(act); gravity = Gravity.CENTER; textSize = 24f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Theme.text); setPadding(0, act.dp(8f), 0, 0)
        })
        if (Profile.email(act).isNotEmpty()) head.addView(TextView(act).apply { text = Profile.email(act); textSize = 15f; setTextColor(Theme.sub) })
        body.addView(head)
        val blue = 0xFF0A84FF.toInt()
        renderGroups(act, body, listOf(
            listOf(
                Entry("Name, Phone Numbers, Email", null, action = { editDialog() }),
                Entry("Password & Security", null, action = { Sys.open(it, android.provider.Settings.ACTION_SECURITY_SETTINGS) }),
                Entry("Payment & Shipping", null, value = { "None" }, action = demo("Payment & Shipping")),
                Entry("Subscriptions", null, action = { openUrl(it, "https://play.google.com/store/account/subscriptions") })
            ),
            listOf(
                Entry("iCloud", Tile(blue, glyph = "☁"), value = { Formatter.formatShortFileSize(it, totalStorage()) }, action = { it.push(ICloudScreen(it)) }),
                Entry("Media & Purchases", Tile(blue, glyph = "▦"), action = { openUrl(it, "https://play.google.com/store/account") }),
                Entry("Find My", Tile(0xFF30D158.toInt(), glyph = "◉"), action = { openUrl(it, "https://www.google.com/android/find") }),
                Entry("Family Sharing", Tile(blue, glyph = "☻"), value = { "Learn more…" }, action = demo("Family Sharing"))
            )
        ), refreshers)
        // perangkat ini
        body.addView(supportCard(act)); val dev = act.card()
        val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(act.dp(16f), act.dp(10f), act.dp(14f), act.dp(10f)) }
        row.addView(TileView(act, Tile(0xFF3A3A3C.toInt(), glyph = "▯"), 36f), LinearLayout.LayoutParams(act.dp(36f), act.dp(36f)).apply { rightMargin = act.dp(14f) })
        val tx = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        tx.addView(TextView(act).apply { text = Over.name(act); textSize = 17f; setTextColor(Theme.text) })
        tx.addView(TextView(act).apply { text = "This ${Over.model(act)}"; textSize = 13f; setTextColor(Theme.sub) })
        row.addView(tx, LinearLayout.LayoutParams(0, -2, 1f))
        dev.addView(row)
        body.addView(dev)
        // keluar
        val out = act.card()
        out.addView(TextView(act).apply {
            text = "Sign Out"; textSize = 17f; setTextColor(0xFFFF453A.toInt()); gravity = Gravity.CENTER; setPadding(0, act.dp(15f), 0, act.dp(15f))
            setOnClickListener {
                IosDialog(act).title("Sign Out?").message("This removes the local profile and its photo from this phone.")
                    .button("Cancel").button("Sign Out", 2) { Profile.clear(act); act.pop() }.show()
            }
        })
        body.addView(out)
    }

    override fun onShow() { if (::body.isInitialized) render() }

    override fun onResult(req: Int, res: Int, data: Intent?) {
        if (req == 31 && res == Activity.RESULT_OK && data?.data != null) { Profile.importPhoto(act, data.data!!); render() }
    }
}

private fun totalStorage(): Long { val st = StatFs(Environment.getDataDirectory().path); return st.blockCountLong * st.blockSizeLong }

private fun openUrl(act: Activity, url: String) {
    try { act.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { }
}

// ------------------------------------------------------------------- iCloud
class ICloudScreen(act: MainActivity) : Screen(act) {
    private val refreshers = ArrayList<() -> Unit>()

    private fun onOff(a: MainActivity, key: String, def: Boolean) = if (Profile.flag(a, key, def)) "On" else "Off"
    private fun toggle(a: MainActivity, key: String, def: Boolean) { Profile.setFlag(a, key, !Profile.flag(a, key, def)); a.refreshTop() }

    override fun build(): View {
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, act.dp(40f)) }
        col.addView(act.navBar2("Apple ID", "iCloud"))
        // kartu penyimpanan (data penyimpanan HP yang asli)
        val st = StatFs(Environment.getDataDirectory().path)
        val total = st.blockCountLong * st.blockSizeLong
        val used = total - st.availableBlocksLong * st.blockSizeLong
        val c = act.card()
        val box = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(act.dp(16f), act.dp(14f), act.dp(16f), act.dp(10f)) }
        val top = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
        top.addView(TextView(act).apply { text = "iCloud"; textSize = 17f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Theme.text) }, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(TextView(act).apply {
            text = "${Formatter.formatShortFileSize(act, used)} of ${Formatter.formatShortFileSize(act, total)} Used"; textSize = 15f; setTextColor(Theme.sub)
        })
        box.addView(top)
        box.addView(StorageBar(act, if (total > 0) used.toFloat() / total else 0f), LinearLayout.LayoutParams(-1, act.dp(10f)).apply { topMargin = act.dp(10f) })
        box.addView(TextView(act).apply { text = "●  Used      ●  Free   (this phone's storage)"; textSize = 12f; setTextColor(Theme.sub); setPadding(0, act.dp(8f), 0, 0) })
        c.addView(box); c.addView(act.sepView(act.dp(16f)))
        val manage = act.makeRow(null, "Manage Account Storage", chevron = true, clickable = true)
        manage.row.setOnClickListener { Sys.open(act, "android.settings.INTERNAL_STORAGE_SETTINGS", android.provider.Settings.ACTION_MEMORY_CARD_SETTINGS) }
        c.addView(manage.row)
        col.addView(c)

        col.addView(act.smallLabel("APPS USING ICLOUD"))
        renderGroups(act, col, listOf(
            listOf(
                Entry("Photos", Tile(0xFFFF9F0A.toInt(), glyph = "❀\uFE0E"), value = { onOff(it, "photos", true) }, action = { toggle(it, "photos", true) }),
                Entry("iCloud Drive", Tile(0xFF0A84FF.toInt(), glyph = "▭"), value = { onOff(it, "drive", true) }, action = { toggle(it, "drive", true) }),
                Entry("iCloud Mail", Tile(0xFF0A84FF.toInt(), glyph = "✉\uFE0E"),
                    value = { if (Profile.flag(it, "mail", false)) "On" else "Set Up" }, action = { it.push(ICloudMailScreen(it)) }),
                Entry("Passwords and Keychain", Tile(0xFF8E8E93.toInt(), glyph = "⌗"), value = { onOff(it, "keys", true) }, action = { toggle(it, "keys", true) }),
                Entry("Show All", null, action = { Demo.dialog(it, "Show All") })
            )
        ), refreshers)
        col.addView(act.smallLabel("DEVICE BACKUPS"))
        renderGroups(act, col, listOf(
            listOf(Entry("iCloud Backup", Tile(0xFF1FA89A.toInt(), glyph = "⟲"), value = { onOff(it, "backup", false) }, action = { toggle(it, "backup", false) }))
        ), refreshers)

        val more = act.card()
        val mb = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(act.dp(16f), act.dp(14f), act.dp(16f), act.dp(6f)) }
        mb.addView(TextView(act).apply { text = "Get More with iCloud+"; textSize = 17f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Theme.text) })
        more.addView(mb)
        listOf("Up to 12 TB of storage" to "☁", "Share with your family" to "☻", "Private Relay" to "◎", "Hide My Email" to "✉\uFE0E", "HomeKit Secure Video" to "▶\uFE0E").forEach { (t, g) ->
            val r = act.makeRow(Tile(0xFF0A84FF.toInt(), glyph = g), t, chevron = false, clickable = false)
            r.row.setPadding(act.dp(16f), act.dp(4f), act.dp(14f), act.dp(4f)); r.row.minimumHeight = act.dp(40f)
            r.row.setOnClickListener { Demo.dialog(act, t) }
            more.addView(r.row)
        }
        col.addView(more)
        return scrollOf(act, col)
    }

    override fun onShow() { refreshers.forEach { it() } }
}

// -------------------------------------------------------------- iCloud Mail
class ICloudMailScreen(act: MainActivity) : Screen(act) {
    private lateinit var sw: IosSwitch

    override fun build(): View {
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, act.dp(40f)) }
        col.addView(act.navBar2("iCloud", "iCloud Mail"))
        val toggleRow = act.makeRow(null, "Use on this iPhone", chevron = false, clickable = false)
        sw = IosSwitch(act).apply {
            checked = Profile.flag(act, "mail", false)
            onToggle = { Profile.setFlag(act, "mail", it); checked = it }
        }
        toggleRow.row.addView(sw, LinearLayout.LayoutParams(act.dp(51f), act.dp(31f)).apply { leftMargin = act.dp(8f) })
        col.addView(act.headerCard(Tile(0xFF0A84FF.toInt(), glyph = "✉\uFE0E"), "iCloud Mail",
            "Use your existing iCloud Mail account on this iPhone or set up a free \"@icloud.com\" email address.", toggleRow.row))
        col.addView(act.smallLabel("ACCOUNT INFORMATION"))
        renderGroups(act, col, listOf(
            listOf(
                Entry("Default Email", null, value = { Profile.email(it).ifEmpty { "@icloud.com" } }, action = { Demo.dialog(it, "Default Email") }),
                Entry("Addresses", null, value = { "1" }, action = { Demo.dialog(it, "Addresses") })
            ),
            listOf(
                Entry("Auto-Reply", null, value = { "Off" }, action = { Demo.dialog(it, "Auto-Reply") }),
                Entry("iCloud Mail Rules", null, value = { "Off" }, action = { Demo.dialog(it, "iCloud Mail Rules") }),
                Entry("Mail Forwarding", null, value = { "Off" }, action = { Demo.dialog(it, "Mail Forwarding") }),
                Entry("Mailbox Behaviors", null, action = { Demo.dialog(it, "Mailbox Behaviors") })
            )
        ), ArrayList())
        col.addView(act.smallLabel("SECURITY"))
        renderGroups(act, col, listOf(listOf(Entry("Signing and Encryption", null, action = { Demo.dialog(it, "Signing and Encryption") }))), ArrayList())
        return scrollOf(act, col)
    }

    override fun onShow() { if (::sw.isInitialized) sw.checked = Profile.flag(act, "mail", false) }
}
