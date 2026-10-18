package com.termux.zerocore.control;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

/** Unified response model: id + capability + action + result/error + duration. */
public final class ControlResponse {

    private String id = "";
    private String capability = "";
    private String action = "";
    private boolean ok;
    private JsonElement result;
    private String error;
    private long durationMs;
    private long timestamp = System.currentTimeMillis();
    private String source = "";

    public static ControlResponse success(ControlRequest request, String capability, String action,
                                          JsonElement result) {
        ControlResponse response = new ControlResponse();
        response.id = request == null ? "" : request.getOrCreateId();
        response.capability = capability == null ? "" : capability;
        response.action = action == null ? "" : action;
        response.ok = true;
        response.result = result;
        return response;
    }

    public static ControlResponse failure(ControlRequest request, String capability, String action,
                                          String error) {
        ControlResponse response = new ControlResponse();
        response.id = request == null ? "" : request.getOrCreateId();
        response.capability = capability == null ? "" : capability;
        response.action = action == null ? "" : action;
        response.ok = false;
        response.error = error == null ? "unknown error" : error;
        return response;
    }

    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCapability() { return capability; }
    public void setCapability(String capability) { this.capability = capability; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public JsonElement getResult() { return result; }
    public void setResult(JsonElement result) { this.result = result; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("capability", capability);
        json.addProperty("action", action);
        json.addProperty("ok", ok);
        json.add("result", result == null ? JsonNull.INSTANCE : result);
        if (error != null) {
            json.addProperty("error", error);
        } else {
            json.add("error", JsonNull.INSTANCE);
        }
        json.addProperty("durationMs", durationMs);
        json.addProperty("timestamp", timestamp);
        json.addProperty("source", source);
        return json;
    }

    public String toJsonString() {
        return ControlJson.GSON.toJson(toJson());
    }
}
