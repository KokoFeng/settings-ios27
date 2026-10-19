package com.ios27.settings

import android.app.Activity
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings

/** Satu baris pengaturan. [action] membuka layar internal atau layar pengaturan Android. */
class Entry(
    val title: String, val tile: Tile?,
    val value: ((MainActivity) -> String?)? = null,
    val toggle: ((MainActivity) -> Boolean)? = null,
    val action: (MainActivity) -> Unit
)

object Sys {
    /** Coba beberapa aksi pengaturan; yang pertama tersedia dibuka, jika tidak ada buka Pengaturan utama. */
    fun open(act: Activity, vararg actions: String) {
        for (a in actions) {
            try { act.startActivity(Intent(a).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return } catch (_: Exception) { }
        }
        try { act.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { }
    }
}

object Info {
    fun battery(ctx: Context): Int? {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
        val lvl = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1); val sc = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (lvl >= 0 && sc > 0) lvl * 100 / sc else null
    }

    fun bluetooth(ctx: Context): String {
        val a = (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter ?: return ""
        return if (a.isEnabled) "On" else "Off"
    }

    fun airplane(ctx: Context): Boolean =
        Settings.Global.getInt(ctx.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1

    @Suppress("DEPRECATION")
    fun currentSsid(wm: WifiManager): String? {
        val ci = try { wm.connectionInfo } catch (_: Exception) { null } ?: return null
        if (ci.networkId == -1) return null
        val s = ci.ssid?.trim('"') ?: return null
        return if (s.isEmpty() || s == "<unknown ssid>") null else s
    }

    fun wifiValue(act: MainActivity): String {
        val wm = act.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wm.isWifiEnabled) return "Off"
        if (!act.hasWifiPerms()) return "On"
        return currentSsid(wm) ?: "Not Connected"
    }

    fun deviceName(ctx: Context): String =
        Settings.Global.getString(ctx.contentResolver, "device_name") ?: Build.MODEL
}

object Catalog {
    private val orange = 0xFFFF9500.toInt()
    private val blue = 0xFF0A84FF.toInt()
    private val green = 0xFF30D158.toInt()
    private val red = 0xFFFF3B30.toInt()
    private val gray = 0xFF8E8E93.toInt()
    private val purple = 0xFF5E5CE6.toInt()
    private val pink = 0xFFFF2D55.toInt()
    private val teal = 0xFF32ADE6.toInt()
    private val dark = 0xFF636366.toInt()

    private fun g(s: String) = Tile(gray, glyph = s)
    private fun pg(key: String): (MainActivity) -> Unit = { it.push(DetailScreen(it, key)) }

    fun home(act: MainActivity): List<List<Entry>> = listOf(
        listOf(
            Entry("Airplane Mode", Tile(orange, glyph = "✈\uFE0E"), toggle = { Info.airplane(it) },
                action = { Sys.open(it, Settings.ACTION_AIRPLANE_MODE_SETTINGS) }),
            Entry("Wi-Fi", Tile(blue, kind = "wifi"), value = { Info.wifiValue(it) }, action = { it.push(WifiScreen(it)) }),
            Entry("Bluetooth", Tile(blue, kind = "bt"), value = { Info.bluetooth(it) }, action = pg("bluetooth")),
            Entry("Cellular", Tile(green, kind = "cell"), action = pg("cellular")),
            Entry("Personal Hotspot", Tile(green, glyph = "⛓\uFE0E"), action = pg("hotspot")),
            Entry("Battery", Tile(green, kind = "battery"), value = { a -> Info.battery(a)?.let { "$it%" } }, action = pg("battery"))
        ),
        listOf(
            Entry("Notifications", Tile(red, kind = "bell"), action = pg("notifications")),
            Entry("Sounds & Haptics", Tile(pink, kind = "speaker"), action = pg("sounds")),
            Entry("Focus", Tile(purple, glyph = "☾"), action = pg("focus")),
            Entry("Screen Time", Tile(purple, glyph = "⌛\uFE0E"), action = pg("screentime"))
        ),
        listOf(
            Entry("General", g("⚙\uFE0E"), action = { it.push(GeneralScreen(it)) }),
            Entry("Accessibility", Tile(blue, glyph = "◎"), action = { it.push(DetailScreen(it, "main")) }),
            Entry("Appearance", Tile(0xFF1C1C1E.toInt(), glyph = "◐"), action = pg("appearance")),
            Entry("Display & Brightness", Tile(blue, glyph = "☀\uFE0E"), action = pg("dispbright")),
            Entry("Home Screen & App Library", Tile(purple, glyph = "▦"), action = pg("homescreen")),
            Entry("Wallpaper", Tile(teal, glyph = "❀\uFE0E"), action = pg("wallpaper")),
            Entry("Apple Intelligence & Siri", Tile(dark, glyph = "⌕"), action = pg("siri")),
            Entry("Face ID & Passcode", Tile(green, kind = "lock"), action = pg("faceid")),
            Entry("Privacy & Security", Tile(blue, glyph = "✋\uFE0E"), action = pg("privacy"))
        ),
        listOf(
            Entry("Passwords & Accounts", g("✉\uFE0E"), action = pg("passwords")),
            Entry("Apps", Tile(blue, glyph = "▦"), action = pg("apps")),
            Entry("Developer", g("</>"), action = pg("developer"))
        )
    )

    fun general(act: MainActivity): List<List<Entry>> = listOf(
        listOf(
            Entry("About", g("i"), action = { it.push(AboutScreen(it)) }),
            Entry("Software Update", g("⟳"), action = pg("swupdate")),
            Entry("iPhone Storage", g("▤"), action = { it.push(StorageScreen(it)) })
        ),
        listOf(Entry("AppleCare & Warranty", Tile(blue, glyph = "✓"), action = { Sys.open(it, Settings.ACTION_DEVICE_INFO_SETTINGS) })),
        listOf(
            Entry("AirDrop", Tile(blue, glyph = "◉"), action = pg("airdrop")),
            Entry("AirPlay & Continuity", Tile(blue, glyph = "▭"), action = pg("airplay")),
            Entry("Screen Capture", g("◎"), action = pg("screencap")),
            Entry("CarPlay", g("▶\uFE0E"), action = pg("carplay"))
        ),
        listOf(
            Entry("Background App Refresh", g("⟳"), action = pg("bgrefresh")),
            Entry("Date & Time", Tile(blue, glyph = "⌚\uFE0E"), action = pg("datetime")),
            Entry("Dictionary", Tile(blue, glyph = "≡"), action = pg("dictionary")),
            Entry("Fonts", g("Aa"), action = pg("fonts")),
            Entry("Game Controller", g("◈"), action = pg("gamecontroller")),
            Entry("Keyboard", g("⌨\uFE0E"), action = pg("keyboard")),
            Entry("Language & Region", Tile(blue, glyph = "⌘"), action = pg("langregion"))
        ),
        listOf(Entry("TV Provider", Tile(dark, glyph = "S"), action = pg("tvprovider"))),
        listOf(Entry("VPN & Device Management", g("⚙\uFE0E"), action = pg("vpn"))),
        listOf(Entry("Legal & Regulatory", g("§"), action = pg("legal"))),
        listOf(Entry("Transfer or Reset iPhone", g("⟲"), action = pg("reset")))
    )

    fun all(act: MainActivity): List<Entry> = (home(act) + general(act)).flatten()
}
