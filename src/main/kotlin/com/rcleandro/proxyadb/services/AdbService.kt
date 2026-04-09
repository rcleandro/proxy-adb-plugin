package com.rcleandro.proxyadb.services

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service responsible for executing ADB shell commands.
 * Registered as an application-level service in plugin.xml.
 */
@Service(Service.Level.APP)
class AdbService {

    private val log = logger<AdbService>()

    /**
     * Enables HTTP proxy on the connected Android device.
     * Runs: adb shell settings put global http_proxy <ip>:<port>
     */
    suspend fun enableProxy(ip: String, port: Int): AdbResult = withContext(Dispatchers.IO) {
        runAdbCommand("shell", "settings", "put", "global", "http_proxy", "$ip:$port")
    }

    /**
     * Disables HTTP proxy on the connected Android device.
     * Runs: adb shell settings put global http_proxy :0
     */
    suspend fun disableProxy(): AdbResult = withContext(Dispatchers.IO) {
        runAdbCommand("shell", "settings", "put", "global", "http_proxy", ":0")
    }

    /**
     * Checks the current proxy setting on the device.
     * Runs: adb shell settings get global http_proxy
     */
    suspend fun getCurrentProxy(): AdbResult = withContext(Dispatchers.IO) {
        runAdbCommand("shell", "settings", "get", "global", "http_proxy")
    }

    /**
     * Lists connected ADB devices.
     */
    suspend fun getDevices(): AdbResult = withContext(Dispatchers.IO) {
        runAdbCommand("devices")
    }

    /**
     * Checks if ADB is installed and available in PATH.
     */
    suspend fun isAdbAvailable(): Boolean = withContext(Dispatchers.IO) {
        runAdbCommand("version").isSuccess
    }

    /**
     * Removes all reverse port forwards on the host.
     */
    suspend fun cleanupHost(): AdbResult = withContext(Dispatchers.IO) {
        runAdbCommand("reverse", "--remove-all")
    }

    private fun runAdbCommand(vararg args: String): AdbResult {
        val commandLine = GeneralCommandLine(resolveAdbPath())
            .withParameters(*args)
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)

        log.info("Running ADB: ${commandLine.commandLineString}")

        return try {
            val output = ExecUtil.execAndGetOutput(commandLine, 5000)
            if (output.exitCode == 0) {
                AdbResult.Success(output.stdout.trim())
            } else {
                AdbResult.Failure("ADB exited with code ${output.exitCode}: ${output.stderr}")
            }
        } catch (e: Exception) {
            log.warn("ADB command failed: ${args.joinToString(" ")}", e)
            AdbResult.Failure("Failed to run ADB: ${e.message}")
        }
    }

    /**
     * Tries to resolve the 'adb' executable path using the Android SDK configured in the IDE.
     */
    private fun resolveAdbPath(): String {
        val home = System.getProperty("user.home")
        val candidates = mutableListOf<String>()

        findInPath()?.let { return it }

        candidates.addAll(listOf(
            "$home/Library/Android/sdk/platform-tools/adb",
            "$home/AppData/Local/Android/Sdk/platform-tools/adb.exe",
            "$home/Android/Sdk/platform-tools/adb",
        ))

        return candidates.firstOrNull { java.io.File(it).exists() } ?: "adb"
    }

    private fun findInPath(): String? {
        val executable = "adb"
        val path = System.getenv("PATH") ?: return null
        val separator = if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) ";" else ":"
        return path.split(separator)
            .map { java.io.File(it, executable) }
            .firstOrNull { it.exists() && it.canExecute() }
            ?.absolutePath
    }
}

/**
 * Sealed class representing the result of an ADB operation.
 */
sealed class AdbResult {
    data class Success(val output: String) : AdbResult()
    data class Failure(val error: String) : AdbResult()

    val isSuccess: Boolean get() = this is Success
    val outputOrNull: String? get() = (this as? Success)?.output
}
