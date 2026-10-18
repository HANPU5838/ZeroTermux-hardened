package com.termux.zerocore.control;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.Collection;

/** Small JSON helpers shared by the control plane. Uses the Gson already on the classpath. */
public final class ControlJson {

    public static final Gson GSON = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();

    private ControlJson() {
    }

    /** Builds a JsonObject from alternating key/value pairs. */
    public static JsonObject obj(Object... keyValues) {
        JsonObject json = new JsonObject();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object key = keyValues[i];
            if (key == null) continue;
            put(json, String.valueOf(key), keyValues[i + 1]);
        }
        return json;
    }

    public static JsonObject put(JsonObject json, String key, Object value) {
        if (value == null) {
            json.add(key, JsonNull.INSTANCE);
        } else if (value instanceof JsonElement) {
            json.add(key, (JsonElement) value);
        } else {
            json.add(key, GSON.toJsonTree(value));
        }
        return json;
    }

    public static JsonArray array(Collection<?> values) {
        JsonArray array = new JsonArray();
        if (values == null) return array;
        for (Object value : values) {
            array.add(GSON.toJsonTree(value));
        }
        return array;
    }

    public static JsonArray array(String... values) {
        JsonArray array = new JsonArray();
        if (values == null) return array;
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    /** Parses JSON, or wraps plain text as a JSON string when parsing fails. */
    public static JsonElement parseLenient(String text) {
        if (text == null) return JsonNull.INSTANCE;
        String trimmed = text.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                return JsonParser.parseString(trimmed);
            } catch (Exception ignored) {
                // fall through to primitive
            }
        }
        return new JsonPrimitive(text);
    }

    public static JsonObject baseResult(Object... keyValues) {
        JsonObject json = obj(keyValues);
        if (!json.has("ok")) {
            json.addProperty("ok", true);
        }
        return json;
    }

    // ---------------------------------------------------------------------
    // JSON Schema helpers (used by MCP tools/list and /openapi.json)
    // ---------------------------------------------------------------------

    public static JsonObject schemaOf() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", new JsonObject());
        return schema;
    }

    public static JsonObject anySchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.addProperty("additionalProperties", true);
        return schema;
    }

    public static JsonObject objectSchema(JsonObject properties, String... required) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", properties == null ? new JsonObject() : properties);
        if (required != null && required.length > 0) {
            JsonArray requiredArray = new JsonArray();
            for (String key : required) {
                requiredArray.add(key);
            }
            schema.add("required", requiredArray);
        }
        return schema;
    }

    public static JsonObject stringProp(String description) {
        JsonObject prop = new JsonObject();
        prop.addProperty("type", "string");
        if (description != null) prop.addProperty("description", description);
        return prop;
    }

    public static JsonObject intProp(String description) {
        JsonObject prop = new JsonObject();
        prop.addProperty("type", "integer");
        if (description != null) prop.addProperty("description", description);
        return prop;
    }

    public static JsonObject boolProp(String description) {
        JsonObject prop = new JsonObject();
        prop.addProperty("type", "boolean");
        if (description != null) prop.addProperty("description", description);
        return prop;
    }

    public static JsonObject arrayProp(String description) {
        JsonObject prop = new JsonObject();
        prop.addProperty("type", "array");
        if (description != null) prop.addProperty("description", description);
        prop.add("items", new JsonObject());
        return prop;
    }
}
