package com.example.aiassistant.settings

import com.example.aiassistant.security.AccessTokenVerifier
import com.example.aiassistant.utils.uiHelpers.BorderHelper
import com.example.aiassistant.utils.uiHelpers.ColorHelper
import com.example.aiassistant.utils.TextHelper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.openapi.util.IconLoader
import com.intellij.util.ui.JBUI
import javax.swing.ImageIcon
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.Icon
import javax.swing.text.AttributeSet
import javax.swing.text.PlainDocument
import javax.swing.JTextField   


class AIAssistantConfigurable : Configurable {

	// UI state
	private var mainPanel: JPanel? = null
	private val tokenField: JTextField = JTextField()
	private val validateButton: JButton = JButton(TextHelper.Buttons.TEST_CONNECTION)
	private val progressIcon: AsyncProcessIcon = AsyncProcessIcon(TextHelper.Status.VALIDATING)
	private val statusMessage: JLabel = JLabel(" ")
	private val statusPill: JLabel = JLabel(TextHelper.Status.IDLE)
	private val toggleVisibilityButton: JButton = JButton(TextHelper.Buttons.SHOW)
	private var defaultEchoChar: Char = 0.toChar()

	private lateinit var required: JLabel

	private val aiIcon = IconLoader.getIcon("/icons/ai.png", javaClass)
	private val aiIconSmall: Icon = (aiIcon as? ImageIcon)?.let {
		ImageIcon(it.image.getScaledInstance(14, 14, java.awt.Image.SCALE_SMOOTH))
	} ?: aiIcon

	// model state
	private var currentToken: String = ""
	private var isValidating: Boolean = false
	private var actualTokenValue: String = ""
	private var isUpdatingField: Boolean = false

	override fun getDisplayName(): String = TextHelper.Settings.TITLE

	override fun createComponent(): JComponent {
		mainPanel = createSettingsPanel()
		loadCurrentToken()
		return mainPanel!!
	}

	// Compact, left-aligned, scroll-safe layout
	private fun createSettingsPanel(): JPanel {
		val root: JPanel = JPanel(BorderLayout())
		root.background = ColorHelper.backgroundColor
		root.border = EmptyBorder(16, 16, 16, 16)

		val stack: JPanel = JPanel()
		stack.layout = BoxLayout(stack, BoxLayout.Y_AXIS)
		stack.background = ColorHelper.backgroundColor

		stack.add(createHeaderSection())
		stack.add(Box.createVerticalStrut(12))
		stack.add(createTokenCard())
		stack.add(Box.createVerticalStrut(12))
		stack.add(createStatusRow())

		root.add(stack, BorderLayout.CENTER)
		return root
	}

	private fun createHeaderSection(): JPanel {
		val header: JPanel = JPanel()
		header.layout = BoxLayout(header, BoxLayout.Y_AXIS)
		header.background = ColorHelper.backgroundColor
		header.alignmentX = Component.LEFT_ALIGNMENT

		val titleRow: JPanel = JPanel()
		titleRow.layout = BoxLayout(titleRow, BoxLayout.X_AXIS)
		titleRow.background = ColorHelper.backgroundColor
		titleRow.alignmentX = Component.LEFT_ALIGNMENT
        

		val iconLabel: JLabel = JLabel(aiIconSmall)
		iconLabel.border = EmptyBorder(0, 0, 0, 8)

		val title: JLabel = JLabel(TextHelper.Settings.TITLE)
		title.font = Font(Font.SANS_SERIF, Font.BOLD, 18)
		title.foreground = ColorHelper.primaryColor
		
		titleRow.add(iconLabel)
		titleRow.add(title)

		val subtitle: JLabel = JLabel(TextHelper.Settings.SUBTITLE_HTML)
		subtitle.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
		subtitle.foreground = ColorHelper.textSecondaryColor
		subtitle.alignmentX = Component.LEFT_ALIGNMENT

		header.add(titleRow)
		header.add(Box.createVerticalStrut(13))
		header.add(subtitle)
		return header
	}

