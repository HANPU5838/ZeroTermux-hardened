package com.termux.zerocore.control.handler;

import com.google.gson.JsonObject;

import com.termux.zerocore.control.ControlCapability;
import com.termux.zerocore.control.ControlJson;
import com.termux.zerocore.control.ControlRequest;
import com.termux.zerocore.control.ControlResponse;
import com.termux.zerocore.workstation.ZtWorkstationSmsHelper;

import java.util.Arrays;
import java.util.List;

/** SMS: threads, messages and sending. */
public final class SmsControlHandler extends AbstractControlHandler {

    public SmsControlHandler() {
        super(ControlCapability.SMS, "workstation/ai-debug");
    }

    @Override
    public List<String> actions() {
        return Arrays.asList("threads", "messages", "send");
    }

    @Override
    public JsonObject parameterSchema(String action) {
        JsonObject properties = new JsonObject();
        if ("messages".equals(action)) {
            properties.add("address", ControlJson.stringProp("phone number / thread address"));
            return ControlJson.objectSchema(properties, "address");
        }
        if ("send".equals(action)) {
            properties.add("address", ControlJson.stringProp("destination phone number"));
            properties.add("body", ControlJson.stringProp("message body"));
            return ControlJson.objectSchema(properties, "address", "body");
        }
        return ControlJson.objectSchema(properties);
    }

    @Override
    public String actionDescription(String action) {
        switch (action) {
            case "threads": return "List SMS threads.";
            case "messages": return "List messages in a thread.";
            case "send": return "Send an SMS.";
            default: return "SMS action.";
        }
    }

    @Override
    public ControlResponse invoke(ControlRequest request) {
        String action = request.getAction();
        try {
            switch (action) {
                case "threads":
                    return json(request, ZtWorkstationSmsHelper.INSTANCE.listThreads(context()));
                case "messages":
                    return json(request, ZtWorkstationSmsHelper.INSTANCE.listMessages(
                            context(), request.paramString("address", "")));
                case "send": {
                    String address = request.paramString("address", "");
                    String body = request.paramString("body", "");
                    if (address.isEmpty() || body.isEmpty()) return fail(request, "address and body required");
                    return json(request, ZtWorkstationSmsHelper.INSTANCE.sendSms(address, body));
                }
                default:
                    return fail(request, "unknown sms action: " + action);
            }
        } catch (Throwable throwable) {
            return fail(request, throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }
}
