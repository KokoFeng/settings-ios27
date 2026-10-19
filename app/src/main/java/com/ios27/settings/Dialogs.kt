package com.ios27.settings

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** Pop-up bergaya iOS (kartu bulat transparan, judul, kolom isian, tombol Cancel | Save). Menggantikan semua AlertDialog. */
class IosDialog(private val act: Activity) {
    private class B(val label: String, val style: Int, val action: () -> Unit)   // style: 0 biasa, 1 tebal, 2 merah

    private var title = ""
    private var msg: String? = null
    private val fields = ArrayList<EditText>()
    private val btns = ArrayList<B>()

    fun title(t: String) = apply { title = t }
    fun message(m: String?) = apply { msg = m }
    fun button(label: String, style: Int = 0, action: () -> Unit = {}) = apply { btns.add(B(label, style, action)) }

    fun field(hint: String, value: String = ""): EditText {
        val e = EditText(act).apply {
            this.hint = hint; setHintTextColor(Theme.sub); setTextColor(Theme.text); setText(value); textSize = 16f; isSingleLine = true
            background = GradientDrawable().apply {
                setColor(if (Theme.dark) 0xFF1C1C1E.toInt() else 0xFFFFFFFF.toInt()); cornerRadius = act.dpf(10f)
                setStroke(1, if (Theme.dark) 0xFF48484A.toInt() else 0xFFD1D1D6.toInt())
            }
            setPadding(act.dp(12f), act.dp(8f), act.dp(12f), act.dp(8f))
        }
        fields.add(e)
        return e
    }

    fun show() {
        val d = Dialog(act)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val box = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(if (Theme.dark) 0xF22C2C2E.toInt() else 0xF2F7F7F7.toInt()); cornerRadius = act.dpf(26f)
            }
            clipToOutline = true
        }
        box.addView(TextView(act).apply {
            text = title; textSize = 17f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setTextColor(Theme.text)
            setPadding(act.dp(20f), act.dp(22f), act.dp(20f), act.dp(if (msg == null && fields.isEmpty()) 22f else 8f))
        })
        msg?.let { m ->
            box.addView(TextView(act).apply {
                text = m; textSize = 13f; gravity = Gravity.CENTER; setTextColor(Theme.sub)
                setPadding(act.dp(20f), 0, act.dp(20f), act.dp(16f))
            })
        }
        fields.forEachIndexed { i, f ->
            box.addView(f, LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(act.dp(16f), if (i == 0) act.dp(4f) else act.dp(8f), act.dp(16f), if (i == fields.size - 1) act.dp(18f) else 0)
            })
        }
        box.addView(View(act).apply { setBackgroundColor(Theme.sep) }, LinearLayout.LayoutParams(-1, 1))
        val stacked = btns.size > 2
        val row = LinearLayout(act).apply { orientation = if (stacked) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL }
        btns.forEachIndexed { i, b ->
            row.addView(TextView(act).apply {
                text = b.label; textSize = 17f; gravity = Gravity.CENTER
                typeface = if (b.style == 1) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                setTextColor(if (b.style == 2) 0xFFFF453A.toInt() else Theme.blue)
                foreground = act.ripple()
                setOnClickListener { d.dismiss(); b.action() }
            }, if (stacked) LinearLayout.LayoutParams(-1, act.dp(50f)) else LinearLayout.LayoutParams(0, act.dp(50f), 1f))
            if (i < btns.size - 1) row.addView(View(act).apply { setBackgroundColor(Theme.sep) },
                if (stacked) LinearLayout.LayoutParams(-1, 1) else LinearLayout.LayoutParams(1, -1))
        }
        box.addView(row)
        d.setContentView(box)
        d.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)); setDimAmount(0.35f)
            setLayout(act.dp(300f), WindowManager.LayoutParams.WRAP_CONTENT)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        d.show()
    }
}

/** Teks tampilan yang bisa diganti pengguna di layar About (hanya tampilan; tidak mengubah sistem). */
object Over {
    private fun sp(c: android.content.Context) = c.getSharedPreferences("about", android.content.Context.MODE_PRIVATE)
    fun get(c: android.content.Context, key: String, def: String): String = sp(c).getString(key, null)?.takeIf { it.isNotBlank() } ?: def
    fun set(c: android.content.Context, key: String, v: String) { sp(c).edit().putString(key, v).apply() }
    fun reset(c: android.content.Context, key: String) { sp(c).edit().remove(key).apply() }
    fun name(c: android.content.Context) = get(c, "name", Info.deviceName(c))
    fun model(c: android.content.Context) = get(c, "model", android.os.Build.MODEL)
    fun manu(c: android.content.Context) = get(c, "manu", android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() })
}