	private fun createTokenCard(): JPanel {
		val card: JPanel = JPanel()
		card.layout = BoxLayout(card, BoxLayout.Y_AXIS)
		card.background = ColorHelper.surfaceColor
		card.alignmentX = Component.LEFT_ALIGNMENT
		card.border = CompoundBorder(BorderHelper.Styles.cardBorder(10), EmptyBorder(16, 16, 16, 16))
		card.maximumSize = Dimension(Int.MAX_VALUE, 160)

		// Header row (left aligned, no icons-with-text)
		val headerRow: JPanel = JPanel()
		headerRow.layout = BoxLayout(headerRow, BoxLayout.X_AXIS)
		headerRow.background = ColorHelper.surfaceColor
		headerRow.alignmentX = Component.LEFT_ALIGNMENT

		val hTitle: JLabel = JLabel(TextHelper.Settings.ACCESS_TOKEN_TITLE)
		hTitle.font = Font(Font.SANS_SERIF, Font.BOLD, 14)
		hTitle.foreground = ColorHelper.primaryColor

		required = JLabel(TextHelper.Settings.REQUIRED_LABEL)
		required.font = Font(Font.SANS_SERIF, Font.BOLD, 10)
		required.foreground = ColorHelper.textPrimaryColor
		required.background = ColorHelper.withAlpha(ColorHelper.errorColor, 0.85f)
		required.isOpaque = true
		required.border = CompoundBorder(BorderHelper.createRoundedBorder(ColorHelper.errorColor, 0, 6), EmptyBorder(2, 8, 2, 8))


		headerRow.add(hTitle)
		headerRow.add(Box.createHorizontalStrut(8))
		headerRow.add(Box.createHorizontalGlue())
		headerRow.add(required)


		// Field label
		val fieldLabel: JLabel = JLabel(TextHelper.Settings.TOKEN_FIELD_LABEL)
		fieldLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
		fieldLabel.foreground = ColorHelper.textPrimaryColor
		fieldLabel.alignmentX = Component.LEFT_ALIGNMENT
		fieldLabel.border = EmptyBorder(0, 0, 6, 0)

		// Token field with trailing toggle button  
		val fieldContainer: JPanel = JPanel(BorderLayout())
		fieldContainer.background = ColorHelper.surfaceColor
		fieldContainer.alignmentX = Component.LEFT_ALIGNMENT
		fieldContainer.preferredSize = Dimension(400, 20)
		fieldContainer.maximumSize = Dimension(Int.MAX_VALUE, 20)

		// Compact token field setup
		tokenField.font = Font(Font.MONOSPACED, Font.PLAIN, 11)
		tokenField.preferredSize = Dimension(380, 22)
		tokenField.maximumSize = Dimension(Int.MAX_VALUE, 22)
		tokenField.alignmentX = Component.LEFT_ALIGNMENT
		tokenField.border = CompoundBorder(
			BorderHelper.Styles.inputBorder(4),
			EmptyBorder(1, 6, 1, 6)
		)

		// Custom password masking
		tokenField.document = object : PlainDocument() {
			override fun insertString(offset: Int, str: String?, attr: AttributeSet?) {
				if (str != null) {
					actualTokenValue = actualTokenValue.substring(0, offset) + str + actualTokenValue.substring(offset)
					super.insertString(offset, if (true) str else "•".repeat(str.length), attr) // Always plain text
				}
			}

			override fun remove(offset: Int, length: Int) {
				actualTokenValue = actualTokenValue.substring(0, offset) + actualTokenValue.substring(offset + length)
				super.remove(offset, length)
			}
		}

		// Add rounded border to the container
		fieldContainer.border = CompoundBorder(
			BorderHelper.createRoundedBorder(ColorHelper.borderColor, 1, 4),
			EmptyBorder(0, 0, 0, 0)
		)

		fieldContainer.add(tokenField, BorderLayout.CENTER)
		fieldContainer.add(toggleVisibilityButton, BorderLayout.EAST)

		// Actions row (compact)
		val actionsRow: JPanel = JPanel()
		actionsRow.layout = BoxLayout(actionsRow, BoxLayout.X_AXIS)
		actionsRow.background = ColorHelper.surfaceColor
		actionsRow.alignmentX = Component.LEFT_ALIGNMENT
		actionsRow.alignmentY = Component.LEFT_ALIGNMENT
		actionsRow.border = EmptyBorder(10, 3, 0, 0)

		styleSecondaryButton(validateButton)
		validateButton.preferredSize = Dimension(140, 32)
		validateButton.maximumSize = Dimension(140, 32)
		validateButton.addActionListener { validateToken() }

		progressIcon.isVisible = false

		// Pill status (subtle, small, modern)
		setupStatusPill(initialText = TextHelper.Status.IDLE)

		actionsRow.add(statusPill)
		actionsRow.add(Box.createHorizontalGlue())
		actionsRow.add(progressIcon)
		actionsRow.add(Box.createHorizontalStrut(10))
		actionsRow.add(validateButton)
		
		// Focus styling
		tokenField.addFocusListener(object : java.awt.event.FocusAdapter() {
			override fun focusGained(e: java.awt.event.FocusEvent) {
				tokenField.border = CompoundBorder(BorderHelper.Styles.inputBorderFocused(6), EmptyBorder(2, 8, 2, 8))
			}
			override fun focusLost(e: java.awt.event.FocusEvent) {
				tokenField.border = CompoundBorder(BorderHelper.Styles.inputBorder(6), EmptyBorder(2, 8, 2, 8))
			}
		})

		// Live change handling
		tokenField.addKeyListener(object : KeyAdapter() {
			override fun keyPressed(e: KeyEvent) {
				if (e.keyCode == KeyEvent.VK_ENTER && !isValidating) validateToken()
			}
		})

		card.add(headerRow)
		card.add(fieldLabel)
		card.add(createCompactTokenField()) // Use the new compact field
		card.add(actionsRow)
		return card
	}

