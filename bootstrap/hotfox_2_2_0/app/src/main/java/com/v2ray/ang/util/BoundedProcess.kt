package com.v2ray.ang.util

import android.os.Build
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Collects process stdout concurrently so a timeout can fire even when the
 * child never closes its output stream.
 */
object BoundedProcess {
    data class Result(
        val finished: Boolean,
        val exitCode: Int?,
        val output: String,
    )

    fun collect(
        process: Process,
        timeout: Long,
        unit: TimeUnit,
        maxBytes: Int = 32_768,
    ): Result {
        val buffer = ByteArrayOutputStream()
        val reader = Thread({
            try {
                process.inputStream.use { input ->
                    val chunk = ByteArray(1024)
                    while (true) {
                        val n = input.read(chunk)
                        if (n < 0) break
                        val remaining = maxBytes - buffer.size()
                        if (remaining <= 0) continue
                        buffer.write(chunk, 0, minOf(n, remaining))
                    }
                }
            } catch (_: Exception) {
            }
        }, "hotfox-proc-out")
        reader.isDaemon = true
        reader.start()
        val finished = ProcessWaitCompat.waitFor(process, timeout, unit)
        if (!finished) {
            destroyQuietly(process)
            try {
                reader.join(250L)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            return Result(false, null, buffer.toString(Charsets.UTF_8.name()))
        }
        try {
            reader.join(500L)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        val code = try {
            process.exitValue()
        } catch (_: IllegalThreadStateException) {
            destroyQuietly(process)
            return Result(false, null, buffer.toString(Charsets.UTF_8.name()))
        }
        return Result(true, code, buffer.toString(Charsets.UTF_8.name()))
    }

    fun destroyQuietly(process: Process) {
        try {
            process.destroy()
        } catch (_: Exception) {
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                process.destroyForcibly()
            } catch (_: Exception) {
            }
        }
    }
}
