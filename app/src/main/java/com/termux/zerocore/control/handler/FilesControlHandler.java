package com.termux.zerocore.control.handler;

import com.google.gson.JsonObject;

import com.termux.zerocore.aidebug.ZtAiDebugFileWriteHelper;
import com.termux.zerocore.control.ControlCapability;
import com.termux.zerocore.control.ControlJson;
import com.termux.zerocore.control.ControlRequest;
import com.termux.zerocore.control.ControlResponse;
import com.termux.zerocore.workstation.ZtWorkstationFileHelper;

import java.util.Arrays;
import java.util.List;

/** FILES: list/read/write/stat and the usual filesystem mutations. */
public final class FilesControlHandler extends AbstractControlHandler {

    public FilesControlHandler() {
        super(ControlCapability.FILES, "ai-debug/workstation/file-http");
    }

    @Override
    public List<String> actions() {
        return Arrays.asList("list", "browse", "read", "write", "stat",
                "mkdir", "delete", "copy", "move", "rename", "create");
    }

    @Override
    public JsonObject parameterSchema(String action) {
        JsonObject properties = new JsonObject();
        switch (action) {
            case "list":
                properties.add("path", ControlJson.stringProp("directory or file path"));
                properties.add("root", ControlJson.boolProp("use a root shell listing"));
                return ControlJson.objectSchema(properties, "path");
            case "browse":
            case "stat":
            case "mkdir":
            case "delete":
            case "create":
                properties.add("path", ControlJson.stringProp("target path"));
                return ControlJson.objectSchema(properties, "path");
            case "read":
                properties.add("path", ControlJson.stringProp("file path"));
                properties.add("maxBytes", ControlJson.intProp("maximum bytes to read"));
                return ControlJson.objectSchema(properties, "path");
            case "write":
                properties.add("path", ControlJson.stringProp("file path"));
                properties.add("content", ControlJson.stringProp("UTF-8 content"));
                properties.add("root", ControlJson.boolProp("write through a root shell"));
                return ControlJson.objectSchema(properties, "path", "content");
            case "copy":
            case "move":
                properties.add("from", ControlJson.stringProp("source path"));
                properties.add("to", ControlJson.stringProp("destination path"));
                return ControlJson.objectSchema(properties, "from", "to");
            case "rename":
                properties.add("path", ControlJson.stringProp("existing path"));
                properties.add("name", ControlJson.stringProp("new file name"));
                return ControlJson.objectSchema(properties, "path", "name");
            default:
                return ControlJson.anySchema();
        }
    }

    @Override
    public String actionDescription(String action) {
        switch (action) {
            case "list": return "List a directory (optionally through root).";
            case "browse": return "List a directory through the workstation file helper.";
            case "read": return "Read a text file.";
            case "write": return "Write a text file.";
            case "stat": return "Stat a path.";
            case "mkdir": return "Create a directory.";
            case "delete": return "Delete a path.";
            case "copy": return "Copy a path.";
            case "move": return "Move a path.";
            case "rename": return "Rename a path.";
            case "create": return "Create an empty file.";
            default: return "Filesystem action.";
        }
    }

    @Override
    public ControlResponse invoke(ControlRequest request) {
        String action = request.getAction();
        try {
            switch (action) {
                case "list":
                    return json(request, ZtAiDebugFileWriteHelper.INSTANCE.listJson(
                            request.paramString("path", ""), request.paramBool("root", false)));
                case "browse":
                    return json(request, ZtWorkstationFileHelper.INSTANCE.listDirectory(
                            request.paramString("path", "")));
                case "read":
                    return json(request, ZtWorkstationFileHelper.INSTANCE.readText(
                            request.paramString("path", ""), request.paramInt("maxBytes", 512 * 1024)));
                case "write": {
                    String path = request.paramString("path", "");
                    String content = request.paramString("content", "");
                    if (path.isEmpty()) return fail(request, "path required");
                    return json(request, ZtAiDebugFileWriteHelper.INSTANCE.writeJson(
                            path, content, "utf-8", request.paramBool("root", false)));
                }
                case "stat":
                    return json(request, ZtWorkstationFileHelper.INSTANCE.stat(request.paramString("path", "")));
                case "mkdir":
                    return json(request, ZtWorkstationFileHelper.INSTANCE.mkdir(request.paramString("path", "")));
                case "delete":
                    return json(request, ZtWorkstationFileHelper.INSTANCE.delete(request.paramString("path", "")));
                case "copy":
                    return json(request, ZtWorkstationFileHelper.INSTANCE.copy(
                            request.paramString("from", ""), request.paramString("to", "")));
                case "move":
                    return json(request, ZtWorkstationFileHelper.INSTANCE.move(
                            request.paramString("from", ""), request.paramString("to", "")));
                case "rename":
                    return json(request, ZtWorkstationFileHelper.INSTANCE.rename(
                            request.paramString("path", ""), request.paramString("name", "")));
                case "create":
                    return json(request, ZtWorkstationFileHelper.INSTANCE.createFile(request.paramString("path", "")));
                default:
                    return fail(request, "unknown files action: " + action);
            }
        } catch (Throwable throwable) {
            return fail(request, throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }
}
