package com.termux.zerocore.control.handler;

import android.util.Base64;

import com.google.gson.JsonObject;

import com.termux.zerocore.aidebug.ZtAiDebugInputHelper;
import com.termux.zerocore.aidebug.ZtAiDebugScreenshotHelper;
import com.termux.zerocore.control.ControlCapability;
import com.termux.zerocore.control.ControlJson;
import com.termux.zerocore.control.ControlRequest;
import com.termux.zerocore.control.ControlResponse;

import java.util.Arrays;
import java.util.List;

/** UI: input injection, app launching and screenshots. */
public final class UiControlHandler extends AbstractControlHandler {

    public UiControlHandler() {
        super(ControlCapability.UI, "ai-debug");
    }

    @Override
    public List<String> actions() {
        return Arrays.asList("tap", "swipe", "text", "keyevent", "launch",
                "force_stop", "screenshot");
    }

    @Override
    public JsonObject parameterSchema(String action) {
        JsonObject properties = new JsonObject();
        switch (action) {
            case "tap":
                properties.add("x", ControlJson.intProp("x coordinate"));
                properties.add("y", ControlJson.intProp("y coordinate"));
                return ControlJson.objectSchema(properties, "x", "y");
            case "swipe":
                properties.add("x1", ControlJson.intProp("start x"));
                properties.add("y1", ControlJson.intProp("start y"));
                properties.add("x2", ControlJson.intProp("end x"));
                properties.add("y2", ControlJson.intProp("end y"));
                properties.add("durationMs", ControlJson.intProp("duration in milliseconds"));
                return ControlJson.objectSchema(properties, "x1", "y1", "x2", "y2");
            case "text":
                properties.add("text", ControlJson.stringProp("text to type"));
                return ControlJson.objectSchema(properties, "text");
            case "keyevent":
                properties.add("code", ControlJson.intProp("Android key code"));
                return ControlJson.objectSchema(properties, "code");
            case "launch":
                properties.add("package", ControlJson.stringProp("package name"));
                properties.add("activity", ControlJson.stringProp("optional activity name"));
                return ControlJson.objectSchema(properties, "package");
            case "force_stop":
                properties.add("package", ControlJson.stringProp("package name"));
                return ControlJson.objectSchema(properties, "package");
            case "screenshot":
                properties.add("source", ControlJson.stringProp("auto | app | root"));
                return ControlJson.objectSchema(properties);
            default:
                return ControlJson.anySchema();
        }
    }

    @Override
    public String actionDescription(String action) {
        switch (action) {
            case "tap": return "Inject a tap.";
            case "swipe": return "Inject a swipe.";
            case "text": return "Inject text.";
            case "keyevent": return "Inject a key event.";
            case "launch": return "Launch an app or activity.";
            case "force_stop": return "Force stop an app.";
            case "screenshot": return "Capture a PNG screenshot (base64 encoded).";
            default: return "UI action.";
        }
    }

    @Override
    public ControlResponse invoke(ControlRequest request) {
        String action = request.getAction();
        try {
            switch (action) {
                case "tap":
                    return json(request, ZtAiDebugInputHelper.INSTANCE.tapJson(
                            request.paramInt("x", 0), request.paramInt("y", 0)));
                case "swipe":
                    return json(request, ZtAiDebugInputHelper.INSTANCE.swipeJson(
                            request.paramInt("x1", 0), request.paramInt("y1", 0),
                            request.paramInt("x2", 0), request.paramInt("y2", 0),
                            request.paramInt("durationMs", 300)));
                case "text":
                    return json(request, ZtAiDebugInputHelper.INSTANCE.textJson(
                            request.paramString("text", "")));
                case "keyevent":
                    return json(request, ZtAiDebugInputHelper.INSTANCE.keyeventJson(
                            request.paramInt("code", 0)));
                case "launch":
                    return json(request, ZtAiDebugInputHelper.INSTANCE.launchJson(
                            request.paramString("package", ""), request.paramString("activity", "")));
                case "force_stop":
                    return json(request, ZtAiDebugInputHelper.INSTANCE.forceStopJson(
                            request.paramString("package", "")));
                case "screenshot": {
                    String source = request.paramString("source", "auto");
                    byte[] bytes = ZtAiDebugScreenshotHelper.INSTANCE.capturePng(context(), source);
                    if (bytes == null || bytes.length == 0) {
                        return fail(request, "screenshot failed (keep ZeroTermux in foreground or use source=root)");
                    }
                    return ok(request, ControlJson.obj(
                            "source", source,
                            "mime", "image/png",
                            "bytes", bytes.length,
                            "base64", Base64.encodeToString(bytes, Base64.NO_WRAP)));
                }
                default:
                    return fail(request, "unknown ui action: " + action);
            }
        } catch (Throwable throwable) {
            return fail(request, throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }
}
