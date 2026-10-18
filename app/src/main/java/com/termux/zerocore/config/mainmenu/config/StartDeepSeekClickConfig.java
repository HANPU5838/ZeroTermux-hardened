package com.termux.zerocore.config.mainmenu.config;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.example.xh_lib.utils.LogUtils;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.zerocore.dialog.SwitchDialog;

public class StartDeepSeekClickConfig extends BaseMenuClickConfig {

    private static final String TAG = "StartDeepSeek";

    public static final String START_COMMAND = "dsh web --no-open";

    @Override
    public Drawable getIcon(Context context) {
        try {
            Drawable drawable = context.getDrawable(R.drawable.deepseek_harness);
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
        return "启动 DeepSeek";
    }

    @Override
    public int getType() {
        return 0;
    }

    @Override
    public void onClick(View view, Context context) {
        if (!(context instanceof TermuxActivity)) {
            LogUtils.e(TAG, "context is not TermuxActivity, abort");
            return;
        }
        final TermuxActivity termuxActivity = (TermuxActivity) context;
        SwitchDialog switchDialog = new SwitchDialog(context);
        switchDialog.createSwitchDialog("是否启动 DeepSeek Web 服务？");
        if (switchDialog.getTitle() != null) {
            switchDialog.getTitle().setText("启动 DeepSeek");
        }
        if (switchDialog.getOther() != null) {
            switchDialog.getOther().setVisibility(View.GONE);
        }
        if (switchDialog.getOk() != null) {
            switchDialog.getOk().setText(context.getString(R.string.confirm));
            switchDialog.getOk().setOnClickListener(v -> {
                switchDialog.dismiss();
                termuxActivity.sendTextToTerminal(START_COMMAND + "\n");
            });
        }
        if (switchDialog.getCancel() != null) {
            switchDialog.getCancel().setText(context.getString(R.string.cancel));
        }
        switchDialog.show();
    }
}
