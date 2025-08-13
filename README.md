## Genie AI Assistant — IntelliJ IDEA Plugin

An IntelliJ IDEA plugin that adds a right-side tool window to chat with an AI assistant. It supports secure access-token storage, token verification against a backend, and a simple chat API integration.

- **Plugin ID**: `org.example.aiassistant`
- **Tool Window**: "AI Assistant"
- **Settings Page**: "AI Assistant"
- **Minimum IntelliJ build**: `242` (IDEA 2024.2)
- **Kotlin**: `2.2.0`
- **JDK**: `17`

### Contents
- [Features](#features)
- [Screens and UX](#screens-and-ux)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Build and Run](#build-and-run)
- [Install the Plugin Into IDE](#install-the-plugin-into-ide)
- [Configuration (Access Token)](#configuration-access-token)
- [Backend Endpoints](#backend-endpoints)
- [API Contracts](#api-contracts)
- [Logging and Troubleshooting](#logging-and-troubleshooting)
- [Development Notes](#development-notes)
- [Release and Publishing](#release-and-publishing)
- [Roadmap Ideas](#roadmap-ideas)
- [License](#license)

### Features
- **Tool Window chat UI**: Compose prompts and view responses inline.
- **Access token storage**: Persisted via IntelliJ services; stored across IDE restarts.
- **Token verification**: Validates the token against a verification endpoint before sending prompts.
- **Notifications**: In-IDE warnings for missing or invalid tokens.

### Screens and UX
- Open from: Tools menu action "Open Genie AI Assistant", or via the dedicated Tool Window on the right.
- Settings: Preferences/Settings → "AI Assistant" to configure the access token.

### Architecture
- **Tool Window**: `com.example.aiassistant.toolwindow.AIAssistantToolWindowFactory` renders the main `AIAssistantPanel`.
- **UI Panel**: `com.example.aiassistant.ui.AIAssistantPanel` contains the chat text areas, send button, and progress indicator.
- **Networking**:
  - `com.example.aiassistant.network.AIClient` performs POST requests to the chat endpoint.
  - `com.example.aiassistant.security.AccessTokenVerifier` validates tokens against the verification endpoint.
- **Settings & Persistence**:
  - `com.example.aiassistant.settings.AccessTokenState` stores the token using IntelliJ persistent state.
  - `com.example.aiassistant.settings.AIAssistantConfigurable` provides the Settings page UI.
- **Notifications**: `com.example.aiassistant.utils.notificationUtil.kt` shows balloon notifications.
- **Plugin metadata**: `src/main/resources/META-INF/plugin.xml` registers tool window, settings configurable, action, and notification group.

### Project Structure
```
intellijPluginAssisstant/
  build.gradle.kts
  settings.gradle.kts
  src/
    main/
      kotlin/
        com/example/aiassistant/
          actions/
            ActionOnOpenAssistant.kt
          network/
            AIClient.kt
          security/
            AccessTokenVerifier.kt
          settings/
            AccessTokenState.kt
            AIAssistantConfigurable.kt
          toolwindow/
            AIAssistantToolWindowFactory.kt
          ui/
            AIAssistantPanel.kt
          utils/
            notificationUtil.kt
      resources/
        META-INF/
          plugin.xml
        icons/
          ai.png
```

### Prerequisites
- JDK 17 installed and selected in your shell/IDE.
- IntelliJ IDEA 2024.2+ (Community or Ultimate) for development.
- macOS, Linux, or Windows.

### Build and Run
- Verify Gradle tasks and IntelliJ plugin settings in `build.gradle.kts`:
  - Kotlin JVM plugin `2.2.0`
  - IntelliJ Gradle plugin `1.17.4`
  - IDE type `IC` and version `2024.2`
  - JVM toolchain set to 17

- Run the plugin in a development IDE instance:
```bash
./gradlew runIde
```

- Build a distributable ZIP (under `build/distributions/*.zip`):
```bash
./gradlew buildPlugin
```

- Run tests (if/when added):
```bash
./gradlew test
```

### Install the Plugin Into IDE
- From source via Gradle: use `runIde` for a sandboxed IDE.
- From disk: after `./gradlew buildPlugin`, install the ZIP in IntelliJ → Settings → Plugins → Gear icon → "Install Plugin from Disk…".

### Configuration (Access Token)
- Open Preferences/Settings → "AI Assistant".
- Enter your access token and apply.
- The plugin verifies the token on first use; invalid or missing tokens trigger a notification and redirect to Settings.

Code references:
- Settings page: `com.example.aiassistant.settings.AIAssistantConfigurable`
- Persistent storage: `com.example.aiassistant.settings.AccessTokenState`
- Verification: `com.example.aiassistant.security.AccessTokenVerifier`

### Backend Endpoints
The current endpoints in `AIAssistantPanel` are placeholders. Replace them with your backend URLs.

Kotlin example with explicit types:
```kotlin
private val verifier: com.example.aiassistant.security.AccessTokenVerifier =
    com.example.aiassistant.security.AccessTokenVerifier(
        verificationEndpoint = "https://your-auth.example.com/verify"
    )

private val client: com.example.aiassistant.network.AIClient =
    com.example.aiassistant.network.AIClient(
        apiEndpoint = "https://your-api.example.com/chat"
    )
```

If you prefer configuration via environment variables or IDE settings, you can refactor `AIAssistantPanel` to read from `System.getenv` or extend `AIAssistantConfigurable` with additional fields.

### API Contracts
- Token verification: `GET /verify`
  - Headers: `Authorization: Bearer <token>`
  - Response: `200 OK` for valid tokens; any other status considered invalid.

- Chat: `POST /chat`
  - Headers:
    - `Content-Type: application/json`
    - `Authorization: Bearer <token>`
  - Request body example:
```json
{"prompt": "Explain IntelliJ plugin PersistentStateComponent with examples."}
```
  - Response: `2xx` with a textual body is treated as success. Non-2xx becomes an error message in the UI.

cURL examples:
```bash
# Verify token
curl -i -H "Authorization: Bearer $TOKEN" https://your-auth.example.com/verify

# Send chat prompt
curl -i -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"prompt":"Hello from IntelliJ!"}' \
     https://your-api.example.com/chat
```

### Logging and Troubleshooting
- Ensure the verification and chat endpoints are reachable from your machine.
- If behind a proxy, configure JVM proxy settings or IntelliJ HTTP proxy.
- The verifier includes a development fallback that treats `SECRET_DEV_TOKEN` and `123sqa!` as valid if the server is unreachable. Remove this branch for production in `AccessTokenVerifier`:
```kotlin
if (token == "SECRET_DEV_TOKEN" || token == "123sqa!") {
    return true
}
```
- Check IDE logs: Help → Show Log in Finder/Explorer.
- Rebuild the plugin if Gradle dependencies or the IntelliJ version change.

### Development Notes
- UI updates are marshalled back to the EDT using `SwingUtilities.invokeLater`.
- Long-running network calls are executed on pooled threads via `ApplicationManager.getApplication().executeOnPooledThread { ... }`.
- Token is stored via `PersistentStateComponent` in `AccessTokenState`. Example usage with explicit types:
```kotlin
val state: com.example.aiassistant.settings.AccessTokenState =
    com.example.aiassistant.settings.AccessTokenState.getInstance()
val token: String? = state.accessToken
state.accessToken = token?.trim()
```
- The settings page display name is "AI Assistant". The tool window and notifications use the label "Genie AI Assistant".

### Release and Publishing
- Update metadata in `src/main/resources/META-INF/plugin.xml`:
  - `<id>`, `<name>`, `<vendor>`, `<description>`, `<change-notes>`, `<idea-version since-build=...>`.
- Update `group` and `version` in `build.gradle.kts`.
- Build plugin ZIP: `./gradlew buildPlugin`.
- Test in multiple IDE versions if you plan to support a range (adjust `intellij.version`).
- Publish to JetBrains Marketplace (requires account and token) or distribute the ZIP directly.

### Roadmap Ideas
- Streaming responses in the UI
- Conversation history and message roles
- Model and temperature controls
- Multi-project awareness and context injection
- Endpoint and model configuration via Settings
- Better error and rate-limit handling

### License
Specify your license here (e.g., MIT). If omitted, the project is considered proprietary by default.


