package com.ios27.settings

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

// =============================================================== penyimpanan & pengikat ke Android
object A11yPrefs {
    private fun sp(c: Context) = c.getSharedPreferences("a11y", Context.MODE_PRIVATE)
    fun get(c: Context, key: String, def: Boolean) = sp(c).getBoolean(key, def)
    fun set(c: Context, key: String, v: Boolean) { sp(c).edit().putBoolean(key, v).apply() }
    fun getF(c: Context, key: String, def: Float) = sp(c).getFloat(key, def)
    fun setF(c: Context, key: String, v: Float) { sp(c).edit().putFloat(key, v).apply() }
    fun getS(c: Context, key: String, def: String) = sp(c).getString(key, def) ?: def
    fun setS(c: Context, key: String, v: String) { sp(c).edit().putString(key, v).apply() }
    fun apps(c: Context): Set<String> = HashSet(sp(c).getStringSet("apps", emptySet()) ?: emptySet())
    fun setApps(c: Context, s: Set<String>) { sp(c).edit().putStringSet("apps", HashSet(s)).apply() }
}

/** Penghubung sakelar ke setelan Android sungguhan: [read] membaca status, [write] mencoba mengubah, [open] layar cadangan. */
class Bind(val read: (Context) -> Boolean?, val write: ((Context, Boolean) -> Boolean)? = null, val open: String? = null)

object Binds {
    private val ACC = Settings.ACTION_ACCESSIBILITY_SETTINGS

    fun askWrite(c: Context) {
        try {
            c.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + c.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Toast.makeText(c, "Allow \"Modify system settings\" for this app, then try again.", Toast.LENGTH_LONG).show()
        } catch (_: Exception) { }
    }

    private fun secureRead(key: String): (Context) -> Boolean? =
        { c -> try { Settings.Secure.getInt(c.contentResolver, key, 0) == 1 } catch (_: Exception) { null } }

    private fun secureWrite(key: String): (Context, Boolean) -> Boolean =
        { c, v -> try { Settings.Secure.putInt(c.contentResolver, key, if (v) 1 else 0) } catch (_: Exception) { false } }

    private fun systemRead(key: String, def: Int): (Context) -> Boolean? =
        { c -> try { Settings.System.getInt(c.contentResolver, key, def) == 1 } catch (_: Exception) { null } }

    private fun systemWrite(key: String): (Context, Boolean) -> Boolean = { c, v ->
        if (!Settings.System.canWrite(c)) { askWrite(c); true }
        else try { Settings.System.putInt(c.contentResolver, key, if (v) 1 else 0) } catch (_: Exception) { false }
    }

    val invert = Bind(secureRead("accessibility_display_inversion_enabled"), secureWrite("accessibility_display_inversion_enabled"), ACC)
    val contrast = Bind(secureRead("high_text_contrast_enabled"), secureWrite("high_text_contrast_enabled"), ACC)
    val magnify = Bind(secureRead("accessibility_display_magnification_enabled"), secureWrite("accessibility_display_magnification_enabled"), ACC)
    val color = Bind(secureRead("accessibility_display_daltonizer_enabled"), secureWrite("accessibility_display_daltonizer_enabled"), ACC)
    val mono = Bind(systemRead("master_mono", 0), systemWrite("master_mono"), ACC)
    val haptic = Bind(systemRead("haptic_feedback_enabled", 1), systemWrite("haptic_feedback_enabled"), ACC)
    val autoBright = Bind(systemRead("screen_brightness_mode", 0), systemWrite("screen_brightness_mode"), Settings.ACTION_DISPLAY_SETTINGS)
    val bold = Bind({ c -> try { Settings.Secure.getInt(c.contentResolver, "font_weight_adjustment", 0) > 0 } catch (_: Exception) { null } }, null, Settings.ACTION_DISPLAY_SETTINGS)
    val lowPower = Bind({ c -> (c.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager)?.isPowerSaveMode }, null, Settings.ACTION_BATTERY_SAVER_SETTINGS)
    val dnd = Bind({ c -> (c.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager)?.currentInterruptionFilter?.let { it != android.app.NotificationManager.INTERRUPTION_FILTER_ALL } }, null, "android.settings.ZEN_MODE_SETTINGS")
    val bt = Bind({ c -> (c.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter?.isEnabled }, null, Settings.ACTION_BLUETOOTH_SETTINGS)
    val vpn = Bind({ c ->
        try {
            val cm = c.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            cm.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) ?: false
        } catch (_: Exception) { null }
    }, null, Settings.ACTION_VPN_SETTINGS)
    val time24 = Bind({ c -> android.text.format.DateFormat.is24HourFormat(c) }, null, Settings.ACTION_DATE_SETTINGS)
    val autoTime = Bind({ c -> try { Settings.Global.getInt(c.contentResolver, Settings.Global.AUTO_TIME, 1) == 1 } catch (_: Exception) { null } }, null, Settings.ACTION_DATE_SETTINGS)
    val voiceOver = Bind({ c -> (c.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager)?.isTouchExplorationEnabled }, null, ACC)
    val reduceMotion = Bind({ c ->
        try { Settings.Global.getFloat(c.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f } catch (_: Exception) { null }
    }, null, ACC)
}

// ============================================================================== model halaman
sealed class Item
class Tog(val title: String, val key: String, val def: Boolean = false, val bind: Bind? = null) : Item()
class Lnk(val title: String, val tile: Tile?, val value: ((MainActivity) -> String?)? = null, val action: (MainActivity) -> Unit) : Item()
class Sld(val key: String, val def: Float) : Item()
class Opt(val title: String, val key: String, val value: String, val def: Boolean = false) : Item()
class Inf(val title: String, val value: (MainActivity) -> String?) : Item()
class Bri : Item()

class Cards(val header: String?, val footer: String?, val items: List<Item>, val upper: Boolean = false)
class Header(val tile: Tile, val title: String, val desc: String, val linkText: String? = null, val linkUrl: String? = null)
class Page(val title: String, val back: String, val header: Header? = null, val blocks: List<Cards>)

// ==================================================================================== isi halaman
object A11y {
    private val blue = 0xFF0A84FF.toInt()
    private val green = 0xFF30D158.toInt()
    private val gray = 0xFF8E8E93.toInt()
    private val purple = 0xFF8E5CE6.toInt()
    private val red = 0xFFFF453A.toInt()
    private val dark = 0xFF3A3A3C.toInt()
    private val teal = 0xFF32ADE6.toInt()

