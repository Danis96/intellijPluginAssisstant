package com.example.aiassistant.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
@State(
    name = "AIAssistant.AccessTokenState",
    storages = [Storage("ai-assistant.xml")]
)
class AccessTokenState : PersistentStateComponent<AccessTokenState.State> {

    data class State(
        var accessToken: String? = null
    )

    private var myState: State = State()

    override fun getState(): State {
        return myState
    }

    override fun loadState(state: State) {
        myState = state
    }

    var accessToken: String?
        get() = myState.accessToken
        set(value: String?) {
            myState.accessToken = value
        }

    companion object {
        fun getInstance(): AccessTokenState {
            val instance: AccessTokenState = service()
            return instance
        }
    }
}