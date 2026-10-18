package com.termux.zerocore.control.handler;

import android.content.Intent;

import com.example.xh_lib.utils.UUtils;
import com.google.gson.JsonObject;

import com.termux.zerocore.code.CodeString;
import com.termux.zerocore.control.ControlCapability;
import com.termux.zerocore.control.ControlJson;
import com.termux.zerocore.control.ControlRequest;
import com.termux.zerocore.control.ControlResponse;
import com.termux.zerocore.url.FileUrl;
import com.termux.zerocore.utils.SingletonCommunicationUtils;
import com.termux.zerocore.utermux_windows.qemu.activity.RunWindowActivity;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * QEMU: the same helpers the main menu uses to run the bundled Windows/Alpine
 * QEMU scripts, plus the RunWindowActivity launcher.
 */
public final class QemuControlHandler extends AbstractControlHandler {

    public QemuControlHandler() {
        super(ControlCapability.QEMU, "main-menu");
    }

    @Override
    public List<String> actions() {
        return Arrays.asList("status", "open_window", "run");
    }

    @Override
    public JsonObject parameterSchema(String action) {
        JsonObject properties = new JsonObject();
        if ("run".equals(action)) {
            properties.add("mode", ControlJson.stringProp("online | local | win7 | winxp"));
        }
        return ControlJson.objectSchema(properties);
    }

    @Override
    public String actionDescription(String action) {
        switch (action) {
            case "status": return "Report QEMU script availability.";
            case "open_window": return "Open RunWindowActivity.";
            case "run": return "Run a bundled QEMU script in the active terminal.";
            default: return "QEMU action.";
        }
    }

    @Override
    public ControlResponse invoke(ControlRequest request) {
        String action = request.getAction();
        try {
            switch (action) {
                case "status": {
                    File home = new File(FileUrl.INSTANCE.getMainHomeUrl());
                    return ok(request, ControlJson.obj(
                            "ok", true,
                            "home", home == null ? "" : home.getAbsolutePath(),
                            "assets", ControlJson.array("qemu/utqemu.sh", "qemu/qemu_win7.sh", "qemu/qemu_winxp.sh"),
                            "modes", ControlJson.array("online", "local", "win7", "winxp")));
                }
                case "open_window": {
                    Intent intent = new Intent(context(), RunWindowActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context().startActivity(intent);
                    return ok(request, ControlJson.obj("opened", true, "activity", RunWindowActivity.class.getName()));
                }
                case "run": {
                    String mode = request.paramString("mode", "online").trim().toLowerCase();
                    switch (mode) {
                        case "online":
                            send(CodeString.INSTANCE.getRunLineQemu() + "\n");
                            break;
                        case "local":
                            writeAsset("qemu/utqemu.sh", "utqemu.sh");
                            send(CodeString.INSTANCE.getRunQemuSh());
                            break;
                        case "win7":
                            writeAsset("qemu/qemu_win7.sh", "qemu_win7.sh");
                            send(CodeString.INSTANCE.getRunWin7Sh());
                            break;
                        case "winxp":
                            writeAsset("qemu/qemu_winxp.sh", "qemu_winxp.sh");
                            send(CodeString.INSTANCE.getRunWinXPSh());
                            break;
                        default:
                            return fail(request, "unknown qemu mode: " + mode);
                    }
                    return ok(request, ControlJson.obj("started", true, "mode", mode));
                }
                default:
                    return fail(request, "unknown qemu action: " + action);
            }
        } catch (Throwable throwable) {
            return fail(request, throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }

    private void writeAsset(String assetPath, String fileName) {
        File home = new File(FileUrl.INSTANCE.getMainHomeUrl());
        if (home != null && !home.exists()) {
            home.mkdirs();
        }
        UUtils.writerFile(assetPath, new File(home, fileName));
    }

    private void send(String text) {
        SingletonCommunicationUtils.getInstance()
                .getmSingletonCommunicationListener()
                .sendTextToTerminal(text);
    }
}
