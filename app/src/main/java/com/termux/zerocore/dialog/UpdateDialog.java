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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    /** 已下载完成的 APK：再次点击直接安装，不重新下载。 */
    private final Map<String, File> downloadedFiles = new HashMap<>();
    /** 每行的操作按钮，用于下载完成后把「下载」改成「安装」。 */
    private final Map<String, TextView> actionViews = new HashMap<>();

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
            if (r.available) {
                if (r.installed == null) {
                    version.setText(activity.getString(R.string.update_version_not_installed, r.version));
                    badge.setText(R.string.update_status_not_installed);
                    badge.setVisibility(View.VISIBLE);
                } else {
                    version.setText(activity.getString(R.string.update_version_line, r.installed, r.version));
                    badge.setText(r.hasUpdate
                        ? activity.getString(R.string.update_status_new)
                        : activity.getString(R.string.update_status_latest));
                    badge.setVisibility(View.VISIBLE);
                }
                if (r.hasUpdate) any = true;
            } else {
                version.setText(R.string.update_version_unknown);
                badge.setVisibility(View.GONE);
            }

            if (r.notes != null && !r.notes.trim().isEmpty() && "main".equals(r.key)) {
                notes.setText(r.notes.trim());
                notes.setVisibility(View.VISIBLE);
            }

            actionViews.put(r.key, action);
            File ready = (r.apkUrl == null || r.apkUrl.isEmpty()) ? null : existingApk(r);
            if (r.apkUrl == null || r.apkUrl.isEmpty()) {
                action.setText(R.string.update_action_open);
                action.setOnClickListener(v -> openUrl(r.repoUrl));
            } else if (ready != null) {
                // 之前已经下载过：直接进入「安装」，不再走网络
                downloadedFiles.put(r.key, ready);
                action.setText(R.string.update_action_install);
                action.setOnClickListener(v -> install(ready));
            } else {
                action.setText(r.hasUpdate
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

    /** 上次已下载完成的 APK：存在且非空时直接复用，避免重复下载。 */
    private File existingApk(AppUpdateManager.Release release) {
        try {
            if (release.apkName == null || release.apkName.isEmpty()) {
                return null;
            }
            File base = activity.getExternalFilesDir(null);
            if (base == null) {
                base = activity.getCacheDir();
            }
            File file = new File(new File(base, "update"), release.apkName);
            return (file.exists() && file.length() > 0) ? file : null;
        } catch (Exception e) {
            return null;
        }
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
                downloadedFiles.put(release.key, file);
                progressBar.setProgress(100);
                progressText.setText(R.string.update_download_done);
                hint.setText(R.string.update_hint_install_ready);
                // 下载完立刻换成「安装」：自动唤起失败时用户可手动再点，不会重新下载
                TextView action = actionViews.get(release.key);
                if (action != null) {
                    action.setText(R.string.update_action_install);
                    action.setOnClickListener(v -> install(file));
                }
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