	private fun createCompactTokenField(): JPanel {
		val container: JPanel = JPanel(BorderLayout())
		container.background = ColorHelper.surfaceColor
		container.alignmentX = Component.LEFT_ALIGNMENT
		container.preferredSize = Dimension(400, 28)
		container.maximumSize = Dimension(Int.MAX_VALUE, 28)

		// Plain text field (no masking)
		tokenField.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
		tokenField.border = EmptyBorder(2, 8, 2, 8)
		tokenField.background = ColorHelper.backgroundColor
		tokenField.foreground = ColorHelper.textPrimaryColor
		tokenField.isEditable = true

		// Update model on every change
		tokenField.document.addDocumentListener(object : DocumentListener {
			override fun insertUpdate(e: DocumentEvent): Unit = onTokenFieldChanged()
			override fun removeUpdate(e: DocumentEvent): Unit = onTokenFieldChanged()
			override fun changedUpdate(e: DocumentEvent): Unit = onTokenFieldChanged()
		})

		// Simple rounded border
		container.border = CompoundBorder(
			BorderHelper.createRoundedBorder(ColorHelper.borderColor, 1, 6),
			EmptyBorder(0, 0, 0, 0)
		)

		container.add(tokenField, BorderLayout.CENTER)
		// Note: no toggle button added
		return container
	}

	private fun createStatusRow(): JPanel {
		val row: JPanel = JPanel()
		row.layout = BoxLayout(row, BoxLayout.Y_AXIS)
		row.background = ColorHelper.backgroundColor
		row.alignmentX = Component.LEFT_ALIGNMENT
		row.maximumSize = Dimension(Int.MAX_VALUE, 80)

		statusMessage.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
		statusMessage.foreground = ColorHelper.textSecondaryColor
		statusMessage.alignmentX = Component.LEFT_ALIGNMENT
		statusMessage.border = EmptyBorder(0, 0, 0, 0)

		// Subtle divider
		val divider: JPanel = JPanel()
		divider.background = ColorHelper.borderColor
		divider.maximumSize = Dimension(Int.MAX_VALUE, 1)
		divider.alignmentX = Component.LEFT_ALIGNMENT
		divider.border = BorderFactory.createEmptyBorder()

		row.add(divider)
		row.add(Box.createVerticalStrut(8))
		row.add(statusMessage)
		return row
	}

