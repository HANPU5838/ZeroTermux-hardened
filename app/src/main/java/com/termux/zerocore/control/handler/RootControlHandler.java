package com.termux.zerocore.control.handler;

import com.google.gson.JsonObject;

import com.termux.zerocore.aidebug.ZtAiDebugRootHelper;
import com.termux.zerocore.control.ControlCapability;
import com.termux.zerocore.control.ControlJson;
import com.termux.zerocore.control.ControlRequest;
import com.termux.zerocore.control.ControlResponse;

import java.util.Arrays;
import java.util.List;

/** ROOT: root shell execution and ADB over TCP. */
public final class RootControlHandler extends AbstractControlHandler {

    public RootControlHandler() {
        super(ControlCapability.ROOT, "ai-debug");
    }

    @Override
    public List<String> actions() {
        return Arrays.asList("status", "exec", "adb_tcp_enable", "adb_tcp_disable");
    }

    @Override
    public JsonObject parameterSchema(String action) {
        JsonObject properties = new JsonObject();
        switch (action) {
            case "exec":
                properties.add("command", ControlJson.stringProp("shell command"));
                properties.add("timeoutMs", ControlJson.intProp("timeout in milliseconds (default 5000)"));
                properties.add("asRoot", ControlJson.boolProp("run through su (default true)"));
                return ControlJson.objectSchema(properties, "command");
            case "adb_tcp_enable":
                properties.add("port", ControlJson.intProp("ADB TCP port (default 5555)"));
                return ControlJson.objectSchema(properties);
            default:
                return ControlJson.objectSchema(properties);
        }
    }

    @Override
    public String actionDescription(String action) {
        switch (action) {
            case "status": return "Root mode and ADB TCP status.";
            case "exec": return "Execute a shell command, optionally as root.";
            case "adb_tcp_enable": return "Enable ADB over TCP.";
            case "adb_tcp_disable": return "Disable ADB over TCP.";
            default: return "Root action.";
        }
    }

    @Override
    public ControlResponse invoke(ControlRequest request) {
        String action = request.getAction();
        try {
            switch (action) {
                case "status":
                    return json(request, ZtAiDebugRootHelper.INSTANCE.statusJson(context()));
                case "exec": {
                    String command = request.paramString("command", "");
                    if (command.isEmpty()) return fail(request, "command required");
                    long timeoutMs = request.paramLong("timeoutMs", 5000L);
                    boolean asRoot = request.paramBool("asRoot", true);
                    ZtAiDebugRootHelper.ExecResult result =
                            ZtAiDebugRootHelper.INSTANCE.exec(command, timeoutMs, asRoot);
                    return json(request, ZtAiDebugRootHelper.INSTANCE.execResultJson(result, command, timeoutMs));
                }
                case "adb_tcp_enable": {
                    int port = request.paramInt("port", 5555);
                    ZtAiDebugRootHelper.ExecResult result = ZtAiDebugRootHelper.INSTANCE.enableAdbTcp(port);
                    return ok(request, ControlJson.obj(
                            "ok", result.getOk(),
                            "port", port,
                            "adb_tcp_port", ZtAiDebugRootHelper.INSTANCE.readAdbTcpPort(),
                            "stdout", result.getStdout(),
                            "stderr", result.getStderr()));
                }
                case "adb_tcp_disable": {
                    ZtAiDebugRootHelper.ExecResult result = ZtAiDebugRootHelper.INSTANCE.disableAdbTcp();
                    return ok(request, ControlJson.obj(
                            "ok", result.getOk(),
                            "adb_tcp_port", ZtAiDebugRootHelper.INSTANCE.readAdbTcpPort(),
                            "stdout", result.getStdout(),
                            "stderr", result.getStderr()));
                }
                default:
                    return fail(request, "unknown root action: " + action);
            }
        } catch (Throwable throwable) {
            return fail(request, throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }
}
