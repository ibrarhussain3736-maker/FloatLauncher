package com.example.floatlauncher

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        root.addView(TextView(this).apply {
            text = "Float Launcher\n\n1. Grant overlay permission\n2. Start the bubble\n3. Tap the bubble, pick an app\n\n" +
                "For true floating windows, enable \"Enable freeform windows\" and " +
                "\"Force activities to be resizable\" in Developer options, then reboot."
            textSize = 16f
        })
        fun btn(label: String, action: () -> Unit) = root.addView(Button(this).apply {
            text = label
            setOnClickListener { action() }
        })

        btn("Grant overlay permission") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        btn("Start floating bubble") {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Grant overlay permission first", Toast.LENGTH_SHORT).show()
            } else {
                if (Build.VERSION.SDK_INT >= 33) {
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
                }
                startForegroundService(Intent(this, FloatingService::class.java))
            }
        }
        btn("Stop floating bubble") { stopService(Intent(this, FloatingService::class.java)) }
        btn("Grant Shizuku permission") {
            if (!Shell.running()) Toast.makeText(this, "Start Shizuku first", Toast.LENGTH_LONG).show()
            else Shell.request()
        }
        btn("Enable freeform (via Shizuku)") {
            if (!Shell.ready()) {
                Toast.makeText(this, "Grant Shizuku permission first", Toast.LENGTH_LONG).show()
            } else {
                Thread {
                    runCatching {
                        Shell.run("settings put global enable_freeform_support 1; settings put global force_resizable_activities 1")
                    }
                }.start()
                Toast.makeText(this, "Done. Reboot your phone.", Toast.LENGTH_LONG).show()
            }
        }
        btn("Open Developer options") {
            try { startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
            catch (e: Exception) { Toast.makeText(this, "Enable Developer options first", Toast.LENGTH_SHORT).show() }
        }
        setContentView(root)
    }
}
