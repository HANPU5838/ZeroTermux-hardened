package com.termux.zerocore.settings

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.cardview.widget.CardView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.config.ZTConstantConfig

class ZTAboutActivity : BaseTitleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ztabout)
        setBaseTitle(UUtils.getString(R.string.zt_about))
        // 本加固版仓库：源码、Release、联系方式都在这里
        findViewById<CardView>(R.id.zt_termux_github_card_view).setOnClickListener {
            UUtils.startUrl(ZTConstantConfig.URL.ZT_REPO_URL)
        }

        // 上游与原作者声明：保留归属说明，链接同样指向本仓库 README
        findViewById<CardView>(R.id.zt_core_manage_termux_github_card_view).setOnClickListener {
            UUtils.startUrl("${ZTConstantConfig.URL.ZT_REPO_URL}#readme")
        }
    }
}
