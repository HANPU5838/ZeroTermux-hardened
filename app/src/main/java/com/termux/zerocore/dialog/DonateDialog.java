package com.termux.zerocore.dialog;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import com.termux.R;

import java.io.OutputStream;

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
        findViewById(R.id.donate_save).setOnClickListener(v -> saveQrToGallery());

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

    /** 把赞赏码保存到系统相册：Pictures/ZeroTermux/。 */
    private void saveQrToGallery() {
        ContentResolver resolver = getContext().getContentResolver();
        Uri uri = null;
        try {
            Bitmap bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.donate_qr);
            if (bitmap == null) {
                toast(R.string.donate_save_fail);
                return;
            }
            String fileName = "ZeroTermux-donate-" + System.currentTimeMillis() + ".png";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/ZeroTermux");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                toast(R.string.donate_save_fail);
                return;
            }
            try (OutputStream out = resolver.openOutputStream(uri)) {
                if (out == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    resolver.delete(uri, null, null);
                    toast(R.string.donate_save_fail);
                    return;
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues done = new ContentValues();
                done.put(MediaStore.Images.Media.IS_PENDING, 0);
                resolver.update(uri, done, null, null);
            }
            toast(R.string.donate_save_ok);
        } catch (Exception e) {
            if (uri != null) {
                try {
                    resolver.delete(uri, null, null);
                } catch (Exception ignored) {
                }
            }
            toast(R.string.donate_save_fail);
        }
    }

    private void toast(int resId) {
        Toast.makeText(getContext(), resId, Toast.LENGTH_SHORT).show();
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
