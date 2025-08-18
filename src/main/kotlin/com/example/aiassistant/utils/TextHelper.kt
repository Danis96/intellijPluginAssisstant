package com.example.aiassistant.utils

/**
 * Centralized text management for the Genie AI Assistant plugin.
 * Provides consistent UI strings and messages across all components.
 * This helper removes hardcoded strings from UI classes for better maintainability.
 */
object TextHelper {
	
	// =================
	// APPLICATION INFO
	// =================
	const val APP_NAME: String = "Genie AI Assistant"
	const val APP_SUBTITLE: String = "Your intelligent coding companion"
	const val SETTINGS_DISPLAY_NAME: String = "AI Assistant"
	
	// =================
	// MAIN UI LABELS
	// =================
	object MainUI {
		const val TITLE: String = "Genie AI Assistant"
		const val SUBTITLE: String = "Your intelligent coding companion"
		const val PROMPT_LABEL: String = "Enter your prompt"
		const val CHAR_COUNTER_FORMAT: String = "%d / 4000 chars"
		const val PLACEHOLDER_TEXT: String = "Ask me anything about your code, request explanations, or describe what you want to build..."
	}
	
	// =================
	// BUTTON LABELS
	// =================
	object Buttons {
		const val SEND: String = "Send"
		const val SENDING: String = "Sending..."
		const val CLEAR_CHAT: String = "Clear Chat"
		const val SETTINGS: String = "Settings"
		const val TEST_CONNECTION: String = "Test Connection"
		const val SHOW: String = "Show"
		const val HIDE: String = "Hide"
	}
	
	// =================
	// TOOLTIPS
	// =================
	object Tooltips {
		const val SEND_BUTTON: String = "Send"
		const val CLEAR_BUTTON: String = "Clear Chat"
		const val SETTINGS_BUTTON: String = "Settings"
	}
	
	// =================
	// WELCOME & MESSAGES
	// =================
	object Welcome {
		const val INITIAL_MESSAGE: String = "Welcome to Genie AI Assistant.\n\nI can help with your coding tasks. Ask about your code, request explanations, or get help with development challenges."
		const val CLEAR_MESSAGE: String = "Chat cleared.\n\nWelcome back! How can I help with your coding today?"
		const val EMPTY_PROMPT_MESSAGE: String = "Please enter a prompt to get started."
	}
	
	// =================
	// SETTINGS PAGE
	// =================
	object Settings {
		const val TITLE: String = "Genie AI Assistant"
		const val HEADER_TEXT: String = "Configure secure access for your assistant."
		const val SUBTITLE_HTML: String = "<html>" +
				"Configure secure access for your assistant.<br><br>" +
				"<span style='font-size:11px;color:#888888;'>" +
				"Your access token is required to connect to the Genie AI service.<br> " +
				"It will be stored securely using your system credential manager and never written to disk in plain text.<br>" +
				"You can test, update, or clear your token at any time. " +
				"Need help? <a href='https://docs.genieai.example.com/auth'>Read the setup guide</a>." +
				"</span>" +
				"</html>"
		
		const val ACCESS_TOKEN_TITLE: String = "Access Token"
		const val REQUIRED_LABEL: String = "REQUIRED"
		const val REQUIRED_LABEL_SUCCESS: String = "Success"
		const val TOKEN_FIELD_LABEL: String = "Enter your access token:"
	}
	
	// =================
	// STATUS MESSAGES
	// =================
	object Status {
		const val IDLE: String = "Idle"
		const val LOADING: String = "Checking"
		const val VALIDATING: String = "Validating"
		const val UNSAVED: String = "Unsaved"
		const val SAVED: String = "Saved"
		const val LOADED: String = "Loaded"
		const val VALID: String = "Valid"
		const val INVALID: String = "Invalid"
		const val ERROR: String = "Error"
		const val MISSING_TOKEN: String = "Missing token"
		
		// Status Messages
		const val ENTER_TOKEN_MESSAGE: String = "Enter a token and click Test Connection to verify"
		const val READY_TO_TEST_MESSAGE: String = "Ready to test connection"
		const val PLEASE_ENTER_TOKEN_MESSAGE: String = "Please enter an access token"
		const val VALIDATING_TOKEN_MESSAGE: String = "Validating token..."
		const val TOKEN_VALID_MESSAGE: String = "Token is valid. You may apply changes."
		const val TOKEN_INVALID_MESSAGE: String = "Invalid token. Please check your credentials."
		const val CONNECTION_FAILED_MESSAGE: String = "Connection failed: %s"
		const val NO_TOKEN_SAVED_MESSAGE: String = "No token saved"
		const val LOADED_TOKEN_MESSAGE: String = "Loaded saved token. You can test or replace it."
		const val TOKEN_CLEARED_MESSAGE: String = "Token cleared"
		const val TOKEN_SAVED_MESSAGE: String = "Token saved. You can test connection."
		const val SETTINGS_RESET_MESSAGE: String = "Settings reset to saved values"
	}
	
