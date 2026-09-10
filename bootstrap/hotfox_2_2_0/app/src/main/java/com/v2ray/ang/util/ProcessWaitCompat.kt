package com.v2ray.ang.util

import android.os.Build
import java.util.concurrent.TimeUnit

/** API 24-safe Process.waitFor(timeout) — Process.waitFor(timeout, unit) is API 26+. */
object ProcessWaitCompat {
    fun waitFor(process: Process, timeout: Long, unit: TimeUnit): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return process.waitFor(timeout, unit)
        }
        val deadlineMs = System.currentTimeMillis() + unit.toMillis(timeout)
        while (System.currentTimeMillis() < deadlineMs) {
            try {
                process.exitValue()
                return true
            } catch (_: IllegalThreadStateException) {
                Thread.sleep(50L)
            }
        }
        return false
    }
}
