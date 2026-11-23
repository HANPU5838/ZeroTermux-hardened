package com.termux.zerocore.config.mainmenu.config;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import com.example.xh_lib.utils.LogUtils;
import com.termux.R;

/**
 * 常用功能 -> 官网：点击后调用系统默认浏览器打开 hanpuhan.cn
 */
public class OfficialWebsiteClickConfig extends BaseMenuClickConfig {

    private static final String TAG = "OfficialWebsite";

    public static final String WEBSITE_URL = "https://hanpuhan.cn";

    @Override
    public Drawable getIcon(Context context) {
        try {
            Drawable drawable = context.getDrawable(R.drawable.ic_menu_website);
            if (drawable != null) {
                return drawable;
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "load icon failed: " + e);
        }
        return context.getDrawable(R.mipmap.ic_launcher);
    }

    @Override
    public String getString(Context context) {
        return context.getString(R.string.official_website);
    }

    @Override
    public void onClick(View view, Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(WEBSITE_URL));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            LogUtils.e(TAG, "open website failed: " + e);
            Toast.makeText(context, R.string.official_website_open_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
