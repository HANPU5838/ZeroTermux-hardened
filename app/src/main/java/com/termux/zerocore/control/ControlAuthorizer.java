package com.termux.zerocore.control;

/**
 * Optional authorisation extension point of the control plane.
 *
 * <p><b>Default is allow-all.</b> The open external interfaces are a product
 * feature, so the router MUST keep working with no authorizer installed.
 * Deployments that want to add their own policy can install one through
 * {@link ControlRouter#setAuthorizer(ControlAuthorizer)}; nothing turns it on
 * automatically.</p>
 */
public interface ControlAuthorizer {

    boolean authorize(ControlRequest request, String source);

    /** The default hook: never blocks anything. */
    ControlAuthorizer ALLOW_ALL = new ControlAuthorizer() {
        @Override
        public boolean authorize(ControlRequest request, String source) {
            return true;
        }
    };
}
