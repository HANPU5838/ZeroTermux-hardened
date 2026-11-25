package com.termux.zerocore.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.xh_lib.utils.UUtils
import com.termux.R

public open class BaseTitleActivity: AppCompatActivity() {
    private var mTitle: TextView? = null
    private var mCancel: ImageView? = null
    private var mBaseFrame: FrameLayout? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        // 让页面背景一直画到状态栏/导航栏后面，避免顶部留一条纯黑的状态栏区域
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
    }

    override fun setContentView(layoutResID: Int) {
        val childView = LayoutInflater.from(this).inflate(layoutResID, null)
        val thisView = LayoutInflater.from(this).inflate(R.layout.activity_base_title, null)
        mTitle = thisView.findViewById<TextView>(R.id.title)
        mBaseFrame = thisView.findViewById<FrameLayout>(R.id.base_frame)
        mCancel = thisView.findViewById<ImageView>(R.id.cancel)
        mCancel!!.setOnClickListener { finish() }
        mBaseFrame!!.addView(childView)
        super.setContentView(thisView)
        ViewCompat.setOnApplyWindowInsetsListener(thisView) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(thisView)
    }

    public fun setBaseTitle(title: String) {
        mTitle!!.text = title
    }

    public fun goneCancelButton() {
        mCancel!!.visibility = View.GONE
    }
}
