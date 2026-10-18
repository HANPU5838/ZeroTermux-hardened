package com.termux.zerocore.aidebug

import android.os.Looper
import com.termux.shared.shell.ShellUtils
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences
import com.termux.terminal.TerminalSession
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object ZtAiDebugTerminalHelper {

    fun snapshot(maxChars: Int = 12000): String {
        val session = currentSession() ?: return ""
        val text = ShellUtils.getTerminalSessionTranscriptText(session, false, true).orEmpty()
        if (text.length <= maxChars) return text
        return text.substring(text.length - maxChars)
    }

    fun sendCommand(command: String) {
        val session = currentSession() ?: return
        val normalized = if (command.endsWith("\n")) command else "$command\n"
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            session.write(normalized)
        }
    }

    fun execAndSnapshot(command: String, waitMs: Long): String {
        sendCommand(command)
        val wait = waitMs.coerceIn(200L, MAX_WAIT_MS)
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // Blocking the UI thread here would risk an ANR; the command was dispatched, so just
            // return the current view instead of sleeping.
            return snapshot()
        }
        // Bounded, interruptible wait instead of Thread.sleep so a shutdown/interrupt can cancel
        // the caller instead of pinning it for up to 30 seconds.
        try {
            CountDownLatch(1).await(wait, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        return snapshot()
    }

    private fun currentSession(): TerminalSession? {
        val service = ZtAiDebugManager.termuxService ?: return null
        val context = com.example.xh_lib.utils.UUtils.getContext()
        val handle = TermuxAppSharedPreferences.build(context, false)?.currentSession
        if (!handle.isNullOrEmpty()) {
            service.getTerminalSessionForHandle(handle)?.let { return it }
        }
        return service.lastTermuxSession?.terminalSession
    }

    private const val MAX_WAIT_MS = 30_000L
}
