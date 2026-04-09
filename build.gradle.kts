import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import java.util.Properties

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.24"
    id("org.jetbrains.intellij.platform") version "2.3.0"
}

group = "com.rcleandro.proxyadb"
version = "1.0.0"

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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")
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
        ideaVersion {
            sinceBuild = "242"
            untilBuild = "251.*"
        }
        changeNotes = """
            <ul>
                <li>1.0.0 - Initial release</li>
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
            ide(IntelliJPlatformType.AndroidStudio, "2024.2.2.14")
        }
    }
}
