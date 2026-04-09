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
import com.rcleandro.proxyadb.services.NetworkService
import com.rcleandro.proxyadb.settings.ProxySettings
import com.rcleandro.proxyadb.util.ProxyADBBundle
import kotlinx.coroutines.runBlocking

/**
 * Action to enable ADB proxy from the Android Tools menu.
 * Uses the currently configured IP and port from ProxySettings.
 */
class EnableProxyAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val settings = ProxySettings.getInstance()
        val adbService: AdbService = service()

        val ip = if (settings.useCustomIp && settings.customIp.isNotBlank()) {
            settings.customIp
        } else {
            NetworkService.getLocalIpAddress()
        }
        val port = settings.port

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, ProxyADBBundle.message("progress.enable.title")) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = ProxyADBBundle.message("progress.enable.text", "$ip:$port")
                val result = runBlocking { adbService.enableProxy(ip, port) }

                when (result) {
                    is AdbResult.Success -> {
                        notify(project, ProxyADBBundle.message("notification.enable.success", "$ip:$port"), NotificationType.INFORMATION)
                    }
                    is AdbResult.Failure -> {
                        notify(project, ProxyADBBundle.message("notification.enable.failure.regexp", "$ip:$port", result.error), NotificationType.ERROR)
                    }
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
