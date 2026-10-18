package com.termux.zerocore.control.handler;

import com.google.gson.JsonObject;

import com.termux.zerocore.control.ControlCapability;
import com.termux.zerocore.control.ControlJson;
import com.termux.zerocore.control.ControlRequest;
import com.termux.zerocore.control.ControlResponse;

import java.util.Collections;
import java.util.List;

/**
 * Wraps one legacy {@code zt} command (the :19951 socket protocol) as a control
 * handler. The original raw-text protocol is untouched; this only makes the
 * same command reachable through the shared router and MCP.
 */
public final class ZtCommandControlHandler extends AbstractControlHandler {

    private final String command;
    private final List<String> actions;
    private final String description;

    public ZtCommandControlHandler(String command, ControlCapability capability, String description) {
        super(capability, "zt-socket:19951");
        this.command = command;
        this.actions = Collections.singletonList(command);
        this.description = description;
    }

    @Override
    public List<String> actions() {
        return actions;
    }

    @Override
    public JsonObject parameterSchema(String action) {
        JsonObject properties = new JsonObject();
        properties.add("command",
                ControlJson.stringProp("full raw zt command line; when omitted the action name is used"));
        properties.add("args",
                ControlJson.stringProp("arguments appended to the action name, e.g. 'hello' for 'toast hello'"));
        return ControlJson.objectSchema(properties);
    }

    @Override
    public String actionDescription(String action) {
        return description;
    }

    @Override
    public ControlResponse invoke(ControlRequest request) {
        String commandLine = request.paramString("command", "").trim();
        if (commandLine.isEmpty()) {
            String args = request.paramString("args", "").trim();
            commandLine = args.isEmpty() ? command : command + " " + args;
        }
        try {
            String result = ZtCommandBridge.execute(context(), commandLine);
            if (result == null || result.isEmpty()) {
                return ok(request, ControlJson.obj("ok", true, "command", commandLine, "executed", true));
            }
            return json(request, result);
        } catch (Throwable throwable) {
            return fail(request, throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }
}
