package com.rcleandro.proxyadb.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.rcleandro.proxyadb.util.ProxyADBBundle

/**
 * Factory that creates the ProxyADB Tool Window content.
 * Registered in plugin.xml under <toolWindow>.
 */
class ProxyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = ProxyToolWindowPanel(project, toolWindow)
        val contentFactory = ContentFactory.getInstance()

        val proxyContent = contentFactory.createContent(
            panel.createProxyPanel(),
            ProxyADBBundle.message("toolwindow.tab.proxy"),
            false
        )
        proxyContent.setDisposer(panel)

        val helpContent = contentFactory.createContent(
            panel.createHelpPanel(),
            ProxyADBBundle.message("toolwindow.tab.help"),
            false
        )

        toolWindow.contentManager.addContent(proxyContent)
        toolWindow.contentManager.addContent(helpContent)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}
