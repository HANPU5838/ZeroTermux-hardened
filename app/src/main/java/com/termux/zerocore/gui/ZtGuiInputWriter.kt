package com.termux.zerocore.gui

import android.util.Log
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** 向 Termux 侧 input 守护进程写入鼠标/点击指令（xdotool）。 */
object ZtGuiInputWriter {

    private const val LOG_TAG = "ZtGuiInputWriter"
    private const val KEEP_ALIVE_SECONDS = 30L

    // corePoolSize=0 with allowCoreThreadTimeOut lets the single worker thread terminate after
    // KEEP_ALIVE_SECONDS of inactivity, so this process-wide singleton does not pin a thread for
    // the whole app lifetime.
    private val ioExecutor = ThreadPoolExecutor(
        0,
        1,
        KEEP_ALIVE_SECONDS,
        TimeUnit.SECONDS,
        LinkedBlockingQueue<Runnable>(),
        { runnable -> Thread(runnable, "ZtGuiInputWriter").apply { isDaemon = true } }
    ).apply { allowCoreThreadTimeOut(true) }

    fun sendClick(x: Int, y: Int) {
        enqueue("click:$x,$y")
    }

    fun sendMove(x: Int, y: Int) {
        enqueue("move:$x,$y")
    }

    /** Stops accepting new work and lets the idle worker thread exit. */
    @Synchronized
    fun shutdown() {
        ioExecutor.shutdown()
    }

    private fun enqueue(line: String) {
        try {
            ioExecutor.execute { writeCommand(line) }
        } catch (e: RejectedExecutionException) {
            Log.w(LOG_TAG, "input executor is shut down, dropping: $line", e)
        }
    }

    private fun writeCommand(line: String) {
        try {
            val dir = ZtGuiPaths.inputDir()
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, "${System.nanoTime()}.cmd")
            file.writeBytes((line + "\n").toByteArray(StandardCharsets.UTF_8))
        } catch (e: Exception) {
            Log.w(LOG_TAG, "input write failed: $line", e)
        }
    }
}
