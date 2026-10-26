package com.example.floatlauncher

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/** Runs shell commands with adb-level rights through Shizuku. */
object Shell {
    fun running(): Boolean = try { Shizuku.pingBinder() } catch (_: Throwable) { false }

    fun ready(): Boolean = try {
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) { false }

    fun request() {
        try { Shizuku.requestPermission(100) } catch (_: Throwable) { }
    }

    /** Call off the main thread. */
    fun run(cmd: String): String {
        val m = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java, Array<String>::class.java, String::class.java
        )
        m.isAccessible = true
        val p = m.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
        val out = p.inputStream.bufferedReader().readText() + p.errorStream.bufferedReader().readText()
        p.waitFor()
        return out
    }
}