    private fun acc(a: MainActivity) = Sys.open(a, Settings.ACTION_ACCESSIBILITY_SETTINGS)
    private fun onOff(b: Bind): (MainActivity) -> String? = { a -> if (b.read(a) == true) "On" else "Off" }
    private fun flagVal(key: String): (MainActivity) -> String? = { a -> if (A11yPrefs.get(a, key, false)) "On" else "Off" }
    private fun go(key: String): (MainActivity) -> Unit = { it.push(DetailScreen(it, key)) }
    private fun to(title: String, tile: Tile?, key: String, value: ((MainActivity) -> String?)? = null) = Lnk(title, tile, value, go(key))
    private fun tog(title: String, key: String, footer: String? = null, def: Boolean = false, bind: Bind? = null) =
        Cards(null, footer, listOf(Tog(title, key, def, bind)))
    private fun link(title: String, tile: Tile?, vararg actions: String) =
        Lnk(title, tile, null) { Sys.open(it, *actions) }
    private fun g(s: String, c: Int = gray) = Tile(c, glyph = s)

    fun page(key: String): Page = when (key) {
        "main" -> Page("", "Settings", Header(Tile(blue, glyph = "♿\uFE0E"), "Accessibility",
            "Personalize iPhone in ways that work best for you with accessibility features for vision, mobility, hearing, speech, and cognition.",
            "Learn more…", "https://support.google.com/accessibility/android"), listOf(
            Cards("Vision", null, listOf(
                to("VoiceOver", g("◉", blue), "voiceover", onOff(Binds.voiceOver)),
                to("Zoom", g("⌕", blue), "zoom", onOff(Binds.magnify)),
                to("Hover Text", g("Aa", blue), "hover", flagVal("hover")),
                to("Display & Text Size", g("AA", blue), "display"),
                to("Motion", g("◌", blue), "motion"),
                to("Read & Speak", g("▶\uFE0E", blue), "read"),
                to("Audio Descriptions", g("◐", blue), "audiodesc", flagVal("audiodesc"))
            )),
            Cards("Physical and Motor", null, listOf(
                to("Touch", g("☝", blue), "touch"),
                to("Face ID & Attention", g("☺", green), "faceid"),
                to("Switch Control", g("▦", dark), "switch", flagVal("switch")),
                to("Voice Control", g("◖", blue), "voice", flagVal("voice")),
                to("Eye Tracking", g("◉", purple), "eye", flagVal("eye")),
                to("Head Tracking", g("☻", green), "head", flagVal("head")),
                to("Side Button", g("▮", blue), "side"),
                to("Camera Control", g("◎", dark), "camera"),
                to("Apple Watch Mirroring", g("◍", blue), "watch"),
                to("Control Nearby Devices", g("▭", blue), "nearby")
            )),
            Cards("Hearing", null, listOf(
                to("Hearing Devices", g("♫", blue), "hearing"),
                to("Sound Recognition", g("♪", red), "sound", flagVal("sound")),
                to("Audio & Visual", g("◑", blue), "audiovisual"),
                link("Subtitles & Captioning", g("▭", blue), Settings.ACTION_CAPTIONING_SETTINGS),
                link("Live Captions", g("▤", blue), Settings.ACTION_CAPTIONING_SETTINGS)
            )),
            Cards("Speech", null, listOf(
                to("Live Speech", g("▤", dark), "livespeech", flagVal("livespeech")),
                to("Personal Voice", g("◖", purple), "personalvoice"),
                to("Vocal Shortcuts", g("◗", red), "vocal", flagVal("vocal"))
            )),
            Cards("Accessories", null, listOf(
                link("Keyboards & Typing", g("⌨\uFE0E", dark), Settings.ACTION_INPUT_METHOD_SETTINGS),
                to("Apple TV Remote", g("▯", dark), "tvremote"),
                to("AirPods", g("◡", dark), "airpods")
            )),
            Cards("General", null, listOf(
                to("Guided Access", g("▣", dark), "guided", flagVal("guided")),
                to("Assistive Access", g("▦", blue), "assistive"),
                link("Siri", g("◌", dark), Settings.ACTION_VOICE_INPUT_SETTINGS),
                to("Accessibility Shortcut", g("♿\uFE0E", blue), "shortcut", { "Ask" }),
                Lnk("Per-App Settings", g("▤", blue), null) { it.push(PerAppScreen(it)) }
            ))
        ))

        "display" -> Page("Display & Text Size", "Accessibility", null, listOf(
            tog("Bold Text", "bold", "Make text on the screen bolder. This is read from Android's Bold text setting.", false, Binds.bold),
            Cards(null, null, listOf(Lnk("Larger Text", null, { a -> "${(Settings.System.getFloat(a.contentResolver, Settings.System.FONT_SCALE, 1f) * 100).toInt()}%" }) { it.push(LargerTextScreen(it)) })),
            tog("Button Shapes", "buttonshapes"),
            tog("On/Off Labels", "onofflabels"),
            tog("Reduce Transparency", "reducetransp", "Improves contrast by reducing transparency and blur on some backgrounds."),
            tog("Increase Contrast", "contrast", "Increases color contrast between app foreground and background colors.", false, Binds.contrast),
            tog("Differentiate Without Color", "diffcolor", "Replaces user interface items that rely only on color to convey information with alternatives."),
            tog("Prefer Horizontal Text", "horiztext", "Prefer horizontal text in languages that support vertical text."),
            tog("Smart Invert", "smartinvert", "Smart Invert reverses the colors of the display, except for images, media and some apps that use dark color styles."),
            tog("Classic Invert", "classicinvert", "Classic Invert reverses the colors of the display.", false, Binds.invert),
            Cards(null, "Color filters can be used to differentiate colors by users who are color blind and aid users who have difficulty reading text on the display.",
                listOf(Lnk("Color Filters", null, onOff(Binds.color)) { Sys.open(it, "android.settings.COLOR_CORRECTION_SETTINGS", Settings.ACTION_ACCESSIBILITY_SETTINGS) })),
            tog("Reduce White Point", "whitepoint", "Reduces the intensity of bright colors."),
            tog("Auto-Brightness", "autobright", "Turning off auto-brightness may affect battery life and long-term display performance.", true, Binds.autoBright)
        ))

        "motion" -> Page("Motion", "Accessibility", null, listOf(
            tog("Reduce Motion", "reducemotion", "Reduce the motion of the user interface, including the parallax effect of icons.", false, Binds.reduceMotion),
            tog("Prefer Cross-Fade Transitions", "crossfade", "Turning on Reduce Motion also replaces some transitions with a cross-fade."),
            tog("Auto-Play Message Effects", "msgeffects", null, true),
            tog("Auto-Play Video Previews", "videoprev", null, true),
            tog("Limit Frame Rate", "limitfps", "Sets the maximum frame rate of the display to 60 frames per second.")
        ))

        "eye" -> Page("Eye Tracking", "Accessibility", null, listOf(
            tog("Eye Tracking", "eye", "Eye Tracking lets you control your device using just your eyes. The device should be on a stable surface, about one foot away from your face."),
            Cards("Smoothing", "Smooths pointer movement. Increasing this value may decrease pointer responsiveness.", listOf(Sld("eye_smooth", 0.5f))),
            Cards(null, "Automatically moves the pointer to nearby items.", listOf(Tog("Snap to Item", "eye_snap"), Tog("Zoom on Keyboard Keys", "eye_zoomkeys", true))),
            tog("Auto-Hide", "eye_autohide"),
            tog("Dwell Control", "eye_dwell", "With Dwell Control, keeping your gaze still for a set duration performs the selected dwell action.")
        ))

        "touch" -> Page("Touch", "Accessibility", null, listOf(
            Cards(null, null, listOf(Lnk("AssistiveTouch", null, flagVal("assistivetouch")) { Sys.open(it, Settings.ACTION_ACCESSIBILITY_SETTINGS) })),
            Cards(null, null, listOf(Lnk("Touch Accommodations", null, flagVal("touchaccom")) { Sys.open(it, Settings.ACTION_ACCESSIBILITY_SETTINGS) })),
            tog("Haptic Touch", "haptictouch", "Touch feedback uses Android's \"Touch feedback\" (haptic) setting.", true, Binds.haptic),
            tog("Tap to Wake", "taptowake", "Wake the screen when you tap on it.", true),
            tog("Shake to Undo", "shakeundo", "If you tend to shake your device by accident, you can turn off Shake to Undo to prevent the Undo alert from appearing.", true),
            tog("Vibration", "vibration", "When this switch is off, all vibration on your device will be disabled.", true, Binds.haptic)
        ))

        "zoom" -> Page("Zoom", "Accessibility", null, listOf(
            tog("Zoom", "zoom", "Zoom magnifies the entire screen. Android's Magnification is controlled by the system.", false, Binds.magnify),
            tog("Follow Focus", "zoom_follow", null, true),
            tog("Smart Typing", "zoom_typing", "Switches to Window Zoom while the keyboard is up.", true),
            tog("Keyboard Shortcuts", "zoom_keys", "Use keyboard shortcuts to control Zoom.")
        ))

        "voiceover" -> Page("VoiceOver", "Accessibility", null, listOf(
            tog("VoiceOver", "voiceover", "VoiceOver speaks items on the screen. Android's TalkBack provides this and is turned on in its own setting.", false, Binds.voiceOver),
            Cards(null, null, listOf(link("Speech", null, Settings.ACTION_ACCESSIBILITY_SETTINGS), link("Braille", null, Settings.ACTION_ACCESSIBILITY_SETTINGS))),
            tog("Verbosity", "vo_verbose", null, true),
            tog("Audio Ducking", "vo_duck", "Lowers the volume of other audio while VoiceOver speaks.", true)
        ))

        "hover" -> Page("Hover Text", "Accessibility", null, listOf(
            tog("Hover Text", "hover", "Show larger text in a bubble when you hover over text on the screen."),
            Cards(null, null, listOf(Lnk("Font", null, { "Helvetica" }) { Demo.dialog(it, "Font") }, Lnk("Size", null, { "Large" }) { Demo.dialog(it, "Size") }))
        ))

        "read" -> Page("Read & Speak", "Accessibility", null, listOf(
            tog("Speak Selection", "speaksel", "Select text and tap Speak to hear it read aloud.", true),
            tog("Speak Screen", "speakscreen", "Swipe down with two fingers from the top of the screen to hear its contents."),
            tog("Highlight Content", "highlight"),
            tog("Typing Feedback", "typingfeedback"),
            Cards(null, "Open Android's text-to-speech settings to choose the voice and speed.", listOf(link("Voices", null, "com.android.settings.TTS_SETTINGS", Settings.ACTION_ACCESSIBILITY_SETTINGS)))
        ))

        "audiodesc" -> Page("Audio Descriptions", "Accessibility", null, listOf(
            tog("Audio Descriptions", "audiodesc", "Hear a description of what is happening in video that supports audio descriptions.")
        ))

        "faceid" -> Page("Face ID & Attention", "Accessibility", null, listOf(
            tog("Require Attention for Face ID", "fid_attn", "Attention awareness provides an additional level of security by verifying that you are looking at the device.", true),
            tog("Attention Aware Features", "fid_aware", "Dims the display and lowers the volume of alerts when you are not looking at the device.", true)
        ))

        "switch" -> Page("Switch Control", "Accessibility", null, listOf(
            tog("Switch Control", "switch", "Switch Control lets you control your device using one or more switches. Android's Switch Access provides this."),
            Cards(null, null, listOf(link("Switches", null, Settings.ACTION_ACCESSIBILITY_SETTINGS), link("Recipes", null, Settings.ACTION_ACCESSIBILITY_SETTINGS)))
        ))

        "voice" -> Page("Voice Control", "Accessibility", null, listOf(
            tog("Voice Control", "voice", "Voice Control lets you control your device with your voice."),
            Cards(null, null, listOf(link("Customize Commands", null, Settings.ACTION_VOICE_INPUT_SETTINGS), link("Vocabulary", null, Settings.ACTION_VOICE_INPUT_SETTINGS)))
        ))

        "head" -> Page("Head Tracking", "Accessibility", null, listOf(
            tog("Head Tracking", "head", "Control the pointer by moving your head."),
            Cards("Sensitivity", null, listOf(Sld("head_sens", 0.5f)))
        ))

        "side" -> Page("Side Button", "Accessibility", null, listOf(
            Cards("Click Speed", null, listOf(Lnk("Default", null, null) { Demo.dialog(it, "Click Speed") })),
            tog("Rest Finger to Unlock", "side_rest", "When Face ID is not available, rest your finger on the button to unlock.", true)
        ))

        "camera" -> Page("Camera Control", "Accessibility", null, listOf(
            tog("Camera Control", "camctl", "Use the dedicated Camera Control button to open the camera.", true),
            tog("Light Press", "camlight", null, true)
        ))

        "watch" -> Page("Apple Watch Mirroring", "Accessibility", null, listOf(
            tog("Apple Watch Mirroring", "watch", "Control your paired watch from this device.")
        ))

        "nearby" -> Page("Control Nearby Devices", "Accessibility", null, listOf(
            Cards(null, "Control other devices nearby from this device.", listOf(Lnk("Devices", null, null) { Sys.open(it, Settings.ACTION_BLUETOOTH_SETTINGS) }))
        ))

        "hearing" -> Page("Hearing Devices", "Accessibility", null, listOf(
            Cards(null, "Pair and manage hearing aids in Android's Bluetooth settings.", listOf(Lnk("Hearing Devices", null, null) { Sys.open(it, Settings.ACTION_BLUETOOTH_SETTINGS) })),
            tog("Mono Audio", "mono", "Makes the left and right channels play the same content.", false, Binds.mono),
            tog("Phone Noise Cancellation", "noisecancel", "Noise cancellation reduces ambient noise on phone calls when you are holding the receiver to your ear.", true),
            tog("Headphone Notifications", "headphonenotif", "To protect your hearing, you receive a notification if you have been listening to loud headphone audio for long enough.", true)
        ))

        "sound" -> Page("Sound Recognition", "Accessibility", null, listOf(
            tog("Sound Recognition", "sound", "Your device continuously listens for certain sounds and notifies you when they are recognized.")
        ))

        "audiovisual" -> Page("Audio & Visual", "Accessibility", null, listOf(
            tog("Mono Audio", "mono2", "Makes the left and right channels play the same content.", false, Binds.mono),
            tog("LED Flash for Alerts", "ledflash", "Flash the LED when you receive notifications."),
            tog("Background Sounds", "bgsounds", "Play calming sounds to minimize distractions.")
        ))

        "livespeech" -> Page("Live Speech", "Accessibility", null, listOf(
            tog("Live Speech", "livespeech", "Type to speak out loud on calls and in person.")
        ))

        "personalvoice" -> Page("Personal Voice", "Accessibility", null, listOf(
            Cards(null, "Create a voice that sounds like you for use with Live Speech.", listOf(Lnk("Create a Personal Voice", null, null) { Demo.dialog(it, "Personal Voice") }))
        ))

        "vocal" -> Page("Vocal Shortcuts", "Accessibility", null, listOf(
            tog("Vocal Shortcuts", "vocal", "Perform actions and complete tasks with custom sounds.")
        ))

        "tvremote" -> Page("Apple TV Remote", "Accessibility", null, listOf(
            tog("Touch Surface Navigation", "tv_touch", null, true)
        ))

        "airpods" -> Page("AirPods", "Accessibility", null, listOf(
            Cards("Press Speed", null, listOf(Lnk("Default", null, null) { Demo.dialog(it, "Press Speed") })),
            tog("Noise Cancellation with One AirPod", "ap_nc", "Allows noise cancellation to be used when only one AirPod is in your ear.")
        ))

        "guided" -> Page("Guided Access", "Accessibility", null, listOf(
            tog("Guided Access", "guided", "Guided Access keeps the device in a single app. Android's Screen pinning does this."),
            Cards(null, null, listOf(Lnk("Passcode Settings", null, null) { Sys.open(it, Settings.ACTION_SECURITY_SETTINGS) }))
        ))

        "assistive" -> Page("Assistive Access", "Accessibility", null, listOf(
            tog("Assistive Access", "assistive", "A simplified experience with larger buttons and fewer choices, designed to help with focus.")
        ))

        "shortcut" -> Page("Accessibility Shortcut", "Accessibility", null, listOf(
            Cards(null, "Triple-click the side button to quickly turn accessibility features on or off.", listOf(
                Tog("Classic Invert", "sc_invert", false, Binds.invert), Tog("Color Filters", "sc_color", false, Binds.color),
                Tog("Zoom", "sc_zoom", false, Binds.magnify), Tog("VoiceOver", "sc_vo", false, Binds.voiceOver)
            ))
        ))

        else -> Page(key, "Accessibility", null, listOf(
            Cards(null, "This setting is available in Android's accessibility settings.",
                listOf(Lnk("Open Android Accessibility Settings", null, null) { Sys.open(it, Settings.ACTION_ACCESSIBILITY_SETTINGS) }))
        ))
    }
}

// ================================================================================== tampilan halaman
private fun footerView(act: MainActivity, text: String) = TextView(act).apply {
    this.text = text; textSize = 13f; setTextColor(Theme.sub); setPadding(act.dp(32f), act.dp(6f), act.dp(32f), act.dp(4f))
}

private fun toggleRow(act: MainActivity, t: Tog, refreshers: MutableList<() -> Unit>): View {
    val rp = act.makeRow(null, t.title, chevron = false, clickable = false)
    val sw = IosSwitch(act)
    fun state(): Boolean = t.bind?.read?.invoke(act) ?: A11yPrefs.get(act, t.key, t.def)
    sw.checked = state()
    sw.onToggle = {
        val b = t.bind
        if (b != null) {
            val ok = b.write?.invoke(act, !state()) ?: false
            if (!ok) {
                Toast.makeText(act, "Android lets only the system change this. Opening its setting…", Toast.LENGTH_SHORT).show()
                Sys.open(act, b.open ?: Settings.ACTION_ACCESSIBILITY_SETTINGS)
            }
            sw.checked = state()
        } else {
            val v = !sw.checked
            A11yPrefs.set(act, t.key, v); sw.checked = v
        }
    }
    rp.row.addView(sw, LinearLayout.LayoutParams(act.dp(51f), act.dp(31f)).apply { leftMargin = act.dp(8f) })
    refreshers.add { sw.checked = state() }
    return rp.row
}

private fun sliderRow(act: MainActivity, s: Sld): View {
    val bar = SeekBar(act).apply {
        max = 100; progress = (A11yPrefs.getF(act, s.key, s.def) * 100).toInt()
        progressTintList = ColorStateList.valueOf(Theme.blue); thumbTintList = ColorStateList.valueOf(Color.WHITE)
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) { if (fromUser) A11yPrefs.setF(act, s.key, p / 100f) }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }
    val box = LinearLayout(act).apply { setPadding(act.dp(12f), act.dp(10f), act.dp(12f), act.dp(10f)) }
    box.addView(bar, LinearLayout.LayoutParams(-1, -2))
    return box
}

