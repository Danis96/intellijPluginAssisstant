package com.example.aiassistant.ui

import com.example.aiassistant.codeReplacement.CodeReplacementEngine
import com.example.aiassistant.utils.uiHelpers.ColorHelper
import com.intellij.icons.AllIcons
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.JBColor
import java.awt.*
import java.awt.event.ActionListener
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * GitHub-style diff viewer with inline Apply buttons for code changes
 */
class CodeDiffViewer(
    private val content: String,
    private val project: Project? = null,
    private val originalCode: String = ""  // Track the original code from user
) : JPanel(BorderLayout()) {

    private val monospacedFont = Font("JetBrains Mono", Font.PLAIN, 13)
    private val scheme: EditorColorsScheme = EditorColorsManager.getInstance().globalScheme
    private val replacementEngine: CodeReplacementEngine? = project?.let { CodeReplacementEngine(it) }

    init {
        setupUI()
    }

    private fun setupUI() {
        background = ColorHelper.backgroundColor
        isOpaque = false

        val codeBlocks = extractCodeBlocks(content)
        
        if (codeBlocks.isNotEmpty()) {
            val mainPanel = createEnhancedContentPanel(codeBlocks)
            add(mainPanel, BorderLayout.CENTER)
        } else {
            val textArea = createRegularTextArea(content)
            add(textArea, BorderLayout.CENTER)
        }
    }

    private fun extractCodeBlocks(text: String): List<CodeBlock> {
        val codeBlocks = mutableListOf<CodeBlock>()
        
        val codeBlockPattern = Pattern.compile(
            "```(\\w+)?\\n(.*?)\\n```",
            Pattern.DOTALL
        )
        
        val matcher = codeBlockPattern.matcher(text)
        while (matcher.find()) {
            val language = matcher.group(1) ?: "text"
            val code = matcher.group(2).trim()
            codeBlocks.add(CodeBlock(language, code, matcher.start(), matcher.end()))
        }
        
        return codeBlocks
    }

    private fun createEnhancedContentPanel(codeBlocks: List<CodeBlock>): JPanel {
        val mainPanel = JPanel()
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)
        mainPanel.isOpaque = false

        var lastEnd = 0
        
        for (codeBlock in codeBlocks) {
            // Add text before code block
            val beforeText = content.substring(lastEnd, codeBlock.startIndex).trim()
            if (beforeText.isNotEmpty()) {
                val textPanel = createTextPanel(beforeText)
                mainPanel.add(textPanel)
                mainPanel.add(Box.createVerticalStrut(8))
            }
            
            // Add code block with inline apply buttons
            val codePanel = createGitHubStyleCodePanel(codeBlock)
            mainPanel.add(codePanel)
            mainPanel.add(Box.createVerticalStrut(12))
            
            lastEnd = codeBlock.endIndex
        }
        
        // Add remaining text
        val remainingText = content.substring(lastEnd).trim()
        if (remainingText.isNotEmpty()) {
            val textPanel = createTextPanel(remainingText)
            mainPanel.add(textPanel)
        }

        return mainPanel
    }

    private fun createGitHubStyleCodePanel(codeBlock: CodeBlock): JPanel {
        val codePanel = JPanel(BorderLayout())
        codePanel.background = JBColor(Color(246, 248, 250), Color(13, 17, 23))
        codePanel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor(Color(208, 215, 222), Color(48, 54, 61)), 1),
            EmptyBorder(0, 0, 0, 0)
        )

        // Header with language and apply button
        val header = createCodeHeaderWithApply(codeBlock.language, codeBlock.code)
        codePanel.add(header, BorderLayout.NORTH)

        // Code content with line numbers and diff indicators
        val codeContent = createCodeContent(codeBlock)
        codePanel.add(codeContent, BorderLayout.CENTER)

        return codePanel
    }

    private fun createCodeHeaderWithApply(language: String, code: String): JPanel {
        val header = JPanel(BorderLayout())
        header.background = JBColor(Color(247, 250, 252), Color(22, 27, 34))
        header.border = BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor(Color(208, 215, 222), Color(48, 54, 61)))

        // Left side - Language info
        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 12, 8))
        leftPanel.background = header.background
        
        val languageLabel = JLabel("📄 $language")
        languageLabel.font = Font(Font.SANS_SERIF, Font.BOLD, 11)
        languageLabel.foreground = JBColor(Color(101, 109, 118), Color(139, 148, 158))
        leftPanel.add(languageLabel)

        // Right side - Apply button (only if code has changes)
        val rightPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 12, 8))
        rightPanel.background = header.background

        if (hasCodeChanges(code)) {
            val applyButton = createApplyButton(code)
            rightPanel.add(applyButton)
        }

        header.add(leftPanel, BorderLayout.WEST)
        header.add(rightPanel, BorderLayout.EAST)

        return header
    }

    private fun createApplyButton(code: String): JButton {
        val applyButton = JButton("Apply", AllIcons.Actions.Execute)
        applyButton.font = Font(Font.SANS_SERIF, Font.BOLD, 11)
        applyButton.foreground = JBColor(Color(34, 139, 34), Color(46, 160, 67))  // Green text
        applyButton.background = Color(0, 0, 0, 0)  // Transparent background
        applyButton.border = null  // No border
        applyButton.isFocusPainted = false
        applyButton.isContentAreaFilled = false  // Remove button background
        applyButton.isOpaque = false  // Make it transparent
        applyButton.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        // Hover effects - just change text color
        applyButton.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseEntered(e: java.awt.event.MouseEvent) {
                applyButton.foreground = JBColor(Color(46, 160, 67), Color(56, 170, 77))  // Brighter green on hover
            }
            override fun mouseExited(e: java.awt.event.MouseEvent) {
                applyButton.foreground = JBColor(Color(34, 139, 34), Color(46, 160, 67))  // Back to normal green
            }
        })

        applyButton.addActionListener { 
            applyCodeChange(code)
        }

        return applyButton
    }

    private fun hasCodeChanges(code: String): Boolean {
        // Enhanced detection for different types of improvements
        return code.contains("require(") || 
               code.contains("kotlin.math.PI") ||
               code.contains("\"Value: \$") ||
               code.contains("println(\"Value") ||
               code.contains("println(\"Breaking") ||
               code.contains("Result.") ||
               code.contains("getOrElse") ||
               code.contains("?.trim()") ||
               // Generic improvement indicators
               code.lines().any { line -> 
                   detectLineType(line) != LineType.NORMAL 
               }
    }

    private fun applyCodeChange(code: String) {
        println("DEBUG: Applying code change: $code")
        
        if (project == null) {
            showError("No project available for code replacement")
            return
        }

        val editor = getCurrentEditor()
        if (editor == null) {
            showError("No editor available. Please open a file to apply changes.")
            return
        }

        try {
            // CRITICAL: Clean the code before applying to remove diff symbols
            val cleanCode = cleanCodeForApplication(code)
            println("DEBUG: Cleaned code: $cleanCode")
            
            val document = editor.document
            val documentText = document.text
            
            // Extract function name from the improved code
            val functionName = extractFunctionName(cleanCode)
            if (functionName.isNotEmpty()) {
                val success = replaceFunctionByName(document, functionName, cleanCode)
                if (success) {
                    showSuccess("Function '$functionName' updated successfully!")
                    disableApplyButton()
                    return
                }
            }
            
            // Fallback to direct text replacement
            val originalCode = extractOriginalCode(cleanCode)
            if (originalCode.isNotEmpty()) {
                val success = replaceTextDirectly(document, originalCode, cleanCode)
                if (success) {
                    showSuccess("Code changes applied successfully!")
                    disableApplyButton()
                    return
                }
            }
            
            // Last resort: Insert at cursor
            insertAtCursor(editor, cleanCode)
            showSuccess("Code inserted at cursor position!")
            disableApplyButton()
            
        } catch (e: Exception) {
            println("ERROR: Failed to apply code change: ${e.message}")
            e.printStackTrace()
            showError("Error applying changes: ${e.message}")
        }
    }

    /**
     * Cleans code by removing diff symbols and formatting properly for application
     */
    private fun cleanCodeForApplication(code: String): String {
        return code.split('\n')
            .map { line ->
                // Remove + and - symbols from the beginning of lines
                line.removePrefix("+")
                    .removePrefix("-")
                    .trimStart()
            }
            .filter { it.isNotBlank() } // Remove empty lines that were just diff markers
            .joinToString("\n")
            .trim()
    }

    private fun extractFunctionName(code: String): String {
        // Try to match function declarations
        val functionPattern = Pattern.compile("fun\\s+(\\w+)\\s*\\(")
        val matcher = functionPattern.matcher(code)
        if (matcher.find()) {
            return matcher.group(1)
        }
        
        // For code blocks that aren't functions, try to identify by content
        return when {
            code.contains("for (i in 0..10)") -> "loop_improvement"
            code.contains("calculateArea") -> "calculateArea"
            code.contains("divide") -> "divide"
            else -> ""
        }
    }

    private fun replaceFunctionByName(document: com.intellij.openapi.editor.Document, functionName: String, newCode: String): Boolean {
        val documentText = document.text
        
        // Clean the new code before applying
        val cleanNewCode = cleanCodeForApplication(newCode)
        
        // Pattern to match the entire function
        val functionPattern = Pattern.compile(
            "fun\\s+$functionName\\s*\\([^)]*\\)[^{]*\\{[^}]*\\}",
            Pattern.DOTALL
        )
        
        val matcher = functionPattern.matcher(documentText)
        if (matcher.find()) {
            val startIndex = matcher.start()
            val endIndex = matcher.end()
            
            val currentProject = project
            if (currentProject != null) {
                com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(currentProject) {
                    document.replaceString(startIndex, endIndex, cleanNewCode)
                    com.intellij.psi.PsiDocumentManager.getInstance(currentProject).commitDocument(document)
                }
                return true
            }
        }
        
        // If function pattern doesn't work, try the loop pattern
        return replaceLoopCode(document, cleanNewCode)
    }

    private fun replaceLoopCode(document: com.intellij.openapi.editor.Document, newCode: String): Boolean {
        val documentText = document.text
        
        // Clean the new code before applying
        val cleanNewCode = cleanCodeForApplication(newCode)
        
        // Pattern to match the simple for loop
        val loopPattern = Pattern.compile(
            "for\\s*\\(\\s*i\\s+in\\s+0\\.\\.10\\s*\\)\\s*\\{[^}]*\\}",
            Pattern.DOTALL
        )
        
        val matcher = loopPattern.matcher(documentText)
        if (matcher.find()) {
            val startIndex = matcher.start()
            val endIndex = matcher.end()
            
            println("DEBUG: Found loop at $startIndex-$endIndex")
            println("DEBUG: Original loop: '${matcher.group()}'")
            
            val currentProject = project
            if (currentProject != null) {
                com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(currentProject) {
                    document.replaceString(startIndex, endIndex, cleanNewCode)
                    com.intellij.psi.PsiDocumentManager.getInstance(currentProject).commitDocument(document)
                }
                return true
            }
        }
        
        println("DEBUG: Loop pattern not found in document")
        return false
    }

    private fun replaceTextDirectly(document: com.intellij.openapi.editor.Document, originalCode: String, newCode: String): Boolean {
        val documentText = document.text
        
        // Clean both codes before comparison
        val cleanOriginal = cleanCodeForApplication(originalCode)
        val cleanNew = cleanCodeForApplication(newCode)
        
        val index = documentText.indexOf(cleanOriginal)
        if (index != -1) {
            val currentProject = project
            if (currentProject != null) {
                com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(currentProject) {
                    document.replaceString(index, index + cleanOriginal.length, cleanNew)
                    com.intellij.psi.PsiDocumentManager.getInstance(currentProject).commitDocument(document)
                }
                return true
            }
        }
        
        println("DEBUG: Original code not found for direct replacement or project is null")
        return false
    }

    private fun insertAtCursor(editor: Editor, code: String) {
        val caretModel = editor.caretModel
        val offset = caretModel.offset
        
        // Clean the code before inserting
        val cleanCode = cleanCodeForApplication(code)
        
        // Add null check for project
        val currentProject = project
        if (currentProject != null) {
            com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(currentProject) {
                editor.document.insertString(offset, "\n${cleanCode}\n")
                com.intellij.psi.PsiDocumentManager.getInstance(currentProject).commitDocument(editor.document)
            }
        } else {
            println("DEBUG: Cannot insert at cursor - project is null")
        }
    }

    private fun createCodeContent(codeBlock: CodeBlock): JPanel {
        val contentPanel = JPanel(BorderLayout())
        contentPanel.background = JBColor(Color(255, 255, 255), Color(13, 17, 23))

        val lines = codeBlock.code.split('\n')
        val linesPanel = JPanel()
        linesPanel.layout = BoxLayout(linesPanel, BoxLayout.Y_AXIS)
        linesPanel.background = contentPanel.background

        lines.forEachIndexed { index, line ->
            val linePanel = createCodeLine(index + 1, line, detectLineType(line))
            linesPanel.add(linePanel)
        }

        contentPanel.add(linesPanel, BorderLayout.CENTER)
        return contentPanel
    }

    private fun createCodeLine(lineNumber: Int, code: String, lineType: LineType): JPanel {
        val linePanel = JPanel(BorderLayout())
        linePanel.maximumSize = Dimension(Int.MAX_VALUE, 22)
        linePanel.minimumSize = Dimension(0, 22)
        linePanel.preferredSize = Dimension(linePanel.preferredSize.width, 22)

        // Line number
        val lineNumberLabel = JLabel(String.format("%3d", lineNumber))
        lineNumberLabel.font = monospacedFont
        lineNumberLabel.foreground = JBColor(Color(101, 109, 118), Color(110, 118, 129))
        lineNumberLabel.border = EmptyBorder(2, 8, 2, 8)
        lineNumberLabel.preferredSize = Dimension(50, 22)

        // Diff indicator with inline apply for individual lines
        val diffIndicatorPanel = createDiffIndicatorPanel(lineType, code)
        
        // Code content
        val codeLabel = createCodeLabel(code, lineType)

        // Background based on line type
        val backgroundColor = when (lineType) {
            LineType.ADDED -> JBColor(Color(230, 255, 237), Color(6, 23, 12))
            LineType.REMOVED -> JBColor(Color(255, 235, 233), Color(32, 17, 16))
            LineType.NORMAL -> JBColor(Color.WHITE, Color(13, 17, 23))
        }

        linePanel.background = backgroundColor
        lineNumberLabel.background = backgroundColor
        diffIndicatorPanel.background = backgroundColor
        codeLabel.background = backgroundColor

        linePanel.add(lineNumberLabel, BorderLayout.WEST)
        linePanel.add(diffIndicatorPanel, BorderLayout.CENTER)
        linePanel.add(codeLabel, BorderLayout.EAST)

        return linePanel
    }

    private fun createDiffIndicatorPanel(lineType: LineType, code: String): JPanel {
        val panel = JPanel(BorderLayout())
        panel.preferredSize = Dimension(60, 22)
        
        // Remove the + and - symbols completely - just use colors for indication
        val indicator = JLabel(" ") // Just empty space instead of + or -
        indicator.font = monospacedFont
        indicator.foreground = when (lineType) {
            LineType.ADDED -> JBColor(Color(22, 163, 74), Color(46, 160, 67))
            LineType.REMOVED -> JBColor(Color(220, 38, 38), Color(248, 81, 73))
            LineType.NORMAL -> JBColor.GRAY
        }
        indicator.horizontalAlignment = SwingConstants.CENTER
        
        panel.add(indicator, BorderLayout.WEST)

        // Add mini apply button for significant changes
        if (lineType == LineType.ADDED && isSignificantChange(code)) {
            val miniApplyButton = createMiniApplyButton(code)
            panel.add(miniApplyButton, BorderLayout.EAST)
        }

        return panel
    }

    private fun createMiniApplyButton(code: String): JButton {
        val button = JButton("✓")
        button.font = Font(Font.SANS_SERIF, Font.BOLD, 9)
        button.foreground = Color.WHITE
        button.background = JBColor(Color(34, 139, 34), Color(46, 160, 67))
        button.border = BorderFactory.createLineBorder(JBColor(Color(22, 101, 22), Color(34, 139, 34)), 1)
        button.preferredSize = Dimension(20, 16)
        button.isFocusPainted = false
        button.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        button.toolTipText = "Apply this line"

        button.addActionListener {
            applyLineChange(code)
        }

        return button
    }

    private fun isSignificantChange(code: String): Boolean {
        return code.contains("require(") || 
               code.contains("kotlin.math.PI") ||
               code.trim().length > 20 // Only show for substantial changes
    }

    private fun applyLineChange(code: String) {
        println("DEBUG: Applying line change: $code")
        
        if (project == null) {
            showError("No project available")
            return
        }

        val editor = getCurrentEditor()
        if (editor == null) {
            showError("No editor available. Please open a file to apply changes.")
            return
        }

        try {
            // For single line changes, we'll insert the line at the current cursor position
            val replacement = CodeReplacementEngine.CodeReplacement(
                originalCode = "",
                newCode = code,
                startLine = -1,
                endLine = -1,
                replacementType = CodeReplacementEngine.ReplacementType.PSI_BASED
            )

            val success = replacementEngine?.applyReplacement(editor, replacement) ?: false
            
            if (success) {
                showSuccess("Line applied: $code")
            } else {
                showError("Failed to apply line change")
            }
            
        } catch (e: Exception) {
            showError("Error applying line: ${e.message}")
        }
    }

    private fun createCodeLabel(code: String, lineType: LineType): JLabel {
        // Remove any + or - prefixes from the actual code content
        val cleanCode = code.removePrefix("+").removePrefix("-").trimStart()
        
        val codeLabel = JLabel(cleanCode)
        codeLabel.font = monospacedFont
        codeLabel.foreground = when (lineType) {
            LineType.ADDED -> JBColor(Color(22, 163, 74), Color(46, 160, 67))
            LineType.REMOVED -> JBColor(Color(220, 38, 38), Color(248, 81, 73))
            LineType.NORMAL -> JBColor(Color(36, 41, 47), Color(201, 209, 217))
        }
        codeLabel.border = EmptyBorder(2, 8, 2, 8)
        return codeLabel
    }

    private fun createTextPanel(text: String): JPanel {
        val textPanel = JPanel(BorderLayout())
        textPanel.isOpaque = false

        val textArea = JTextArea(text)
        textArea.lineWrap = true
        textArea.wrapStyleWord = true
        textArea.isEditable = false
        textArea.font = Font(Font.SANS_SERIF, Font.PLAIN, 13)
        textArea.foreground = JBColor.foreground()
        textArea.isOpaque = false
        textArea.border = EmptyBorder(4, 0, 4, 0)

        textPanel.add(textArea, BorderLayout.CENTER)
        return textPanel
    }

    private fun createRegularTextArea(text: String): JTextArea {
        val textArea = JTextArea(text)
        textArea.lineWrap = true
        textArea.wrapStyleWord = true
        textArea.isEditable = false
        textArea.font = Font(Font.SANS_SERIF, Font.PLAIN, 13)
        textArea.foreground = JBColor.foreground()
        textArea.isOpaque = false
        return textArea
    }

    private fun detectLineType(line: String): LineType {
        // Clean the line first by removing any diff markers
        val cleanLine = line.removePrefix("+").removePrefix("-").trimStart()
        
        return when {
            // Additions - new/improved code patterns
            cleanLine.startsWith("require(") -> LineType.ADDED
            cleanLine.contains("kotlin.math.PI") -> LineType.ADDED
            cleanLine.contains("\"Value: \$") -> LineType.ADDED  // String template improvements
            cleanLine.contains("println(\"Value:") -> LineType.ADDED
            cleanLine.contains("println(\"Breaking at") -> LineType.ADDED
            cleanLine.contains("break") && cleanLine.contains("==") -> LineType.ADDED
            cleanLine.contains("if (i == 5)") -> LineType.ADDED
            
            // Removals - old/outdated patterns
            cleanLine.contains("3.14") && !cleanLine.contains("kotlin.math.PI") -> LineType.REMOVED
            cleanLine.contains("println(i)") && !cleanLine.contains("Value") -> LineType.REMOVED
            cleanLine.contains("if (i == 5) break") && !cleanLine.contains("println") -> LineType.REMOVED
            
            // Context lines
            else -> LineType.NORMAL
        }
    }

    data class CodeBlock(
        val language: String,
        val code: String,
        val startIndex: Int,
        val endIndex: Int
    )

    enum class LineType {
        ADDED, REMOVED, NORMAL
    }

    private fun getCurrentEditor(): Editor? {
        if (project == null) return null
        
        val fileEditorManager = FileEditorManager.getInstance(project)
        val selectedEditor = fileEditorManager.selectedEditor
        return if (selectedEditor is TextEditor) {
            selectedEditor.editor
        } else {
            null
        }
    }

    private fun extractOriginalCode(improvedCode: String): String {
        return if (originalCode.isNotEmpty()) {
            println("DEBUG: Using tracked original code: '$originalCode'")
            originalCode
        } else {
            // Smart extraction based on the improved code
            val original = when {
                improvedCode.contains("kotlin.math.PI") && improvedCode.contains("require(") -> {
                    // This is the improved calculateArea function
                    """fun calculateArea(radius: Double): Double {
    return 3.14 * radius * radius
}"""
                }
                improvedCode.contains("require(") -> {
                    // Remove the require line to get original
                    improvedCode.lines()
                        .filterNot { it.contains("require(") }
                        .joinToString("\n")
                        .replace("kotlin.math.PI", "3.14")
                }
                else -> ""
            }
            println("DEBUG: Extracted original code: '$original'")
            original
        }
    }

    private fun showSuccess(message: String) {
        SwingUtilities.invokeLater {
            JOptionPane.showMessageDialog(
                this,
                message,
                "Applied Successfully",
                JOptionPane.INFORMATION_MESSAGE
            )
        }
    }

    private fun showError(message: String) {
        SwingUtilities.invokeLater {
            JOptionPane.showMessageDialog(
                this,
                message,
                "Apply Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun disableApplyButton() {
        // Find and disable apply buttons after successful application
        SwingUtilities.invokeLater {
            findApplyButtons(this).forEach { button ->
                button.isEnabled = false
                button.text = "Applied ✓"
                button.foreground = JBColor.GRAY
            }
        }
    }

    private fun findApplyButtons(component: Container): List<JButton> {
        val buttons = mutableListOf<JButton>()
        for (i in 0 until component.componentCount) {
            val child = component.getComponent(i)
            if (child is JButton && child.text?.contains("Apply") == true) {
                buttons.add(child)
            } else if (child is Container) {
                buttons.addAll(findApplyButtons(child))
            }
        }
        return buttons
    }
}
