package com.termux.zerocore.control.handler;

import com.google.gson.JsonObject;

import com.termux.zerocore.aidebug.ZtAiDebugLogHelper;
import com.termux.zerocore.aidebug.ZtAiDebugSystemHelper;
import com.termux.zerocore.control.ControlCapability;
import com.termux.zerocore.control.ControlJson;
import com.termux.zerocore.control.ControlRequest;
import com.termux.zerocore.control.ControlResponse;
import com.termux.zerocore.crashhistory.ZtAiDebugCrashHelper;

import java.util.Arrays;
import java.util.List;

/** LOGS: logcat, dmesg and crash history. */
public final class LogsControlHandler extends AbstractControlHandler {

    public LogsControlHandler() {
        super(ControlCapability.LOGS, "ai-debug");
    }

    @Override
    public List<String> actions() {
        return Arrays.asList("logcat", "dmesg", "crash_list", "crash_detail",
                "crash_delete", "crash_clear");
    }

    @Override
    public JsonObject parameterSchema(String action) {
        JsonObject properties = new JsonObject();
        switch (action) {
            case "logcat":
                properties.add("lines", ControlJson.intProp("number of lines (default 300)"));
                properties.add("filter", ControlJson.stringProp("comma separated logcat tags"));
                return ControlJson.objectSchema(properties);
            case "dmesg":
                properties.add("lines", ControlJson.intProp("number of lines (default 200)"));
                return ControlJson.objectSchema(properties);
            case "crash_detail":
            case "crash_delete":
                properties.add("id", ControlJson.stringProp("crash record id"));
                return ControlJson.objectSchema(properties, "id");
            default:
                return ControlJson.objectSchema(properties);
        }
    }

    @Override
    public String actionDescription(String action) {
        switch (action) {
            case "logcat": return "Read logcat.";
            case "dmesg": return "Read the kernel ring buffer.";
            case "crash_list": return "List stored crash records.";
            case "crash_detail": return "Read one crash record.";
            case "crash_delete": return "Delete one crash record.";
            case "crash_clear": return "Clear all crash records.";
            default: return "Logs action.";
        }
    }

    @Override
    public ControlResponse invoke(ControlRequest request) {
        String action = request.getAction();
        try {
            switch (action) {
                case "logcat":
                    return json(request, ZtAiDebugLogHelper.INSTANCE.readLogcat(
                            request.paramInt("lines", 300), request.paramString("filter", null)));
                case "dmesg":
                    return json(request, ZtAiDebugSystemHelper.INSTANCE.dmesgJson(request.paramInt("lines", 200)));
                case "crash_list":
                    return json(request, ZtAiDebugCrashHelper.INSTANCE.listJson(context()));
                case "crash_detail":
                    return json(request, ZtAiDebugCrashHelper.INSTANCE.detailJson(
                            context(), request.paramString("id", "")));
                case "crash_delete":
                    return json(request, ZtAiDebugCrashHelper.INSTANCE.deleteJson(
                            context(), request.paramString("id", "")));
                case "crash_clear":
                    return json(request, ZtAiDebugCrashHelper.INSTANCE.clearJson(context()));
                default:
                    return fail(request, "unknown logs action: " + action);
            }
        } catch (Throwable throwable) {
            return fail(request, throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }
}
