package com.termux.zerocore.control.handler;

import com.google.gson.JsonObject;

import com.termux.zerocore.aidebug.ZtAiDebugTerminalHelper;
import com.termux.zerocore.control.ControlCapability;
import com.termux.zerocore.control.ControlEvent;
import com.termux.zerocore.control.ControlJson;
import com.termux.zerocore.control.ControlRegistry;
import com.termux.zerocore.control.ControlRequest;
import com.termux.zerocore.control.ControlResponse;

import java.util.Arrays;
import java.util.List;

/** TERMINAL: snapshot, key injection and command execution. */
public final class TerminalControlHandler extends AbstractControlHandler {

    public TerminalControlHandler() {
        super(ControlCapability.TERMINAL, "ai-debug/workstation");
    }

    @Override
    public List<String> actions() {
        return Arrays.asList("snapshot", "exec", "send");
    }

    @Override
    public JsonObject parameterSchema(String action) {
        JsonObject properties = new JsonObject();
        switch (action) {
            case "snapshot":
                properties.add("maxChars", ControlJson.intProp("maximum characters to return (1000-50000)"));
                return ControlJson.objectSchema(properties);
            case "exec":
                properties.add("command", ControlJson.stringProp("command line to type into the active terminal"));
                properties.add("waitMs", ControlJson.intProp("milliseconds to wait before snapshot (default 2500)"));
                return ControlJson.objectSchema(properties, "command");
            case "send":
                properties.add("command", ControlJson.stringProp("text to send to the active terminal"));
                return ControlJson.objectSchema(properties, "command");
            default:
                return ControlJson.anySchema();
        }
    }

    @Override
    public String actionDescription(String action) {
        switch (action) {
            case "snapshot": return "Read the active terminal transcript.";
            case "exec": return "Type a command into the active terminal and return the resulting screen.";
            case "send": return "Send raw text to the active terminal without waiting.";
            default: return "Terminal action.";
        }
    }

    @Override
    public ControlResponse invoke(ControlRequest request) {
        String action = request.getAction();
        try {
            switch (action) {
                case "snapshot": {
                    int maxChars = request.paramInt("maxChars", 12000);
                    String text = ZtAiDebugTerminalHelper.INSTANCE.snapshot(maxChars);
                    JsonObject data = ControlJson.obj(
                            "snapshot", text,
                            "length", text.length(),
                            "maxChars", maxChars);
                    ControlRegistry.get().publish(ControlEvent.terminal("snapshot", data));
                    return ok(request, data);
                }
                case "exec": {
                    String command = request.paramString("command", "");
                    if (command.isEmpty()) return fail(request, "command required");
                    long waitMs = request.paramLong("waitMs", 2500L);
                    String snapshot = ZtAiDebugTerminalHelper.INSTANCE.execAndSnapshot(command, waitMs);
                    JsonObject data = ControlJson.obj(
                            "command", command,
                            "waitMs", waitMs,
                            "snapshot", snapshot);
                    ControlRegistry.get().publish(ControlEvent.terminal("exec", data));
                    return ok(request, data);
                }
                case "send": {
                    String command = request.paramString("command", "");
                    if (command.isEmpty()) return fail(request, "command required");
                    ZtAiDebugTerminalHelper.INSTANCE.sendCommand(command);
                    JsonObject data = ControlJson.obj("sent", true, "command", command);
                    ControlRegistry.get().publish(ControlEvent.terminal("send", data));
                    return ok(request, data);
                }
                default:
                    return fail(request, "unknown terminal action: " + action);
            }
        } catch (Throwable throwable) {
            return fail(request, throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }
}
