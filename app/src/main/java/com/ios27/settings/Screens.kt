package com.ios27.settings

import android.app.AlertDialog
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.text.format.Formatter
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Menggambar kelompok baris (kartu) dari daftar [Entry]. */
fun renderGroups(act: MainActivity, parent: LinearLayout, groups: List<List<Entry>>, refreshers: MutableList<() -> Unit>) {
    for (g in groups) {
        val card = act.card()
        g.forEachIndexed { i, e ->
            val tg = e.toggle
            val rp = act.makeRow(e.tile, e.title, chevron = tg == null, clickable = true)
            rp.row.setOnClickListener { e.action(act) }
            if (tg != null) {
                val sw = IosSwitch(act)
                sw.onToggle = { e.action(act) }
                sw.checked = tg(act)
                rp.row.addView(sw, LinearLayout.LayoutParams(act.dp(51f), act.dp(31f)).apply { leftMargin = act.dp(8f) })
                refreshers.add { sw.checked = tg(act) }
            } else {
                val vf = e.value
                if (vf != null) {
                    rp.value.text = vf(act) ?: ""
                    refreshers.add { rp.value.text = vf(act) ?: "" }
                }
            }
            card.addView(rp.row)
            if (i < g.size - 1) card.addView(act.sepView(if (e.tile != null) act.dp(60f) else act.dp(16f)))
        }
        parent.addView(card)
    }
}

private fun scroll(act: MainActivity, col: LinearLayout): ScrollView =
    ScrollView(act).apply { addView(col); isVerticalScrollBarEnabled = false }

// ------------------------------------------------------------------ layar utama
class HomeScreen(act: MainActivity) : Screen(act) {
    private val refreshers = ArrayList<() -> Unit>()
    private var searchBar: LinearLayout? = null
    private var resColumn: LinearLayout? = null

    override fun onKeyboard(h: Int) {
        val b = searchBar ?: return
        (b.layoutParams as FrameLayout.LayoutParams).bottomMargin = act.dp(24f) + h
        b.requestLayout()
        resColumn?.setPadding(0, act.statusBarH() + act.dp(60f), 0, act.dp(120f) + h)
    }

