package com.termux.zerocore.control.handler;

import android.content.Context;

import com.termux.zerocore.config.ztcommand.ZTSocketService;
import com.termux.zerocore.config.ztcommand.navigation.ZtCommandExecutor;

/**
 * Bridge that runs a raw {@code zt} command from the control plane.
 *
 * <p>When the zt socket service is alive we reuse its headless execution path
 * so forward/interactive commands behave exactly like a real socket client.
 * Otherwise we fall back to the shared {@link ZtCommandExecutor}, which is the
 * same entry point the in-app AI uses.</p>
 */
public final class ZtCommandBridge {

    private ZtCommandBridge() {
    }

    public static String execute(Context context, String commandLine) {
        if (commandLine == null || commandLine.trim().isEmpty()) {
            return "";
        }
        ZTSocketService service = ZTSocketService.getInstance();
        if (service != null) {
            String result = service.executeHeadless(context, commandLine);
            if (result != null && !result.isEmpty()) {
                return result;
            }
        }
        return ZtCommandExecutor.execute(commandLine);
    }
}
