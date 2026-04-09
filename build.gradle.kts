import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import java.util.Properties

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.14.0"
}

group = "com.rcleandro.proxyadb"
version = "1.0.1"

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        androidStudio("2024.2.2.14")
        bundledPlugins(
            "org.jetbrains.android",
            "com.intellij.java"
        )
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.1")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "ADB proxy"
        description = """
            <b>ProxyADB</b> — Configure HTTP proxy on connected Android devices via ADB directly from Android Studio.<br/><br/>

            <b>Features:</b>
            <ul>
                <li>🔍 Auto-detects your local machine's IP address</li>
                <li>⚡ One-click enable/disable proxy on connected device</li>
                <li>🔧 Configurable proxy port (default: 8888)</li>
                <li>📡 Real-time ADB connection status</li>
                <li>🖥️ Preview the exact ADB command that will run</li>
                <li>💾 Persistent settings across IDE sessions</li>
            </ul>
        """.trimIndent()
        ideaVersion {
            sinceBuild = "242"
            untilBuild = "261.*"
        }
        changeNotes = """
            <b>1.0.1</b>
            <ul>
                <li>Fixed build error.</li>
                <li>Updated Kotlin JVM plugin to 2.1.0.</li>
                <li>Updated IntelliJ Platform plugin to 2.14.0.</li>
                <li>Updated kotlinx-coroutines-swing to 1.10.1.</li>
                <li>Updated until-build restriction for better compatibility.</li>
            </ul>
            <b>1.0.0</b>
            <ul>
                <li>Initial release</li>
            </ul>
        """.trimIndent()
    }

    signing {
        val certPath = providers.environmentVariable("CERTIFICATE_CHAIN")
            .orElse(localProperties.getProperty("CERTIFICATE_CHAIN") ?: "").get()
        val keyPath = providers.environmentVariable("PRIVATE_KEY")
            .orElse(localProperties.getProperty("PRIVATE_KEY") ?: "").get()

        if (certPath.isNotEmpty()) {
            certificateChain = providers.fileContents(layout.projectDirectory.file(certPath)).asText
        }
        if (keyPath.isNotEmpty()) {
            privateKey = providers.fileContents(layout.projectDirectory.file(keyPath)).asText
        }
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
            .orElse(localProperties.getProperty("PRIVATE_KEY_PASSWORD") ?: "")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
            .orElse(localProperties.getProperty("PUBLISH_TOKEN") ?: "")
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    buildSearchableOptions {
        enabled = false
    }
}
