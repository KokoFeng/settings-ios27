package com.ios27.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import java.util.Locale

/** Halaman pengaturan di dalam aplikasi (gaya iOS 27). Saklar yang terhubung ke Android membaca status asli; sisanya tersimpan di aplikasi ini. */
object Pg {
    private fun tg(t: String, k: String, d: Boolean = false, b: Bind? = null) = Tog(t, k, d, b)
    private fun one(t: String, k: String, f: String? = null, d: Boolean = false, b: Bind? = null) = Cards(null, f, listOf(Tog(t, k, d, b)))
    private fun c(h: String?, f: String?, vararg i: Item) = Cards(h, f, i.toList())
    private fun go(k: String): (MainActivity) -> Unit = { it.push(DetailScreen(it, k)) }
    private fun ln(t: String, k: String, v: ((MainActivity) -> String?)? = null) = Lnk(t, null, v, go(k))
    private fun sys(t: String, vararg a: String) = Lnk(t, null, null) { Sys.open(it, *a) }
    private fun p(title: String, back: String, vararg b: Cards) = Page(title, back, null, b.toList())
    private val S = "Settings"; private val G = "General"

    fun page(key: String): Page? = when (key) {
        "appearance" -> p("Appearance", S,
            c("Appearance", null, Opt("Light", "appearance", "light"), Opt("Dark", "appearance", "dark"), Opt("Auto", "appearance", "auto", true)),
            c(null, "Choose how transparent Liquid Glass looks, from Clear to Tinted.", ln("Liquid Glass", "liquidglass")),
            c("Icon Style", null, Opt("Default", "iconstyle", "default", true), Opt("Dark", "iconstyle", "dark"), Opt("Clear", "iconstyle", "clear"), Opt("Tinted", "iconstyle", "tinted")),
            c(null, null, Lnk("Text Size", null, null) { it.push(LargerTextScreen(it)) }))
        "liquidglass" -> p("Liquid Glass", "Appearance",
            c("Clear  ←→  Tinted", "Clear is more see-through and Tinted is more opaque. In this app it changes the search bar; Android does not let one app restyle others.", Sld("lg", 0.5f)))
        "sounds" -> p("Sounds & Haptics", S,
            c("Ringtone and Alert Volume", null, Sld("vol_ring", 0.6f)), one("Change with Buttons", "vol_btn", null, true),
            c("Alarms and Timers", "Alarms and timers can have their own volume, or match the ringtone volume.", tg("Match Ringtone Volume", "match_ring", true), Sld("vol_alarm", 0.7f)),
            c("Sounds and Haptic Patterns", null, sys("Ringtone", Settings.ACTION_SOUND_SETTINGS), sys("Text Tone", Settings.ACTION_SOUND_SETTINGS)),
            c(null, null, tg("Keyboard Clicks", "kbclick", true), tg("Lock Sound", "locksound", true), tg("System Haptics", "syshaptic", true, Binds.haptic)),
            c(null, "Open Android's own sound settings for the real volumes.", sys("Android Sound Settings", Settings.ACTION_SOUND_SETTINGS)))
        "notifications" -> p("Notifications", S,
            c("Display As", null, Opt("Count", "notif_style", "count"), Opt("Stack", "notif_style", "stack", true), Opt("List", "notif_style", "list")),
            c(null, null, tg("Show Previews", "previews", true), tg("Scheduled Summary", "summary"), tg("Screen Sharing", "sharing", true)),
            c(null, null, sys("Android Notification Settings", "android.settings.ALL_APPS_NOTIFICATION_SETTINGS", Settings.ACTION_SOUND_SETTINGS)))
        "focus" -> p("Focus", S,
            one("Do Not Disturb", "dnd", "Silences calls and notifications. This reads Android's Do Not Disturb state.", false, Binds.dnd),
            c(null, null, tg("Sleep", "f_sleep"), tg("Personal", "f_personal"), tg("Work", "f_work"), tg("Driving", "f_drive")),
            c(null, null, sys("Android Do Not Disturb", "android.settings.ZEN_MODE_SETTINGS", Settings.ACTION_SOUND_SETTINGS)))
        "screentime" -> p("Screen Time", S,
            c(null, "See how much time you spend on your device.", sys("Android Digital Wellbeing", "com.android.settings.action.DIGITAL_WELLBEING", Settings.ACTION_SETTINGS)),
            c(null, null, tg("Downtime", "st_down"), tg("App Limits", "st_limits"), tg("Communication Limits", "st_comm"), tg("Content & Privacy Restrictions", "st_content")))
        "battery" -> p("Battery", S,
            one("Low Power Mode", "lowpower", "Low Power Mode reduces background activity to save battery.", false, Binds.lowPower),
            c(null, null, tg("Battery Percentage", "battpct", true), Inf("Battery Level") { a -> Info.battery(a)?.let { "$it%" } }),
            c(null, null, sys("Battery Usage (Android)", Intent.ACTION_POWER_USAGE_SUMMARY)))
        "cellular" -> p("Cellular", S, one("Cellular Data", "celldata", null, true), one("Data Roaming", "roam"), one("Low Data Mode", "lowdata"),
            c(null, null, sys("Android Data Usage", "android.settings.DATA_USAGE_SETTINGS", Settings.ACTION_WIRELESS_SETTINGS)))
        "hotspot" -> p("Personal Hotspot", S, one("Allow Others to Join", "hs", "Share your internet connection with other devices."),
            c(null, null, sys("Android Hotspot Settings", "android.settings.TETHER_SETTINGS", Settings.ACTION_WIRELESS_SETTINGS)))
        "bluetooth" -> p("Bluetooth", S, one("Bluetooth", "bt", "Pair and manage devices in Android's Bluetooth screen.", true, Binds.bt),
            c(null, null, sys("Android Bluetooth Settings", Settings.ACTION_BLUETOOTH_SETTINGS)))
        "dispbright" -> p("Display & Brightness", S,
            c("Brightness", null, Bri()), one("Auto-Brightness", "autobright", null, true, Binds.autoBright),
            c(null, null, Lnk("Text Size", null, null) { it.push(LargerTextScreen(it)) }, tg("Bold Text", "bold2", false, Binds.bold)),
            c(null, null, tg("Raise to Wake", "raise", true), sys("Auto-Lock (Android)", Settings.ACTION_DISPLAY_SETTINGS)))
        "homescreen" -> p("Home Screen & App Library", S,
            c("Newly Downloaded Apps", null, Opt("Add to Home Screen", "newapps", "home", true), Opt("App Library Only", "newapps", "lib")),
            c("Notification Badges", null, tg("Show in App Library", "badgelib", true), tg("Show on Home Screen", "badgehome", true)),
            c(null, null, sys("Android Home Settings", Settings.ACTION_HOME_SETTINGS)))
        "wallpaper" -> p("Wallpaper", S,
            c(null, "Choose a wallpaper for your home and lock screens.", sys("Add New Wallpaper", Intent.ACTION_SET_WALLPAPER)),
            c(null, null, tg("Dark Appearance Dims Wallpaper", "dimwp", true), tg("Depth Effect", "depth", true)))
        "siri" -> p("Apple Intelligence & Siri", S,
            c(null, "Talk to Siri or type to it.", tg("Talk & Type to Siri", "siri_tt", true)),
            c("Visual Intelligence", "Highlight an object on screen to search for it.", tg("Highlight to Image Search", "hl_img")),
            c("Extensions", null, tg("ChatGPT", "chatgpt")),
            c(null, null, sys("Android Assistant Settings", Settings.ACTION_VOICE_INPUT_SETTINGS)))
        "faceid" -> p("Face ID & Passcode", S,
            c("Use Face ID For", null, tg("iPhone Unlock", "fid1", true), tg("App Store", "fid2", true), tg("Wallet & Apple Pay", "fid3")),
            c(null, null, sys("Change Passcode (Android Security)", Settings.ACTION_SECURITY_SETTINGS)))
        "privacy" -> p("Privacy & Security", S,
            c(null, null, sys("Location Services", Settings.ACTION_LOCATION_SOURCE_SETTINGS), sys("Permission Manager (Android)", Settings.ACTION_PRIVACY_SETTINGS)),
            one("Analytics & Improvements", "analytics", null, true))
        "passwords" -> p("Passwords & Accounts", S, c(null, null, sys("Accounts", Settings.ACTION_SYNC_SETTINGS)), one("Autofill Passwords", "autofill", null, true))
        "apps" -> p("Apps", S, c(null, null, ln("Camera", "pg_camera"), ln("Messages", "pg_messages"), ln("Photos", "pg_photos")),
            c(null, null, sys("All Apps (Android)", Settings.ACTION_APPLICATION_SETTINGS)))
        "pg_camera" -> p("Camera", "Apps", c(null, null, tg("Preserve Settings", "cam1", true), tg("Grid", "cam2"), tg("Mirror Front Camera", "cam3", true), tg("View Outside the Frame", "cam4")))
        "pg_messages" -> p("Messages", "Apps",
            c("Show in Text Field", "Choose which button appears in the message text field.", Opt("Record Audio", "msg_tf", "audio", true), Opt("Start Dictation", "msg_tf", "dict"), Opt("None", "msg_tf", "none")),
            c(null, null, tg("Send as SMS", "msg1", true), tg("Send Read Receipts", "msg2")))
        "pg_photos" -> p("Photos", "Apps",
            c(null, null, tg("Sync Immediately", "ph1", true), tg("Show Rating Controls", "ph2")),
            c(null, null, tg("Show Hidden Album", "ph3"), tg("Show Recently Deleted", "ph4", true)))
        "developer" -> p("Developer", S, c(null, null, sys("Android Developer Options", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)), one("Enable UI Automation", "uiauto"))
        // ---- General
        "swupdate" -> p("Software Update", G,
            c(null, "Your software is up to date.", Inf("Android Version") { Build.VERSION.RELEASE }, Inf("Security Update") { Build.VERSION.SECURITY_PATCH }),
            one("Automatic Updates", "autoupd", null, true),
            c(null, null, sys("Check for Updates (Android)", "android.settings.SYSTEM_UPDATE_SETTINGS", Settings.ACTION_DEVICE_INFO_SETTINGS)))
        "airdrop" -> p("AirDrop", G,
            c("Allow me to be discovered by", null, Opt("Receiving Off", "airdrop", "off"), Opt("Contacts Only", "airdrop", "contacts", true), Opt("Everyone for 10 Minutes", "airdrop", "all")),
            one("Bringing Devices Together", "bring", null, true), c(null, "Android uses Quick Share instead.", sys("Quick Share (Android)", Settings.ACTION_BLUETOOTH_SETTINGS)))
        "airplay" -> p("AirPlay & Continuity", G, one("Automatically AirPlay to TVs", "ap_tv", null, true), one("Handoff", "handoff", null, true),
            c(null, null, sys("Cast (Android)", Settings.ACTION_CAST_SETTINGS)))
        "carplay" -> p("CarPlay", G, c(null, "Connect your phone to your car to use it on the car display.", sys("Android Auto", Settings.ACTION_SETTINGS)))
        "screencap" -> p("Screen Capture", G, c(null, null, tg("Screenshot Preview", "sc_prev", true), tg("Full-Page Screenshots", "sc_full")))
        "bgrefresh" -> p("Background App Refresh", G,
            c("Background App Refresh", null, Opt("Off", "bgr", "off"), Opt("Wi-Fi", "bgr", "wifi"), Opt("Wi-Fi & Cellular Data", "bgr", "both", true)),
            c(null, null, sys("Battery Optimization (Android)", Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)))
        "datetime" -> p("Date & Time", G, one("24-Hour Time", "t24", null, false, Binds.time24), one("Set Automatically", "autotime", null, true, Binds.autoTime),
            c(null, null, sys("Android Date & Time", Settings.ACTION_DATE_SETTINGS)))
        "dictionary" -> p("Dictionary", G, c(null, "Words you add are managed by Android.", sys("Android Personal Dictionary", "android.settings.USER_DICTIONARY_SETTINGS", Settings.ACTION_INPUT_METHOD_SETTINGS)))
        "fonts" -> p("Fonts", G, c(null, "Installed fonts are managed by Android.", sys("Android Display Settings", Settings.ACTION_DISPLAY_SETTINGS)))
        "gamecontroller" -> p("Game Controller", G, c(null, null, sys("Pair a Controller", Settings.ACTION_BLUETOOTH_SETTINGS)), one("Share Gameplay", "gameplay"))
        "keyboard" -> p("Keyboards", G,
            c(null, null, sys("Keyboards", Settings.ACTION_INPUT_METHOD_SETTINGS), sys("Hardware Keyboard", Settings.ACTION_INPUT_METHOD_SETTINGS)),
            c("All Keyboards", null, tg("Auto-Capitalization", "kb1", true), tg("Auto-Correction", "kb2", true), tg("Check Spelling", "kb3", true), tg("Enable Caps Lock", "kb4", true),
                tg("Predictive", "kb5", true), tg("Smart Punctuation", "kb6", true), tg("Character Preview", "kb7", true), tg(". Shortcut", "kb8", true)),
            c("Dictation", null, tg("Enable Dictation", "dic1", true), tg("Auto-Punctuation", "dic2", true)),
            c("Emoji", "Suggested Genmoji are created from your photos and your commonly typed phrases.", tg("Genmoji Suggestions", "gm"), tg("Stickers", "stk", true)))
        "langregion" -> p("Language & Region", G,
            c(null, null, Inf("iPhone Language") { Locale.getDefault().displayLanguage }, Inf("Region") { Locale.getDefault().displayCountry }),
            c(null, null, sys("Android Language Settings", Settings.ACTION_LOCALE_SETTINGS)))
        "tvprovider" -> p("TV Provider", G, c(null, "Sign in once to watch TV apps with your provider.", Lnk("Sign In", null, null) { Demo.dialog(it, "TV Provider") }))
        "vpn" -> p("VPN & Device Management", G, one("VPN", "vpn", "Reads Android's VPN connection state.", false, Binds.vpn),
            c(null, null, sys("Android VPN Settings", Settings.ACTION_VPN_SETTINGS)))
        "legal" -> p("Legal & Regulatory", G,
            c(null, null, Inf("Developer") { "Feng iOS on Droid" }, Inf("App") { "Settings iOS 27 1.0" }),
            c(null, "This app is an independent iOS-style front end for Android's settings and is not affiliated with Apple.",
                Lnk("Android Legal Information", null, null) { Sys.open(it, Settings.ACTION_DEVICE_INFO_SETTINGS) }))
        "reset" -> p("Transfer or Reset iPhone", G,
            c(null, "Prepare for a new device or erase this one.", Lnk("Erase All Content & Settings", null, null) { a ->
                IosDialog(a).title("Erase All Content & Settings?").message("This opens Android's reset screen. Nothing is erased by this app.")
                    .button("Cancel").button("Continue", 2) { Sys.open(a, Settings.ACTION_PRIVACY_SETTINGS) }.show()
            }))
        else -> null
    }
}
