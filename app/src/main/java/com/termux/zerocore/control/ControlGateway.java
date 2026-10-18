package com.termux.zerocore.control;

import android.content.Context;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Protocol surface of the control plane: capability discovery, OpenAPI 3.1,
 * MCP JSON-RPC subset and the audit query.
 */
public final class ControlGateway {

    public static final String MCP_PROTOCOL_VERSION = "2024-11-05";

    private ControlGateway() {
    }

    public static JsonObject capabilities(Context context) {
        return ControlRegistry.get().capabilitiesJson(context);
    }

    public static JsonObject audit(int limit) {
        return ControlRouter.get().audit().toJson(limit);
    }

    // ---------------------------------------------------------------------
    // MCP JSON-RPC subset: initialize / tools/list / tools/call
    // ---------------------------------------------------------------------

    /** Builds the MCP {@code tools/list} payload from the registry. */
    public static JsonObject toolsList() {
        JsonArray tools = new JsonArray();
        for (ControlHandler handler : ControlRegistry.get().handlers()) {
            ControlCapability capability = handler.capability();
            if (capability == null) continue;
            for (String action : handler.actions()) {
                if (action == null || action.isEmpty()) continue;
                JsonObject tool = new JsonObject();
                tool.addProperty("name", toolName(capability, action));
                tool.addProperty("description", toolDescription(handler, action));
                tool.add("inputSchema", handler.parameterSchema(action));
                tools.add(tool);
            }
        }
        JsonObject result = new JsonObject();
        result.add("tools", tools);
        return result;
    }

    public static String toolName(ControlCapability capability, String action) {
        return capability.id() + "__" + action;
    }

    private static String toolDescription(ControlHandler handler, String action) {
        String description = handler.actionDescription(action);
        if (description == null || description.isEmpty()) {
            description = handler.capability().description();
        }
        JsonObject metadata = handler.metadata();
        if (metadata != null && metadata.has("source")) {
            JsonElement source = metadata.get("source");
            if (source != null && source.isJsonPrimitive()) {
                description = description + " (source: " + source.getAsString() + ")";
            }
        }
        return description;
    }

    /**
     * Handles one MCP JSON-RPC message.
     *
     * @return the JSON-RPC response, or {@code null} for notifications (which
     * must not receive a response).
     */
    public static JsonObject handleMcp(Context context, JsonObject request) {
        if (request == null) {
            return jsonRpcError(JsonNull.INSTANCE, -32700, "parse error");
        }
        JsonElement id = request.has("id") ? request.get("id") : JsonNull.INSTANCE;
        JsonElement methodElement = request.get("method");
        String method = methodElement == null || methodElement.isJsonNull()
                ? "" : methodElement.getAsString();
        JsonObject params = new JsonObject();
        JsonElement paramsElement = request.get("params");
        if (paramsElement != null && paramsElement.isJsonObject()) {
            params = paramsElement.getAsJsonObject();
        }

        switch (method) {
            case "initialize": {
                JsonObject serverInfo = new JsonObject();
                serverInfo.addProperty("name", "zerotermux-control");
                serverInfo.addProperty("version", "1.0.0");
                JsonObject capabilities = new JsonObject();
                capabilities.add("tools", new JsonObject());
                JsonObject result = new JsonObject();
                result.addProperty("protocolVersion", MCP_PROTOCOL_VERSION);
                result.add("capabilities", capabilities);
                result.add("serverInfo", serverInfo);
                result.addProperty("instructions",
                        "ZeroTermux control plane. Every tool is capability__action. "
                        + "Call tools/list for the live catalogue; tools/call routes through ControlRouter.");
                return jsonRpcResult(id, result);
            }
            case "notifications/initialized":
            case "notifications/cancelled":
                return null;
            case "ping":
                return jsonRpcResult(id, new JsonObject());
            case "tools/list":
                return jsonRpcResult(id, toolsList());
            case "tools/call": {
                JsonElement nameElement = params.get("name");
                if (nameElement == null || nameElement.isJsonNull()) {
                    return jsonRpcError(id, -32602, "tools/call requires params.name");
                }
                String name = nameElement.getAsString();
                int split = name.indexOf("__");
                if (split <= 0 || split + 2 >= name.length()) {
                    return jsonRpcError(id, -32602, "invalid tool name: " + name);
                }
                String capability = name.substring(0, split);
                String action = name.substring(split + 2);
                JsonObject arguments = new JsonObject();
                JsonElement argumentsElement = params.get("arguments");
                if (argumentsElement != null && argumentsElement.isJsonObject()) {
                    arguments = argumentsElement.getAsJsonObject();
                }
                ControlRequest controlRequest = new ControlRequest();
                controlRequest.setId(id.isJsonNull() ? name : id.getAsString());
                controlRequest.setCapability(capability);
                controlRequest.setAction(action);
                controlRequest.setParams(arguments);
                controlRequest.setSource("mcp");
                ControlResponse response = ControlRouter.get().route(controlRequest, "mcp");

                JsonObject callResult = new JsonObject();
                JsonArray content = new JsonArray();
                JsonObject text = new JsonObject();
                text.addProperty("type", "text");
                text.addProperty("text", response.toJsonString());
                content.add(text);
                callResult.add("content", content);
                callResult.addProperty("isError", !response.isOk());
                if (response.getResult() != null && response.getResult().isJsonObject()
                        && !response.getResult().isJsonNull()) {
                    callResult.add("structuredContent", response.getResult());
                }
                return jsonRpcResult(id, callResult);
            }
            default:
                return jsonRpcError(id, -32601, "method not found: " + method);
        }
    }