    override fun build(): View {
        val root = FrameLayout(act)
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, act.statusBarH() + act.dp(8f), 0, act.dp(120f))
        }
        col.addView(TextView(act).apply {
            text = "Settings"; textSize = 34f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Theme.text)
            setPadding(act.dp(20f), act.dp(24f), 0, act.dp(8f))
        })
        profileHolder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        col.addView(profileHolder)
        renderProfile()
        renderGroups(act, col, Catalog.home(act), refreshers); col.addView(supportCard(act))
        val main = scroll(act, col)
        root.addView(main, FrameLayout.LayoutParams(-1, -1))

        // hasil pencarian
        val resCol = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, act.statusBarH() + act.dp(60f), 0, act.dp(120f))
        }
        val results = scroll(act, resCol).apply { visibility = View.GONE; setBackgroundColor(Theme.bg) }
        root.addView(results, FrameLayout.LayoutParams(-1, -1))

        // bilah cari mengambang di bawah
        val bar = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(act.dp(16f), 0, act.dp(12f), 0)
            background = GradientDrawable().apply {
                setColor((Theme.card and 0x00FFFFFF) or ((0x66 + (A11yPrefs.getF(act, "lg", 0.5f) * 0x8C).toInt()) shl 24)); cornerRadius = act.dpf(26f)
                setStroke(1, Theme.sep)
            }
            elevation = act.dpf(6f)
        }
        bar.addView(TextView(act).apply { text = "⌕"; textSize = 24f; setTextColor(Theme.text) })
        val input = EditText(act).apply {
            hint = "Search"; setHintTextColor(Theme.sub); setTextColor(Theme.text); textSize = 17f
            background = null; isSingleLine = true; setPadding(act.dp(10f), 0, 0, 0)
        }
        bar.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        searchBar = bar; resColumn = resCol
        root.addView(bar, FrameLayout.LayoutParams(-1, act.dp(50f), Gravity.BOTTOM).apply {
            setMargins(act.dp(16f), 0, act.dp(16f), act.dp(24f))
        })
        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(e: Editable?) {
                val q = e?.toString()?.trim() ?: ""
                resCol.removeAllViews()
                if (q.isEmpty()) { results.visibility = View.GONE; return }
                results.visibility = View.VISIBLE
                val hits = Catalog.all(act).filter { it.title.contains(q, ignoreCase = true) }
                if (hits.isEmpty()) {
                    resCol.addView(act.infoCard("No Results for \"$q\"", null, null))
                } else {
                    val card = act.card()
                    hits.forEachIndexed { i, en ->
                        val rp = act.makeRow(en.tile, en.title, chevron = true, clickable = true)
                        rp.row.setOnClickListener { en.action(act) }
                        card.addView(rp.row)
                        if (i < hits.size - 1) card.addView(act.sepView(if (en.tile != null) act.dp(60f) else act.dp(16f)))
                    }
                    resCol.addView(card)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        return root
    }

    private lateinit var profileHolder: LinearLayout

    private fun renderProfile() {
        if (!::profileHolder.isInitialized) return
        profileHolder.removeAllViews(); profileHolder.addView(profileCard())
    }

    private fun profileCard(): LinearLayout {
        val c = act.card()
        val has = Profile.has(act)
        val row = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(act.dp(16f), act.dp(14f), act.dp(14f), act.dp(14f)); foreground = act.ripple()
            setOnClickListener { act.push(if (has) AppleIdScreen(act) else AccountSetupScreen(act)) }
        }
        val av = AvatarView(act, 60f).apply { load(); initials = if (has) Profile.initials(act) else "" }
        row.addView(av, LinearLayout.LayoutParams(act.dp(60f), act.dp(60f)).apply { rightMargin = act.dp(14f) })
        val tx = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        tx.addView(TextView(act).apply {
            text = if (has) Profile.name(act) else "Sign in to your iPhone"
            textSize = 20f; typeface = Typeface.DEFAULT_BOLD; setTextColor(if (has) Theme.text else Theme.blue)
        })
        tx.addView(TextView(act).apply {
            text = if (has) "Apple ID, iCloud, Media & Purchases" else "Set up iCloud, the App Store, and more."
            textSize = 13f; setTextColor(Theme.sub)
        })
        row.addView(tx, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(TextView(act).apply { text = "›"; textSize = 24f; setTextColor(Theme.chev) })
        c.addView(row)
        return c
    }

    override fun onShow() { renderProfile(); refreshers.forEach { it() } }
}

// --------------------------------------------------------------------- General
class GeneralScreen(act: MainActivity) : Screen(act) {
    override fun build(): View {
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, act.dp(40f)) }
        col.addView(act.navBar(null))
        col.addView(act.headerCard(Tile(0xFF8E8E93.toInt(), glyph = "⚙\uFE0E"), "General",
            "Manage your overall setup and preferences for Device, such as software updates, device language, CarPlay, AirDrop, and more."))
        renderGroups(act, col, Catalog.general(act), ArrayList())
        return scroll(act, col)
    }
}

// ----------------------------------------------------------------------- About
class AboutScreen(act: MainActivity) : Screen(act) {
    private lateinit var body: LinearLayout

    private class AR(val k: String, val v: String, val key: String? = null)

