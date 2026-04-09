package com.rcleandro.proxyadb.settings

import com.intellij.openapi.components.*

/**
 * Persistent application-level settings for ProxyADB.
 * Values are stored in ProxyADBSettings.xml inside the IDE config directory.
 */
@State(
    name = "ProxyADBSettings",
    storages = [Storage("ProxyADBSettings.xml")]
)
@Service(Service.Level.APP)
class ProxySettings : PersistentStateComponent<ProxySettings.State> {

    data class State(
        var port: Int = 8888,
        var customIp: String = "",
        var useCustomIp: Boolean = false
    )

    private var myState = State()

    var port: Int
        get() = myState.port
        set(value) { myState = myState.copy(port = value) }

    var customIp: String
        get() = myState.customIp
        set(value) { myState = myState.copy(customIp = value) }

    var useCustomIp: Boolean
        get() = myState.useCustomIp
        set(value) { myState = myState.copy(useCustomIp = value) }

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        fun getInstance(): ProxySettings = service()
    }
}
