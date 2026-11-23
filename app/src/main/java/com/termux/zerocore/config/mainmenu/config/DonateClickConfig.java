package com.termux.zerocore.config.mainmenu.config;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.example.xh_lib.utils.LogUtils;
import com.termux.R;
import com.termux.zerocore.dialog.DonateDialog;

/**
 * 常用功能 -> 支持作者：打开赞赏码弹窗。
 * 从底部整宽卡片迁移为菜单项后，视觉与其它菜单保持同一套网格样式。
 */
public class DonateClickConfig extends BaseMenuClickConfig {

    private static final String TAG = "DonateClick";

    @Override
    public Drawable getIcon(Context context) {
        try {
            Drawable drawable = context.getDrawable(R.drawable.ic_donate_heart);
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
        return context.getString(R.string.donate_title);
    }

    @Override
    public void onClick(View view, Context context) {
        try {
            new DonateDialog(context).show();
        } catch (Exception e) {
            LogUtils.e(TAG, "open donate dialog failed: " + e);
        }
    }
}
