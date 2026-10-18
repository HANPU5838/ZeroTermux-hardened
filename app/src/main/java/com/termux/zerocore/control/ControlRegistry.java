package com.termux.zerocore.control;

import android.content.Context;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry of every control handler plus the capability-discovery and
 * event-subscription surface.
 *
 * <p>Registration is idempotent: all four external services call
 * {@code ControlBootstrap.ensureRegistered(...)} and the first call wins for a
 * given capability + action.</p>
 */
public final class ControlRegistry {

    private static final ControlRegistry INSTANCE = new ControlRegistry();

    private final Map<ControlCapability, LinkedHashMap<String, ControlHandler>> handlers =
            new EnumMap<>(ControlCapability.class);
    private final CopyOnWriteArrayList<ControlEventListener> listeners = new CopyOnWriteArrayList<>();

    private ControlRegistry() {
    }

    public static ControlRegistry get() {
        return INSTANCE;
    }

    public synchronized void register(ControlHandler handler) {
        if (handler == null || handler.capability() == null || handler.actions() == null) return;
        LinkedHashMap<String, ControlHandler> byAction = handlers.get(handler.capability());
        if (byAction == null) {
            byAction = new LinkedHashMap<>();
            handlers.put(handler.capability(), byAction);
        }
        for (String action : handler.actions()) {
            if (action == null || action.isEmpty()) continue;
            if (!byAction.containsKey(action)) {
                byAction.put(action, handler);
            }
        }
    }

    public synchronized void unregister(ControlHandler handler) {
        if (handler == null) return;
        LinkedHashMap<String, ControlHandler> byAction = handlers.get(handler.capability());
        if (byAction == null) return;
        byAction.values().removeIf(value -> value == handler);
    }

    public synchronized ControlHandler find(String capability, String action) {
        ControlCapability cap = ControlCapability.fromId(capability);
        if (cap == null || action == null) return null;
        LinkedHashMap<String, ControlHandler> byAction = handlers.get(cap);
        if (byAction == null) return null;
        return byAction.get(action);
    }

    public synchronized List<ControlHandler> handlers() {
        List<ControlHandler> result = new ArrayList<>();
        for (LinkedHashMap<String, ControlHandler> byAction : handlers.values()) {
            for (ControlHandler handler : byAction.values()) {
                if (!result.contains(handler)) result.add(handler);
            }
        }
        return result;
    }

    public synchronized List<String> actionsOf(ControlCapability capability) {
        LinkedHashMap<String, ControlHandler> byAction = handlers.get(capability);
        if (byAction == null) return new ArrayList<>();
        return new ArrayList<>(byAction.keySet());
    }

    public synchronized int size() {
        int total = 0;
        for (LinkedHashMap<String, ControlHandler> byAction : handlers.values()) {
            total += byAction.size();
        }
        return total;
    }

    public void subscribe(ControlEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unsubscribe(ControlEventListener listener) {
        listeners.remove(listener);
    }

    public void publish(ControlEvent event) {
        if (event == null) return;
        for (ControlEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception ignored) {
                // a broken subscriber must never break routing
            }
        }
    }

    /**
     * Builds the {@code /control/capabilities} document: device info, per
     * capability availability, protocol list and the (reported, not enforced)
     * auth mode.
     */
    public synchronized JsonObject capabilitiesJson(Context context) {
        JsonObject root = new JsonObject();
        root.addProperty("ok", true);
        root.addProperty("service", "ZeroTermux Control Plane");
        root.addProperty("version", 1);
        root.addProperty("generatedAt", System.currentTimeMillis());
        root.addProperty("registeredActions", size());

        // --- auth: report only, never enforce here -------------------------
        ControlAuthorizer authorizer = ControlRouter.get().getAuthorizer();
        boolean allowAll = authorizer == ControlAuthorizer.ALLOW_ALL;
        JsonObject auth = new JsonObject();
        auth.addProperty("mode", allowAll ? "open" : "custom");
        auth.addProperty("enforced", false);
        auth.addProperty("hook", allowAll ? "ControlAuthorizer.ALLOW_ALL" : authorizer.getClass().getName());
        auth.addProperty("notes", "Control routes keep the existing open product behaviour. "
                + "An optional authorizer can be installed with ControlRouter.setAuthorizer(...); "
                + "nothing enables one by default.");
        root.add("auth", auth);

        // --- capabilities --------------------------------------------------
        JsonArray capabilities = new JsonArray();
        for (ControlCapability capability : ControlCapability.values()) {
            LinkedHashMap<String, ControlHandler> byAction = handlers.get(capability);
            JsonObject capJson = new JsonObject();
            capJson.addProperty("capability", capability.id());
            capJson.addProperty("label", capability.label());
            capJson.addProperty("description", capability.description());
            boolean available = byAction != null && !byAction.isEmpty();
            capJson.addProperty("available", available);
            JsonArray actions = new JsonArray();
            if (byAction != null) {
                for (Map.Entry<String, ControlHandler> entry : byAction.entrySet()) {
                    JsonObject actionJson = new JsonObject();
                    actionJson.addProperty("action", entry.getKey());
                    actionJson.addProperty(
                            "description",
                            entry.getValue().actionDescription(entry.getKey()));
                    actionJson.add("schema", entry.getValue().parameterSchema(entry.getKey()));
                    actions.add(actionJson);
                }
            }
            capJson.add("actions", actions);
            capJson.addProperty("actionCount", actions.size());
            capabilities.add(capJson);
        }
        root.add("capabilities", capabilities);

        // --- protocols -----------------------------------------------------
        JsonArray protocols = new JsonArray();
        protocols.add(protocol("zt-socket", "tcp", 19951, "/", "zt command socket, raw text protocol"));
        protocols.add(protocol("ai-debug-http", "http", 19998, "/api/*", "external AI debug server (match code)"));
        protocols.add(protocol("ai-debug-control", "http", 19998, "/control/*", "unified control gateway (match code)"));
        protocols.add(protocol("ai-debug-mcp", "http", 19998, "/mcp", "MCP JSON-RPC subset"));
        protocols.add(protocol("workstation-http", "http", 19999, "/*", "browser workstation"));
        protocols.add(protocol("file-http", "http", 19956, "/*", "file browse/download/upload"));
        root.add("protocols", protocols);

        // --- endpoint map --------------------------------------------------
        JsonObject endpoints = new JsonObject();
        endpoints.addProperty("capabilities", "/control/capabilities");
        endpoints.addProperty("invoke", "/control/invoke");
        endpoints.addProperty("audit", "/control/audit");
        endpoints.addProperty("events", "/control/events");
        endpoints.addProperty("openapi", "/openapi.json");
        endpoints.addProperty("mcp", "/mcp");
        root.add("endpoints", endpoints);

        // --- device info ---------------------------------------------------
        if (context != null) {
            try {
                String device = com.termux.zerocore.aidebug.ZtAiDebugInfoHelper.INSTANCE.deviceInfo(context);
                JsonElement parsed = ControlJson.parseLenient(device);
                root.add("device", parsed);
            } catch (Throwable throwable) {
                root.addProperty("deviceError", String.valueOf(throwable.getMessage()));
            }
        }
        return root;
    }

    private static JsonObject protocol(String id, String transport, int port, String path, String description) {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("transport", transport);
        json.addProperty("port", port);
        json.addProperty("path", path);
        json.addProperty("description", description);
        json.addProperty("open", true);
        return json;
    }
}
