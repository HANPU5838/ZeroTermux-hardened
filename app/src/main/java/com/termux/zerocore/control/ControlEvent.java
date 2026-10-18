package com.termux.zerocore.control;

import com.google.gson.JsonObject;

/**
 * A single control-plane event. Used both for the in-memory audit ring buffer
 * and for the {@code /control/events} SSE stream.
 */
public final class ControlEvent {

    public static final String TYPE_AUDIT = "audit";
    public static final String TYPE_TERMINAL = "terminal";

    private String type = TYPE_AUDIT;
    private long timestamp = System.currentTimeMillis();
    private String capability = "";
    private String action = "";
    private String source = "";
    private boolean ok;
    private long durationMs;
    private String error;
    private JsonObject data;

    public static ControlEvent audit(ControlResponse response) {
        ControlEvent event = new ControlEvent();
        event.type = TYPE_AUDIT;
        event.timestamp = response.getTimestamp() > 0 ? response.getTimestamp() : System.currentTimeMillis();
        event.capability = response.getCapability();
        event.action = response.getAction();
        event.source = response.getSource();
        event.ok = response.isOk();
        event.durationMs = response.getDurationMs();
        event.error = response.getError();
        JsonObject data = new JsonObject();
        data.addProperty("id", response.getId());
        event.data = data;
        return event;
    }

    public static ControlEvent terminal(String action, JsonObject data) {
        ControlEvent event = new ControlEvent();
        event.type = TYPE_TERMINAL;
        event.timestamp = System.currentTimeMillis();
        event.capability = ControlCapability.TERMINAL.id();
        event.action = action;
        event.source = "terminal";
        event.ok = true;
        event.data = data;
        return event;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getCapability() { return capability; }
    public void setCapability(String capability) { this.capability = capability; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public JsonObject getData() { return data; }
    public void setData(JsonObject data) { this.data = data; }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        json.addProperty("timestamp", timestamp);
        json.addProperty("capability", capability);
        json.addProperty("action", action);
        json.addProperty("source", source);
        json.addProperty("ok", ok);
        json.addProperty("durationMs", durationMs);
        if (error != null) {
            json.addProperty("error", error);
        }
        json.add("data", data == null ? new JsonObject() : data);
        return json;
    }

    /** Serialises this event as one SSE frame. */
    public String toSse() {
        StringBuilder builder = new StringBuilder();
        builder.append("event: ").append(type).append('\n');
        builder.append("data: ").append(ControlJson.GSON.toJson(toJson())).append('\n');
        builder.append('\n');
        return builder.toString();
    }
}
