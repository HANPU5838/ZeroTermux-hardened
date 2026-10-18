package com.termux.zerocore.control;

import com.google.gson.JsonObject;

/**
 * Routes {@code capability + action} to the matching {@link ControlHandler} with
 * one shared authorisation hook, audit trail and error envelope.
 */
public final class ControlRouter {

    private static final ControlRouter INSTANCE = new ControlRouter();

    private final ControlAudit audit = new ControlAudit(200);
    private volatile ControlAuthorizer authorizer = ControlAuthorizer.ALLOW_ALL;

    private ControlRouter() {
    }

    public static ControlRouter get() {
        return INSTANCE;
    }

    public ControlAudit audit() {
        return audit;
    }

    public ControlAuthorizer getAuthorizer() {
        return authorizer;
    }

    /** Installs an optional authorizer. Never called automatically. */
    public void setAuthorizer(ControlAuthorizer authorizer) {
        this.authorizer = authorizer == null ? ControlAuthorizer.ALLOW_ALL : authorizer;
    }

    public ControlResponse route(ControlRequest request, String source) {
        long startNanos = System.nanoTime();
        ControlRequest req = request == null ? new ControlRequest() : request;
        req.getOrCreateId();
        String effectiveSource = source;
        if (effectiveSource == null || effectiveSource.isEmpty()) {
            effectiveSource = req.getSource() == null || req.getSource().isEmpty()
                    ? "unknown" : req.getSource();
        }

        ControlResponse response;
        try {
            String capability = req.getCapability() == null ? "" : req.getCapability();
            String action = req.getAction() == null ? "" : req.getAction();
            if (capability.isEmpty() || action.isEmpty()) {
                response = ControlResponse.failure(req, capability, action,
                        "capability and action are required");
            } else if (!authorizer.authorize(req, effectiveSource)) {
                response = ControlResponse.failure(req, capability, action,
                        "unauthorized by control authorizer");
            } else {
                ControlHandler handler = ControlRegistry.get().find(capability, action);
                if (handler == null) {
                    response = ControlResponse.failure(req, capability, action,
                            "unknown action: " + capability + "." + action);
                } else {
                    ControlResponse invoked = handler.invoke(req);
                    response = invoked != null ? invoked
                            : ControlResponse.success(req, capability, action, new JsonObject());
                }
            }
        } catch (Throwable throwable) {
            response = ControlResponse.failure(req, req.getCapability(), req.getAction(),
                    throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }

        long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
        response.setDurationMs(durationMs);
        response.setTimestamp(System.currentTimeMillis());
        response.setSource(effectiveSource);
        if (response.getCapability() == null || response.getCapability().isEmpty()) {
            response.setCapability(req.getCapability());
        }
        if (response.getAction() == null || response.getAction().isEmpty()) {
            response.setAction(req.getAction());
        }
        if (response.getId() == null || response.getId().isEmpty()) {
            response.setId(req.getId());
        }

        ControlEvent event = ControlEvent.audit(response);
        audit.record(event);
        ControlRegistry.get().publish(event);
        return response;
    }
}
