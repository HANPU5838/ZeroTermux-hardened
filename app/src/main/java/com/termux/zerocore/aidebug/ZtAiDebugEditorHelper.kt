package com.termux.zerocore.aidebug

import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.termux.shared.termux.TermuxConstants
import java.io.File

/** Opens a file in the built-in editor, optionally on the X11/GUI dock tab. */
object ZtAiDebugEditorHelper {

    private val gson = Gson()

    fun openEditorJson(context: Context, path: String, openX11Tab: Boolean = false, autoRun: Boolean = false): String {
        val trimmed = path.trim()
        if (trimmed.isEmpty()) {
            return gson.toJson(mapOf("ok" to false, "error" to "path required"))
        }
        val resolved = resolvePath(trimmed)
        return try {
            val intent = Intent().apply {
                setClassName(context.packageName, "com.termux.zerocore.activity.EditTextActivity")
                action = "com.termux.zerocore.activity.edittextactivity"
                putExtra("edit_path", resolved)
                putExtra(EXTRA_OPEN_X11_TAB, openX11Tab)
                putExtra(EXTRA_AUTO_RUN, autoRun)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            gson.toJson(
                mapOf(
                    "ok" to true,
                    "path" to resolved,
                    "open_x11_tab" to openX11Tab,
                    "auto_run" to autoRun,
                    "hint_for_ai" to "Editor opened. With open_x11_tab/auto_run the GUI dock runs build.sh automatically after rebuild."
                )
            )
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to (e.message ?: "start failed")))
        }
    }

    private fun resolvePath(path: String): String {
        if (path.startsWith("/")) return path
        val home = TermuxConstants.TERMUX_HOME_DIR_PATH
        return File(home, path).absolutePath
    }

    const val EXTRA_OPEN_X11_TAB = "zt_open_x11_tab"
    const val EXTRA_AUTO_RUN = "zt_auto_run"
}
