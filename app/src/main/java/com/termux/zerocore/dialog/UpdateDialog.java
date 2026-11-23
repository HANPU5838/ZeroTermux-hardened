package com.termux.zerocore.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.xh_lib.utils.UUtils;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.termux.R;
import com.termux.zerocore.update.AppUpdateManager;

import java.io.File;
import java.util.List;

/**
 * 检查更新弹窗：三个正式版仓库的版本一览 + 一键下载安装。
 */
public class UpdateDialog extends Dialog {

    private final Activity activity;
    private LinearLayout list;
    private LinearLayout progressArea;
    private ProgressBar progressBar;
    private TextView progressText;
    private TextView hint;
    private View panel;
    private boolean downloading = false;

    public UpdateDialog(Activity activity) {
        super(activity);
        this.activity = activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_app_update);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.72f);
            window.setGravity(Gravity.CENTER);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        View root = findViewById(R.id.update_root);
        panel = findViewById(R.id.update_panel);
        list = findViewById(R.id.update_list);
        progressArea = findViewById(R.id.update_progress_area);
        progressBar = findViewById(R.id.update_progress);
        progressText = findViewById(R.id.update_progress_text);
        hint = findViewById(R.id.update_hint);

        setCanceledOnTouchOutside(false);
        root.setOnClickListener(v -> dismissSmoothly());
        panel.setOnClickListener(null);
        findViewById(R.id.update_close).setOnClickListener(v -> dismissSmoothly());

        panel.setAlpha(0f);
        panel.setScaleX(0.88f);
        panel.setScaleY(0.88f);
        panel.setTranslationY(20f);
        panel.animate()
            .alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
            .setDuration(300)
            .setInterpolator(new DecelerateInterpolator(1.4f))
            .start();

        hint.setText(R.string.update_loading);
        AppUpdateManager.fetchAll(activity, new AppUpdateManager.FetchCallback() {
            @Override
            public void onResult(List<AppUpdateManager.Release> releases) {
                render(releases);
            }

            @Override
            public void onError(String message) {
                hint.setText(R.string.update_fetch_fail);
                renderFallback();
            }
        });
    }

    private void render(List<AppUpdateManager.Release> releases) {
        list.removeAllViews();
        boolean any = false;
        for (AppUpdateManager.Release r : releases) {
            View row = LayoutInflater.from(activity).inflate(R.layout.item_update_row, list, false);
            TextView name = row.findViewById(R.id.row_name);
            TextView badge = row.findViewById(R.id.row_badge);
            TextView version = row.findViewById(R.id.row_version);
            TextView notes = row.findViewById(R.id.row_notes);
            TextView action = row.findViewById(R.id.row_action);

            name.setText(r.name);
            boolean isMain = "main".equals(r.key);
            if (r.available) {
                if (r.installed == null) {
                    version.setText(activity.getString(R.string.update_version_not_installed, r.version));
                    badge.setText(R.string.update_status_not_installed);
                    badge.setVisibility(View.VISIBLE);
                } else {
                    version.setText(activity.getString(R.string.update_version_line, r.installed, r.version));
                    if (isMain) {
                        badge.setText(r.hasUpdate
                            ? activity.getString(R.string.update_status_new)
                            : activity.getString(R.string.update_status_latest));
                        badge.setVisibility(View.VISIBLE);
                    } else {
                        // 配套组件版本号体系与主程序不同，不做「是否最新」判断
                        badge.setVisibility(View.GONE);
                    }
                }
                if (isMain && r.hasUpdate) any = true;
            } else {
                version.setText(R.string.update_version_unknown);
                badge.setVisibility(View.GONE);
            }

            if (r.notes != null && !r.notes.trim().isEmpty() && "main".equals(r.key)) {
                notes.setText(r.notes.trim());
                notes.setVisibility(View.VISIBLE);
            }

            if (r.apkUrl == null || r.apkUrl.isEmpty()) {
                action.setText(R.string.update_action_open);
                action.setOnClickListener(v -> openUrl(r.repoUrl));
            } else {
                action.setText(("main".equals(r.key) && r.hasUpdate)
                    ? activity.getString(R.string.update_action_update)
                    : activity.getString(R.string.update_action_download));
                action.setOnClickListener(v -> startDownload(r));
            }
            list.addView(row);
        }
        hint.setText(any ? R.string.update_hint_has_new : R.string.update_hint_latest);
    }

    private void renderFallback() {
        list.removeAllViews();
        View row = LayoutInflater.from(activity).inflate(R.layout.item_update_row, list, false);
        ((TextView) row.findViewById(R.id.row_name)).setText(R.string.update_dialog_title);
        ((TextView) row.findViewById(R.id.row_version)).setText(R.string.update_fetch_fail);
        TextView action = row.findViewById(R.id.row_action);
        action.setText(R.string.update_action_open);
        action.setOnClickListener(v -> openUrl("https://github.com/HANPU5838/ZeroTermux-hardened/releases"));
        list.addView(row);
    }

    private void startDownload(final AppUpdateManager.Release release) {
        if (downloading) return;
        downloading = true;
        progressArea.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        progressText.setText(activity.getString(R.string.update_downloading, 0));
        hint.setText(R.string.update_hint_downloading);

        AppUpdateManager.download(activity, release, new AppUpdateManager.DownloadCallback() {
            @Override
            public void onProgress(int percent, long downloaded, long total) {
                progressBar.setProgress(percent);
                progressText.setText(activity.getString(R.string.update_downloading, percent));
            }

            @Override
            public void onSuccess(File file) {
                downloading = false;
                progressBar.setProgress(100);
                progressText.setText(R.string.update_download_done);
                hint.setText(R.string.update_hint_install);
                install(file);
            }

            @Override
            public void onError(String message) {
                downloading = false;
                progressArea.setVisibility(View.GONE);
                hint.setText(activity.getString(R.string.update_download_fail, message));
                openUrl(release.repoUrl + "/releases");
            }
        });
    }

    private void install(final File file) {
        XXPermissions.with(activity)
            .permission(Permission.REQUEST_INSTALL_PACKAGES)
            .request(new OnPermissionCallback() {
                @Override
                public void onGranted(List<String> permissions, boolean all) {
                    if (all) {
                        try {
                            UUtils.installApk(UUtils.getContext(), file.getAbsolutePath());
                        } catch (Exception e) {
                            hint.setText(R.string.update_install_fail);
                        }
                    } else {
                        hint.setText(R.string.update_install_permission);
                    }
                }

                @Override
                public void onDenied(List<String> permissions, boolean never) {
                    hint.setText(R.string.update_install_permission);
                    if (never) {
                        XXPermissions.startPermissionActivity(activity, permissions);
                    }
                }
            });
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    private void dismissSmoothly() {
        if (panel == null) { dismiss(); return; }
        panel.animate()
            .alpha(0f).scaleX(0.94f).scaleY(0.94f)
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
