package com.termux.zerocore.control;

import com.google.gson.JsonObject;

import java.util.List;

/**
 * A control-plane handler owns one capability and one or more actions.
 *
 * <p>The existing services (zt socket, AI debug HTTP, workstation HTTP, file
 * HTTP) implement this interface so every external line can be reached through
 * the shared {@link ControlRouter}.</p>
 */
public interface ControlHandler {

    ControlCapability capability();

    /** Action names handled by this instance, without the capability prefix. */
    List<String> actions();

    ControlResponse invoke(ControlRequest request);

    /** JSON Schema for one action's params, surfaced through MCP tools/list. */
    default JsonObject parameterSchema(String action) {
        return ControlJson.anySchema();
    }

    /** Human readable description for one action, surfaced through MCP/docs. */
    default String actionDescription(String action) {
        return "";
    }

    /** Free-form metadata reported by capability discovery. */
    default JsonObject metadata() {
        return new JsonObject();
    }
}
