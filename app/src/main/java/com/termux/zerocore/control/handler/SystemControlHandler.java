package com.termux.zerocore.control.handler;

import com.google.gson.JsonObject;

import com.termux.zerocore.aidebug.ZtAiDebugInfoHelper;
import com.termux.zerocore.aidebug.ZtAiDebugPermissionHelper;
import com.termux.zerocore.aidebug.ZtAiDebugSystemHelper;
import com.termux.zerocore.aidebug.ZtAiDebugTimerHelper;
import com.termux.zerocore.control.ControlCapability;
import com.termux.zerocore.control.ControlJson;
import com.termux.zerocore.control.ControlRequest;
import com.termux.zerocore.control.ControlResponse;
import com.termux.zerocore.workstation.ZtWorkstationDeviceHelper;

import java.util.Arrays;
import java.util.List;

/** SYSTEM: device/system information, properties and the timer status. */
public final class SystemControlHandler extends AbstractControlHandler {

    public SystemControlHandler() {
        super(ControlCapability.SYSTEM, "ai-debug/workstation");
    }

    @Override
    public List<String> actions() {
        return Arrays.asList("info", "status", "permissions", "getprop", "setprop",
                "dumpsys", "packages", "processes", "timer", "device");
    }

    @Override
    public JsonObject parameterSchema(String action) {
        JsonObject properties = new JsonObject();
        switch (action) {
            case "getprop":
                properties.add("key", ControlJson.stringProp("property key; omit for all"));
                return ControlJson.objectSchema(properties);
            case "setprop":
                properties.add("key", ControlJson.stringProp("property key"));
                properties.add("value", ControlJson.stringProp("property value"));
                return ControlJson.objectSchema(properties, "key");
            case "dumpsys":
                properties.add("service", ControlJson.stringProp("service name (default activity)"));
                return ControlJson.objectSchema(properties);
            case "packages":
                properties.add("filter", ControlJson.stringProp("package name filter"));
                return ControlJson.objectSchema(properties);
            default:
                return ControlJson.objectSchema(properties);
        }
    }

    @Override
    public String actionDescription(String action) {
        switch (action) {
            case "info": return "Device info and base URLs.";
            case "status": return "System status (memory, disk, load).";
            case "permissions": return "Runtime permission status.";
            case "getprop": return "Read system properties.";
            case "setprop": return "Set a system property.";
            case "dumpsys": return "Run dumpsys.";
            case "packages": return "List installed packages.";
            case "processes": return "List running processes.";
            case "timer": return "Timer status.";
            case "device": return "Workstation device info (battery, versions).";
            default: return "System action.";
        }
    }

    @Override
    public ControlResponse invoke(ControlRequest request) {
        String action = request.getAction();
        try {
            switch (action) {
                case "info":
                    return json(request, ZtAiDebugInfoHelper.INSTANCE.deviceInfo(context()));
                case "status":
                    return json(request, ZtAiDebugSystemHelper.INSTANCE.statusJson(context()));
                case "permissions":
                    return json(request, ZtAiDebugPermissionHelper.INSTANCE.statusJson(context()));
                case "getprop":
                    return json(request, ZtAiDebugSystemHelper.INSTANCE.getpropJson(
                            request.paramString("key", null)));
                case "setprop":
                    return json(request, ZtAiDebugSystemHelper.INSTANCE.setpropJson(
                            request.paramString("key", ""), request.paramString("value", "")));
                case "dumpsys":
                    return json(request, ZtAiDebugSystemHelper.INSTANCE.dumpsysJson(
                            request.paramString("service", "activity")));
                case "packages":
                    return json(request, ZtAiDebugSystemHelper.INSTANCE.packagesJson(
                            request.paramString("filter", null)));
                case "processes":
                    return json(request, ZtAiDebugSystemHelper.INSTANCE.processesJson());
                case "timer":
                    return json(request, ZtAiDebugTimerHelper.INSTANCE.statusJson(context()));
                case "device":
                    return json(request, ZtWorkstationDeviceHelper.INSTANCE.getDeviceInfo(context()));
                default:
                    return fail(request, "unknown system action: " + action);
            }
        } catch (Throwable throwable) {
            return fail(request, throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }
}
