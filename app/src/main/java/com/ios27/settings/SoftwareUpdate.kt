package com.ios27.settings

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Layar Software Update bergaya iOS 27 beta (tampilan; pembaruan Android dipasang oleh sistem). */
class SoftwareUpdateScreen(act: MainActivity) : Screen(act) {
    private val refreshers = ArrayList<() -> Unit>()

    private fun betaLabel(a: MainActivity): String =
        when (A11yPrefs.getS(a, "su_beta", "dev")) { "off" -> "Off"; "pub" -> "iOS 27 Public Beta"; else -> "iOS 27 Developer Beta" }

    override fun build(): View {
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, act.dp(40f)) }
        col.addView(act.navBar("Software Update"))
        renderCards(act, col, listOf(Cards(null, null, listOf(
            Lnk("Automatic Updates", null, { a ->
                if (A11yPrefs.get(a, "su_dl", true) || A11yPrefs.get(a, "su_inst", true)) "On" else "Off"
            }) { it.push(AutoUpdatesScreen(it)) },
            Lnk("Beta Updates", null, { a -> betaLabel(a) }) { it.push(BetaUpdatesScreen(it)) }
        ))), refreshers)

        val c = act.card()
        val box = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(act.dp(16f), act.dp(16f), act.dp(16f), act.dp(8f)) }
        val head = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(ImageView(act).apply { setImageResource(R.drawable.ios27_icon); scaleType = ImageView.ScaleType.FIT_CENTER },
            LinearLayout.LayoutParams(act.dp(60f), act.dp(60f)).apply { rightMargin = act.dp(14f) })
        val tx = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        tx.addView(TextView(act).apply { text = "iOS 27 Beta"; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Theme.text) })
        tx.addView(TextView(act).apply { text = "18.4 GB"; textSize = 15f; setTextColor(Theme.text) })
        head.addView(tx)
        box.addView(head)
        box.addView(TextView(act).apply {
            text = "iOS beta gives you an early preview of upcoming apps, features, and technologies. Please back up your iPhone before you install the beta.\n\nFor more information, please visit one of the following programs:"
            textSize = 17f; setTextColor(Theme.text); setPadding(0, act.dp(14f), 0, 0)
        })
        box.addView(linkLine("Apple Beta Software Program at", "beta.apple.com"))
        box.addView(linkLine("Apple Developer Program at", "developer.apple.com"))
        c.addView(box)
        c.addView(act.sepView(0))
        val btns = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(act.dp(16f), act.dp(14f), act.dp(16f), act.dp(8f)) }
        btns.addView(TextView(act).apply {
            text = "Update Now"; textSize = 17f; typeface = Typeface.DEFAULT_BOLD; setTextColor(0xFFFFFFFF.toInt()); gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(0xFF3B86F5.toInt()); cornerRadius = act.dpf(30f) }
            setPadding(0, act.dp(16f), 0, act.dp(16f))
            setOnClickListener {
                IosDialog(act).title("Update Now")
                    .message("This is an iOS-style update screen. Android installs its own updates. Open Android's system update screen?")
                    .button("Cancel")
                    .button("Open", 1) { Sys.open(act, "android.settings.SYSTEM_UPDATE_SETTINGS", android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS) }
                    .show()
            }
        })
        btns.addView(TextView(act).apply {
            text = "Update Tonight"; textSize = 17f; setTextColor(Theme.blue); gravity = Gravity.CENTER; setPadding(0, act.dp(18f), 0, act.dp(12f))
            setOnClickListener {
                IosDialog(act).title("Update Tonight")
                    .message("iPhone will try to update when the device is locked and the battery has enough charge.")
                    .button("Cancel").button("OK", 1).show()
            }
        })
        c.addView(btns)
        col.addView(c)
        col.addView(TextView(act).apply {
            text = "If you select Update Tonight, iPhone will try to update when the device is locked and the battery has enough charge."
            textSize = 13f; setTextColor(Theme.sub); setPadding(act.dp(32f), act.dp(6f), act.dp(32f), 0)
        })
        col.addView(TextView(act).apply {
            text = "This phone: Android ${android.os.Build.VERSION.RELEASE}, security update ${android.os.Build.VERSION.SECURITY_PATCH}."
            textSize = 13f; setTextColor(Theme.sub); setPadding(act.dp(32f), act.dp(10f), act.dp(32f), 0)
        })
        return ScrollView(act).apply { addView(col); isVerticalScrollBarEnabled = false }
    }

    private fun linkLine(prefix: String, url: String): TextView {
        val tv = TextView(act)
        tv.textSize = 17f; tv.setTextColor(Theme.text); tv.setPadding(act.dp(12f), act.dp(4f), 0, 0)
        val sp = SpannableString("•  $prefix $url")
        val start = sp.length - url.length
        sp.setSpan(ForegroundColorSpan(Theme.blue), start, sp.length, 0)
        sp.setSpan(UnderlineSpan(), start, sp.length, 0)
        tv.text = sp
        tv.setOnClickListener {
            try { act.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://$url")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { }
        }
        return tv
    }

    override fun onShow() { refreshers.forEach { it() } }
}

class AutoUpdatesScreen(act: MainActivity) : Screen(act) {
    private val refreshers = ArrayList<() -> Unit>()

    override fun build(): View {
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, act.dp(40f)) }
        col.addView(act.navBar2("Software Update", "Automatic Updates"))
        renderCards(act, col, listOf(
            Cards(null, "Your iPhone will try to download updates automatically.", listOf(Tog("Download iOS Updates", "su_dl", true))),
            Cards(null, "Your iPhone will try to install updates tonight when it is locked and charging.", listOf(Tog("Install iOS Updates", "su_inst", true))),
            Cards(null, "Security responses and system files are installed automatically.", listOf(Tog("Security Responses & System Files", "su_sec", true)))
        ), refreshers)
        return ScrollView(act).apply { addView(col); isVerticalScrollBarEnabled = false }
    }

    override fun onShow() { refreshers.forEach { it() } }
}

class BetaUpdatesScreen(act: MainActivity) : Screen(act) {
    private val refreshers = ArrayList<() -> Unit>()

    override fun build(): View {
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, act.dp(40f)) }
        col.addView(act.navBar2("Software Update", "Beta Updates"))
        renderCards(act, col, listOf(
            Cards(null, "Choose which beta updates are offered on this device.", listOf(
                Opt("Off", "su_beta", "off"),
                Opt("iOS 27 Developer Beta", "su_beta", "dev", true),
                Opt("iOS 27 Public Beta", "su_beta", "pub")
            ))
        ), refreshers)
        return ScrollView(act).apply { addView(col); isVerticalScrollBarEnabled = false }
    }

    override fun onShow() { refreshers.forEach { it() } }
}