private fun briRow(act: MainActivity): View {
    val cur = try { Settings.System.getInt(act.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) } catch (_: Exception) { 128 }
    val bar = SeekBar(act).apply {
        max = 255; progress = cur
        progressTintList = ColorStateList.valueOf(Theme.blue); thumbTintList = ColorStateList.valueOf(Color.WHITE)
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                if (Settings.System.canWrite(act)) {
                    try { Settings.System.putInt(act.contentResolver, Settings.System.SCREEN_BRIGHTNESS, maxOf(10, sb?.progress ?: 128)) } catch (_: Exception) { }
                } else Binds.askWrite(act)
            }
        })
    }
    return LinearLayout(act).apply { setPadding(act.dp(12f), act.dp(10f), act.dp(12f), act.dp(10f)); addView(bar, LinearLayout.LayoutParams(-1, -2)) }
}

fun renderCards(act: MainActivity, parent: LinearLayout, blocks: List<Cards>, refreshers: MutableList<() -> Unit>) {
    for (b in blocks) {
        if (b.header != null) parent.addView(if (b.upper) act.smallLabel(b.header.uppercase()) else act.sectionLabel(b.header))
        val card = act.card()
        b.items.forEachIndexed { i, item ->
            val v: View = when (item) {
                is Tog -> toggleRow(act, item, refreshers)
                is Sld -> sliderRow(act, item)
                is Inf -> { val rp = act.makeRow(null, item.title, chevron = false, clickable = false); rp.value.text = item.value(act) ?: ""; rp.row }
                is Bri -> briRow(act)
                is Opt -> {
                    val rp = act.makeRow(null, item.title, chevron = false, clickable = true)
                    val ck = TextView(act).apply { text = "✓"; textSize = 20f; setTextColor(Theme.blue); typeface = Typeface.DEFAULT_BOLD }
                    rp.row.addView(ck)
                    fun sel() = A11yPrefs.getS(act, item.key, if (item.def) item.value else "") == item.value
                    ck.visibility = if (sel()) View.VISIBLE else View.INVISIBLE
                    refreshers.add { ck.visibility = if (sel()) View.VISIBLE else View.INVISIBLE }
                    rp.row.setOnClickListener {
                        A11yPrefs.setS(act, item.key, item.value)
                        if (item.key == "appearance") act.recreate() else refreshers.forEach { r -> r() }
                    }
                    rp.row
                }
                is Lnk -> {
                    val rp = act.makeRow(item.tile, item.title, chevron = true, clickable = true)
                    rp.row.setOnClickListener { item.action(act) }
                    val vf = item.value
                    if (vf != null) { rp.value.text = vf(act) ?: ""; refreshers.add { rp.value.text = vf(act) ?: "" } }
                    rp.row
                }
            }
            card.addView(v)
            if (i < b.items.size - 1) card.addView(act.sepView(if (item is Lnk && item.tile != null) act.dp(60f) else act.dp(16f)))
        }
        parent.addView(card)
        if (b.footer != null) parent.addView(footerView(act, b.footer))
    }
}

