package com.termux.zerocore.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.termux.TermuxConstants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 常用功能 -> SSH：填写地址/端口/用户名/私钥（可粘贴或导入），直接新建一个 ssh 会话，
 * 不在终端里敲命令。最近连接会记住，点一下就能再次连上。
 */
public class SshConnectDialog extends Dialog {

    public static final int REQ_PICK_KEY = 0x5A11;
    private static final int MAX_RECENT = 6;

    private final TermuxActivity activity;
    private EditText hostInput;
    private EditText portInput;
    private EditText userInput;
    private EditText keyInput;
    private TextView keyPathText;

    private Uri pickedKeyUri;

    public SshConnectDialog(TermuxActivity activity) {
        super(activity);
        this.activity = activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_ssh_connect);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.72f);
            window.setGravity(Gravity.CENTER);
            int screenH = activity.getResources().getDisplayMetrics().heightPixels;
            int fixedH = Math.min(dp(560), (int) (screenH * 0.86f));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, fixedH);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        hostInput = findViewById(R.id.ssh_host);
        portInput = findViewById(R.id.ssh_port);
        userInput = findViewById(R.id.ssh_user);
        keyInput = findViewById(R.id.ssh_key);
        keyPathText = findViewById(R.id.ssh_key_path);

        findViewById(R.id.ssh_root).setOnClickListener(v -> dismiss());
        findViewById(R.id.ssh_cancel).setOnClickListener(v -> dismiss());
        findViewById(R.id.ssh_pick_key).setOnClickListener(v -> pickKeyFile());
        findViewById(R.id.ssh_connect).setOnClickListener(v -> connect());
        findViewById(R.id.ssh_tab_new).setOnClickListener(v -> switchTab(0));
        findViewById(R.id.ssh_tab_recent).setOnClickListener(v -> switchTab(1));
        findViewById(R.id.ssh_tab_help).setOnClickListener(v -> switchTab(2));
        findViewById(R.id.ssh_recent_clear).setOnClickListener(v -> {
            com.termux.zerocore.utils.SshStore.clearAll();
            renderRecent();
        });
        switchTab(0);
        renderRecent();

        findViewById(R.id.ssh_panel).setAlpha(0f);
        findViewById(R.id.ssh_panel).animate().alpha(1f).setDuration(220)
            .setInterpolator(new DecelerateInterpolator(1.4f)).start();
    }

    private void switchTab(int index) {
        View[] panes = {findViewById(R.id.ssh_pane_new), findViewById(R.id.ssh_pane_recent),
            findViewById(R.id.ssh_pane_help)};
        TextView[] tabs = {findViewById(R.id.ssh_tab_new), findViewById(R.id.ssh_tab_recent),
            findViewById(R.id.ssh_tab_help)};
        for (int i = 0; i < panes.length; i++) {
            boolean selected = i == index;
            panes[i].setVisibility(selected ? View.VISIBLE : View.GONE);
            tabs[i].setTextColor(selected ? Color.parseColor("#FF4D8DFF")
                : Color.parseColor("#FF8FA0B8"));
        }
    }

    private void renderRecent() {
        LinearLayout area = findViewById(R.id.ssh_recent_area);
        TextView empty = findViewById(R.id.ssh_recent_empty);
        TextView pathText = findViewById(R.id.ssh_recent_path);
        area.removeAllViews();
        pathText.setText(getContext().getString(R.string.ssh_recent_path,
            com.termux.zerocore.utils.SshStore.getDir().getAbsolutePath()));
        java.util.List<com.termux.zerocore.utils.SshStore.Entry> entries =
            com.termux.zerocore.utils.SshStore.list();
        if (entries.size() > MAX_RECENT) {
            entries = entries.subList(0, MAX_RECENT);
        }
        empty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
        for (com.termux.zerocore.utils.SshStore.Entry item : entries) {
            String host = item.host;
            String user = item.user;
            int port = item.port;
            String keyPath = item.key;

            TextView row = new TextView(activity);
            row.setText(user + "@" + host + "  ·  " + port);
            row.setTextColor(Color.parseColor("#FFD6E2F5"));
            row.setTextSize(13f);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            row.setBackgroundResource(R.drawable.bg_ssh_pick);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = dp(6);
            row.setLayoutParams(params);
            row.setOnClickListener(v -> {
                hostInput.setText(host);
                userInput.setText(user);
                portInput.setText(String.valueOf(port));
                if (!TextUtils.isEmpty(keyPath)) {
                    keyPathText.setText(keyPath);
                }
                connect();
            });
            row.setOnLongClickListener(v -> {
                com.termux.zerocore.utils.SshStore.delete(item);
                renderRecent();
                toast(R.string.ssh_recent_deleted);
                return true;
            });
            area.addView(row);
        }
    }

    private void pickKeyFile() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            activity.pendingSshConnectDialog = this;
            activity.startActivityForResult(intent, REQ_PICK_KEY);
        } catch (Exception e) {
            toast(R.string.ssh_pick_fail);
        }
    }

    /** 由 TermuxActivity.onActivityResult 转发回来。 */
    public void onKeyFilePicked(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        pickedKeyUri = data.getData();
        String name = pickedKeyUri.getLastPathSegment();
        keyPathText.setText(name == null ? pickedKeyUri.toString() : name);
    }

    private void connect() {
        String host = hostInput.getText().toString().trim();
        String user = userInput.getText().toString().trim();
        String keyText = keyInput.getText().toString().trim();
        int port = 22;
        try {
            String portText = portInput.getText().toString().trim();
            if (!TextUtils.isEmpty(portText)) {
                port = Integer.parseInt(portText);
            }
        } catch (Exception e) {
            port = 22;
        }
        if (TextUtils.isEmpty(host) || TextUtils.isEmpty(user)) {
            toast(R.string.ssh_missing);
            return;
        }
        if (port <= 0 || port > 65535) {
            port = 22;
        }

        File keyFile = null;
        try {
            if (!TextUtils.isEmpty(keyText) || pickedKeyUri != null) {
                File sshDir = new File(TermuxConstants.TERMUX_HOME_DIR, ".ssh");
                if (!sshDir.exists() && !sshDir.mkdirs()) {
                    toast(R.string.ssh_key_save_fail);
                    return;
                }
                String safeHost = host.replaceAll("[^a-zA-Z0-9_.\\-]", "_");
                keyFile = new File(sshDir, "zt_server_" + safeHost + ".key");
                if (!TextUtils.isEmpty(keyText)) {
                    try (OutputStream out = new FileOutputStream(keyFile)) {
                        out.write(keyText.getBytes("UTF-8"));
                        out.write('\n');
                    }
                } else {
                    try (InputStream in = activity.getContentResolver().openInputStream(pickedKeyUri);
                         OutputStream out = new FileOutputStream(keyFile)) {
                        if (in == null) {
                            toast(R.string.ssh_key_save_fail);
                            return;
                        }
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                    }
                }
                // 私钥权限必须是 600，否则 ssh 会拒绝使用
                //noinspection ResultOfMethodCallIgnored
                keyFile.setReadable(false, false);
                //noinspection ResultOfMethodCallIgnored
                keyFile.setReadable(true, true);
                //noinspection ResultOfMethodCallIgnored
                keyFile.setWritable(false, false);
                //noinspection ResultOfMethodCallIgnored
                keyFile.setWritable(true, true);
            }
        } catch (Exception e) {
            toast(R.string.ssh_key_save_fail);
            return;
        }

        String keyPath = keyFile == null ? "" : keyFile.getAbsolutePath();
        com.termux.zerocore.utils.SshStore.save(host, user, port, keyPath);
        boolean started = activity.startSshSession(host, port, user, keyPath);
        if (!started) {
            toast(R.string.ssh_start_fail);
            return;
        }
        dismiss();
    }

    private int dp(int value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density);
    }

    private void toast(int resId) {
        Toast.makeText(getContext(), resId, Toast.LENGTH_SHORT).show();
    }
}
