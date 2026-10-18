package com.termux.zerocore.control;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * In-memory ring buffer of the most recent control-plane calls.
 *
 * <p>Nothing is persisted: the audit trail is a debugging aid that lives with
 * the process, exactly like the rest of the runtime state of these services.</p>
 */
public final class ControlAudit {

    private final int capacity;
    private final Deque<ControlEvent> entries;

    public ControlAudit(int capacity) {
        this.capacity = Math.max(16, capacity);
        this.entries = new ArrayDeque<>(this.capacity);
    }

    public synchronized void record(ControlEvent event) {
        if (event == null) return;
        entries.addFirst(event);
        while (entries.size() > capacity) {
            entries.removeLast();
        }
    }

    public synchronized List<ControlEvent> recent(int limit) {
        int max = limit <= 0 ? capacity : Math.min(limit, capacity);
        List<ControlEvent> result = new ArrayList<>(max);
        int count = 0;
        for (ControlEvent event : entries) {
            if (count++ >= max) break;
            result.add(event);
        }
        return result;
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized int capacity() {
        return capacity;
    }

    public synchronized JsonObject toJson(int limit) {
        JsonArray array = new JsonArray();
        for (ControlEvent event : recent(limit)) {
            array.add(event.toJson());
        }
        JsonObject json = new JsonObject();
        json.addProperty("ok", true);
        json.addProperty("size", entries.size());
        json.addProperty("capacity", capacity);
        json.add("entries", array);
        return json;
    }
}
