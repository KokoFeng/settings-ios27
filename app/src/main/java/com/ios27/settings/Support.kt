package com.ios27.settings

import android.content.Intent
import android.net.Uri
import android.widget.LinearLayout
import android.widget.TextView

private const val DONATE_URL = "https://sociabuzz.com/feng_ios/tribe"

/** Kartu "Support Developer": membuka halaman donasi setelah konfirmasi. */
fun supportCard(act: MainActivity): LinearLayout {
    val wrap = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
    val c = act.card()
    val rp = act.makeRow(Tile(0xFFFF2D55.toInt(), glyph = "♥\uFE0E"), "Support Developer", chevron = true, clickable = true)
    rp.row.setOnClickListener {
        IosDialog(act).title("Support Developer")
            .message("Open the donation page of Feng iOS on Droid in your browser?")
            .button("Cancel")
            .button("Open", 1) {
                try { act.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DONATE_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { }
            }.show()
    }
    c.addView(rp.row)
    wrap.addView(c)
    wrap.addView(TextView(act).apply {
        text = "Thank you for supporting Feng iOS on Droid."
        textSize = 13f; setTextColor(Theme.sub); setPadding(act.dp(32f), act.dp(2f), act.dp(32f), 0)
    })
    return wrap
}
