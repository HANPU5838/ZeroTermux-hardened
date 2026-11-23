package com.termux.zerocore.config.mainmenu.config;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.example.xh_lib.utils.LogUtils;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.zerocore.dialog.SshConnectDialog;

/**
 * 常用功能 -> 连接服务器：填写地址/端口/用户名/私钥后，在终端里直接 ssh 登录远程主机。
 */
public class ServerConnectClickConfig extends BaseMenuClickConfig {

    private static final String TAG = "ServerConnect";

    @Override
    public Drawable getIcon(Context context) {
        try {
            Drawable drawable = context.getDrawable(R.drawable.ic_menu_ssh);
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
        return context.getString(R.string.server_connect);
    }

    @Override
    public void onClick(View view, Context context) {
        if (!(context instanceof TermuxActivity)) {
            return;
        }
        new SshConnectDialog((TermuxActivity) context).show();
    }
}
