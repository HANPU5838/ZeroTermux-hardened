package com.termux.zerocore.control.handler;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.termux.zerocore.control.ControlBootstrap;
import com.termux.zerocore.control.ControlCapability;
import com.termux.zerocore.control.ControlHandler;
import com.termux.zerocore.control.ControlJson;
import com.termux.zerocore.control.ControlRequest;
import com.termux.zerocore.control.ControlResponse;

/** Shared plumbing for the concrete control handlers. */
public abstract class AbstractControlHandler implements ControlHandler {

    private final ControlCapability capability;
    private final String source;

    protected AbstractControlHandler(ControlCapability capability, String source) {
        this.capability = capability;
        this.source = source;
    }

    @Override
    public ControlCapability capability() {
        return capability;
    }

    @Override
    public JsonObject metadata() {
        return ControlJson.obj("source", source);
    }

    protected ControlResponse ok(ControlRequest request, JsonElement result) {
        return ControlResponse.success(request, capability.id(), request.getAction(), result);
    }

    protected ControlResponse ok(ControlRequest request, Object... keyValues) {
        return ok(request, ControlJson.baseResult(keyValues));
    }

    protected ControlResponse json(ControlRequest request, String rawJson) {
        return ok(request, ControlJson.parseLenient(rawJson));
    }

    protected ControlResponse fail(ControlRequest request, String error) {
        return ControlResponse.failure(request, capability.id(), request.getAction(), error);
    }

    protected android.content.Context context() {
        return ControlBootstrap.requireContext();
    }

    /** Convenience used by subclasses when declaring JSON Schema. */
    protected static JsonObject schema(JsonObject properties, String... required) {
        return ControlJson.objectSchema(properties, required);
    }
}
