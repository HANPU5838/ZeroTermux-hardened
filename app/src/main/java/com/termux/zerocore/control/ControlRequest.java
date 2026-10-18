package com.termux.zerocore.control;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Unified request model: id + capability + action + params. */
public final class ControlRequest {

    private String id = "";
    private String capability = "";
    private String action = "";
    private JsonObject params = new JsonObject();
    private String source = "";
    private long timestamp = System.currentTimeMillis();

    public static ControlRequest fromJson(JsonObject json) {
        ControlRequest request = new ControlRequest();
        if (json == null) {
            request.id = Long.toString(System.currentTimeMillis());
            return request;
        }
        request.id = str(json, "id", "");
        request.capability = str(json, "capability", "").trim().toLowerCase();
        request.action = str(json, "action", "").trim();
        JsonElement params = json.get("params");
        if (params == null || !params.isJsonObject()) {
            params = json.get("arguments");
        }
        if (params != null && params.isJsonObject()) {
            request.params = params.getAsJsonObject();
        }
        request.source = str(json, "source", "");
        if (request.id.isEmpty()) {
            request.id = Long.toString(System.currentTimeMillis());
        }
        return request;
    }

    private static String str(JsonObject json, String key, String fallback) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) return fallback;
        try {
            return element.getAsString();
        } catch (Exception e) {
            return fallback;
        }
    }

    public String paramString(String key, String fallback) {
        JsonElement element = params.get(key);
        if (element == null || element.isJsonNull()) return fallback;
        try {
            return element.getAsString();
        } catch (Exception e) {
            return fallback;
        }
    }

    public int paramInt(String key, int fallback) {
        JsonElement element = params.get(key);
        if (element == null || element.isJsonNull()) return fallback;
        try {
            return element.getAsInt();
        } catch (Exception e) {
            return fallback;
        }
    }

    public long paramLong(String key, long fallback) {
        JsonElement element = params.get(key);
        if (element == null || element.isJsonNull()) return fallback;
        try {
            return element.getAsLong();
        } catch (Exception e) {
            return fallback;
        }
    }

    public double paramDouble(String key, double fallback) {
        JsonElement element = params.get(key);
        if (element == null || element.isJsonNull()) return fallback;
        try {
            return element.getAsDouble();
        } catch (Exception e) {
            return fallback;
        }
    }

    public boolean paramBool(String key, boolean fallback) {
        JsonElement element = params.get(key);
        if (element == null || element.isJsonNull()) return fallback;
        try {
            return element.getAsBoolean();
        } catch (Exception e) {
            return fallback;
        }
    }

    public JsonObject paramObject(String key) {
        JsonElement element = params.get(key);
        if (element != null && element.isJsonObject()) return element.getAsJsonObject();
        return null;
    }

    public String getOrCreateId() {
        if (id == null || id.isEmpty()) {
            id = Long.toString(System.currentTimeMillis());
        }
        return id;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCapability() { return capability; }
    public void setCapability(String capability) { this.capability = capability; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public JsonObject getParams() { return params; }
    public void setParams(JsonObject params) { this.params = params == null ? new JsonObject() : params; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
