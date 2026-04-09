# ProxyADB Plugin 🔌

Plugin for **Android Studio** that simplifies HTTP proxy configuration on Android devices via ADB — directly from the IDE, no external tools needed.

## ✨ Features

- 🔍 **Automatic IP detection** — Identifies your local network IP (Wi-Fi/Ethernet)
- ⚡ **One-click toggle** — Enable/disable proxy on the connected device
- 🔧 **Configurable port** — Default 8888, customizable via Settings
- 📡 **Real-time status** — Monitors ADB connection and connected devices
- 💾 **Persistent settings** — Keeps your preferences across IDE sessions
- 🔔 **IDE notifications** — Visual success/error feedback via balloon notifications

## 🚀 How to use

### Prerequisites
- Android Studio Ladybug (2024.2) or higher
- ADB installed and configured in the system PATH
- Android device connected via USB with **USB Debugging** enabled

### Tool Window
1. Open the **ADB proxy** panel in the Android Studio bottom bar
2. Check the detected IP and adjust the port if needed
3. Click **✅ Enable Proxy** to activate the proxy on the device
4. Click **❌ Disable Proxy** to deactivate it

### Quick actions
Access via **Tools → Android → Enable ADB proxy / Disable ADB proxy**

### Settings
Access via **Preferences → Tools → ProxyADB** to:
- Set a default port
- Use a custom IP instead of the auto-detected one

## 🛠️ Tech stack

| Technology                         | Usage                                        |
|------------------------------------|----------------------------------------------|
| Kotlin                             | Main language                                |
| IntelliJ Platform SDK              | Plugin APIs (Tool Window, Actions, Services) |
| Kotlin UI DSL                      | Declarative Swing interface                  |
| Kotlinx Coroutines                 | Async operations (ADB commands)              |
| IntelliJ Platform Gradle Plugin v2 | Build and packaging                          |

## 🏗️ Project structure
```
src/main/kotlin/com/rcleandro/proxyadb/
├── actions/
│   ├── EnableProxyAction.kt          # Action in Tools → Android menu
│   └── DisableProxyAction.kt
├── services/
│   ├── AdbService.kt                 # ADB command execution
│   └── NetworkService.kt             # Local IP detection
├── settings/
│   ├── ProxySettings.kt              # Persistent state (XML)
│   └── ProxySettingsConfigurable.kt  # Preferences page
├── toolwindow/
│   ├── ProxyToolWindowFactory.kt     # Panel registration
│   └── ProxyToolWindowPanel.kt       # Main UI
└── util/
    └── ProxyADBBundle.kt             # I18n support
```
## 🔨 How to build

```bash
# Opens Android Studio with the plugin installed (development mode)
./gradlew runIde

# Generates the installable .zip
./gradlew buildPlugin
```

The `.zip` generated in `build/distributions/` can be installed via:
**Settings → Plugins → ⚙️ → Install Plugin from Disk...**

## 📦 Compatibility

| IDE            | Version                    |
|----------------|----------------------------|
| Android Studio | Ladybug (2024.2) and above |
| IntelliJ IDEA  | 2024.2 and above           |

---

Developed by [rcleandro](https://github.com/rcleandro)