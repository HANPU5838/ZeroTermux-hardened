package com.termux.zerocore.control.handler;

import com.google.gson.JsonObject;

import com.termux.zerocore.control.ControlCapability;
import com.termux.zerocore.control.ControlJson;
import com.termux.zerocore.control.ControlRequest;
import com.termux.zerocore.control.ControlResponse;
import com.termux.zerocore.workstation.ZtWorkstationContactsHelper;

import java.util.Collections;
import java.util.List;

/** CONTACTS: read the device contact list. */
public final class ContactsControlHandler extends AbstractControlHandler {

    public ContactsControlHandler() {
        super(ControlCapability.CONTACTS, "workstation/ai-debug");
    }

    @Override
    public List<String> actions() {
        return Collections.singletonList("list");
    }

    @Override
    public JsonObject parameterSchema(String action) {
        return ControlJson.objectSchema(new JsonObject());
    }

    @Override
    public String actionDescription(String action) {
        return "List device contacts.";
    }

    @Override
    public ControlResponse invoke(ControlRequest request) {
        if (!"list".equals(request.getAction())) {
            return fail(request, "unknown contacts action: " + request.getAction());
        }
        try {
            return json(request, ZtWorkstationContactsHelper.INSTANCE.listContacts(context()));
        } catch (Throwable throwable) {
            return fail(request, throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }
}
