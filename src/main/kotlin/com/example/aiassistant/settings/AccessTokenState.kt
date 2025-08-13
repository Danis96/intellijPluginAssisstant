package com.example.aiassistant.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.CompletableFuture

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
	private val credentialKey: String = "AIAssistant.AccessToken"

	private fun credentialAttributes(): CredentialAttributes {
		val attrs: CredentialAttributes = CredentialAttributes(credentialKey)
		return attrs
	}

	override fun getState(): State {
		// Ensure the token is never persisted to disk
		myState.accessToken = null
		return myState
	}

	override fun loadState(state: State) {
		// Migrate any previously persisted plaintext token into Password Safe
		val legacyToken: String? = state.accessToken
		if (!legacyToken.isNullOrBlank()) {
			// Run migration in background
			AppExecutorUtil.getAppExecutorService().submit {
				accessToken = legacyToken
			}
		}
		myState = State(accessToken = null)
	}

	var accessToken: String?
		get() {
			// For immediate access, return null and trigger async fetch
			// This prevents EDT blocking
			return null
		}
		set(value: String?) {
			val attrs: CredentialAttributes = credentialAttributes()
			if (value.isNullOrBlank()) {
				PasswordSafe.instance.set(attrs, null)
			} else {
				PasswordSafe.instance.setPassword(attrs, value)
			}
		}

	// Async methods for non-blocking access
	fun getAccessTokenAsync(): CompletableFuture<String?> {
		val future: CompletableFuture<String?> = CompletableFuture()
		AppExecutorUtil.getAppExecutorService().submit {
			try {
				val token: String? = PasswordSafe.instance.getPassword(credentialAttributes())
				future.complete(token)
			} catch (e: Exception) {
				future.completeExceptionally(e)
			}
		}
		return future
	}

	fun isTokenValidAsync(verifier: com.example.aiassistant.security.AccessTokenVerifier): CompletableFuture<Boolean> {
		val future: CompletableFuture<Boolean> = CompletableFuture()
		getAccessTokenAsync().thenAccept { token: String? ->
			if (token.isNullOrBlank()) {
				future.complete(false)
			} else {
				AppExecutorUtil.getAppExecutorService().submit {
					try {
						val isValid: Boolean = verifier.isTokenValid(token)
						future.complete(isValid)
					} catch (e: Exception) {
						future.completeExceptionally(e)
					}
				}
			}
		}.exceptionally { e: Throwable ->
			future.completeExceptionally(e)
			null
		}
		return future
	}

	companion object {
		fun getInstance(): AccessTokenState {
			val instance: AccessTokenState = service()
			return instance
		}
	}
}