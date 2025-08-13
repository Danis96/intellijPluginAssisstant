package com.example.aiassistant.ui

import com.example.aiassistant.network.AIClient
import com.example.aiassistant.security.AccessTokenVerifier
import com.example.aiassistant.settings.AccessTokenState
import com.example.aiassistant.utils.BorderHelper
import com.example.aiassistant.utils.ColorHelper
import com.example.aiassistant.utils.TextHelper
import com.example.aiassistant.utils.notifyWarn
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class AIAssistantPanel(
    private val project: Project
) : JPanel(BorderLayout()) {

    private val promptField: JBTextArea = JBTextArea()
    private val sendButton: JButton = JButton()
    private val clearButton: JButton = JButton()
    private val settingsButton: JButton = JButton()
    private val progressIcon: AsyncProcessIcon = AsyncProcessIcon("sending")
    private val charCounter: JLabel = JLabel(TextHelper.formatCharCounter(0))
    private val outputArea: JBTextArea = JBTextArea()
    private val promptPanel: JPanel = JPanel(BorderLayout())

    // Replace with your endpoints
    private val verifier: AccessTokenVerifier = AccessTokenVerifier(TextHelper.API.DEFAULT_AUTH_ENDPOINT)
    private val client: AIClient = AIClient(TextHelper.API.DEFAULT_CHAT_ENDPOINT)

    init {
        background = ColorHelper.backgroundColor
        setupUI()
        setupActions()
        setupKeyBindings()
        setupDocumentListeners()
        ensureAuthorizedOrRedirect()
    }

    private fun setupUI(): Unit {
        // Main header with gradient-like effect
        val headerPanel: JPanel = createHeaderPanel()
        add(headerPanel, BorderLayout.NORTH)

        // Chat output area with modern styling
        val outputPanel: JPanel = createOutputPanel()
        add(outputPanel, BorderLayout.CENTER)

        // Distinguished prompt input area
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

        // Main title with icon
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

        // Action buttons with modern styling
        val actionsPanel: JPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0))
        actionsPanel.background = ColorHelper.cardColor

        styleModernButton(settingsButton, "⚙️", TextHelper.Tooltips.SETTINGS_BUTTON, false)
        styleModernButton(clearButton, "🗑️", TextHelper.Tooltips.CLEAR_BUTTON, false)

        progressIcon.isVisible = false
        
        actionsPanel.add(settingsButton)
        actionsPanel.add(clearButton)
        actionsPanel.add(progressIcon)

        headerPanel.add(actionsPanel, BorderLayout.EAST)

        return headerPanel
    }

    private fun createOutputPanel(): JPanel {
        val outputPanel: JPanel = JPanel(BorderLayout())
        outputPanel.background = ColorHelper.backgroundColor
        outputPanel.border = EmptyBorder(12, 20, 12, 20)

        // Output area with modern card styling
        outputArea.isEditable = false
        outputArea.background = ColorHelper.cardColor
        outputArea.font = Font(Font.MONOSPACED, Font.PLAIN, 13)
        outputArea.lineWrap = true
        outputArea.wrapStyleWord = true
        outputArea.border = EmptyBorder(16, 16, 16, 16)

        val outputScroll: JBScrollPane = JBScrollPane(outputArea)
        outputScroll.border = CompoundBorder(
            BorderHelper.Styles.cardBorder(12),
            EmptyBorder(0, 0, 0, 0)
        )
        outputScroll.background = ColorHelper.cardColor
        outputScroll.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED

        // Welcome message
        outputArea.text = TextHelper.Welcome.INITIAL_MESSAGE

        outputPanel.add(outputScroll, BorderLayout.CENTER)
        return outputPanel
    }

    private fun createPromptInputPanel(): JPanel {
        val mainInputPanel: JPanel = JPanel(BorderLayout())
        mainInputPanel.background = ColorHelper.backgroundColor
        mainInputPanel.border = EmptyBorder(0, 20, 20, 20)

        // Prompt input card with distinguished styling
        promptPanel.background = ColorHelper.cardColor
        promptPanel.border = CompoundBorder(
            BorderHelper.Styles.cardBorderPrimary(12),
            EmptyBorder(16, 16, 16, 16)
        )

        // Input header
        val inputHeaderPanel: JPanel = JPanel(BorderLayout())
        inputHeaderPanel.background = ColorHelper.cardColor

        val promptLabel: JLabel = JLabel(TextHelper.MainUI.PROMPT_LABEL)
        promptLabel.font = Font(Font.SANS_SERIF, Font.BOLD, 14)
        promptLabel.foreground = ColorHelper.primaryColor

        charCounter.font = Font(Font.SANS_SERIF, Font.PLAIN, 11)
        charCounter.foreground = ColorHelper.textSecondaryColor

        inputHeaderPanel.add(promptLabel, BorderLayout.WEST)
        inputHeaderPanel.add(charCounter, BorderLayout.EAST)

        // Prompt text area with modern styling
        promptField.lineWrap = true
        promptField.wrapStyleWord = true
        promptField.font = Font(Font.SANS_SERIF, Font.PLAIN, 14)
        promptField.background = ColorHelper.cardColor
        promptField.border = EmptyBorder(8, 0, 8, 0)
        promptField.minimumSize = Dimension(100, 60)
        promptField.preferredSize = Dimension(100, 80)

        // Add placeholder-like behavior
        setupPlaceholderBehavior()

        val inputScroll: JBScrollPane = JBScrollPane(promptField)
        inputScroll.border = null
        inputScroll.background = ColorHelper.cardColor
        inputScroll.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED

        // Action buttons panel
        val buttonPanel: JPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0))
        buttonPanel.background = ColorHelper.cardColor

        styleModernButton(sendButton, "🚀", TextHelper.Tooltips.SEND_BUTTON, true)
        sendButton.preferredSize = Dimension(100, 36)

        buttonPanel.add(sendButton)

        // Assemble the prompt panel
        promptPanel.add(inputHeaderPanel, BorderLayout.NORTH)
        promptPanel.add(Box.createVerticalStrut(8), BorderLayout.WEST)
        promptPanel.add(inputScroll, BorderLayout.CENTER)
        promptPanel.add(buttonPanel, BorderLayout.SOUTH)

        mainInputPanel.add(promptPanel, BorderLayout.CENTER)
        return mainInputPanel
    }

    private fun styleModernButton(button: JButton, icon: String, text: String, isPrimary: Boolean): Unit {
        button.text = if (isPrimary) "$icon $text" else icon
        button.toolTipText = text
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

        // Hover effects
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
                    appendOutput(TextHelper.format(TextHelper.Chat.Prefixes.USER, prompt), "user")
                    setSending(true)
                    promptField.text = ""
                    updateCharCounter()
                    updatePromptPanelBorder()
                }

                ApplicationManager.getApplication().executeOnPooledThread {
                    try {
                        val reply: String = client.chat(token, prompt)
                        SwingUtilities.invokeLater {
                            appendOutput(TextHelper.format(TextHelper.Chat.Prefixes.ASSISTANT, reply), "assistant")
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

    private fun onClear(): Unit {
        outputArea.text = TextHelper.Welcome.CLEAR_MESSAGE
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
        val current: String = outputArea.text
        val timestamp: String = java.time.LocalTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern(TextHelper.Chat.TIMESTAMP_FORMAT)
        )
        
        val formattedText: String = TextHelper.formatChatMessage(type, timestamp, text)
        
        val next: String = if (current.contains(TextHelper.Chat.WELCOME_CHECK_TEXT)) {
            current + formattedText
        } else {
            formattedText
        }
        
        outputArea.text = next
        outputArea.caretPosition = outputArea.document.length
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
}
