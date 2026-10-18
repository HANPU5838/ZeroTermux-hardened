package com.termux.zerocore.control.handler;

import android.util.Base64;

import com.google.gson.JsonObject;

import com.termux.zerocore.aidebug.ZtAiDebugManager;
import com.termux.zerocore.control.ControlCapability;
import com.termux.zerocore.control.ControlJson;
import com.termux.zerocore.control.ControlRequest;
import com.termux.zerocore.control.ControlResponse;
import com.termux.zerocore.workstation.ZtWorkstationCameraHelper;
import com.termux.zerocore.workstation.ZtWorkstationManager;

import java.util.Arrays;
import java.util.List;

/** CAMERA: frame capture plus camera lifecycle. */
public final class CameraControlHandler extends AbstractControlHandler {

    public CameraControlHandler() {
        super(ControlCapability.CAMERA, "ai-debug/workstation");
    }

    @Override
    public List<String> actions() {
        return Arrays.asList("frame", "start", "release", "status");
    }

    @Override
    public JsonObject parameterSchema(String action) {
        JsonObject properties = new JsonObject();
        if ("frame".equals(action)) {
            properties.add("facing", ControlJson.stringProp("camera facing: back, front or external"));
        }
        return ControlJson.objectSchema(properties);
    }

    @Override
    public String actionDescription(String action) {
        switch (action) {
            case "frame": return "Capture one JPEG frame and return it base64 encoded.";
            case "start": return "Start the camera helper.";
            case "release": return "Release the camera helper.";
            case "status": return "Report whether the camera helper is available.";
            default: return "Camera action.";
        }
    }

    private ZtWorkstationCameraHelper helper() {
        ZtWorkstationCameraHelper helper = ZtWorkstationManager.INSTANCE.getCameraHelper();
        if (helper == null) {
            helper = ZtAiDebugManager.INSTANCE.getCameraHelper();
        }
        return helper;
    }

    @Override
    public ControlResponse invoke(ControlRequest request) {
        String action = request.getAction();
        try {
            ZtWorkstationCameraHelper helper = helper();
            switch (action) {
                case "status":
                    return ok(request, ControlJson.obj(
                            "ok", helper != null,
                            "available", helper != null,
                            "enabled", helper != null && helper.isEnabled()));
                case "start":
                    if (helper == null) return fail(request, "camera helper unavailable");
                    helper.startCameras();
                    return ok(request, ControlJson.obj("started", true));
                case "release":
                    if (helper == null) return fail(request, "camera helper unavailable");
                    helper.releaseCameras();
                    return ok(request, ControlJson.obj("released", true));
                case "frame": {
                    if (helper == null) return fail(request, "camera helper unavailable");
                    String facing = request.paramString("facing", "back");
                    byte[] bytes = helper.getFrame(facing);
                    if (bytes == null || bytes.length == 0) {
                        return fail(request, "camera frame unavailable (facing=" + facing + ")");
                    }
                    return ok(request, ControlJson.obj(
                            "facing", facing,
                            "mime", "image/jpeg",
                            "bytes", bytes.length,
                            "base64", Base64.encodeToString(bytes, Base64.NO_WRAP)));
                }
                default:
                    return fail(request, "unknown camera action: " + action);
            }
        } catch (Throwable throwable) {
            return fail(request, throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }
}
