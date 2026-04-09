package com.rcleandro.proxyadb.toolwindow

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.JBUI
import com.rcleandro.proxyadb.services.AdbResult
import com.rcleandro.proxyadb.services.AdbService
import com.rcleandro.proxyadb.services.NetworkService
import com.rcleandro.proxyadb.settings.ProxySettings
import com.rcleandro.proxyadb.util.ProxyADBBundle
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*

/**
 * Custom style Switch component for Swing.
 */
class MaterialSwitch : JComponent() {
    var isSelected: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                repaint()
            }
        }

    private var actionListener: ((Boolean) -> Unit)? = null

    init {
        preferredSize = Dimension(60, 32)
        minimumSize = Dimension(60, 32)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (isEnabled) {
                    isSelected = !isSelected
                    actionListener?.invoke(isSelected)
                }
            }
        })
    }

    fun addActionListener(listener: (Boolean) -> Unit) {
        actionListener = listener
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val w = width
        val h = height
        val margin = 2

        val trackColor = if (!isEnabled) {
            JBColor(0xE0E0E0, 0x3C3F41)
        } else if (isSelected) {
            JBColor(0x4CAF50, 0x66BB6A)
        } else {
            JBColor(0xCED0D6, 0x5E6066)
        }

        val thumbColor = if (!isEnabled) {
            JBColor(0xBDBDBD, 0x5E6060)
        } else {
            JBColor.WHITE
        }

        g2.color = trackColor
        g2.fill(RoundRectangle2D.Float(0f, 0f, w.toFloat(), h.toFloat(), h.toFloat(), h.toFloat()))

        val thumbSize = h - (margin * 2)
        val thumbX = if (isSelected) (w - thumbSize - margin).toFloat() else margin.toFloat()
        g2.color = thumbColor
        g2.fill(Ellipse2D.Float(thumbX, margin.toFloat(), thumbSize.toFloat(), thumbSize.toFloat()))
    }
}

/**
 * Main Tool Window UI panel for ProxyADB.
 */
class ProxyToolWindowPanel(private val project: Project, private val toolWindow: ToolWindow) : Disposable {

    private val adbService: AdbService = service()
    private val settings: ProxySettings = ProxySettings.getInstance()
    private val scope = CoroutineScope(Dispatchers.Swing + SupervisorJob())

    private var isUpdatingSwitch = false

    private val deviceChangeListener = object : AndroidDebugBridge.IDeviceChangeListener {
        override fun deviceConnected(device: IDevice) {
            scope.launch { refreshStatus() }
        }

        override fun deviceDisconnected(device: IDevice) {
            val wasProxyActive = proxySwitch.isSelected
            scope.launch {
                adbService.cleanupHost()
                withContext(Dispatchers.Swing) {
                    if (wasProxyActive) {
                        showNotification(ProxyADBBundle.message("notification.disconnect.cleanup"), NotificationType.WARNING)
                    }
                    refreshStatus()
                }
            }
        }

        override fun deviceChanged(device: IDevice, changeMask: Int) {
            scope.launch { refreshStatus() }
        }
    }

    override fun dispose() {
        AndroidDebugBridge.removeDeviceChangeListener(deviceChangeListener)
        scope.cancel()
    }

    private val adbStatusLabel = JBLabel(ProxyADBBundle.message("toolwindow.status.adb.checking"), AllIcons.General.Warning, SwingConstants.LEFT).apply {
        font = font.deriveFont(Font.BOLD)
    }

    private val deviceStatusLabel = JBLabel(ProxyADBBundle.message("toolwindow.status.device.checking"), AllIcons.General.Information, SwingConstants.LEFT).apply {
        font = font.deriveFont(Font.BOLD)
    }

    private val currentProxyLabel = JBLabel("—").apply {
        font = font.deriveFont(Font.BOLD)
    }

    private val warningTitleLabel = JBLabel("<html>${ProxyADBBundle.message("toolwindow.status.warning.title")}</html>", AllIcons.General.Warning, SwingConstants.LEFT).apply {
        foreground = JBColor.RED
        font = font.deriveFont(Font.BOLD)
        border = JBUI.Borders.emptyTop(10)
    }

    private val warningBodyIntro = JBLabel("<html>${ProxyADBBundle.message("toolwindow.status.warning.body.intro")}</html>")
    
    private val learnMoreLabel = JBLabel("<html><a href=''>${ProxyADBBundle.message("toolwindow.status.warning.learn_more")}</a></html>").apply {
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    }

    private val warningBodyQuestion = JBLabel("<html><b>${ProxyADBBundle.message("toolwindow.status.warning.body.question")}</b></html>").apply {
        border = JBUI.Borders.emptyTop(10)
    }
    private val warningBodyAnswer = JBLabel("<html>${ProxyADBBundle.message("toolwindow.status.warning.body.answer")}</html>")
    private val warningBodyTip = JBLabel("<html><i>${ProxyADBBundle.message("toolwindow.status.warning.body.tip")}</i></html>").apply {
        border = JBUI.Borders.emptyTop(10)
    }