	private fun setupStatusPill(initialText: String): Unit {
		statusPill.text = initialText
		statusPill.font = Font(Font.SANS_SERIF, Font.BOLD, 11)
		statusPill.isOpaque = false
		statusPill.border = CompoundBorder(
			BorderHelper.createRoundedBackgroundBorder(
				ColorHelper.withAlpha(ColorHelper.infoColor, 0.12f),
				ColorHelper.withAlpha(ColorHelper.infoColor, 0.35f),
				1,
				10
			),
			EmptyBorder(4, 10, 4, 10)
		)
		statusPill.foreground = ColorHelper.infoColor
	}



	private fun setPill(type: String, text: String): Unit {
		val colors: Triple<java.awt.Color, java.awt.Color, java.awt.Color> = when (type) {
			"success" -> Triple(
				ColorHelper.withAlpha(ColorHelper.successColor, 0.14f),
				ColorHelper.withAlpha(ColorHelper.successColor, 0.35f),
				ColorHelper.successColor
			)
			"error" -> Triple(
				ColorHelper.withAlpha(ColorHelper.errorColor, 0.14f),
				ColorHelper.withAlpha(ColorHelper.errorColor, 0.35f),
				ColorHelper.errorColor
			)
			"warning" -> Triple(
				ColorHelper.withAlpha(ColorHelper.warningColor, 0.16f),
				ColorHelper.withAlpha(ColorHelper.warningColor, 0.38f),
				ColorHelper.warningColor
			)
			"loading" -> Triple(
				ColorHelper.withAlpha(ColorHelper.primaryColor, 0.12f),
				ColorHelper.withAlpha(ColorHelper.primaryColor, 0.35f),
				ColorHelper.primaryColor
			)
			else -> Triple(
				ColorHelper.withAlpha(ColorHelper.infoColor, 0.12f),
				ColorHelper.withAlpha(ColorHelper.infoColor, 0.35f),
				ColorHelper.infoColor
			)
		}

		val (bg, br, fg) = colors

		statusPill.text = text
		statusPill.border = CompoundBorder(
			BorderHelper.createRoundedBackgroundBorder(bg, br, 1, 10),
            JBUI.Borders.empty(4, 10)
		)
		statusPill.foreground = fg
		statusPill.repaint()

		updateRequiredBackground()
	}

	private fun updateRequiredBackground(): Unit {
		val isStatusValid: Boolean = statusPill.text == TextHelper.Status.VALID
        required.text = if (isStatusValid) TextHelper.Settings.REQUIRED_LABEL_SUCCESS else TextHelper.Settings.REQUIRED_LABEL
		required.background = if (isStatusValid) ColorHelper.withAlpha(ColorHelper.successColor, 0.85f) else ColorHelper.withAlpha(ColorHelper.errorColor, 0.85f)
        required.foreground = if (isStatusValid) ColorHelper.whiteColor else ColorHelper.textPrimaryColor
		required.repaint()
	}

	private fun onTokenFieldChanged(): Unit {
		val value: String = tokenField.text
		actualTokenValue = value

		if (actualTokenValue.isBlank()) {
			statusMessage.text = TextHelper.Status.ENTER_TOKEN_MESSAGE
			setPill("neutral", TextHelper.Status.IDLE)
			validateButton.isEnabled = false
		} else {
			statusMessage.text = TextHelper.Status.READY_TO_TEST_MESSAGE
			setPill("warning", TextHelper.Status.UNSAVED)
			validateButton.isEnabled = !isValidating
		}

		updateRequiredBackground()
	}

	private fun validateToken(): Unit {
		val token: String = actualTokenValue.trim()
		if (token.isEmpty()) {
			statusMessage.text = TextHelper.Status.PLEASE_ENTER_TOKEN_MESSAGE
			setPill("error", TextHelper.Status.MISSING_TOKEN)
			return
		}

		setValidationState(true)
		statusMessage.text = TextHelper.Status.VALIDATING_TOKEN_MESSAGE
		setPill("loading", TextHelper.Status.LOADING)

		val verifier: AccessTokenVerifier = AccessTokenVerifier(TextHelper.API.DEFAULT_AUTH_ENDPOINT)
		ApplicationManager.getApplication().executeOnPooledThread {
			try {
				Thread.sleep(2000L) // show progress for ~2 seconds before actual validation
				val ok: Boolean = verifier.isTokenValid(token)
				SwingUtilities.invokeLater {
					setValidationState(false)
					if (ok) {
						statusMessage.text = TextHelper.Status.TOKEN_VALID_MESSAGE
						setPill("success", TextHelper.Status.VALID)
					} else {
						statusMessage.text = TextHelper.Status.TOKEN_INVALID_MESSAGE
						setPill("error", TextHelper.Status.INVALID)
					}
				}
			} catch (e: Exception) {
				SwingUtilities.invokeLater {
					setValidationState(false)
					statusMessage.text = TextHelper.formatConnectionError(e.message ?: "Unknown error")
					setPill("error", TextHelper.Status.ERROR)
				}
			}
		}
	}