class DetailScreen(act: MainActivity, private val key: String) : Screen(act) {
    private val refreshers = ArrayList<() -> Unit>()

    override fun build(): View {
        val page = Pg.page(key) ?: A11y.page(key)
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, act.dp(40f)) }
        col.addView(act.navBar2(page.back, page.title))
        val h = page.header
        if (h != null) {
            val c = act.card()
            val top = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(act.dp(16f), act.dp(16f), act.dp(16f), act.dp(14f)) }
            top.addView(TileView(act, h.tile, 60f))
            top.addView(TextView(act).apply { text = h.title; textSize = 28f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Theme.text); setPadding(0, act.dp(8f), 0, act.dp(4f)) })
            top.addView(TextView(act).apply { text = h.desc; textSize = 15f; setTextColor(Theme.sub) })
            if (h.linkText != null) top.addView(TextView(act).apply {
                text = h.linkText; textSize = 15f; setTextColor(Theme.blue); setPadding(0, act.dp(4f), 0, 0)
                setOnClickListener { try { act.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(h.linkUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { } }
            })
            c.addView(top); col.addView(c)
            col.addView(footerView(act, "Switches linked to Android show the real state; the others are saved only in this app."))
        }
        renderCards(act, col, page.blocks, refreshers)
        if (key == "main") ServicesSection.render(act, col, refreshers)
        return ScrollView(act).apply { addView(col); isVerticalScrollBarEnabled = false }
    }

    override fun onShow() { refreshers.forEach { it() } }
}

