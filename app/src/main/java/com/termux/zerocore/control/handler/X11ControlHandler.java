package com.termux.zerocore.control.handler;

import com.google.gson.JsonObject;

import com.termux.zerocore.aidebug.ZtAiDebugVncHelper;
import com.termux.zerocore.control.ControlCapability;
import com.termux.zerocore.control.ControlJson;
import com.termux.zerocore.control.ControlRequest;
import com.termux.zerocore.control.ControlResponse;

import java.util.Arrays;
import java.util.List;

/** X11: VNC/editor status, start and editor opening. */
public final class X11ControlHandler extends AbstractControlHandler {

    public X11ControlHandler() {
        super(ControlCapability.X11, "ai-debug");
    }

    @Override
    public List<String> actions() {
        return Arrays.asList("status", "start", "open_editor");
    }

    @Override
    public JsonObject parameterSchema(String action) {
        JsonObject properties = new JsonObject();
        if ("open_editor".equals(action)) {
            properties.add("path", ControlJson.stringProp("file path to open"));
            properties.add("openX11Tab", ControlJson.boolProp("open the X11 tab"));
            properties.add("autoRun", ControlJson.boolProp("auto run the file"));
            return ControlJson.objectSchema(properties, "path");
        }
        return ControlJson.objectSchema(properties);
    }

    @Override
    public String actionDescription(String action) {
        switch (action) {
            case "status": return "VNC/X11 diagnostic status.";
            case "start": return "Start the editor VNC session.";
            case "open_editor": return "Open a file in the editor (optionally with X11).";
            default: return "X11 action.";
        }
    }

    @Override
    public ControlResponse invoke(ControlRequest request) {
        String action = request.getAction();
        try {
            switch (action) {
                case "status":
                    return json(request, ZtAiDebugVncHelper.INSTANCE.statusJson());
                case "start":
                    return json(request, ZtAiDebugVncHelper.INSTANCE.startJson());
                case "open_editor": {
                    String path = request.paramString("path", "");
                    if (path.isEmpty()) return fail(request, "path required");
                    return json(request, ZtAiDebugVncHelper.INSTANCE.openEditorJson(
                            context(), path,
                            request.paramBool("openX11Tab", false),
                            request.paramBool("autoRun", false)));
                }
                default:
                    return fail(request, "unknown x11 action: " + action);
            }
        } catch (Throwable throwable) {
            return fail(request, throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }
}