	private fun setValidationState(running: Boolean): Unit {
		isValidating = running
		validateButton.isEnabled = !running && actualTokenValue.trim().isNotEmpty()
		progressIcon.isVisible = running

		validateButton.cursor = if (running) Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
		else Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
	}

	private fun loadCurrentToken(): Unit {
		AccessTokenState.getInstance().getAccessTokenAsync().thenAccept { token: String? ->
			SwingUtilities.invokeLater {
				currentToken = token ?: ""
				actualTokenValue = currentToken
				tokenField.text = actualTokenValue  // show plain text

				if (currentToken.isBlank()) {
					statusMessage.text = TextHelper.Status.NO_TOKEN_SAVED_MESSAGE
					setPill("neutral", TextHelper.Status.IDLE)
					validateButton.isEnabled = false
				} else {
					statusMessage.text = TextHelper.Status.LOADED_TOKEN_MESSAGE
					setPill("warning", TextHelper.Status.LOADED)
					validateButton.isEnabled = true
				}
			}
		}
	}

	private fun styleSecondaryButton(button: JButton): Unit {
		button.font = Font(Font.SANS_SERIF, Font.BOLD, 12)
		button.isFocusPainted = false
		button.isBorderPainted = false
		button.isContentAreaFilled = false
		button.isOpaque = false
		button.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
		button.background = ColorHelper.Buttons.Secondary.background
		button.foreground = ColorHelper.Buttons.Secondary.foreground
		button.border = CompoundBorder(
			BorderHelper.Styles.buttonBackgroundSecondary(8),
			EmptyBorder(0, 16, 0, 16)
		)

		// Hover effects
		button.addMouseListener(object : java.awt.event.MouseAdapter() {
			override fun mouseEntered(e: java.awt.event.MouseEvent): Unit {
				button.background = ColorHelper.Buttons.Secondary.hover
			}
			override fun mouseExited(e: java.awt.event.MouseEvent): Unit {
				button.background = ColorHelper.Buttons.Secondary.background
			}
		})
	}

	private fun styleInlineLinkButton(button: JButton): Unit {
		button.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
		button.isFocusPainted = false
		button.isBorderPainted = false
		button.isContentAreaFilled = false
		button.isOpaque = false
		button.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
		button.foreground = ColorHelper.textLinkColor
		button.border = EmptyBorder(0, 0, 0, 0)

		button.addMouseListener(object : java.awt.event.MouseAdapter() {
			override fun mouseEntered(e: java.awt.event.MouseEvent): Unit {
				button.foreground = ColorHelper.getDarkerVariant(ColorHelper.textLinkColor)
			}
			override fun mouseExited(e: java.awt.event.MouseEvent): Unit {
				button.foreground = ColorHelper.textLinkColor
			}
		})
	}

	override fun isModified(): Boolean {
		val now: String = actualTokenValue.trim()
		return now != currentToken
	}

	override fun apply(): Unit {
		val newToken: String = actualTokenValue.trim()
		AccessTokenState.getInstance().accessToken = newToken.ifBlank { null }
		currentToken = newToken
		if (newToken.isBlank()) {
			statusMessage.text = TextHelper.Status.TOKEN_CLEARED_MESSAGE
			setPill("neutral", TextHelper.Status.IDLE)
		} else {
			statusMessage.text = TextHelper.Status.TOKEN_SAVED_MESSAGE
			setPill("warning", TextHelper.Status.SAVED)
		}
	}

	override fun reset(): Unit {
		loadCurrentToken()
		statusMessage.text = TextHelper.Status.SETTINGS_RESET_MESSAGE
	}
}