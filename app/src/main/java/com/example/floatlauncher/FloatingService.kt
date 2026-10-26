package com.example.floatlauncher

import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.abs

class FloatingService : Service() {

    private lateinit var wm: WindowManager
    private var bubble: TextView? = null
    private var panel: LinearLayout? = null
    private val dp get() = resources.displayMetrics.density

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        addBubble()
    }

    private fun startAsForeground() {
        val channel = "float_launcher"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(channel, "Float Launcher", NotificationManager.IMPORTANCE_LOW)
        )
        val n = Notification.Builder(this, channel)
            .setContentTitle("Float Launcher running")
            .setContentText("Tap the bubble to open apps")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    private fun overlayParams(flags: Int, w: Int, h: Int) = WindowManager.LayoutParams(
        w, h,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        flags,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private fun addBubble() {
        val size = (56 * dp).toInt()
        val b = TextView(this).apply {
            text = "▣"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#CC3F51B5"))
            }
        }
        val lp = overlayParams(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, size, size
        ).apply { x = (16 * dp).toInt(); y = (200 * dp).toInt() }

        var startX = 0; var startY = 0; var touchX = 0f; var touchY = 0f; var moved = false
        val longPress = Runnable { stopSelf() }
        b.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x; startY = lp.y; touchX = e.rawX; touchY = e.rawY; moved = false
                    v.postDelayed(longPress, 1500)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - touchX).toInt(); val dy = (e.rawY - touchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) { moved = true; v.removeCallbacks(longPress) }
                    lp.x = startX + dx; lp.y = startY + dy
                    wm.updateViewLayout(v, lp)
                }
                MotionEvent.ACTION_UP -> {
                    v.removeCallbacks(longPress)
                    if (!moved) togglePanel()
                }
            }
            true
        }
        wm.addView(b, lp)
        bubble = b
    }

    private fun togglePanel() {
        if (panel != null) { hidePanel(); return }
        val dm = resources.displayMetrics
        val w = (dm.widthPixels * 0.75).toInt()
        val h = (dm.heightPixels * 0.55).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F2FFFFFF"))
                cornerRadius = 16 * dp
                setStroke((1 * dp).toInt(), Color.LTGRAY)
            }
        }
        root.addView(TextView(this).apply {
            text = "Launch in floating window    ✕"
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (12 * dp).toInt())
            setOnClickListener { hidePanel() }
        })

        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val pm = packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(launcherIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
            .forEach { ri ->
                val icon = ri.loadIcon(pm).apply {
                    val s = (36 * dp).toInt(); setBounds(0, 0, s, s)
                }
                list.addView(TextView(this).apply {
                    text = ri.loadLabel(pm)
                    textSize = 15f
                    setTextColor(Color.BLACK)
                    gravity = Gravity.CENTER_VERTICAL
                    compoundDrawablePadding = (12 * dp).toInt()
                    setCompoundDrawables(icon, null, null, null)
                    setPadding((16 * dp).toInt(), (8 * dp).toInt(), (16 * dp).toInt(), (8 * dp).toInt())
                    setOnClickListener { launchFloating(ri.activityInfo.packageName) }
                })
            }
        root.addView(ScrollView(this).apply { addView(list) })

        val lp = overlayParams(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, w, h
        ).apply { x = (dm.widthPixels - w) / 2; y = (dm.heightPixels - h) / 2 }
        wm.addView(root, lp)
        panel = root
    }

    private fun hidePanel() {
        panel?.let { runCatching { wm.removeView(it) } }
        panel = null
    }

    private fun launchFloating(pkg: String) {
        val intent = packageManager.getLaunchIntentForPackage(pkg) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        val dm = resources.displayMetrics
        val w = (dm.widthPixels * 0.8).toInt()
        val h = (dm.heightPixels * 0.5).toInt()
        val l = (dm.widthPixels - w) / 2
        val t = (dm.heightPixels - h) / 2

        val opts = ActivityOptions.makeBasic().setLaunchBounds(Rect(l, t, l + w, t + h))
        // Hidden API: 5 = WINDOWING_MODE_FREEFORM. Ignored if the device blocks it.
        try {
            ActivityOptions::class.java
                .getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
                .invoke(opts, 5)
        } catch (_: Throwable) { }

        hidePanel()
        startActivity(intent, opts.toBundle())
    }

    override fun onDestroy() {
        hidePanel()
        bubble?.let { runCatching { wm.removeView(it) } }
        bubble = null
        super.onDestroy()
    }
}
