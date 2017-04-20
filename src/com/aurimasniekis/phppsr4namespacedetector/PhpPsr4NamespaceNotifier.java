package com.aurimasniekis.phppsr4namespacedetector;

import com.intellij.notification.*;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.util.PlatformUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.swing.event.HyperlinkEvent;

public class PhpPsr4NamespaceNotifier {
    private static final NotificationGroup NOTIFICATION_GROUP = NotificationGroup.balloonGroup(
            MessageBundle.message("actions.detect.namespace.roots.notification.id")
    );

    public PhpPsr4NamespaceNotifier() {
    }

    public static void showNotificationByMessageId(final Project project, @NotNull String messageId) {
        NotificationListener listener = null;

        if (PlatformUtils.isPhpStorm()) {
            listener = new NotificationListener() {
                public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, "Directories");
                    notification.expire();
                }
            };
        }

        showNotificationByMessageId(project, messageId, listener);
    }

    public static void showNotificationByMessageId(Project project, @NotNull String messageId, @Nullable NotificationListener listener) {
        String settingsPointer = "actions.detect.namespace.roots.manually.edit.idea";

        if (PlatformUtils.isPhpStorm()) {
            settingsPointer = "actions.detect.namespace.roots.manually.edit";
        }

        showNotification(
            project,
            MessageBundle.message(messageId, MessageBundle.message(settingsPointer)),
            listener
        );
    }

    public static void showNotification(Project project, @NotNull String message, @Nullable NotificationListener listener) {
        Notification notification = new Notification(
            NOTIFICATION_GROUP.getDisplayId(),
            MessageBundle.message("actions.detect.namespace.roots.notification.title"),
            message,
            NotificationType.INFORMATION,
            listener
        );

        Notifications.Bus.notify(notification, project);
    }
}
