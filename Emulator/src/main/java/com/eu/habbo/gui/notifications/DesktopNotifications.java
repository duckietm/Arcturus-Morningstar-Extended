package com.eu.habbo.gui.notifications;

import java.awt.*;

public class DesktopNotifications {

    private static TrayIcon trayIcon;

    public static void setTrayIcon(TrayIcon icon) {
        trayIcon = icon;
    }

    public static void notify(String title, String message, TrayIcon.MessageType type) {
        if (trayIcon == null || !SystemTray.isSupported()) return;
        try {
            trayIcon.displayMessage(title, message, type);
        } catch (Exception ignored) {
        }
    }
}