    override fun build(): View {
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, act.dp(40f)) }
        col.addView(act.navBar("About"))
        body = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        col.addView(body)
        render()
        return scroll(act, col)
    }

    /** Ketuk Name / Model Name / Manufacturer untuk mengganti teksnya (hanya tampilan). */
    private fun edit(label: String, key: String, current: String) {
        val d = IosDialog(act).title(label)
        val f = d.field(label, current)
        d.button("Cancel").button("Reset") { Over.reset(act, key); render() }
            .button("Save", 1) { if (!f.text.isNullOrBlank()) { Over.set(act, key, f.text.toString().trim()); render() } }
        d.show()
    }

    private fun render() {
        body.removeAllViews()
        val mi = ActivityManager.MemoryInfo()
        (act.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
        val st = StatFs(Environment.getDataDirectory().path)
        val total = st.blockCountLong * st.blockSizeLong
        val free = st.availableBlocksLong * st.blockSizeLong
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION") act.windowManager.defaultDisplay.getRealMetrics(dm)
        val up = SystemClock.elapsedRealtime() / 1000
        val groups = listOf(
            listOf(AR("Developer", "Feng iOS on Droid"), AR("Name", Over.name(act), "name"),
                AR("Model Name", Over.model(act), "model"), AR("Manufacturer", Over.manu(act), "manu")),
            listOf(
                AR("Android Version", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"),
                AR("Security Update", Build.VERSION.SECURITY_PATCH ?: "-"),
                AR("Build", Build.DISPLAY),
                AR("Kernel", System.getProperty("os.version") ?: "-")
            ),
            listOf(
                AR("Processor", Build.SUPPORTED_ABIS.firstOrNull() ?: "-"),
                AR("Memory", Formatter.formatFileSize(act, mi.totalMem)),
                AR("Capacity", Formatter.formatFileSize(act, total)),
                AR("Available", Formatter.formatFileSize(act, free)),
                AR("Screen", "${dm.widthPixels} x ${dm.heightPixels} (${dm.densityDpi} dpi)"),
                AR("Battery", Info.battery(act)?.let { "$it%" } ?: "-"),
                AR("Up Time", "${up / 3600} h ${(up % 3600) / 60} min")
            )
        )
        for (g in groups) {
            val c = act.card()
            g.forEachIndexed { i, r ->
                val editable = r.key != null
                val rp = act.makeRow(null, r.k, chevron = editable, clickable = editable)
                rp.value.text = r.v; rp.value.maxWidth = act.dp(230f)
                if (editable) rp.row.setOnClickListener { edit(r.k, r.key!!, r.v) }
                c.addView(rp.row)
                if (i < g.size - 1) c.addView(act.sepView(act.dp(16f)))
            }
            body.addView(c)
        }
    }

    override fun onShow() { if (::body.isInitialized) render() }
}

// --------------------------------------------------------------------- Storage
class StorageScreen(act: MainActivity) : Screen(act) {
    override fun build(): View {
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, act.dp(40f)) }
        col.addView(act.navBar("iPhone Storage"))
        val st = StatFs(Environment.getDataDirectory().path)
        val total = st.blockCountLong * st.blockSizeLong
        val free = st.availableBlocksLong * st.blockSizeLong
        val used = total - free
        val c = act.card()
        val box = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(act.dp(16f), act.dp(16f), act.dp(16f), act.dp(16f)) }
        box.addView(TextView(act).apply {
            text = "${Formatter.formatFileSize(act, used)} of ${Formatter.formatFileSize(act, total)} Used"
            textSize = 17f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Theme.text)
        })
        box.addView(StorageBar(act, if (total > 0) used.toFloat() / total else 0f),
            LinearLayout.LayoutParams(-1, act.dp(10f)).apply { topMargin = act.dp(12f) })
        box.addView(TextView(act).apply {
            text = "${Formatter.formatFileSize(act, free)} available"; textSize = 13f; setTextColor(Theme.sub); setPadding(0, act.dp(8f), 0, 0)
        })
        c.addView(box)
        col.addView(c)
        val entries = listOf(
            listOf(
                Entry("Manage Storage", Tile(0xFF8E8E93.toInt(), glyph = "▤"),
                    action = { Sys.open(it, "android.settings.INTERNAL_STORAGE_SETTINGS", Settings.ACTION_MEMORY_CARD_SETTINGS) }),
                Entry("Apps", Tile(0xFF0A84FF.toInt(), glyph = "▦"), action = { Sys.open(it, Settings.ACTION_APPLICATION_SETTINGS) })
            )
        )
        renderGroups(act, col, entries, ArrayList())
        return scroll(act, col)
    }
}

