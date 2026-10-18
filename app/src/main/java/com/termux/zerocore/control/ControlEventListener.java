package com.termux.zerocore.control;

/** Receives control-plane events (audit + terminal output) for the SSE stream. */
public interface ControlEventListener {

    void onEvent(ControlEvent event);
}
