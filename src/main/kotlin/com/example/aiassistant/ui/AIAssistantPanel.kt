package com.example.aiassistant.ui

import com.example.aiassistant.codeReplacement.CodeReplacementUI
import com.example.aiassistant.codeReplacement.MockAIResponseService
import com.example.aiassistant.network.AIClient
import com.example.aiassistant.security.AccessTokenVerifier
import com.example.aiassistant.settings.AccessTokenState
import com.example.aiassistant.utils.uiHelpers.BorderHelper
import com.example.aiassistant.utils.uiHelpers.ColorHelper
import com.example.aiassistant.utils.TextHelper
import com.example.aiassistant.utils.notifyWarn
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.AsyncProcessIcon
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import com.example.aiassistant.ui.CodeDiffViewer

class AIAssistantPanel(
	private val project: Project
) : JPanel(BorderLayout()) {

	private val promptField: JBTextArea = JBTextArea()
	private val sendButton: JButton = JButton()
	private val clearButton: JButton = JButton()
	private val settingsButton: JButton = JButton()
	private val progressIcon: AsyncProcessIcon = AsyncProcessIcon("sending")
	private val charCounter: JLabel = JLabel(TextHelper.formatCharCounter(0))
	private val promptPanel: JPanel = JPanel(BorderLayout())

	// New chat container for message bubbles
	private val chatContainer: JPanel = JPanel()
	private val chatScrollPane: JBScrollPane = JBScrollPane(chatContainer)

	// Replace with your endpoints
	private val verifier: AccessTokenVerifier = AccessTokenVerifier(TextHelper.API.DEFAULT_AUTH_ENDPOINT)
	private val client: AIClient = AIClient(TextHelper.API.DEFAULT_CHAT_ENDPOINT)

	// Add a property to track the last sent code
	private var lastSentCode: String = ""

	init {
		background = ColorHelper.backgroundColor
		setupUI()
		setupActions()
		setupKeyBindings()
		setupDocumentListeners()
		ensureAuthorizedOrRedirect()
	}

	private fun setupUI(): Unit {
		val headerPanel: JPanel = createHeaderPanel()
		add(headerPanel, BorderLayout.NORTH)

		val outputPanel: JPanel = createOutputPanel()
		add(outputPanel, BorderLayout.CENTER)

		val inputPanel: JPanel = createPromptInputPanel()
		add(inputPanel, BorderLayout.SOUTH)
	}

	private fun createHeaderPanel(): JPanel {
		val headerPanel: JPanel = JPanel(BorderLayout())
		headerPanel.background = ColorHelper.cardColor
		headerPanel.border = CompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorHelper.borderColor),
			EmptyBorder(16, 20, 16, 20)
		)

		val titlePanel: JPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
		titlePanel.background = ColorHelper.cardColor

		val titleLabel: JLabel = JLabel(TextHelper.MainUI.TITLE)
		titleLabel.font = Font(Font.SANS_SERIF, Font.BOLD, 18)
		titleLabel.foreground = ColorHelper.primaryColor

		val subtitleLabel: JLabel = JLabel(TextHelper.MainUI.SUBTITLE)
		subtitleLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
		subtitleLabel.foreground = ColorHelper.textSecondaryColor

		val titleContainer: JPanel = JPanel()
		titleContainer.layout = BoxLayout(titleContainer, BoxLayout.Y_AXIS)
		titleContainer.background = ColorHelper.cardColor
		titleContainer.add(titleLabel)
		titleContainer.add(Box.createVerticalStrut(4))
		titleContainer.add(subtitleLabel)

		titlePanel.add(titleContainer)
		headerPanel.add(titlePanel, BorderLayout.WEST)

		val actionsPanel: JPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0))
		actionsPanel.background = ColorHelper.cardColor

		styleModernButton(settingsButton, TextHelper.Buttons.SETTINGS, TextHelper.Tooltips.SETTINGS_BUTTON, false)
		// styleModernButton(clearButton, TextHelper.Buttons.CLEAR_CHAT, TextHelper.Tooltips.CLEAR_BUTTON, false)

		progressIcon.isVisible = false

		actionsPanel.add(settingsButton)
		// actionsPanel.add(clearButton)
		actionsPanel.add(progressIcon)

		headerPanel.add(actionsPanel, BorderLayout.EAST)

		return headerPanel
	}

	private fun createOutputPanel(): JPanel {
		val outputPanel: JPanel = JPanel(BorderLayout())
		outputPanel.background = ColorHelper.backgroundColor
		outputPanel.border = EmptyBorder(12, 20, 12, 20)

		chatContainer.layout = BoxLayout(chatContainer, BoxLayout.Y_AXIS)
		chatContainer.background = ColorHelper.backgroundColor
		chatContainer.border = EmptyBorder(8, 0, 8, 0)
		
		// Add initial welcome message
		addMessageBubble(
			role = "system",
			content = TextHelper.Welcome.INITIAL_MESSAGE,
			type = "info"
		)

		chatScrollPane.border = CompoundBorder(
			BorderHelper.Styles.cardBorder(12),
			EmptyBorder(0, 0, 0, 0)
		)
		chatScrollPane.background = ColorHelper.cardColor
		chatScrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
		chatScrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
		
		// Enable smooth scrolling
		chatScrollPane.getVerticalScrollBar().setUnitIncrement(16)

		outputPanel.add(chatScrollPane, BorderLayout.CENTER)
		return outputPanel
	}

	private fun createPromptInputPanel(): JPanel {
		val mainInputPanel: JPanel = JPanel(BorderLayout())
		mainInputPanel.background = ColorHelper.backgroundColor
		mainInputPanel.border = EmptyBorder(0, 20, 20, 20)

		promptPanel.background = ColorHelper.cardColor
		promptPanel.border = CompoundBorder(
			BorderHelper.Styles.cardBorderPrimary(12),
			EmptyBorder(16, 16, 16, 16)
		)

		val inputHeaderPanel: JPanel = JPanel(BorderLayout())
		inputHeaderPanel.background = ColorHelper.cardColor

		val promptLabel: JLabel = JLabel(TextHelper.MainUI.PROMPT_LABEL)
		promptLabel.font = Font(Font.SANS_SERIF, Font.BOLD, 14)
		promptLabel.foreground = ColorHelper.primaryColor

		charCounter.font = Font(Font.SANS_SERIF, Font.PLAIN, 11)
		charCounter.foreground = ColorHelper.textSecondaryColor

		inputHeaderPanel.add(promptLabel, BorderLayout.WEST)
		inputHeaderPanel.add(charCounter, BorderLayout.EAST)

		promptField.lineWrap = true
		promptField.wrapStyleWord = true
		promptField.font = Font(Font.SANS_SERIF, Font.PLAIN, 14)
		promptField.background = ColorHelper.cardColor
		promptField.border = EmptyBorder(8, 0, 8, 0)
		promptField.rows = 6
		promptField.minimumSize = Dimension(100, 60)
		promptField.preferredSize = Dimension(100, 120)

		setupPlaceholderBehavior()

		val inputScroll: JBScrollPane = JBScrollPane(promptField)
		inputScroll.border = null
		inputScroll.background = ColorHelper.cardColor
		inputScroll.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
		inputScroll.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
		inputScroll.preferredSize = Dimension(100, 160)
		inputScroll.maximumSize = Dimension(Int.MAX_VALUE, 200)

		val buttonPanel: JPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0))
		buttonPanel.background = ColorHelper.cardColor

		styleModernButton(sendButton, TextHelper.Buttons.SEND, TextHelper.Tooltips.SEND_BUTTON, true)
		sendButton.preferredSize = Dimension(100, 36)
		
		// Add test button for quick testing
		val testButton: JButton = JButton("Test")
		styleModernButton(testButton, "Test", "Test code replacement", false)
		testButton.addActionListener { testCodeReplacement() }

		buttonPanel.add(testButton)
		buttonPanel.add(sendButton)

		promptPanel.add(inputHeaderPanel, BorderLayout.NORTH)
		promptPanel.add(Box.createVerticalStrut(8), BorderLayout.WEST)
		promptPanel.add(inputScroll, BorderLayout.CENTER)
		promptPanel.add(buttonPanel, BorderLayout.SOUTH)

		mainInputPanel.add(promptPanel, BorderLayout.CENTER)
		return mainInputPanel
	}

	private fun styleModernButton(button: JButton, label: String, tooltip: String, isPrimary: Boolean): Unit {
		button.text = label
		button.toolTipText = tooltip
		button.font = Font(Font.SANS_SERIF, if (isPrimary) Font.BOLD else Font.PLAIN, 12)
		button.isFocusPainted = false
		button.isBorderPainted = false
		button.isContentAreaFilled = false
		button.isOpaque = false
		button.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

		if (isPrimary) {
			button.background = ColorHelper.Buttons.Primary.background
			button.foreground = ColorHelper.Buttons.Primary.foreground
			button.border = CompoundBorder(
				BorderHelper.Styles.buttonBackgroundPrimary(8),
				EmptyBorder(8, 16, 8, 16)
			)
		} else {
			button.background = ColorHelper.Buttons.Secondary.background
			button.foreground = ColorHelper.Buttons.Secondary.foreground
			button.border = CompoundBorder(
				BorderHelper.Styles.buttonBackgroundSecondary(8),
				EmptyBorder(6, 12, 6, 12)
			)
		}

		button.addMouseListener(object : MouseAdapter() {
			override fun mouseEntered(e: MouseEvent): Unit {
				if (isPrimary) {
					button.background = ColorHelper.Buttons.Primary.hover
					button.border = CompoundBorder(
						BorderHelper.createRoundedBackgroundBorder(
							ColorHelper.Buttons.Primary.hover,
							null,
							0,
							8
						),
						EmptyBorder(8, 16, 8, 16)
					)
				} else {
					button.background = ColorHelper.Buttons.Primary.background
					button.foreground = ColorHelper.Buttons.Primary.foreground
					button.border = CompoundBorder(
						BorderHelper.createRoundedBackgroundBorder(
							ColorHelper.Buttons.Primary.background,
							null,
							0,
							8
						),
						EmptyBorder(6, 12, 6, 12)
					)
				}
				button.repaint()
			}

			override fun mouseExited(e: MouseEvent): Unit {
				if (isPrimary) {
					button.background = ColorHelper.Buttons.Primary.background
					button.border = CompoundBorder(
						BorderHelper.Styles.buttonBackgroundPrimary(8),
						EmptyBorder(8, 16, 8, 16)
					)
				} else {
					button.background = ColorHelper.Buttons.Secondary.background
					button.foreground = ColorHelper.Buttons.Secondary.foreground
					button.border = CompoundBorder(
						BorderHelper.Styles.buttonBackgroundSecondary(8),
						EmptyBorder(6, 12, 6, 12)
					)
				}
				button.repaint()
			}
		})
	}

	private fun setupPlaceholderBehavior(): Unit {
		promptField.foreground = ColorHelper.textSecondaryColor
		promptField.text = TextHelper.MainUI.PLACEHOLDER_TEXT

		promptField.addFocusListener(object : java.awt.event.FocusAdapter() {
			override fun focusGained(e: java.awt.event.FocusEvent): Unit {
				if (promptField.text == TextHelper.MainUI.PLACEHOLDER_TEXT) {
					promptField.text = ""
					promptField.foreground = JBColor.foreground()
				}
			}

			override fun focusLost(e: java.awt.event.FocusEvent): Unit {
				if (promptField.text.trim().isEmpty()) {
					promptField.foreground = ColorHelper.textSecondaryColor
					promptField.text = TextHelper.MainUI.PLACEHOLDER_TEXT
				}
			}
		})
	}

	private fun setupActions(): Unit {
		sendButton.addActionListener { onSend() }
		clearButton.addActionListener { onClear() }
		settingsButton.addActionListener { onOpenSettings() }
	}

	private fun setupKeyBindings(): Unit {
		// Enter to send; Shift+Enter for newline
		promptField.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send")
		promptField.actionMap.put("send", object : AbstractAction() {
			override fun actionPerformed(e: ActionEvent): Unit {
				onSend()
			}
		})

		// Ctrl+L to clear
		promptField.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK), "clear")
		promptField.actionMap.put("clear", object : AbstractAction() {
			override fun actionPerformed(e: ActionEvent): Unit {
				onClear()
			}
		})
	}

	private fun setupDocumentListeners(): Unit {
		promptField.document.addDocumentListener(object : DocumentListener {
			override fun insertUpdate(e: DocumentEvent): Unit {
				updateCharCounter()
				updatePromptPanelBorder()
			}
			override fun removeUpdate(e: DocumentEvent): Unit {
				updateCharCounter()
				updatePromptPanelBorder()
			}
			override fun changedUpdate(e: DocumentEvent): Unit {
				updateCharCounter()
				updatePromptPanelBorder()
			}
		})
		updateCharCounter()
	}

	private fun updatePromptPanelBorder(): Unit {
		val hasContent: Boolean = promptField.text.isNotBlank() &&
			promptField.text != TextHelper.MainUI.PLACEHOLDER_TEXT

		val border = if (hasContent) {
			BorderHelper.Styles.cardBorderAccent(12)
		} else {
			BorderHelper.Styles.cardBorderPrimary(12)
		}

		promptPanel.border = CompoundBorder(
			border,
			EmptyBorder(16, 16, 16, 16)
		)
		promptPanel.repaint()
	}

	private fun ensureAuthorizedOrRedirect(): Unit {
		val verifier: AccessTokenVerifier = AccessTokenVerifier(TextHelper.API.DEFAULT_AUTH_ENDPOINT)
		AccessTokenState.getInstance().isTokenValidAsync(verifier).thenAccept { isValid: Boolean ->
			if (!isValid) {
				SwingUtilities.invokeLater {
					notifyWarn(
						project,
						TextHelper.Notifications.WARNING_TITLE,
						TextHelper.Errors.TOKEN_MISSING_NOTIFICATION
					)
					ShowSettingsUtil.getInstance().showSettingsDialog(project, TextHelper.SETTINGS_DISPLAY_NAME)
				}
			}
		}
	}

	private fun setSending(sending: Boolean): Unit {
		sendButton.isEnabled = !sending
		promptField.isEnabled = !sending
		progressIcon.isVisible = sending

		if (sending) {
			sendButton.text = TextHelper.Buttons.SENDING
			sendButton.background = ColorHelper.textSecondaryColor
			sendButton.border = CompoundBorder(
				BorderHelper.createRoundedBackgroundBorder(
					ColorHelper.textSecondaryColor,
					null,
					0,
					8
				),
				EmptyBorder(8, 16, 8, 16)
			)
		} else {
			sendButton.text = TextHelper.Buttons.SEND
			sendButton.background = ColorHelper.Buttons.Primary.background
			sendButton.border = CompoundBorder(
				BorderHelper.Styles.buttonBackgroundPrimary(8),
				EmptyBorder(8, 16, 8, 16)
			)
		}
		sendButton.repaint()
	}

	private fun onSend(): Unit {
		val verifier: AccessTokenVerifier = AccessTokenVerifier(TextHelper.API.DEFAULT_AUTH_ENDPOINT)
		AccessTokenState.getInstance().isTokenValidAsync(verifier).thenAccept { isValid: Boolean ->
			if (!isValid) {
				SwingUtilities.invokeLater {
					appendOutput(TextHelper.Errors.INVALID_TOKEN, "error")
					ensureAuthorizedOrRedirect()
				}
				return@thenAccept
			}

			AccessTokenState.getInstance().getAccessTokenAsync().thenAccept { token: String? ->
				if (token.isNullOrBlank()) {
					SwingUtilities.invokeLater {
						appendOutput(TextHelper.Errors.NO_TOKEN_CONFIGURED, "error")
						ensureAuthorizedOrRedirect()
					}
					return@thenAccept
				}

				val prompt: String = promptField.text.trim()

				if (prompt.isBlank() || prompt == TextHelper.MainUI.PLACEHOLDER_TEXT) {
					SwingUtilities.invokeLater {
						appendOutput(TextHelper.Welcome.EMPTY_PROMPT_MESSAGE, "info")
					}
					return@thenAccept
				}

				SwingUtilities.invokeLater {
					// Show user bubble with raw content (no prefixes)
					appendOutput(prompt, "user")
					setSending(true)
					promptField.text = ""
					updateCharCounter()
					updatePromptPanelBorder()
				}

				ApplicationManager.getApplication().executeOnPooledThread {
					try {
						// Check if this is a mock scenario for testing
						val reply: String = if (shouldUseMockResponse(prompt)) {
							MockAIResponseService.generateMockResponse(extractCodeFromPrompt(prompt))
						} else {
							client.chat(token, prompt)
						}
						
						SwingUtilities.invokeLater {
							// Show assistant bubble with raw reply
							appendOutput(reply, "assistant")
							
							setSending(false)
						}
					} catch (e: Exception) {
						val message: String = TextHelper.formatGenericError(e.message ?: "Unknown error")
						SwingUtilities.invokeLater {
							appendOutput(message, "error")
							setSending(false)
						}
					}
				}
			}
		}
	}

	private fun shouldUseMockResponse(prompt: String): Boolean {
		// Enable mock responses for testing - you can customize this logic
		val shouldUseMock = prompt.contains("test:") || 
			   prompt.contains("calculateArea") ||
			   prompt.contains("for (i in 0..10)") ||
			   prompt.contains("fun divide") ||
			   prompt.contains("```kotlin")
		
		println("DEBUG: shouldUseMockResponse - prompt: '$prompt', result: $shouldUseMock")
		return shouldUseMock
	}

	private fun extractCodeFromPrompt(prompt: String): String {
		// Extract code from markdown code blocks
		val codeBlockPattern: java.util.regex.Pattern = java.util.regex.Pattern.compile(
			"```(?:kotlin|java|python|javascript|typescript)?\n(.*?)\n```",
			java.util.regex.Pattern.DOTALL
		)
		val matcher = codeBlockPattern.matcher(prompt)
		return if (matcher.find()) matcher.group(1).trim() else prompt
	}

	private fun containsCodeReplacements(response: String): Boolean {
		val hasCodeBlocks = response.contains("```")
		val hasKeywords = response.contains("diff") || 
						  response.contains("improved", ignoreCase = true) || 
						  response.contains("replace", ignoreCase = true) ||
						  response.contains("better", ignoreCase = true) ||
						  response.contains("fix", ignoreCase = true)
		val result = hasCodeBlocks && hasKeywords
		
		// Debug logging
		println("DEBUG: containsCodeReplacements - hasCodeBlocks: $hasCodeBlocks, hasKeywords: $hasKeywords, result: $result")
		println("DEBUG: Response preview: ${response.take(200)}...")
		
		return result
	}

	private fun showCodeReplacementUI(aiResponse: String) {
		println("DEBUG: showCodeReplacementUI called")
		
		// Get current editor
		val editor: Editor? = getCurrentEditor()
		println("DEBUG: Current editor: $editor")
		
		// Create and show code replacement UI
		val replacementUI: CodeReplacementUI = CodeReplacementUI(project, editor)
		replacementUI.showReplacements(aiResponse)
		
		// Add to chat as a special component
		addCodeReplacementBubble(replacementUI)
		println("DEBUG: Code replacement bubble added")
	}

	private fun getCurrentEditor(): Editor? {
		val fileEditorManager: FileEditorManager = FileEditorManager.getInstance(project)
		val selectedEditor = fileEditorManager.selectedEditor
		return if (selectedEditor is TextEditor) {
			selectedEditor.editor
		} else {
			null
		}
	}

	private fun addCodeReplacementBubble(replacementUI: CodeReplacementUI) {
		val wrapper: JPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 4))
		wrapper.isOpaque = false
		wrapper.background = ColorHelper.backgroundColor

		// Container for the replacement UI
		val container: JPanel = JPanel(BorderLayout())
		container.background = ColorHelper.surfaceColor
		container.border = CompoundBorder(
			BorderHelper.createRoundedBackgroundBorder(
				ColorHelper.surfaceColor, 
				ColorHelper.withAlpha(ColorHelper.primaryColor, 0.3f), 
				2, 
				12
			),
			EmptyBorder(12, 16, 12, 16)
		)
		
		container.add(replacementUI, BorderLayout.CENTER)
		
		// Set reasonable size
		val containerWidth: Int = 600
		container.minimumSize = Dimension(containerWidth, 0)
		container.preferredSize = Dimension(containerWidth, container.preferredSize.height)
		container.maximumSize = Dimension(containerWidth, Int.MAX_VALUE)

		wrapper.add(container)

		chatContainer.add(wrapper)
		chatContainer.add(Box.createVerticalStrut(8))
		chatContainer.revalidate()
		chatContainer.repaint()
		
		scrollToBottom()
	}

	private fun onClear(): Unit {
		chatContainer.removeAll()
		chatContainer.revalidate()
		chatContainer.repaint()
		addMessageBubble(
			role = "system",
			content = TextHelper.Welcome.CLEAR_MESSAGE,
			type = "info"
		)
		scrollToBottom()
	}

	private fun onOpenSettings(): Unit {
		ShowSettingsUtil.getInstance().showSettingsDialog(project, TextHelper.SETTINGS_DISPLAY_NAME)
	}

	private fun updateCharCounter(): Unit {
		val len: Int = promptField.text.length
		val actualLen: Int = if (promptField.text == TextHelper.MainUI.PLACEHOLDER_TEXT) 0 else len

		charCounter.text = TextHelper.formatCharCounter(actualLen)

		if (actualLen > 3500) {
			charCounter.foreground = ColorHelper.errorColor
		} else if (actualLen > 3000) {
			charCounter.foreground = ColorHelper.warningColor
		} else {
			charCounter.foreground = ColorHelper.textSecondaryColor
		}
	}

	private fun appendOutput(text: String, type: String = "normal"): Unit {
		val timestamp: String = java.time.LocalTime.now().format(
			java.time.format.DateTimeFormatter.ofPattern(TextHelper.Chat.TIMESTAMP_FORMAT)
		)
		val role: String = when (type) {
			"user" -> "user"
			"assistant" -> "assistant"
			"error" -> "error"
			"info" -> "info"
			else -> "system"
		}
		addMessageBubble(role = role, content = text, type = type, timestamp = timestamp)
		scrollToBottom()
	}

	private fun addMessageBubble(
		role: String,
		content: String,
		type: String,
		timestamp: String = java.time.LocalTime.now().format(
			java.time.format.DateTimeFormatter.ofPattern(TextHelper.Chat.TIMESTAMP_FORMAT)
		)
	): Unit {
		// Parse content for images
		val imageParser = ImageParser()
		val images: List<ImageContent> = imageParser.parseImages(content)
		val textContent: String = if (images.isNotEmpty()) {
			imageParser.removeImageMarkdown(content)
		} else {
			content
		}

		val wrapper: JPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 4))
		wrapper.isOpaque = false
		wrapper.background = ColorHelper.backgroundColor

		// Create enhanced content component with image support
		val contentComponent: JComponent = if (images.isNotEmpty()) {
			createMultiMediaContentPanel(textContent, images, role)
		} else if (role == "assistant" && textContent.contains("```")) {
			CodeDiffViewer(textContent, project, lastSentCode)
		} else {
			// Regular text area for non-code content
			val textArea = JTextArea(textContent)
			textArea.lineWrap = true
			textArea.wrapStyleWord = true
			textArea.isEditable = false
			textArea.font = Font(Font.SANS_SERIF, Font.PLAIN, 13)
			textArea.foreground = JBColor.foreground()
			textArea.isOpaque = false
			textArea.columns = 60
			textArea.rows = 0
			
			// Calculate proper height for the content
			val fontMetrics = textArea.getFontMetrics(textArea.font)
			val lineHeight = fontMetrics.height
			val lines = textContent.split('\n').sumOf { line ->
				Math.max(1, (fontMetrics.stringWidth(line) / (60 * fontMetrics.charWidth('M'))) + 1)
			}
			
			val preferredWidth = 500
			val preferredHeight = lines * lineHeight + 10
			textArea.preferredSize = Dimension(preferredWidth, preferredHeight)
			textArea.minimumSize = Dimension(preferredWidth, preferredHeight)
			textArea
		}

		// Timestamp
		val tsLabel: JLabel = JLabel(timestamp)
		tsLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 10)
		tsLabel.foreground = ColorHelper.textSecondaryColor
		tsLabel.horizontalAlignment = if (role == "user") SwingConstants.RIGHT else SwingConstants.LEFT

		// Bubble background color
		val bubbleBg: Color = when (role) {
			"user" -> ColorHelper.withAlpha(ColorHelper.primaryColor, 0.12f)
			"assistant" -> ColorHelper.surfaceColor
			"error" -> ColorHelper.withAlpha(ColorHelper.errorColor, 0.10f)
			"info" -> ColorHelper.withAlpha(ColorHelper.infoColor, 0.10f)
			else -> ColorHelper.cardColor
		}

		// Bubble panel that expands to content
		val bubble: JPanel = JPanel(BorderLayout())
		bubble.background = bubbleBg
		bubble.border = CompoundBorder(
			BorderHelper.createRoundedBackgroundBorder(bubbleBg, ColorHelper.withAlpha(ColorHelper.borderColor, 0.3f), 1, 12),
			EmptyBorder(12, 16, 12, 16)
		)
		bubble.add(contentComponent, BorderLayout.CENTER)

		// Container for bubble and timestamp
		val container: JPanel = JPanel()
		container.layout = BoxLayout(container, BoxLayout.Y_AXIS)
		container.isOpaque = false
		container.add(bubble)
		container.add(Box.createVerticalStrut(4))
		container.add(tsLabel)

		// Let container size itself based on content - NO height restrictions
		val bubbleWidth: Int = if (images.isNotEmpty()) 700 else 650  // Wider for images
		container.preferredSize = Dimension(bubbleWidth, container.preferredSize.height)
		container.maximumSize = Dimension(bubbleWidth, Int.MAX_VALUE)
		container.minimumSize = Dimension(bubbleWidth, 40)

		wrapper.add(container)

		chatContainer.add(wrapper)
		chatContainer.add(Box.createVerticalStrut(12))
		chatContainer.revalidate()
		chatContainer.repaint()
	}

	private fun createMultiMediaContentPanel(
		text: String, 
		images: List<ImageContent>, 
		role: String
	): JPanel {
		val mainPanel = JPanel()
		mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)
		mainPanel.isOpaque = false
		
		// Add text content if present
		if (text.isNotBlank()) {
			val textComponent: JComponent = if (role == "assistant" && text.contains("```")) {
				CodeDiffViewer(text, project, lastSentCode)
			} else {
				val textArea = JTextArea(text)
				textArea.lineWrap = true
				textArea.wrapStyleWord = true
				textArea.isEditable = false
				textArea.font = Font(Font.SANS_SERIF, Font.PLAIN, 13)
				textArea.foreground = JBColor.foreground()
				textArea.isOpaque = false
				textArea.border = EmptyBorder(0, 0, 8, 0)
				textArea
			}
			mainPanel.add(textComponent)
			
			if (images.isNotEmpty()) {
				mainPanel.add(Box.createVerticalStrut(12))
			}
		}
		
		// Add images
		images.forEach { imageContent: ImageContent ->
			val imageComponent: ImageDisplayComponent = ImageDisplayComponent(imageContent, project)
			imageComponent.maximumSize = Dimension(Int.MAX_VALUE, imageComponent.preferredSize.height)
			mainPanel.add(imageComponent)
			mainPanel.add(Box.createVerticalStrut(8))
		}
		
		return mainPanel
	}

	private fun scrollToBottom(): Unit {
		SwingUtilities.invokeLater {
			val bar: JScrollBar = chatScrollPane.verticalScrollBar
			bar.value = bar.maximum
		}
	}

	fun insertPreparedPrompt(text: String, replace: Boolean): Unit {
		val isPlaceholder: Boolean = promptField.text == TextHelper.MainUI.PLACEHOLDER_TEXT
		if (replace || isPlaceholder) {
			promptField.text = text
		} else {
			val current: String = promptField.text
			promptField.text = if (current.isBlank()) text else "$current\n\n$text"
		}
		updateCharCounter()
		updatePromptPanelBorder()
		promptField.requestFocusInWindow()
		promptField.caretPosition = promptField.text.length
	}
	
	private fun testCodeReplacement(): Unit {
		println("DEBUG: Test button clicked")
		
		// Simulate different types of test prompts
		val testPrompts: List<String> = listOf(
			// Code improvement test
			"""
Please review this code:
```kotlin
fun calculateArea(radius: Double): Double {
    return 3.14 * radius * radius
}
```
""".trim(),
			
			// Diagram test
			"Can you show me a diagram of the authentication flow?",
			
			// Image test
			"Show me an example of image support in the chat"
		)
		
		// Cycle through test prompts
		val promptIndex: Int = (System.currentTimeMillis() % testPrompts.size).toInt()
		val testPrompt: String = testPrompts[promptIndex]
		
		// Track the original code if it's a code prompt
		if (testPrompt.contains("```kotlin")) {
			lastSentCode = extractCodeFromPrompt(testPrompt)
		}
		
		// Generate appropriate response
		val mockResponse: String = if (testPrompt.contains("diagram") || testPrompt.contains("image")) {
			MockAIResponseService.generateImageResponse(testPrompt)
		} else {
			MockAIResponseService.generateMockResponse(testPrompt)
		}
		
		SwingUtilities.invokeLater {
			// Add user message
			appendOutput(testPrompt, "user")
			
			// Add assistant response with potential images
			appendOutput(mockResponse, "assistant")
		}
	}
}