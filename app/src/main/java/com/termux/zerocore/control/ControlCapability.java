package com.termux.zerocore.control;

/**
 * Capability vocabulary of the ZeroTermux control plane.
 *
 * <p>The control plane is deliberately additive: it does not replace any of the
 * existing external interfaces (zt socket :19951, AI debug HTTP :19998,
 * workstation HTTP :19999, file HTTP :19956). It only gives them one shared
 * routing vocabulary.</p>
 */
public enum ControlCapability {
    TERMINAL("terminal", "Terminal", "Termux terminal session access: snapshot, key injection and command execution."),
    FILES("files", "Files", "File system access (app-private, shared storage and root assisted paths)."),
    CAMERA("camera", "Camera", "Camera frame capture and camera lifecycle."),
    SMS("sms", "SMS", "Read SMS threads/messages and send SMS."),
    CONTACTS("contacts", "Contacts", "Read the device contact list."),
    ROOT("root", "Root", "Root shell execution and ADB over TCP."),
    X11("x11", "X11 / VNC", "X11/VNC status, start and editor integration."),
    QEMU("qemu", "QEMU", "QEMU / Windows VM helpers shipped with ZeroTermux."),
    UI("ui", "UI automation", "Input injection, app launching, screenshots and zt UI commands."),
    LOGS("logs", "Logs", "logcat, dmesg and crash history."),
    SYSTEM("system", "System", "Device/system information and properties.");

    private final String id;
    private final String label;
    private final String description;

    ControlCapability(String id, String label, String description) {
        this.id = id;
        this.label = label;
        this.description = description;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public static ControlCapability fromId(String id) {
        if (id == null) return null;
        String needle = id.trim().toLowerCase();
        for (ControlCapability capability : values()) {
            if (capability.id.equals(needle)) return capability;
        }
        return null;
    }
}
