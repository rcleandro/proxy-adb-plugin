package com.rcleandro.proxyadb.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.rcleandro.proxyadb.services.NetworkService
import com.rcleandro.proxyadb.util.ProxyADBBundle

/**
 * Settings page accessible via Preferences → Tools → ProxyADB
 */
class ProxySettingsConfigurable : BoundConfigurable(ProxyADBBundle.message("settings.title")) {

    private val settings = ProxySettings.getInstance()

    override fun createPanel(): DialogPanel = panel {
        group(ProxyADBBundle.message("settings.config.group")) {
            row(ProxyADBBundle.message("settings.config.port")) {
                intTextField(range = 1024..65535)
                    .bindIntText(settings::port)
                    .comment(ProxyADBBundle.message("settings.config.port.comment"))
                    .focused()
            }

            row {
                checkBox(ProxyADBBundle.message("settings.config.custom_ip.use"))
                    .bindSelected(settings::useCustomIp)
            }

            row(ProxyADBBundle.message("settings.config.custom_ip.label")) {
                textField()
                    .bindText(settings::customIp)
                    .validationOnApply {
                        if (settings.useCustomIp && !NetworkService.isValidIp(it.text)) {
                            error(ProxyADBBundle.message("settings.config.custom_ip.error"))
                        } else null
                    }
                    .comment(ProxyADBBundle.message("settings.config.custom_ip.comment"))
            }
        }

        group(ProxyADBBundle.message("settings.info.group")) {
            row(ProxyADBBundle.message("settings.info.auto_detected")) {
                label(NetworkService.getLocalIpAddress())
                    .comment(ProxyADBBundle.message("settings.info.auto_detected.comment"))
            }
            row(ProxyADBBundle.message("settings.info.all_ips")) {
                label(NetworkService.getAllLocalIpAddresses().joinToString(", "))
            }
        }
    }
}
