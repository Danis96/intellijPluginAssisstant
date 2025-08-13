package com.example.aiassistant.utils

import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project


fun notifyWarn(project: Project, title: String, content: String) {
    val group: NotificationGroup? = NotificationGroupManager.getInstance().getNotificationGroup(TextHelper.Notifications.GROUP_ID)
    val notification: Notification =
        group?.createNotification(title, content, NotificationType.WARNING)
            ?: Notification(TextHelper.Notifications.GROUP_ID, title, content, NotificationType.WARNING)
    Notifications.Bus.notify(notification, project)
}