package com.termux.zerocore.config.mainmenu.config;

import static com.termux.zerocore.config.mainmenu.MainMenuConfig.CODE_ONLINE_FEATURES;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;

import com.example.xh_lib.utils.LogUtils;
import com.termux.R;

/**
 * 线上功能 -> 公共仓库：直接打开维护者的 GitHub 主页。
 */
public class PublicWarehouseClickConfig extends BaseMenuClickConfig {

    private static final String TAG = "PublicWarehouse";

    /** 公共仓库指向的 GitHub 主页。 */
    public static final String REPO_URL = "https://github.com/HANPU5838";

    @Override
    public int getType() {
        return CODE_ONLINE_FEATURES;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getDrawable(R.mipmap.gongongcangku);
    }

    @Override
    public String getString(Context context) {
        return context.getString(R.string.公共仓库);
    }

    @Override
    public void onClick(View view, Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            LogUtils.e(TAG, "open repo failed: " + e);
        }
    }
}
