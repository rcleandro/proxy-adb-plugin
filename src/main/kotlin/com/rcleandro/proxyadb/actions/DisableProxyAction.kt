package com.rcleandro.proxyadb.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.rcleandro.proxyadb.services.AdbResult
import com.rcleandro.proxyadb.services.AdbService
import com.rcleandro.proxyadb.util.ProxyADBBundle
import kotlinx.coroutines.runBlocking

/**
 * Action to disable ADB proxy from the Android Tools menu.
 */
class DisableProxyAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val adbService: AdbService = service()

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, ProxyADBBundle.message("progress.disable.title")) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = ProxyADBBundle.message("progress.disable.text")
                val result = runBlocking { adbService.disableProxy() }

                when (result) {
                    is AdbResult.Success -> notify(project, ProxyADBBundle.message("notification.disable.success"), NotificationType.INFORMATION)
                    is AdbResult.Failure -> notify(project, ProxyADBBundle.message("notification.disable.failure", result.error), NotificationType.ERROR)
                }
            }
        })
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    private fun notify(project: com.intellij.openapi.project.Project, message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("ProxyADB.Notifications")
            .createNotification(ProxyADBBundle.message("notification.title"), message, type)
            .notify(project)
    }
}
