package com.termux.zerocore.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;

import com.termux.R;

/**
 * 菜单底部「赞赏码」弹窗：白色圆角卡片 + 缩放淡入，点击卡片外或按钮平滑退出。
 */
public class DonateDialog extends Dialog {

    private View panel;
    private View root;

    public DonateDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_donate);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.72f);
            window.setGravity(Gravity.CENTER);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        root = findViewById(R.id.donate_root);
        panel = findViewById(R.id.donate_panel);

        // 不交给系统直接 dismiss，保证点外部与点按钮都是同一套退出动画
        setCanceledOnTouchOutside(false);
        root.setOnClickListener(v -> dismissSmoothly());
        panel.setOnClickListener(null);
        findViewById(R.id.donate_close).setOnClickListener(v -> dismissSmoothly());

        panel.setAlpha(0f);
        panel.setScaleX(0.86f);
        panel.setScaleY(0.86f);
        panel.setTranslationY(24f);
        panel.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(320)
            .setInterpolator(new DecelerateInterpolator(1.4f))
            .start();
    }

    private void dismissSmoothly() {
        if (panel == null) {
            dismiss();
            return;
        }
        panel.animate()
            .alpha(0f)
            .scaleX(0.94f)
            .scaleY(0.94f)
            .setDuration(180)
            .setInterpolator(new DecelerateInterpolator(1.2f))
            .withEndAction(this::dismiss)
            .start();
    }

    @Override
    public void onBackPressed() {
        dismissSmoothly();
    }
}
