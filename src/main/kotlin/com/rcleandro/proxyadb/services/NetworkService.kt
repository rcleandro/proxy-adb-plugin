package com.rcleandro.proxyadb.services

import java.net.NetworkInterface

/**
 * Utility object to resolve the local machine's IP address.
 * Mirrors the IP detection logic from the KMP ProxyADB desktop app.
 */
object NetworkService {

    /**
     * Returns the first non-loopback IPv4 address found on any active network interface.
     * Prefers Wi-Fi / Ethernet interfaces. Falls back to 127.0.0.1 if none found.
     */
    fun getLocalIpAddress(): String {
        return try {
            NetworkInterface.getNetworkInterfaces()
                ?.asSequence()
                ?.filter { it.isUp && !it.isLoopback && !it.isVirtual }
                ?.flatMap { iface ->
                    iface.inetAddresses.asSequence().map { addr -> iface to addr }
                }
                ?.filter { (_, addr) ->
                    !addr.isLoopbackAddress &&
                            !addr.isLinkLocalAddress &&
                            addr.hostAddress.contains('.')
                }?.maxByOrNull { (iface, _) ->
                    when {
                        iface.name.startsWith("en") -> 3
                        iface.name.startsWith("wl") -> 2
                        iface.name.startsWith("eth") -> 1
                        else -> 0
                    }
                }
                ?.second
                ?.hostAddress
                ?: "127.0.0.1"
        } catch (_: Exception) {
            "127.0.0.1"
        }
    }

    /**
     * Returns all available local IPv4 addresses for multi-NIC machines.
     */
    fun getAllLocalIpAddresses(): List<String> {
        return try {
            NetworkInterface.getNetworkInterfaces()
                ?.asSequence()
                ?.filter { it.isUp && !it.isLoopback && !it.isVirtual }
                ?.flatMap { it.inetAddresses.asSequence() }
                ?.filter { !it.isLoopbackAddress && !it.isLinkLocalAddress && it.hostAddress.contains('.') }
                ?.map { it.hostAddress }
                ?.toList()
                ?: listOf("127.0.0.1")
        } catch (_: Exception) {
            listOf("127.0.0.1")
        }
    }

    fun isValidIp(ip: String): Boolean {
        val parts = ip.split(".")
        if (parts.size != 4) return false
        return parts.all { part ->
            part.toIntOrNull()?.let { it in 0..255 } ?: false
        }
    }
}