// ============================================================================= Larger Text (nyata)
class LargerTextScreen(act: MainActivity) : Screen(act) {
    private val scales = floatArrayOf(0.85f, 1.0f, 1.15f, 1.3f, 1.5f)
    private lateinit var holder: LinearLayout
    private lateinit var sample: TextView

    override fun build(): View {
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, act.dp(40f)) }
        col.addView(act.navBar2("Display & Text Size", "Larger Text"))
        holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        col.addView(holder)
        render()
        return ScrollView(act).apply { addView(col); isVerticalScrollBarEnabled = false }
    }

    private fun current(): Float = try { Settings.System.getFloat(act.contentResolver, Settings.System.FONT_SCALE, 1f) } catch (_: Exception) { 1f }

    private fun render() {
        if (!::holder.isInitialized) return
        holder.removeAllViews()
        if (!Settings.System.canWrite(act)) {
            holder.addView(act.infoCard("To change the text size of the whole phone, Android needs you to allow \"Modify system settings\" for this app.", "Allow") {
                try { act.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + act.packageName))) } catch (_: Exception) { }
            })
        }
        val c = act.card()
        val box = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(act.dp(16f), act.dp(16f), act.dp(16f), act.dp(14f)) }
        sample = TextView(act).apply { text = "Aa  The quick brown fox jumps over the lazy dog."; setTextColor(Theme.text); textSize = 17f * current() }
        box.addView(sample)
        val idx = scales.indices.minByOrNull { Math.abs(scales[it] - current()) } ?: 1
        val bar = SeekBar(act).apply {
            max = scales.size - 1; progress = idx
            progressTintList = ColorStateList.valueOf(Theme.blue); thumbTintList = ColorStateList.valueOf(Color.WHITE)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) { if (fromUser) sample.textSize = 17f * scales[p] }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {
                    val v = scales[sb?.progress ?: 1]
                    try { if (Settings.System.canWrite(act)) Settings.System.putFloat(act.contentResolver, Settings.System.FONT_SCALE, v) } catch (_: Exception) { }
                }
            })
        }
        box.addView(bar, LinearLayout.LayoutParams(-1, -2).apply { topMargin = act.dp(14f) })
        val labels = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
        labels.addView(TextView(act).apply { text = "A"; textSize = 13f; setTextColor(Theme.sub) }, LinearLayout.LayoutParams(0, -2, 1f))
        labels.addView(TextView(act).apply { text = "A"; textSize = 24f; setTextColor(Theme.sub) })
        box.addView(labels)
        c.addView(box); holder.addView(c)
        holder.addView(footerView(act, "Drag the slider to choose your preferred text size. It changes Android's system font size."))
    }

    override fun onShow() { render() }
}

