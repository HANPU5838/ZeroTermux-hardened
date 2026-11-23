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
    /** 从最近连接带入的私钥路径（用户没重新选文件时沿用它）。 */
    private String pendingKeyPath = "";

    public SshConnectDialog(TermuxActivity activity) {
        super(activity);
        this.activity = activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_ssh_connect);
        com.termux.zerocore.utils.SshStore.attach(activity);
        com.termux.zerocore.utils.SshStore.migrateIfNeeded();

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.72f);
            window.setGravity(Gravity.CENTER);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        android.view.View panel = findViewById(R.id.ssh_panel);
        android.view.ViewGroup.LayoutParams panelParams = panel.getLayoutParams();
        int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
        panelParams.height = Math.min(dp(520), (int) (screenHeight * 0.86f));
        panel.setLayoutParams(panelParams);

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
            tabs[i].setTextColor(selected ? Color.parseColor("#FF7FB2FF")
                : Color.parseColor("#FF8FA0B8"));
            tabs[i].setTypeface(selected ? android.graphics.Typeface.DEFAULT_BOLD
                : android.graphics.Typeface.DEFAULT);
            tabs[i].setBackgroundResource(selected ? R.drawable.bg_ssh_tab_selected : 0);
        }
    }

    private void renderRecent() {
        LinearLayout area = findViewById(R.id.ssh_recent_area);
        TextView empty = findViewById(R.id.ssh_recent_empty);
        area.removeAllViews();
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

            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.bg_ssh_pick);
            row.setPadding(dp(12), dp(6), dp(12), dp(6));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = dp(6);
            row.setLayoutParams(params);

            TextView label = new TextView(activity);
            label.setText(user + "@" + host + "  ·  " + port);
            label.setTextColor(Color.parseColor("#FFD6E2F5"));
            label.setTextSize(13f);
            label.setSingleLine(true);
            label.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(label, labelParams);

            TextView delete = new TextView(activity);
            delete.setText(R.string.ssh_recent_delete);
            delete.setTextColor(Color.parseColor("#FF8A93A6"));
            delete.setTextSize(13f);
            delete.setPadding(dp(8), dp(6), dp(8), dp(6));
            row.addView(delete);

            TextView action = new TextView(activity);
            action.setText(R.string.ssh_recent_connect);
            action.setTextColor(Color.parseColor("#FF4D8DFF"));
            action.setTextSize(13f);
            action.setPadding(dp(12), dp(6), dp(4), dp(6));
            row.addView(action);

            View.OnClickListener connectClick = v -> {
                pendingKeyPath = keyPath;
                hostInput.setText(host);
                userInput.setText(user);
                portInput.setText(String.valueOf(port));
                if (!TextUtils.isEmpty(keyPath)) {
                    keyPathText.setText(keyPath);
                }
                connect();
            };
            row.setOnClickListener(connectClick);
            action.setOnClickListener(connectClick);
            delete.setOnClickListener(v -> {
                com.termux.zerocore.utils.SshStore.delete(item);
                renderRecent();
                toast(R.string.ssh_recent_deleted);
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
        pendingKeyPath = "";
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
            File sshDir = com.termux.zerocore.utils.SshStore.getKeyDir();
            String safeHost = host.replaceAll("[^a-zA-Z0-9_.\\-]", "_");
            File autoKey = new File(sshDir, "zt_server_" + safeHost + ".key");
            if (!TextUtils.isEmpty(keyText)) {
                keyFile = autoKey;
                try (OutputStream out = new FileOutputStream(keyFile)) {
                    out.write(keyText.getBytes("UTF-8"));
                    out.write('\n');
                }
            } else if (pickedKeyUri != null) {
                keyFile = autoKey;
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
            } else if (!TextUtils.isEmpty(pendingKeyPath) && new File(pendingKeyPath).exists()) {
                keyFile = new File(pendingKeyPath);
            } else if (!TextUtils.isEmpty(pendingKeyPath)) {
                File moved = new File(sshDir, new File(pendingKeyPath).getName());
                if (moved.exists()) {
                    keyFile = moved;
                }
            } else if (autoKey.exists() && autoKey.length() > 0) {
                keyFile = autoKey;
            }
            if (keyFile != null) {
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
