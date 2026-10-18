package com.termux.zerocore.gui

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ZtGuiFramePoller(
    private val onFrame: (width: Int, height: Int, rgb: ByteArray) -> Unit,
    private val onActivity: (() -> Unit)? = null
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var decodeExecutor: ExecutorService? = null
    private val decodeBusy = AtomicBoolean(false)
    private var polling = false
    private var lastModified = 0L
    private var pollToken = 0

    fun start(intervalMs: Long = POLL_MS) {
        stop()
        // A fresh single-thread executor per polling session; stop() shuts it down so repeated
        // start/stop cycles (tab show/hide, activity pause/resume) do not accumulate threads.
        decodeExecutor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "ZtGuiFrameDecode").apply { isDaemon = true }
        }
        polling = true
        pollToken++
        schedule(pollToken, intervalMs)
    }

    fun stop() {
        polling = false
        pollToken++
        mainHandler.removeCallbacksAndMessages(null)
        decodeBusy.set(false)
        // shutdown() lets an in-flight decode finish; its main-thread callback is ignored because
        // polling is false and the token no longer matches.
        decodeExecutor?.shutdown()
        decodeExecutor = null
    }

    fun hasRecentFrame(maxAgeMs: Long = 2500L): Boolean {
        val file = ZtGuiPaths.frameFile()
        if (!file.isFile || file.length() <= 0) return false
        return System.currentTimeMillis() - file.lastModified() <= maxAgeMs
    }

    private fun schedule(token: Int, intervalMs: Long) {
        mainHandler.postDelayed({
            if (!polling || token != pollToken) return@postDelayed
            pollOnce()
            schedule(token, intervalMs)
        }, intervalMs)
    }

    private fun pollOnce() {
        if (!polling) return
        val file = ZtGuiPaths.frameFile()
        if (!file.isFile || file.length() <= 0) return
        val modified = file.lastModified()
        if (modified == lastModified || !decodeBusy.compareAndSet(false, true)) return
        lastModified = modified
        val executor = decodeExecutor
        if (executor == null) {
            decodeBusy.set(false)
            return
        }
        val token = pollToken
        val path = file.absolutePath
        executor.execute {
            try {
                val bytes = File(path).readBytes()
                val frame = if (bytes.isNotEmpty()) ZtGuiFrameDecoder.decode(bytes) else null
                if (frame == null) {
                    // Release the in-flight flag on the main thread even when there is nothing to
                    // decode, otherwise decodeBusy stays true and polling silently stops forever.
                    mainHandler.post { decodeBusy.set(false) }
                    return@execute
                }
                frame.bitmap?.recycle()
                mainHandler.post {
                    decodeBusy.set(false)
                    if (!polling || token != pollToken) return@post
                    onActivity?.invoke()
                    onFrame(frame.width, frame.height, frame.rgb)
                }
            } catch (e: Exception) {
                Log.w(LOG_TAG, "decode frame failed", e)
                mainHandler.post { decodeBusy.set(false) }
            }
        }
    }

    companion object {
        private const val LOG_TAG = "ZtGuiFramePoller"
        private const val POLL_MS = 50L
    }
}