// =============================================================================== Per-App Settings
class PerAppScreen(act: MainActivity) : Screen(act) {
    private lateinit var holder: LinearLayout

    override fun build(): View {
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, act.dp(40f)) }
        col.addView(act.navBar2("Accessibility", "Per-App Settings"))
        holder = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        col.addView(holder)
        render()
        return ScrollView(act).apply { addView(col); isVerticalScrollBarEnabled = false }
    }

    private fun appRow(pkg: String): View? {
        val pm = act.packageManager
        val info = try { pm.getApplicationInfo(pkg, 0) } catch (_: Exception) { return null }
        val row = act.makeRow(null, pm.getApplicationLabel(info).toString(), chevron = true, clickable = true)
        val icon = ImageView(act).apply { setImageDrawable(pm.getApplicationIcon(info)) }
        row.row.addView(icon, 0, LinearLayout.LayoutParams(act.dp(30f), act.dp(30f)).apply { rightMargin = act.dp(14f) })
        row.row.setOnClickListener { act.push(AppDetailScreen(act, pkg)) }
        return row.row
    }

    private fun render() {
        if (!::holder.isInitialized) return
        holder.removeAllViews()
        val top = act.card()
        val add = act.makeRow(null, "Add App", chevron = false, clickable = true)
        (add.row.getChildAt(0) as TextView).setTextColor(Theme.blue)
        add.row.setOnClickListener { act.push(AppPickerScreen(act)) }
        top.addView(add.row)
        holder.addView(top)
        val apps = A11yPrefs.apps(act).toList().sorted()
        if (apps.isNotEmpty()) {
            holder.addView(act.smallLabel("APP CUSTOMIZATION"))
            val c = act.card()
            val rows = apps.mapNotNull { appRow(it) }
            rows.forEachIndexed { i, r -> c.addView(r); if (i < rows.size - 1) c.addView(act.sepView(act.dp(60f))) }
            holder.addView(c)
        } else holder.addView(footerView(act, "Choose an app to customize its accessibility settings separately."))
    }

    override fun onShow() { render() }
}

class AppPickerScreen(act: MainActivity) : Screen(act) {
    private var all: List<Pair<String, String>> = emptyList()
    private var shown: List<Pair<String, String>> = emptyList()
    private lateinit var adapter: BaseAdapter