// ----------------------------------------------------------------------- Wi-Fi
class Net(val ssid: String, val level: Int, val rssi: Int, val caps: String, val freq: Int, val bssid: String) {
    val security: String get() = when {
        caps.contains("SAE") || caps.contains("WPA3") -> "WPA3"
        caps.contains("WPA2") -> "WPA2"
        caps.contains("WPA") -> "WPA"
        caps.contains("WEP") -> "WEP"
        caps.contains("OWE") -> "Enhanced Open"
        else -> "Open"
    }
    val secured: Boolean get() = security != "Open" && security != "Enhanced Open"
    val band: String get() = when {
        freq in 2400..2500 -> "2.4 GHz"
        freq in 4900..5900 -> "5 GHz"
        freq >= 5925 -> "6 GHz"
        else -> "$freq MHz"
    }
}

class WifiScreen(act: MainActivity) : Screen(act) {
    private val wm = act.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var list: LinearLayout
    private lateinit var sw: IosSwitch
    private var scanning = false
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) { scanning = false; render() }
    }
    private val tick = object : Runnable {
        override fun run() { startScan(); handler.postDelayed(this, 12000) }
    }

    override fun build(): View {
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, act.dp(40f)) }
        col.addView(act.navBar(null))
        val toggleRow = act.makeRow(null, "Wi-Fi", chevron = false, clickable = false)
        sw = IosSwitch(act)
        sw.onToggle = { openPanel() }
        toggleRow.row.addView(sw, LinearLayout.LayoutParams(act.dp(51f), act.dp(31f)).apply { leftMargin = act.dp(8f) })
        col.addView(act.headerCard(Tile(0xFF0A84FF.toInt(), kind = "wifi"), "Wi-Fi",
            "Connect to Wi-Fi, view available networks, and manage settings for joining networks and nearby hotspots.", toggleRow.row))
        list = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        col.addView(list)
        return scroll(act, col)
    }

    private fun openPanel() {
        if (Build.VERSION.SDK_INT >= 30) Sys.open(act, Settings.Panel.ACTION_WIFI, Settings.ACTION_WIFI_SETTINGS)
        else Sys.open(act, Settings.ACTION_WIFI_SETTINGS)
    }

    private fun locationOn(): Boolean {
        val lm = act.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (Build.VERSION.SDK_INT >= 28) lm.isLocationEnabled else true
    }

    @Suppress("DEPRECATION")
    private fun startScan() {
        if (!wm.isWifiEnabled || !act.hasWifiPerms()) return
        scanning = true; render()
        try { wm.startScan() } catch (_: Exception) { }
        handler.postDelayed({ if (scanning) { scanning = false; render() } }, 7000)
    }

    @Suppress("DEPRECATION")
    private fun networks(): List<Net> {
        val raw = try { wm.scanResults } catch (_: SecurityException) { emptyList<ScanResult>() }
        val map = LinkedHashMap<String, Net>()
        for (s in raw) {
            val name = (s.SSID ?: "").trim('"')
            if (name.isEmpty()) continue
            val n = Net(name, WifiManager.calculateSignalLevel(s.level, 5), s.level, s.capabilities ?: "", s.frequency, s.BSSID ?: "")
            val old = map[name]
            if (old == null || n.rssi > old.rssi) map[name] = n
        }
        return map.values.sortedByDescending { it.rssi }
    }

    private fun render() {
        if (!::list.isInitialized) return
        sw.checked = wm.isWifiEnabled
        list.removeAllViews()
        if (!act.hasWifiPerms()) {
            list.addView(act.infoCard("To show nearby Wi-Fi networks, Android requires location access for this app.", "Allow Access") { act.askWifiPerms() })
            return
        }
        if (!wm.isWifiEnabled) {
            list.addView(TextView(act).apply {
                text = "Wi-Fi is turned off. Turn it on to see available networks."; textSize = 15f; setTextColor(Theme.sub)
                setPadding(act.dp(32f), act.dp(16f), act.dp(32f), 0)
            })
            return
        }
        if (!locationOn()) {
            list.addView(act.infoCard("Location Services is off. Turn it on so Android can list nearby networks.", "Open Location Settings") {
                Sys.open(act, Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            })
        }
        val nets = networks()
        val cur = Info.currentSsid(wm)
        val connected = nets.firstOrNull { it.ssid == cur }
        if (cur != null) {
            val c = act.card()
            c.addView(networkRow(connected ?: Net(cur, 4, -50, "", 0, ""), true))
            list.addView(c)
        }
        val spin = if (scanning) ProgressBar(act, null, android.R.attr.progressBarStyleSmall).apply {
            indeterminateTintList = ColorStateList.valueOf(Theme.sub)
        } else null
        list.addView(act.sectionLabel("Other Networks", spin))
        val c = act.card()
        val others = nets.filter { it.ssid != cur }
        others.forEach { n -> c.addView(networkRow(n, false)); c.addView(act.sepView(act.dp(44f))) }
        val other = act.makeRow(null, "Other…", chevron = false, clickable = true)
        other.row.setOnClickListener { Sys.open(act, Settings.ACTION_WIFI_SETTINGS) }
        c.addView(other.row)
        list.addView(c)
        list.addView(TextView(act).apply {
            text = "Tap a network to open Android's Wi-Fi settings and join it. The (i) button shows its details."
            textSize = 13f; setTextColor(Theme.sub); setPadding(act.dp(32f), act.dp(6f), act.dp(32f), 0)
        })
    }

    private fun networkRow(n: Net, connected: Boolean): LinearLayout {
        val row = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            minimumHeight = act.dp(52f); setPadding(act.dp(14f), 0, act.dp(12f), 0); foreground = act.ripple()
        }
        row.addView(TextView(act).apply {
            text = if (connected) "✓" else ""; textSize = 20f; setTextColor(Theme.blue); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(act.dp(28f), -2))
        row.addView(TextView(act).apply {
            text = n.ssid; textSize = 17f; setTextColor(Theme.text); maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
        }, LinearLayout.LayoutParams(0, -2, 1f))
        if (n.secured) row.addView(LockView(act), LinearLayout.LayoutParams(act.dp(18f), act.dp(18f)).apply { rightMargin = act.dp(8f) })
        row.addView(SignalView(act, n.level), LinearLayout.LayoutParams(act.dp(22f), act.dp(22f)).apply { rightMargin = act.dp(10f) })
        row.addView(InfoButton(act).apply { setOnClickListener { showInfo(n) } }, LinearLayout.LayoutParams(act.dp(30f), act.dp(30f)))
        row.setOnClickListener {
            Toast.makeText(act, "Join \"${n.ssid}\" in Android's Wi-Fi settings", Toast.LENGTH_SHORT).show()
            Sys.open(act, Settings.ACTION_WIFI_SETTINGS)
        }
        return row
    }

    private fun showInfo(n: Net) {
        val msg = "Security: ${n.security}\nSignal: ${n.rssi} dBm (${n.level}/4)\nBand: ${n.band}" +
            (if (n.bssid.isNotEmpty()) "\nBSSID: ${n.bssid}" else "")
        IosDialog(act).title(n.ssid).message(msg)
            .button("Wi-Fi Settings") { Sys.open(act, Settings.ACTION_WIFI_SETTINGS) }.button("OK", 1).show()
    }

    override fun onShow() {
        if (!registered) {
            val f = IntentFilter()
            f.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION); f.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            f.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            act.registerReceiver(receiver, f); registered = true
        }
        render()
        handler.removeCallbacks(tick); handler.post(tick)
    }

    override fun onHide() {
        handler.removeCallbacks(tick)
        if (registered) { try { act.unregisterReceiver(receiver) } catch (_: Exception) { }; registered = false }
    }

    override fun onPermission() { render(); startScan() }
}
