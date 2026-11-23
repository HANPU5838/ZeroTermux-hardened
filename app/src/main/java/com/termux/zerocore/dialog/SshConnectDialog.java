package com.termux.zerocore.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.termux.TermuxConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 常用功能 -> 连接服务器：填写地址/端口/用户名/私钥（可粘贴或从文件导入），
 * 点连接后在当前终端里执行 ssh，直接进入远程服务器会话。
 * 没填私钥时走密码登录，由 ssh 在终端里提示输入。
 */
public class SshConnectDialog extends Dialog {

    public static final int REQ_PICK_KEY = 0x5A11;

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
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
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

        findViewById(R.id.ssh_panel).setAlpha(0f);
        findViewById(R.id.ssh_panel).animate().alpha(1f).setDuration(220)
            .setInterpolator(new DecelerateInterpolator(1.4f)).start();
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
            }
        } catch (Exception e) {
            toast(R.string.ssh_key_save_fail);
            return;
        }

        StringBuilder command = new StringBuilder();
        command.append("mkdir -p ~/.ssh && chmod 700 ~/.ssh; ");
        command.append("command -v ssh >/dev/null 2>&1 || pkg install -y openssh >/dev/null 2>&1; ");
        if (keyFile != null) {
            command.append("chmod 600 '").append(keyFile.getAbsolutePath()).append("'; ");
        }
        command.append("ssh -o StrictHostKeyChecking=accept-new -p ").append(port).append(" ");
        if (keyFile != null) {
            command.append("-i '").append(keyFile.getAbsolutePath()).append("' ");
        }
        command.append(user).append("@").append(host);

        dismiss();
        activity.sendTextToTerminal(command.toString() + "\n");
    }

    private void toast(int resId) {
        Toast.makeText(getContext(), resId, Toast.LENGTH_SHORT).show();
    }
}