    private static JsonObject jsonRpcResult(JsonElement id, JsonObject result) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", id == null ? JsonNull.INSTANCE : id);
        response.add("result", result);
        return response;
    }

    private static JsonObject jsonRpcError(JsonElement id, int code, String message) {
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", id == null ? JsonNull.INSTANCE : id);
        response.add("error", error);
        return response;
    }

    // ---------------------------------------------------------------------
    // OpenAPI 3.1
    // ---------------------------------------------------------------------

    public static JsonObject openApi(Context context) {
        JsonObject root = new JsonObject();
        root.addProperty("openapi", "3.1.0");

        JsonObject info = new JsonObject();
        info.addProperty("title", "ZeroTermux Control Plane");
        info.addProperty("version", "1.0.0");
        info.addProperty("description",
                "Unified gateway over the existing ZeroTermux external interfaces. "
                + "Every handler registered by the zt socket, AI debug, workstation and file "
                + "services is reachable through POST /control/invoke and /mcp.");
        root.add("info", info);

        JsonObject paths = new JsonObject();
        paths.add("/control/capabilities", pathGet("Capability discovery (device, availability, protocols, auth mode)."));
        paths.add("/control/audit", pathGet("Recent control calls (in-memory ring buffer)."));
        paths.add("/control/events", pathGet("Server-sent events: audit trail + terminal output."));
        paths.add("/openapi.json", pathGet("This document."));
        paths.add("/control/invoke", pathInvoke());
        paths.add("/mcp", pathMcp());
        root.add("paths", paths);

        JsonArray actions = new JsonArray();
        Map<String, JsonObject> byCapability = new LinkedHashMap<>();
        for (ControlHandler handler : ControlRegistry.get().handlers()) {
            ControlCapability capability = handler.capability();
            if (capability == null) continue;
            for (String action : handler.actions()) {
                if (action == null || action.isEmpty()) continue;
                JsonObject entry = new JsonObject();
                entry.addProperty("capability", capability.id());
                entry.addProperty("action", action);
                entry.addProperty("tool", toolName(capability, action));
                entry.addProperty("description", handler.actionDescription(action));
                JsonObject invoke = new JsonObject();
                invoke.addProperty("method", "POST");
                invoke.addProperty("path", "/control/invoke");
                JsonObject body = new JsonObject();
                JsonObject params = new JsonObject();
                params.addProperty("capability", capability.id());
                params.addProperty("action", action);
                params.add("params", handler.parameterSchema(action));
                body.add("params", params);
                invoke.add("exampleBody", body);
                entry.add("http", invoke);
                actions.add(entry);

                byCapability.computeIfAbsent(capability.id(), key -> new JsonObject());
                byCapability.get(capability.id()).addProperty(action, handler.actionDescription(action));
            }
        }
        root.add("x-zerotermux-actions", actions);
        root.add("x-zerotermux-capabilities", ControlJson.GSON.toJsonTree(byCapability));
        if (context != null) {
            root.add("x-zerotermux-registered", ControlJson.GSON.toJsonTree(ControlRegistry.get().size()));
        }
        return root;
    }

    private static JsonObject pathGet(String summary) {
        JsonObject get = new JsonObject();
        get.addProperty("summary", summary);
        JsonObject response = new JsonObject();
        JsonObject ok = new JsonObject();
        ok.addProperty("description", "Success");
        response.add("200", ok);
        get.add("responses", response);
        JsonObject path = new JsonObject();
        path.add("get", get);
        return path;
    }

    private static JsonObject pathInvoke() {
        JsonObject post = new JsonObject();
        post.addProperty("summary", "Invoke any capability action.");
        post.addProperty("operationId", "controlInvoke");
        JsonObject response = new JsonObject();
        JsonObject ok = new JsonObject();
        ok.addProperty("description", "ControlResponse envelope");
        response.add("200", ok);
        post.add("responses", response);
        JsonObject path = new JsonObject();
        path.add("post", post);
        return path;
    }

    private static JsonObject pathMcp() {
        JsonObject post = new JsonObject();
        post.addProperty("summary", "MCP JSON-RPC endpoint (initialize / tools/list / tools/call).");
        post.addProperty("operationId", "mcpJsonRpc");
        JsonObject response = new JsonObject();
        JsonObject ok = new JsonObject();
        ok.addProperty("description", "JSON-RPC 2.0 response");
        response.add("200", ok);
        post.add("responses", response);
        JsonObject path = new JsonObject();
        path.add("post", post);
        return path;
    }

    /** Convenience for the HTTP servers: pretty-ish JSON string of capabilities. */
    public static String capabilitiesString(Context context) {
        return ControlJson.GSON.toJson(capabilities(context));
    }

    public static String auditString(int limit) {
        return ControlJson.GSON.toJson(audit(limit));
    }

    public static String openApiString(Context context) {
        return ControlJson.GSON.toJson(openApi(context));
    }
}