    /** Daftar memakai ListView: hanya baris yang terlihat yang memuat ikon, jadi hemat memori. */
    private fun newRow(): LinearLayout {
        val row = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(act.dp(16f), act.dp(8f), act.dp(14f), act.dp(8f)); setBackgroundColor(Theme.card); foreground = act.ripple()
        }
        row.addView(ImageView(act), LinearLayout.LayoutParams(act.dp(34f), act.dp(34f)).apply { rightMargin = act.dp(14f) })
        row.addView(TextView(act).apply { textSize = 17f; setTextColor(Theme.text); maxLines = 1 }, LinearLayout.LayoutParams(0, -2, 1f))
        return row
    }

    override fun build(): View {
        val pm = act.packageManager
        val launch = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        all = launch.map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }.filter { it.first != act.packageName }.sortedBy { it.second.lowercase() }
        shown = all
        val root = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        root.addView(act.navBar2("Per-App Settings", "Choose App to Customize"))
        val search = EditText(act).apply {
            hint = "Search"; setHintTextColor(Theme.sub); setTextColor(Theme.text); textSize = 17f; isSingleLine = true
            background = GradientDrawable().apply { setColor(if (Theme.dark) 0xFF1C1C1E.toInt() else 0xFFE5E5EA.toInt()); cornerRadius = act.dpf(12f) }
            setPadding(act.dp(14f), act.dp(10f), act.dp(14f), act.dp(10f))
        }
        root.addView(search, LinearLayout.LayoutParams(-1, -2).apply { setMargins(act.dp(16f), act.dp(8f), act.dp(16f), act.dp(8f)) })
        adapter = object : BaseAdapter() {
            override fun getCount() = shown.size
            override fun getItem(i: Int) = shown[i]
            override fun getItemId(i: Int) = i.toLong()
            override fun getView(i: Int, cv: View?, parent: ViewGroup?): View {
                val row = (cv as? LinearLayout) ?: newRow()
                val (pkg, label) = shown[i]
                (row.getChildAt(0) as ImageView).setImageDrawable(try { act.packageManager.getApplicationIcon(pkg) } catch (_: Exception) { null })
                (row.getChildAt(1) as TextView).text = label
                row.setOnClickListener {
                    val s = A11yPrefs.apps(act).toMutableSet(); s.add(pkg); A11yPrefs.setApps(act, s)
                    act.pop()
                }
                return row
            }
        }
        val lv = ListView(act).apply {
            divider = ColorDrawable(Theme.sep); dividerHeight = 1; isVerticalScrollBarEnabled = false
            this.adapter = this@AppPickerScreen.adapter
        }
        root.addView(lv, LinearLayout.LayoutParams(-1, 0, 1f).apply { setMargins(act.dp(16f), 0, act.dp(16f), act.dp(16f)) })
        lv.background = GradientDrawable().apply { setColor(Theme.card); cornerRadius = act.dpf(20f) }
        lv.clipToOutline = true
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(e: Editable?) {
                val q = e?.toString()?.trim() ?: ""
                shown = if (q.isEmpty()) all else all.filter { it.second.contains(q, ignoreCase = true) }
                adapter.notifyDataSetChanged()
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        return root
    }
}

class AppDetailScreen(act: MainActivity, private val pkg: String) : Screen(act) {
    private val refreshers = ArrayList<() -> Unit>()

    override fun build(): View {
        val pm = act.packageManager
        val label = try { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() } catch (_: Exception) { pkg }
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, act.dp(40f)) }
        col.addView(act.navBar2("Per-App Settings", label))
        fun t(title: String, k: String, footer: String? = null) = Cards(null, footer, listOf(Tog(title, "app_${pkg}_$k", false)))
        renderCards(act, col, listOf(
            t("Bold Text", "bold"), t("Larger Text", "larger"), t("Increase Contrast", "contrast"),
            t("Reduce Motion", "motion", "These options are saved for this app in Settings iOS 27 only. Android does not apply accessibility options per app."),
            Cards(null, null, listOf(Lnk("App Info (Android)", null, null) {
                try { it.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { }
            }))
        ), refreshers)
        val rm = act.card()
        rm.addView(TextView(act).apply {
            text = "Remove App"; textSize = 17f; setTextColor(0xFFFF453A.toInt()); gravity = Gravity.CENTER; setPadding(0, act.dp(15f), 0, act.dp(15f))
            setOnClickListener { val s = A11yPrefs.apps(act).toMutableSet(); s.remove(pkg); A11yPrefs.setApps(act, s); act.pop() }
        })
        col.addView(rm)
        return ScrollView(act).apply { addView(col); isVerticalScrollBarEnabled = false }
    }

    override fun onShow() { refreshers.forEach { it() } }
}

// ============================================================ aplikasi aksesibilitas yang terpasang di HP
/** Layanan aksesibilitas Android yang terdeteksi (misalnya aplikasi layar kunci yang meminta izin aksesibilitas). */
object Services {
    private fun am(ctx: Context) = ctx.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager

    fun installed(ctx: Context): List<AccessibilityServiceInfo> =
        try { am(ctx)?.installedAccessibilityServiceList ?: emptyList() } catch (_: Exception) { emptyList() }

    fun enabledIds(ctx: Context): Set<String> =
        try { am(ctx)?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)?.map { it.id }?.toSet() ?: emptySet() }
        catch (_: Exception) { emptySet() }

    fun isSystem(info: AccessibilityServiceInfo): Boolean =
        (info.resolveInfo.serviceInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

    fun label(ctx: Context, info: AccessibilityServiceInfo): String = info.resolveInfo.loadLabel(ctx.packageManager).toString()

    /** Mengubah layanan langsung hanya mungkin bila aplikasi diberi WRITE_SECURE_SETTINGS (lewat adb/Shizuku). */
    fun setEnabled(ctx: Context, info: AccessibilityServiceInfo, on: Boolean): Boolean = try {
        val cr = ctx.contentResolver
        val cur = (Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: "").split(":").filter { it.isNotBlank() }
        val target = ComponentName.unflattenFromString(info.id)
        val list = cur.filter { ComponentName.unflattenFromString(it) != target }.toMutableList()
        if (on) list.add(info.id)
        Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, list.joinToString(":"))
        Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, if (list.isEmpty()) 0 else 1)
        true
    } catch (_: Exception) { false }

    fun openSettings(act: Activity, info: AccessibilityServiceInfo) {
        val tries = listOf(
            Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").putExtra("android.intent.extra.COMPONENT_NAME", info.id),
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        )
        for (i in tries) { try { act.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return } catch (_: Exception) { } }
    }
}