	// =================
	// ERROR MESSAGES
	// =================
	object Errors {
		const val INVALID_TOKEN: String = "Error: Invalid access token."
		const val NO_TOKEN_CONFIGURED: String = "Error: No access token configured."
		const val GENERIC_ERROR_FORMAT: String = "Error: %s"
		
		// Notification messages
		const val TOKEN_MISSING_NOTIFICATION: String = "Access token is missing or invalid. Please configure it in Settings → AI Assistant."
	}
	
	// =================
	// CHAT FORMATTING
	// =================
	object Chat {
		object Prefixes {
			const val USER: String = "You: %s"
			const val ASSISTANT: String = "Assistant: %s"
			const val ERROR: String = "[%s] %s"
			const val INFO: String = "[%s] %s"
			const val TIMESTAMP_USER: String = "[%s] %s"
			const val TIMESTAMP_ASSISTANT: String = "[%s] %s"
			const val TIMESTAMP_GENERIC: String = "[%s] %s"
		}
		
		const val TIMESTAMP_FORMAT: String = "HH:mm"
		const val WELCOME_CHECK_TEXT: String = "Welcome to Genie AI Assistant."
	}
	
	// =================
	// NOTIFICATION SETTINGS
	// =================
	object Notifications {
		const val GROUP_ID: String = "Genie AI Assistant Notifications"
		const val WARNING_TITLE: String = "AI Assistant"
	}
	
	// =================
	// API ENDPOINTS (for consistency)
	// =================
	object API {
		const val DEFAULT_AUTH_ENDPOINT: String = "https://your-auth.example.com/verify"
		const val DEFAULT_CHAT_ENDPOINT: String = "https://your-api.example.com/chat"
	}
	
	object Prompts {
		const val CHECK_CODE_ACTION_TEXT: String = "Check Code in Genie Assistant"
		const val CHECK_CODE_TEMPLATE: String =
			"Please review this code. Consider correctness, edge cases, performance, readability, and best practices. " +
			"Suggest improvements and provide refactored examples.\n\nCode:\n%s"
		const val NO_SELECTION_WARNING: String = "No code selected. Select a code block in the editor and try again."
		const val UNABLE_TO_FIND_PANEL_WARNING: String = "Unable to find the assistant panel instance."
		const val UNDERSECTION1_ACTION_TEXT: String = "UNDERSECTION1"
		const val UNDERSECTION2_ACTION_TEXT: String = "UNDERSECTION2"
		const val UNDERSECTION1_TEMPLATE: String =
			"Please perform a quick review of the following code and list the top issues succinctly.\n\nCode:\n%s"
		const val UNDERSECTION2_TEMPLATE: String =
			"Please perform a deep review focused on security, performance, and readability. Provide step-by-step improvements.\n\nCode:\n%s"
	}
	
	// =================
	// HELPER FUNCTIONS
	// =================
	
	/**
	 * Formats a string with the provided arguments
	 * @param format The format string
	 * @param args The arguments to format
	 * @return The formatted string
	 */
	fun format(format: String, vararg args: Any): String {
		return String.format(format, *args)
	}
	
	/**
	 * Gets a chat message prefix formatted with timestamp
	 * @param type The message type ("user", "assistant", "error", "info")
	 * @param timestamp The timestamp string
	 * @param content The message content
	 * @return The formatted message
	 */
	fun formatChatMessage(type: String, timestamp: String, content: String): String {
		return when (type) {
			"user" -> format(Chat.Prefixes.TIMESTAMP_USER, timestamp, content)
			"assistant" -> format(Chat.Prefixes.TIMESTAMP_ASSISTANT, timestamp, content)
			"error" -> format(Chat.Prefixes.ERROR, timestamp, content)
			"info" -> format(Chat.Prefixes.INFO, timestamp, content)
			else -> format(Chat.Prefixes.TIMESTAMP_GENERIC, timestamp, content)
		}
	}
	
	/**
	 * Formats connection error message
	 * @param errorMessage The error message
	 * @return The formatted error message
	 */
	fun formatConnectionError(errorMessage: String): String {
		return format(Status.CONNECTION_FAILED_MESSAGE, errorMessage)
	}
	
	/**
	 * Formats generic error message
	 * @param errorMessage The error message
	 * @return The formatted error message
	 */
	fun formatGenericError(errorMessage: String): String {
		return format(Errors.GENERIC_ERROR_FORMAT, errorMessage)
	}
	
	/**
	 * Formats character counter text
	 * @param count The current character count
	 * @return The formatted counter text
	 */
	fun formatCharCounter(count: Int): String {
		return format(MainUI.CHAR_COUNTER_FORMAT, count)
	}
}