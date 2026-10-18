package com.termux.zerocore.control;

import android.content.Context;

import com.termux.zerocore.control.handler.CameraControlHandler;
import com.termux.zerocore.control.handler.ContactsControlHandler;
import com.termux.zerocore.control.handler.FilesControlHandler;
import com.termux.zerocore.control.handler.LogsControlHandler;
import com.termux.zerocore.control.handler.QemuControlHandler;
import com.termux.zerocore.control.handler.RootControlHandler;
import com.termux.zerocore.control.handler.SmsControlHandler;
import com.termux.zerocore.control.handler.SystemControlHandler;
import com.termux.zerocore.control.handler.TerminalControlHandler;
import com.termux.zerocore.control.handler.UiControlHandler;
import com.termux.zerocore.control.handler.X11ControlHandler;
import com.termux.zerocore.control.handler.ZtCommandControlHandler;

/**
 * Idempotent wiring of every existing external line into the shared registry.
 *
 * <p>Call {@link #ensureRegistered(Context)} from any service start; the zt
 * socket service additionally calls {@link #registerZtCommandHandlers()} so its
 * legacy 18-command protocol is explicitly owned by the socket line.</p>
 */
public final class ControlBootstrap {

    private static volatile Context appContext;
    private static volatile boolean registered;

    private ControlBootstrap() {
    }

    public static synchronized void ensureRegistered(Context context) {
        if (context != null) {
            try {
                appContext = context.getApplicationContext();
            } catch (Throwable ignored) {
                appContext = context;
            }
        }
        registerZtCommandHandlers();
        if (registered) return;
        ControlRegistry registry = ControlRegistry.get();
        registry.register(new TerminalControlHandler());
        registry.register(new FilesControlHandler());
        registry.register(new CameraControlHandler());
        registry.register(new ContactsControlHandler());
        registry.register(new SmsControlHandler());
        registry.register(new RootControlHandler());
        registry.register(new X11ControlHandler());
        registry.register(new QemuControlHandler());
        registry.register(new UiControlHandler());
        registry.register(new LogsControlHandler());
        registry.register(new SystemControlHandler());
        registered = true;
    }

    /**
     * Wraps the 18 legacy {@code zt} commands as control handlers. The raw text
     * protocol on :19951 keeps working unchanged; this only adds a second door
     * into the exact same command implementations.
     */
    public static synchronized void registerZtCommandHandlers() {
        ControlRegistry registry = ControlRegistry.get();
        zt(registry, "toast", ControlCapability.UI, "Show a toast: toast hello");
        zt(registry, "help", ControlCapability.SYSTEM, "Show the zt command help text.");
        zt(registry, "version", ControlCapability.SYSTEM, "Show the ZeroTermux version.");
        zt(registry, "openleft", ControlCapability.UI, "Open the left drawer.");
        zt(registry, "openright", ControlCapability.UI, "Open the right drawer.");
        zt(registry, "reboot", ControlCapability.SYSTEM, "Restart the Termux service.");
        zt(registry, "ln", ControlCapability.FILES, "Create a symlink: ln <link> <target>.");
        zt(registry, "vnc", ControlCapability.X11, "Open AVnc with a server profile.");
        zt(registry, "x11commandshow", ControlCapability.X11, "Show the X11 command bar.");
        zt(registry, "x11commandhide", ControlCapability.X11, "Hide the X11 command bar.");
        zt(registry, "x11status", ControlCapability.X11, "Query the X11 connection status.");
        zt(registry, "x11keyboardshow", ControlCapability.X11, "Show the X11 keyboard.");
        zt(registry, "x11keyboardhide", ControlCapability.X11, "Hide the X11 keyboard.");
        zt(registry, "dialog", ControlCapability.UI, "Show a confirmed / cancel / edit dialog.");
        zt(registry, "qr", ControlCapability.UI, "Open the QR code scanner/encoder.");
        zt(registry, "backgroundimage", ControlCapability.UI, "Set the terminal background image.");
        zt(registry, "menu", ControlCapability.UI, "Update or reset the main menu XML.");
        zt(registry, "openpage", ControlCapability.UI, "Open an in-app page by id.");
    }

    private static void zt(ControlRegistry registry, String command, ControlCapability capability,
                           String description) {
        registry.register(new ZtCommandControlHandler(command, capability, description));
    }

    /** Application context for handler invocation; falls back to UUtils. */
    public static Context requireContext() {
        Context context = appContext;
        if (context != null) return context;
        try {
            return com.example.xh_lib.utils.UUtils.getContext();
        } catch (Throwable throwable) {
            return null;
        }
    }

    public static boolean isRegistered() {
        return registered;
    }
}
