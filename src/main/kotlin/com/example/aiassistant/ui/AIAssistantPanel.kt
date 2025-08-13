package com.example.aiassistant.ui

import com.example.aiassistant.network.AIClient
import com.example.aiassistant.security.AccessTokenVerifier
import com.example.aiassistant.settings.AccessTokenState
import com.example.aiassistant.utils.notifyWarn
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class AIAssistantPanel(
	private val project: Project
) : JPanel(BorderLayout()) {

	private val promptField: JBTextArea = JBTextArea()
	private val sendButton: JButton = JButton("Send")
	private val clearButton: JButton = JButton("Clear")
	private val settingsButton: JButton = JButton("Settings")
	private val progressIcon: AsyncProcessIcon = AsyncProcessIcon("sending")
	private val charCounter: JLabel = JLabel("0 chars")
	private val outputArea: JBTextArea = JBTextArea()

	// Replace with your endpoints
	private val verifier: AccessTokenVerifier = AccessTokenVerifier("https://your-auth.example.com/verify")
	private val client: AIClient = AIClient("https://your-api.example.com/chat")

	init {
		// Header + actions toolbar
		val header: JLabel = JLabel(
			"<html><b>Genie AI Assistant</b><br/><br/>Enter a prompt below. Configure access token in Settings → Genie AI Assistant.</html>"
		)
		header.border = JBUI.Borders.empty(6, 6, 0, 6)

		progressIcon.isVisible = false

		val actionsPanel: JPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0))
		actionsPanel.border = JBUI.Borders.empty(6, 6, 0, 6)
		actionsPanel.add(charCounter)
//		actionsPanel.add(clearButton)
//		actionsPanel.add(settingsButton)
		actionsPanel.add(progressIcon)

		val headerWrapper: JPanel = JPanel(BorderLayout())
		headerWrapper.add(header, BorderLayout.CENTER)
		headerWrapper.add(actionsPanel, BorderLayout.EAST)
		add(headerWrapper, BorderLayout.NORTH)

		// Conversation output
		outputArea.isEditable = false
		val outputScroll: JBScrollPane = JBScrollPane(outputArea)
		outputScroll.border = JBUI.Borders.empty(4)
		add(outputScroll, BorderLayout.CENTER)

		// Input area
		val inputPanel: JPanel = JPanel(BorderLayout())
		promptField.lineWrap = true
		promptField.wrapStyleWord = true
		promptField.minimumSize = Dimension(100, 80)
		val inputScroll: JBScrollPane = JBScrollPane(promptField)
		inputScroll.border = JBUI.Borders.empty(4)
		inputPanel.add(inputScroll, BorderLayout.CENTER)
		inputPanel.add(sendButton, BorderLayout.EAST)
		add(inputPanel, BorderLayout.SOUTH)

		// Actions
		sendButton.addActionListener { onSend() }
		clearButton.addActionListener { onClear() }
		settingsButton.addActionListener { onOpenSettings() }

		// Enter to send; Shift+Enter for newline
		promptField.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send")
		promptField.actionMap.put("send", object : AbstractAction() {
			override fun actionPerformed(e: ActionEvent) {
				onSend()
			}
		})

		// Char counter
		promptField.document.addDocumentListener(object : DocumentListener {
			override fun insertUpdate(e: DocumentEvent) {
				updateCharCounter()
			}
			override fun removeUpdate(e: DocumentEvent) {
				updateCharCounter()
			}
			override fun changedUpdate(e: DocumentEvent) {
				updateCharCounter()
			}
		})
		updateCharCounter()

		ensureAuthorizedOrRedirect()
	}

	private fun ensureAuthorizedOrRedirect(): Unit {
		val token: String? = AccessTokenState.getInstance().accessToken
		if (token.isNullOrBlank() || !verifier.isTokenValid(token)) {
			notifyWarn(
				project,
				"Genie AI Assistant",
				"Access token is missing or invalid. Please configure it in Settings → AI Assistant."
			)
			ShowSettingsUtil.getInstance().showSettingsDialog(project, "AI Assistant")
		}
	}

	private fun setSending(sending: Boolean): Unit {
		sendButton.isEnabled = !sending
		promptField.isEnabled = !sending
		progressIcon.isVisible = sending
	}

	private fun onSend(): Unit {
		val token: String? = AccessTokenState.getInstance().accessToken
		if (token.isNullOrBlank()) {
			appendOutput("Error: No access token configured.")
			ensureAuthorizedOrRedirect()
			return
		}
		if (!verifier.isTokenValid(token)) {
			appendOutput("Error: Invalid access token.")
			ensureAuthorizedOrRedirect()
			return
		}
		val prompt: String = promptField.text.trim()
		if (prompt.isBlank()) {
			appendOutput("Please enter a prompt.")
			return
		}

		// UI updates before background work
		appendOutput("You: $prompt")
		setSending(true)

		// Clear prompt for next input
		promptField.text = ""
		updateCharCounter()

		// Run network call off the EDT
		ApplicationManager.getApplication().executeOnPooledThread {
			try {
				val reply: String = client.chat(token, prompt)
				SwingUtilities.invokeLater {
					appendOutput("Assistant: $reply")
					setSending(false)
				}
			} catch (e: Exception) {
				val message: String = "Error: ${e.message}"
				SwingUtilities.invokeLater {
					appendOutput(message)
					setSending(false)
				}
			}
		}
	}

	private fun onClear(): Unit {
		outputArea.text = ""
	}

	private fun onOpenSettings(): Unit {
		ShowSettingsUtil.getInstance().showSettingsDialog(project, "AI Assistant")
	}

	private fun updateCharCounter(): Unit {
		val len: Int = promptField.text.length
		charCounter.text = "$len chars"
	}

	private fun appendOutput(text: String): Unit {
		val current: String = outputArea.text
		val next: String = if (current.isBlank()) text else current + "\n" + text
		outputArea.text = next
	}
}