    private val ipField = ComboBox<String>().apply {
        isEditable = true
        NetworkService.getAllLocalIpAddresses().forEach { addItem(it) }
        selectedItem = settings.customIp.ifBlank { NetworkService.getLocalIpAddress() }
    }

    private val portField = ComboBox<String>().apply {
        isEditable = true
        listOf("8888", "8080", "8000", "9090", "1080").forEach { addItem(it) }
        selectedItem = settings.port.toString()
    }

    private val proxySwitch = MaterialSwitch().apply {
        addActionListener { toggleProxy(it) }
        isEnabled = false
    }

    private val clearButton = JButton(ProxyADBBundle.message("toolwindow.actions.clear"), AllIcons.Actions.GC).apply {
        addActionListener { clearAllConfigs() }
    }

    private val refreshButton = JButton(ProxyADBBundle.message("toolwindow.status.refresh"), AllIcons.Actions.Refresh).apply {
        addActionListener { refreshStatus() }
    }

    fun createProxyPanel(): JComponent {
        val proxyPanel = panel {
            row {
                label(ProxyADBBundle.message("toolwindow.status.group")).bold()
                cell(refreshButton).align(AlignX.RIGHT)
            }
            
            separator().bottomGap(BottomGap.SMALL)
            
            row { cell(adbStatusLabel) }
            row { cell(deviceStatusLabel) }
            
            row(ProxyADBBundle.message("toolwindow.status.proxy")) {
                cell(currentProxyLabel).bold()
            }.topGap(TopGap.SMALL).bottomGap(BottomGap.MEDIUM)

            group(ProxyADBBundle.message("toolwindow.config.group")) {
                row(ProxyADBBundle.message("toolwindow.config.ip")) {
                    cell(ipField).align(AlignX.FILL)
                }
                
                row(ProxyADBBundle.message("toolwindow.config.port")) {
                    cell(portField).align(AlignX.FILL)
                }
            }

            separator().topGap(TopGap.MEDIUM)

            row {
                label(ProxyADBBundle.message("toolwindow.config.proxy_enabled")).bold()
                cell(proxySwitch)
                cell(clearButton).align(AlignX.RIGHT)
            }.topGap(TopGap.MEDIUM)

            separator().topGap(TopGap.MEDIUM)

            row { cell(warningTitleLabel).align(AlignX.FILL) }
            row { cell(warningBodyIntro).align(AlignX.FILL) }
            row { cell(learnMoreLabel).align(AlignX.LEFT) }.bottomGap(BottomGap.MEDIUM)
        }.apply {
            border = JBUI.Borders.empty(16)
        }

        learnMoreLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val helpContent = toolWindow.contentManager.findContent(ProxyADBBundle.message("toolwindow.tab.help"))
                if (helpContent != null) {
                    toolWindow.contentManager.setSelectedContent(helpContent)
                }
            }
        })

        refreshStatus()
        AndroidDebugBridge.addDeviceChangeListener(deviceChangeListener)

        return JBScrollPane(proxyPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER).apply {
            border = JBUI.Borders.empty()
            viewport.background = proxyPanel.background
        }
    }

    fun createHelpPanel(): JComponent {
        val helpPanel = panel {
            row { cell(warningBodyQuestion).align(AlignX.FILL) }
            row { cell(warningBodyAnswer).align(AlignX.FILL) }
            row { cell(warningBodyTip).align(AlignX.FILL) }.bottomGap(BottomGap.MEDIUM)
        }.apply {
            border = JBUI.Borders.empty(16)
        }

        return JBScrollPane(helpPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER).apply {
            border = JBUI.Borders.empty()
            viewport.background = helpPanel.background
        }
    }

    private fun clearAllConfigs() {
        scope.launch {
            val result1 = adbService.disableProxy()
            val result2 = adbService.cleanupHost()
            withContext(Dispatchers.Swing) {
                if (result1 is AdbResult.Success && result2 is AdbResult.Success) {
                    showNotification(ProxyADBBundle.message("notification.clear.success"), NotificationType.INFORMATION)
                }
                refreshStatus()
            }
        }
    }

    private fun toggleProxy(enable: Boolean) {
        if (isUpdatingSwitch) return
        
        if (enable) {
            enableProxy()
        } else {
            disableProxy()
        }
    }

    private fun enableProxy() {
        val ip = resolveIp()
        val port = resolvePort()
        setSwitchEnabled(false)
        scope.launch {
            val result = adbService.enableProxy(ip, port)
            withContext(Dispatchers.Swing) {
                handleResult(
                    result,
                    onSuccess = {
                        settings.port = port
                        showNotification(ProxyADBBundle.message("notification.enable.success", "$ip:$port"), NotificationType.INFORMATION)
                        refreshStatus()
                    },
                    onFailure = { err ->
                        showNotification(ProxyADBBundle.message("notification.enable.failure.regexp", "$ip:$port", err), NotificationType.ERROR)
                        updateSwitchState(false)
                        setSwitchEnabled(true)
                    }
                )
            }
        }
    }

    private fun disableProxy() {
        setSwitchEnabled(false)
        scope.launch {
            val result = adbService.disableProxy()
            withContext(Dispatchers.Swing) {
                handleResult(
                    result,
                    onSuccess = {
                        showNotification(ProxyADBBundle.message("notification.disable.success"), NotificationType.INFORMATION)
                        refreshStatus()
                    },
                    onFailure = { err ->
                        showNotification(ProxyADBBundle.message("notification.disable.failure", err), NotificationType.ERROR)
                        updateSwitchState(true)
                        setSwitchEnabled(true)
                    }
                )
            }
        }
    }

    private fun refreshStatus() {
        refreshButton.isEnabled = false
        scope.launch {
            val adbAvailable = adbService.isAdbAvailable()
            val devicesResult = if (adbAvailable) adbService.getDevices() else null
            val proxyResult = if (adbAvailable) adbService.getCurrentProxy() else null

            withContext(Dispatchers.Swing) {
                updateAdbStatus(adbAvailable)
                updateDeviceStatus(devicesResult)
                updateCurrentProxy(proxyResult)
                refreshButton.isEnabled = true
            }
        }
    }

    private fun updateAdbStatus(available: Boolean) {
        if (available) {
            adbStatusLabel.text = ProxyADBBundle.message("toolwindow.status.adb.available")
            adbStatusLabel.foreground = JBColor(0x2E7D32, 0x66BB6A)
            adbStatusLabel.icon = AllIcons.General.InspectionsOK
        } else {
            adbStatusLabel.text = ProxyADBBundle.message("toolwindow.status.adb.not_found")
            adbStatusLabel.foreground = JBColor(0xC62828, 0xEF5350)
            adbStatusLabel.icon = AllIcons.General.Error
            setSwitchEnabled(false)
        }
    }

    private fun updateDeviceStatus(result: AdbResult?) {
        if (result == null) {
            deviceStatusLabel.text = ProxyADBBundle.message("toolwindow.status.device.no_adb")
            deviceStatusLabel.foreground = JBColor.GRAY
            deviceStatusLabel.icon = AllIcons.General.Warning
            setSwitchEnabled(false)
            return
        }

        val output = result.outputOrNull ?: ""
        val deviceLines = output.lines()
            .drop(1)
            .filter { it.contains("device") || it.contains("offline") }

        if (deviceLines.isEmpty()) {
            deviceStatusLabel.text = ProxyADBBundle.message("toolwindow.status.device.none")
            deviceStatusLabel.foreground = JBColor(0xE65100, 0xFFA726)
            deviceStatusLabel.icon = AllIcons.General.Warning
            setSwitchEnabled(false)
        } else {
            val count = deviceLines.size
            deviceStatusLabel.text = ProxyADBBundle.message("toolwindow.status.device.connected", count)
            deviceStatusLabel.foreground = JBColor(0x2E7D32, 0x66BB6A)
            deviceStatusLabel.icon = AllIcons.General.InspectionsOK
            setSwitchEnabled(true)
        }
    }

    private fun updateCurrentProxy(result: AdbResult?) {
        val output = result?.outputOrNull?.trim()
        
        if (output == null) {
            currentProxyLabel.text = "—"
            return
        }

        val isProxyDisabled = output.isBlank() || output == "null" || output == ":0"
        
        currentProxyLabel.text = if (isProxyDisabled) {
            ProxyADBBundle.message("toolwindow.status.proxy.none")
        } else {
            output
        }
        
        updateSwitchState(!isProxyDisabled)

        if (!isProxyDisabled) {
            val currentIp = NetworkService.getLocalIpAddress()
            if (!output.startsWith(currentIp)) {
                scope.launch {
                    val cleanupResult = adbService.disableProxy()
                    if (cleanupResult is AdbResult.Success) {
                        withContext(Dispatchers.Swing) {
                            showNotification(ProxyADBBundle.message("notification.cleanup.success"), NotificationType.INFORMATION)
                            refreshStatus()
                        }
                    }
                }
            }
        }
    }

    private fun updateSwitchState(enabled: Boolean) {
        isUpdatingSwitch = true
        proxySwitch.isSelected = enabled
        isUpdatingSwitch = false
    }

    private fun resolveIp(): String {
        val selectedIp = ipField.selectedItem?.toString()?.trim() ?: ""
        if (selectedIp.isNotBlank()) {
            settings.customIp = selectedIp
            settings.useCustomIp = true
        }
        return selectedIp.ifBlank { NetworkService.getLocalIpAddress() }
    }

    private fun resolvePort(): Int {
        val selectedPort = portField.selectedItem?.toString()?.trim() ?: "8888"
        val port = selectedPort.toIntOrNull() ?: 8888
        settings.port = port
        return port
    }

    private fun setSwitchEnabled(enabled: Boolean) {
        proxySwitch.isEnabled = enabled
    }

    private fun handleResult(
        result: AdbResult,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        when (result) {
            is AdbResult.Success -> onSuccess()
            is AdbResult.Failure -> onFailure(result.error)
        }
    }

    private fun showNotification(message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("ProxyADB.Notifications")
            .createNotification(ProxyADBBundle.message("notification.title"), message, type)
            .notify(project)
    }
}
