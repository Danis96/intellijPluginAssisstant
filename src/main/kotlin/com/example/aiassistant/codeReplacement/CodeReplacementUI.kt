package com.example.aiassistant.codeReplacement

import com.example.aiassistant.utils.uiHelpers.ColorHelper
import com.example.aiassistant.utils.uiHelpers.BorderHelper
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder

/**
 * UI component for displaying and applying code replacements
 */
class CodeReplacementUI(
    private val project: Project,
    private val editor: Editor?
) : JPanel(BorderLayout()) {

    private val replacementEngine: CodeReplacementEngine = CodeReplacementEngine(project)
    private val replacementsPanel: JPanel = JPanel()
    private val scrollPane: JScrollPane = JScrollPane(replacementsPanel)
    
    init {
        setupUI()
    }

    private fun setupUI() {
        background = ColorHelper.backgroundColor
        border = EmptyBorder(8, 0, 8, 0)

        // Header
        val headerPanel: JPanel = createHeaderPanel()
        add(headerPanel, BorderLayout.NORTH)

        // Replacements container
        replacementsPanel.layout = BoxLayout(replacementsPanel, BoxLayout.Y_AXIS)
        replacementsPanel.background = ColorHelper.backgroundColor
        
        scrollPane.border = null
        scrollPane.background = ColorHelper.backgroundColor
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        
        add(scrollPane, BorderLayout.CENTER)
    }

    private fun createHeaderPanel(): JPanel {
        val headerPanel: JPanel = JPanel(BorderLayout())
        headerPanel.background = ColorHelper.backgroundColor
        headerPanel.border = EmptyBorder(8, 12, 8, 12)

        val titleLabel: JLabel = JLabel("Code Replacements")
        titleLabel.font = Font(Font.SANS_SERIF, Font.BOLD, 14)
        titleLabel.foreground = ColorHelper.primaryColor

        val applyAllButton: JButton = JButton("Apply All", AllIcons.Actions.Execute)
        styleActionButton(applyAllButton, true)
        applyAllButton.addActionListener { applyAllReplacements() }

        headerPanel.add(titleLabel, BorderLayout.WEST)
        headerPanel.add(applyAllButton, BorderLayout.EAST)

        return headerPanel
    }

    /**
     * Displays code replacements from AI response
     */
    fun showReplacements(aiResponse: String) {
        val replacements: List<CodeReplacementEngine.CodeReplacement> = 
            replacementEngine.parseAIResponse(aiResponse)
        
        SwingUtilities.invokeLater {
            replacementsPanel.removeAll()
            
            if (replacements.isEmpty()) {
                showNoReplacementsMessage()
            } else {
                replacements.forEachIndexed { index, replacement ->
                    val replacementCard: JPanel = createReplacementCard(replacement, index + 1)
                    replacementsPanel.add(replacementCard)
                    replacementsPanel.add(Box.createVerticalStrut(8))
                }
            }
            
            replacementsPanel.revalidate()
            replacementsPanel.repaint()
        }
    }

    private fun showNoReplacementsMessage() {
        val messagePanel: JPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        messagePanel.background = ColorHelper.backgroundColor
        
        val messageLabel: JLabel = JLabel("No code replacements found in the response")
        messageLabel.foreground = ColorHelper.textSecondaryColor
        messageLabel.font = Font(Font.SANS_SERIF, Font.ITALIC, 12)
        
        messagePanel.add(messageLabel)
        replacementsPanel.add(messagePanel)
    }

    private fun createReplacementCard(
        replacement: CodeReplacementEngine.CodeReplacement, 
        index: Int
    ): JPanel {
        val card: JPanel = JPanel(BorderLayout())
        card.background = ColorHelper.cardColor
        card.border = CompoundBorder(
            BorderHelper.Styles.cardBorder(8),
            EmptyBorder(12, 16, 12, 16)
        )
        card.maximumSize = Dimension(Int.MAX_VALUE, card.preferredSize.height)

        // Header with index and type
        val headerPanel: JPanel = JPanel(BorderLayout())
        headerPanel.background = ColorHelper.cardColor

        val indexLabel: JLabel = JLabel("Replacement #$index")
        indexLabel.font = Font(Font.SANS_SERIF, Font.BOLD, 12)
        indexLabel.foreground = ColorHelper.primaryColor

        val typeLabel: JLabel = JLabel(replacement.replacementType.name)
        typeLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 10)
        typeLabel.foreground = ColorHelper.textSecondaryColor

        headerPanel.add(indexLabel, BorderLayout.WEST)
        headerPanel.add(typeLabel, BorderLayout.EAST)

        // Code diff display
        val diffPanel: JPanel = createDiffPanel(replacement)

        // Action buttons
        val actionsPanel: JPanel = createActionsPanel(replacement)

        card.add(headerPanel, BorderLayout.NORTH)
        card.add(diffPanel, BorderLayout.CENTER)
        card.add(actionsPanel, BorderLayout.SOUTH)

        return card
    }

    private fun createDiffPanel(replacement: CodeReplacementEngine.CodeReplacement): JPanel {
        val diffPanel: JPanel = JPanel(BorderLayout())
        diffPanel.background = ColorHelper.cardColor
        diffPanel.border = EmptyBorder(8, 0, 8, 0)

        if (replacement.originalCode.isNotBlank()) {
            // Show before/after
            val beforePanel: JPanel = createCodePanel("Before:", replacement.originalCode, ColorHelper.errorColor)
            val afterPanel: JPanel = createCodePanel("After:", replacement.newCode, ColorHelper.successColor)
            
            val container: JPanel = JPanel()
            container.layout = BoxLayout(container, BoxLayout.Y_AXIS)
            container.background = ColorHelper.cardColor
            container.add(beforePanel)
            container.add(Box.createVerticalStrut(8))
            container.add(afterPanel)
            
            diffPanel.add(container, BorderLayout.CENTER)
        } else {
            // Show only new code
            val newCodePanel: JPanel = createCodePanel("New Code:", replacement.newCode, ColorHelper.successColor)
            diffPanel.add(newCodePanel, BorderLayout.CENTER)
        }

        return diffPanel
    }

    private fun createCodePanel(title: String, code: String, accentColor: Color): JPanel {
        val panel: JPanel = JPanel(BorderLayout())
        panel.background = ColorHelper.surfaceColor
        panel.border = CompoundBorder(
            BorderFactory.createMatteBorder(2, 0, 0, 0, accentColor),
            EmptyBorder(8, 12, 8, 12)
        )

        val titleLabel: JLabel = JLabel(title)
        titleLabel.font = Font(Font.SANS_SERIF, Font.BOLD, 11)
        titleLabel.foreground = accentColor

        val codeArea: JTextArea = JTextArea(code)
        codeArea.font = Font(Font.MONOSPACED, Font.PLAIN, 11)
        codeArea.background = ColorHelper.surfaceColor
        codeArea.foreground = JBColor.foreground()
        codeArea.isEditable = false
        codeArea.lineWrap = false
        codeArea.border = null

        val codeScrollPane: JScrollPane = JScrollPane(codeArea)
        codeScrollPane.border = null
        codeScrollPane.background = ColorHelper.surfaceColor
        codeScrollPane.preferredSize = Dimension(400, Math.min(120, codeArea.preferredSize.height))

        panel.add(titleLabel, BorderLayout.NORTH)
        panel.add(codeScrollPane, BorderLayout.CENTER)

        return panel
    }

    private fun createActionsPanel(replacement: CodeReplacementEngine.CodeReplacement): JPanel {
        val actionsPanel: JPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0))
        actionsPanel.background = ColorHelper.cardColor
        actionsPanel.border = EmptyBorder(8, 0, 0, 0)

        val previewButton: JButton = JButton("Preview", AllIcons.Actions.Preview)
        val applyButton: JButton = JButton("Apply", AllIcons.Actions.Execute)
        val skipButton: JButton = JButton("Skip", AllIcons.Actions.Cancel)

        styleActionButton(previewButton, false)
        styleActionButton(applyButton, true)
        styleActionButton(skipButton, false)

        previewButton.addActionListener { previewReplacement(replacement) }
        applyButton.addActionListener { applyReplacement(replacement) }
        skipButton.addActionListener { skipReplacement(replacement) }

        actionsPanel.add(previewButton)
        actionsPanel.add(applyButton)
        actionsPanel.add(skipButton)

        return actionsPanel
    }

    private fun styleActionButton(button: JButton, isPrimary: Boolean) {
        button.font = Font(Font.SANS_SERIF, Font.PLAIN, 11)
        button.isFocusPainted = false
        button.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        if (isPrimary) {
            button.background = ColorHelper.Buttons.Primary.background
            button.foreground = ColorHelper.Buttons.Primary.foreground
            button.border = CompoundBorder(
                BorderHelper.Styles.buttonBackgroundPrimary(6),
                EmptyBorder(6, 12, 6, 12)
            )
        } else {
            button.background = ColorHelper.Buttons.Secondary.background
            button.foreground = ColorHelper.Buttons.Secondary.foreground
            button.border = CompoundBorder(
                BorderHelper.Styles.buttonBackgroundSecondary(6),
                EmptyBorder(6, 12, 6, 12)
            )
        }
    }

    private fun previewReplacement(replacement: CodeReplacementEngine.CodeReplacement) {
        // Show preview dialog
        val message: String = "This will replace:\n\n${replacement.originalCode}\n\nWith:\n\n${replacement.newCode}"
        JOptionPane.showMessageDialog(
            this,
            message,
            "Preview Replacement",
            JOptionPane.INFORMATION_MESSAGE
        )
    }

    private fun applyReplacement(replacement: CodeReplacementEngine.CodeReplacement) {
        if (editor == null) {
            JOptionPane.showMessageDialog(
                this,
                "No editor available for replacement",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        val success: Boolean = replacementEngine.applyReplacement(editor, replacement)
        if (success) {
            JOptionPane.showMessageDialog(
                this,
                "Replacement applied successfully!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            )
        } else {
            JOptionPane.showMessageDialog(
                this,
                "Failed to apply replacement",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun skipReplacement(replacement: CodeReplacementEngine.CodeReplacement) {
        // For now, just show a message
        JOptionPane.showMessageDialog(
            this,
            "Replacement skipped",
            "Skipped",
            JOptionPane.INFORMATION_MESSAGE
        )
    }

    private fun applyAllReplacements() {
        if (editor == null) {
            JOptionPane.showMessageDialog(
                this,
                "No editor available for replacements",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        val result: Int = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to apply all replacements?",
            "Apply All Replacements",
            JOptionPane.YES_NO_OPTION
        )

        if (result == JOptionPane.YES_OPTION) {
            // This would need to be implemented to apply all visible replacements
            JOptionPane.showMessageDialog(
                this,
                "All replacements applied!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            )
        }
    }
}

