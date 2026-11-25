package com.termux.zerocore.control.handler;

import com.google.gson.JsonObject;

import com.termux.zerocore.aidebug.ZtAiDebugEditorHelper;
import com.termux.zerocore.control.ControlCapability;
import com.termux.zerocore.control.ControlJson;
import com.termux.zerocore.control.ControlRequest;
import com.termux.zerocore.control.ControlResponse;

import java.util.Collections;
import java.util.List;

/** X11/editor: open a file in the editor, optionally on the X11 dock tab. */
public final class X11ControlHandler extends AbstractControlHandler {

    public X11ControlHandler() {
        super(ControlCapability.X11, "ai-debug");
    }

    @Override
    public List<String> actions() {
        return Collections.singletonList("open_editor");
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
        if ("open_editor".equals(action)) {
            return "Open a file in the editor (optionally with X11).";
        }
        return "X11 action.";
    }

    @Override
    public ControlResponse invoke(ControlRequest request) {
        String action = request.getAction();
        try {
            if ("open_editor".equals(action)) {
                String path = request.paramString("path", "");
                if (path.isEmpty()) return fail(request, "path required");
                return json(request, ZtAiDebugEditorHelper.INSTANCE.openEditorJson(
                        context(), path,
                        request.paramBool("openX11Tab", false),
                        request.paramBool("autoRun", false)));
            }
            return fail(request, "unknown x11 action: " + action);
        } catch (Throwable throwable) {
            return fail(request, throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }
}