object ServicesSection {
    fun render(act: MainActivity, parent: LinearLayout, refreshers: MutableList<() -> Unit>) {
        val all = Services.installed(act)
        if (all.isEmpty()) return
        val pm = act.packageManager
        val groups = listOf(
            "Downloaded Apps" to all.filter { !Services.isSystem(it) },
            "Built-in Services" to all.filter { Services.isSystem(it) }
        )
        for ((title, list) in groups) {
            if (list.isEmpty()) continue
            parent.addView(act.sectionLabel(title))
            val card = act.card()
            val sorted = list.sortedBy { Services.label(act, it).lowercase() }
            sorted.forEachIndexed { i, info ->
                val rp = act.makeRow(null, Services.label(act, info), chevron = true, clickable = true)
                try {
                    rp.row.addView(ImageView(act).apply { setImageDrawable(info.resolveInfo.loadIcon(pm)) }, 0,
                        LinearLayout.LayoutParams(act.dp(30f), act.dp(30f)).apply { rightMargin = act.dp(14f) })
                } catch (_: Exception) { }
                rp.row.setOnClickListener { act.push(ServiceScreen(act, info.id)) }
                val upd = { rp.value.text = if (Services.enabledIds(act).contains(info.id)) "On" else "Off" }
                upd(); refreshers.add { upd() }
                card.addView(rp.row)
                if (i < sorted.size - 1) card.addView(act.sepView(act.dp(60f)))
            }
            parent.addView(card)
        }
        parent.addView(footerView(act, "Apps listed here are accessibility services installed on this phone. Android requires you to turn them on in its own setting."))
    }
}

class ServiceScreen(act: MainActivity, private val id: String) : Screen(act) {
    private lateinit var sw: IosSwitch
    private var info: AccessibilityServiceInfo? = null

    override fun build(): View {
        info = Services.installed(act).firstOrNull { it.id == id }
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, act.dp(40f)) }
        col.addView(act.navBar2("Accessibility", ""))
        val i = info
        if (i == null) {
            col.addView(act.infoCard("This accessibility service is no longer installed.", null, null))
            return ScrollView(act).apply { addView(col) }
        }
        val pm = act.packageManager
        val label = Services.label(act, i)
        val c = act.card()
        val top = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(act.dp(16f), act.dp(16f), act.dp(16f), act.dp(14f)) }
        try { top.addView(ImageView(act).apply { setImageDrawable(i.resolveInfo.loadIcon(pm)) }, LinearLayout.LayoutParams(act.dp(60f), act.dp(60f))) } catch (_: Exception) { }
        top.addView(TextView(act).apply { text = label; textSize = 28f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Theme.text); setPadding(0, act.dp(8f), 0, act.dp(4f)) })
        val desc = try { i.loadDescription(pm) } catch (_: Exception) { null }
        top.addView(TextView(act).apply {
            text = if (desc.isNullOrBlank()) "$label is an accessibility service installed on this phone." else desc
            textSize = 15f; setTextColor(Theme.sub)
        })
        c.addView(top); c.addView(act.sepView(act.dp(16f)))
        val toggleRow = act.makeRow(null, "Use $label", chevron = false, clickable = false)
        sw = IosSwitch(act)
        sw.checked = Services.enabledIds(act).contains(id)
        sw.onToggle = {
            val ok = Services.setEnabled(act, i, !sw.checked)
            if (!ok) {
                Toast.makeText(act, "Android requires you to turn this on in its own accessibility screen. Opening it…", Toast.LENGTH_LONG).show()
                Services.openSettings(act, i)
            }
            sw.checked = Services.enabledIds(act).contains(id)
        }
        toggleRow.row.addView(sw, LinearLayout.LayoutParams(act.dp(51f), act.dp(31f)).apply { leftMargin = act.dp(8f) })
        c.addView(toggleRow.row)
        col.addView(c)

        val caps = ArrayList<String>()
        val cp = i.capabilities
        if (cp and AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT != 0) caps.add("Observe your actions and read screen content")
        if (cp and AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES != 0) caps.add("Perform gestures such as taps and swipes")
        if (cp and AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS != 0) caps.add("Observe the keys you type")
        if (cp and AccessibilityServiceInfo.CAPABILITY_CAN_CONTROL_MAGNIFICATION != 0) caps.add("Control display magnification")
        if (cp and AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_TOUCH_EXPLORATION != 0) caps.add("Take over touch exploration")
        if (caps.isNotEmpty()) {
            col.addView(act.smallLabel("THIS APP CAN"))
            val pc = act.card()
            caps.forEachIndexed { n, t ->
                pc.addView(act.makeRow(null, t, chevron = false, clickable = false).row)
                if (n < caps.size - 1) pc.addView(act.sepView(act.dp(16f)))
            }
            col.addView(pc)
        }
        val links = act.card()
        val open = act.makeRow(null, "Open in Android Accessibility Settings", chevron = true, clickable = true)
        open.row.setOnClickListener { Services.openSettings(act, i) }
        links.addView(open.row); links.addView(act.sepView(act.dp(16f)))
        val appInfo = act.makeRow(null, "App Info", chevron = true, clickable = true)
        appInfo.row.setOnClickListener {
            try { act.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + i.resolveInfo.serviceInfo.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { }
        }
        links.addView(appInfo.row)
        col.addView(links)
        col.addView(footerView(act, "Accessibility services can read what is on your screen. Turn on only apps you trust."))
        return ScrollView(act).apply { addView(col); isVerticalScrollBarEnabled = false }
    }

    override fun onShow() { if (::sw.isInitialized) sw.checked = Services.enabledIds(act).contains(id) }
}
