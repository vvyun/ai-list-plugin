package com.vyunfei.ailist.notify;

import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.openapi.project.Project;

import static com.intellij.notification.NotificationType.ERROR;
import static com.intellij.notification.NotificationType.INFORMATION;
import static com.intellij.notification.NotificationType.WARNING;

public class Notifications {

    private static final NotificationGroup BALLOON_GROUP =
            NotificationGroupManager.getInstance().getNotificationGroup("AIListIDEABalloonGroup");
    private static final NotificationGroup LOG_ONLY_GROUP =
            NotificationGroupManager.getInstance().getNotificationGroup("AIListIDEALogOnlyGroup");

    private Notifications() {
    }

    public static void showInfo(final Project project,
                                final String infoText,
                                final NotificationAction action) {
        BALLOON_GROUP
                .createNotification("", infoText, INFORMATION)
                .addAction(action)
                .notify(project);
    }

    public static void showWarning(final Project project,
                                   final String warningText) {
        BALLOON_GROUP
                .createNotification("", warningText, WARNING)
                .notify(project);
    }

    public static void showError(final Project project,
                                 final String errorText) {
        BALLOON_GROUP
                .createNotification("", errorText, ERROR)
                .notify(project);
    }

    public static void showException(final Project project,
                                     final Throwable t) {
        LOG_ONLY_GROUP
                .createNotification("", t.getLocalizedMessage(), ERROR)
                .notify(project);

    }

}
