package com.termux.zerocore.dialog

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import com.termux.R

class LoadingDialog : BaseDialogCentre {
    public var msg: TextView? = null
    private var isStart: Boolean = false
    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)
    private var isTime: Int = 300

    private val handler = Handler(Looper.getMainLooper())

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!isStart) return
            if (isTime > -1) {
                isTime--
                handler.postDelayed(this, TICK_MS)
                return
            }
            isStart = false
            try {
                if (isShowing()) {
                    dismiss()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun initViewDialog(mView: View?) {
        msg = mView?.findViewById(R.id.msg)
    }

    override fun getContentView(): Int {
        return R.layout.dialog_loading
    }

    override fun show() {
        super.show()
        setCancelable(false)
        // Start the countdown only once the dialog is shown. initViewDialog() is invoked from the
        // base class constructor, before this class's fields (handler/tickRunnable) are initialized.
        setAnim()
    }

    override fun dismiss() {
        // Always stop the countdown, even when the dialog is dismissed before it is shown.
        // Otherwise the posted runnable keeps rescheduling itself forever and leaks.
        isStart = false
        handler.removeCallbacks(tickRunnable)
        try {
            super.dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //动画
    private fun setAnim() {
        if (isStart) {
            return
        }
        isStart = true
        isTime = 300
        handler.postDelayed(tickRunnable, TICK_MS)
    }

    companion object {
        private const val TICK_MS = 100L
    }
}
