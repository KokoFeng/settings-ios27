package com.ios27.settings

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout

/** Navigasi sederhana: tumpukan layar; tombol kembali sistem = kembali satu layar. */
abstract class Screen(val act: MainActivity) {
    private var cached: View? = null
    fun view(): View = cached ?: build().also { cached = it }
    abstract fun build(): View
    open fun onShow() {}
    open fun onHide() {}
    open fun onPermission() {}
    open fun onResult(req: Int, res: Int, data: android.content.Intent?) {}
    open fun onKeyboard(h: Int) {}
}

class MainActivity : Activity() {
    private val stack = ArrayList<Screen>()
    private lateinit var root: FrameLayout
    private var kbH = 0

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val sysDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        Theme.dark = when (A11yPrefs.getS(this, "appearance", "auto")) { "dark" -> true; "light" -> false; else -> sysDark }
        window.setBackgroundDrawable(ColorDrawable(Theme.bg))
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Theme.bg
        var flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        if (!Theme.dark) {
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            if (Build.VERSION.SDK_INT >= 26) flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        window.decorView.systemUiVisibility = flags
        root = FrameLayout(this).apply { setBackgroundColor(Theme.bg) }
        setContentView(root)
        // Mode layar penuh membuat adjustResize tidak bekerja; hitung tinggi keyboard sendiri agar bilah cari naik.
        val decor = window.decorView
        decor.viewTreeObserver.addOnGlobalLayoutListener {
            val r = android.graphics.Rect(); decor.getWindowVisibleDisplayFrame(r)
            val loc = IntArray(2); root.getLocationOnScreen(loc)
            val overlap = maxOf(0, loc[1] + root.height - r.bottom)
            val kb = if (overlap > dp(120f)) overlap else 0
            if (kb != kbH) { kbH = kb; stack.lastOrNull()?.onKeyboard(kb) }
        }
        push(HomeScreen(this))
    }

    fun push(s: Screen) {
        stack.lastOrNull()?.onHide()
        stack.add(s)
        show(s)
    }

    fun pop() {
        if (stack.size > 1) {
            stack.removeAt(stack.size - 1).onHide()
            show(stack.last())
        } else finish()
    }

    private fun show(s: Screen) {
        root.removeAllViews()
        root.addView(s.view(), FrameLayout.LayoutParams(-1, -1))
        s.onShow()
        s.onKeyboard(kbH)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { pop() }

    override fun onResume() { super.onResume(); stack.lastOrNull()?.onShow() }
    override fun onPause() { super.onPause(); stack.lastOrNull()?.onHide() }

    fun refreshTop() { stack.lastOrNull()?.onShow() }

    /** Pilih foto (galeri/berkas); hasil dikirim ke layar teratas lewat onResult. */
    fun pickPhoto() {
        startActivityForResult(android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(android.content.Intent.CATEGORY_OPENABLE); type = "image/*"
        }, 31)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        stack.lastOrNull()?.onResult(requestCode, resultCode, data)
    }

    // ---- izin Wi-Fi
    fun wifiPerms(): Array<String> =
        if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasWifiPerms(): Boolean = wifiPerms().all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    fun askWifiPerms() { requestPermissions(wifiPerms(), 100) }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        stack.lastOrNull()?.onPermission()
    }
}
