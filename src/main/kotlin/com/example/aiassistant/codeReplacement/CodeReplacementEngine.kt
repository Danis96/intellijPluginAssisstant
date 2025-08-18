package com.example.aiassistant.codeReplacement

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import java.util.regex.Pattern


class CodeReplacementEngine(private val project: Project) {

    private val psiManager: PsiManager = PsiManager.getInstance(project)
    private val documentManager: PsiDocumentManager = PsiDocumentManager.getInstance(project)

    data class CodeReplacement(
        val originalCode: String,
        val newCode: String,
        val startLine: Int,
        val endLine: Int,
        val replacementType: ReplacementType = ReplacementType.DIFF_BASED
    )

    enum class ReplacementType {
        DIFF_BASED,     // Simple diff replacement
        PSI_BASED,      // AST-level replacement  
        HYBRID          // Kombinacija diff i PSI-based
    }

    fun parseAIResponse(response: String): List<CodeReplacement> {
        val replacements: MutableList<CodeReplacement> = mutableListOf()
        
        // Pattern za matchanje diff-a
        val codeBlockPattern: Pattern = Pattern.compile(
            "```(?:diff|kotlin|java|python|javascript|typescript)?\n(.*?)\n```",
            Pattern.DOTALL
        )
        
        val matcher = codeBlockPattern.matcher(response)
        while (matcher.find()) {
            val codeContent: String = matcher.group(1).trim()
            
            if (codeContent.contains("@@") || codeContent.contains("+++") || codeContent.contains("---")) {
                val diffReplacements: List<CodeReplacement> = parseDiffContent(codeContent)
                replacements.addAll(diffReplacements)
            } else {
                replacements.add(
                    CodeReplacement(
                        originalCode = "",
                        newCode = codeContent,
                        startLine = -1,
                        endLine = -1,
                        replacementType = ReplacementType.PSI_BASED
                    )
                )
            }
        }
        
        return replacements
    }

    /**
     * Parses diff content to extract specific line changes
     */
    private fun parseDiffContent(diffContent: String): List<CodeReplacement> {
        val replacements: MutableList<CodeReplacement> = mutableListOf()
        val lines: List<String> = diffContent.split("\n")
        
        var currentReplacement: CodeReplacement? = null
        val oldLines: MutableList<String> = mutableListOf()
        val newLines: MutableList<String> = mutableListOf()
        var startLine: Int = -1
        
        for (line: String in lines) {
            when {
                line.startsWith("@@") -> {
                    // Save previous replacement if exists
                    currentReplacement?.let { replacements.add(it) }
                    
                    // Parse line numbers from @@ -start,count +start,count @@
                    val lineNumberPattern: Pattern = Pattern.compile("@@\\s*-([0-9]+)")
                    val matcher = lineNumberPattern.matcher(line)
                    if (matcher.find()) {
                        startLine = matcher.group(1).toInt()
                    }
                    oldLines.clear()
                    newLines.clear()
                }
                line.startsWith("-") && !line.startsWith("---") -> {
                    oldLines.add(line.substring(1))
                }
                line.startsWith("+") && !line.startsWith("+++") -> {
                    newLines.add(line.substring(1))
                }
                line.startsWith(" ") -> {
                    // Context line - if we have pending changes, create replacement
                    if (oldLines.isNotEmpty() || newLines.isNotEmpty()) {
                        currentReplacement = CodeReplacement(
                            originalCode = oldLines.joinToString("\n"),
                            newCode = newLines.joinToString("\n"),
                            startLine = startLine,
                            endLine = startLine + oldLines.size - 1,
                            replacementType = ReplacementType.HYBRID
                        )
                        oldLines.clear()
                        newLines.clear()
                    }
                }
                else -> {
                    // Handle any other line types - this makes the when exhaustive
                    // For now, we just ignore unknown line types
                }
            }
        }
        
        // Add final replacement if exists
        if (oldLines.isNotEmpty() || newLines.isNotEmpty()) {
            currentReplacement = CodeReplacement(
                originalCode = oldLines.joinToString("\n"),
                newCode = newLines.joinToString("\n"),
                startLine = startLine,
                endLine = startLine + oldLines.size - 1,
                replacementType = ReplacementType.HYBRID
            )
            currentReplacement?.let { replacements.add(it) }
        }
        
        return replacements
    }

    /**
     * Applies a code replacement to the given editor using the appropriate strategy
     */
    fun applyReplacement(editor: Editor, replacement: CodeReplacement): Boolean {
        val document: Document = editor.document
        val psiFile: PsiFile? = documentManager.getPsiFile(document)
        
        return when (replacement.replacementType) {
            ReplacementType.DIFF_BASED -> applyDiffReplacement(document, replacement)
            ReplacementType.PSI_BASED -> applyPsiReplacement(psiFile, replacement)
            ReplacementType.HYBRID -> applyHybridReplacement(editor, psiFile, replacement)
        }
    }

    /**
     * Simple diff-based replacement using document operations
     */
    private fun applyDiffReplacement(document: Document, replacement: CodeReplacement): Boolean {
        return try {
            WriteCommandAction.runWriteCommandAction(project) {
                if (replacement.startLine >= 0 && replacement.endLine >= 0) {
                    val startOffset: Int = document.getLineStartOffset(replacement.startLine - 1)
                    val endOffset: Int = document.getLineEndOffset(replacement.endLine - 1)
                    document.replaceString(startOffset, endOffset, replacement.newCode)
                } else {
                    // Append at end if no specific lines specified
                    document.insertString(document.textLength, "\n" + replacement.newCode)
                }
                documentManager.commitDocument(document)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * PSI-based replacement using AST operations
     */
    private fun applyPsiReplacement(psiFile: PsiFile?, replacement: CodeReplacement): Boolean {
        if (psiFile == null) return false
        
        return try {
            WriteCommandAction.runWriteCommandAction(project) {
                // For Kotlin files, use KtPsiFactory
                if (psiFile is KtFile) {
                    val factory: KtPsiFactory = KtPsiFactory(project)
                    val newElement: KtElement? = createKotlinElement(factory, replacement.newCode)
                    newElement?.let { 
                        val targetElement: PsiElement? = findTargetElement(psiFile, replacement.originalCode)
                        targetElement?.replace(it)
                    }
                } else {
                    // Fallback for non-Kotlin files
                    val doc: Document? = documentManager.getDocument(psiFile)
                    if (doc != null) {
                        applyDiffReplacement(doc, replacement)
                    }
                }
                
                documentManager.getDocument(psiFile)?.let { doc ->
                    documentManager.commitDocument(doc)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Hybrid approach: Use diff precision with PSI safety
     */
    private fun applyHybridReplacement(
        editor: Editor, 
        psiFile: PsiFile?, 
        replacement: CodeReplacement
    ): Boolean {
        if (psiFile == null) return false
        
        return try {
            WriteCommandAction.runWriteCommandAction(project) {
                // First, locate the target using diff information
                val document: Document = editor.document
                val targetRange: TextRange? = findTargetRange(document, replacement)
                
                if (targetRange != null) {
                    // Find corresponding PSI element
                    val targetElement: PsiElement? = psiFile.findElementAt(targetRange.startOffset)
                    
                    if (targetElement != null && psiFile is KtFile) {
                        // Use PSI to safely replace for Kotlin files
                        val factory: KtPsiFactory = KtPsiFactory(project)
                        val newElement: KtElement? = createKotlinElement(factory, replacement.newCode)
                        newElement?.let {
                            val elementToReplace: PsiElement = findParentElementToReplace(targetElement)
                            elementToReplace.replace(it)
                        }
                    } else {
                        // Fallback to document replacement
                        document.replaceString(targetRange.startOffset, targetRange.endOffset, replacement.newCode)
                    }
                } else {
                    // Fallback to simple append
                    document.insertString(document.textLength, "\n" + replacement.newCode)
                }
                
                documentManager.commitDocument(document)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun findTargetRange(document: Document, replacement: CodeReplacement): TextRange? {
        return if (replacement.startLine >= 0 && replacement.endLine >= 0) {
            val startOffset: Int = document.getLineStartOffset(replacement.startLine - 1)
            val endOffset: Int = document.getLineEndOffset(replacement.endLine - 1)
            TextRange(startOffset, endOffset)
        } else {
            null
        }
    }

    /**
     * Creates Kotlin PSI elements using KtPsiFactory
     */
    private fun createKotlinElement(factory: KtPsiFactory, code: String): KtElement? {
        return try {
            // Try to create as expression first
            factory.createExpression(code)
        } catch (e: Exception) {
            try {
                // Try as function
                factory.createFunction(code)
            } catch (e2: Exception) {
                try {
                    // Try as property
                    factory.createProperty(code)
                } catch (e3: Exception) {
                    try {
                        // Try as block
                        factory.createBlock(code)
                    } catch (e4: Exception) {
                        null
                    }
                }
            }
        }
    }

    private fun findTargetElement(psiFile: PsiFile, originalCode: String): PsiElement? {
        // Simple text-based search for now
        val text: String = psiFile.text
        val index: Int = text.indexOf(originalCode.trim())
        return if (index >= 0) psiFile.findElementAt(index) else null
    }

    /**
     * Finds the most appropriate parent element to replace for Kotlin code
     */
    private fun findParentElementToReplace(element: PsiElement): PsiElement {
        // For Kotlin files, try Kotlin-specific elements first
        element.getParentOfType<KtExpression>(strict = false)?.let { return it }
        element.getParentOfType<KtFunction>(strict = false)?.let { return it }
        element.getParentOfType<KtProperty>(strict = false)?.let { return it }
        element.getParentOfType<KtClassOrObject>(strict = false)?.let { return it }
        
        // Fallback to generic PSI elements
        PsiTreeUtil.getParentOfType(element, PsiStatement::class.java)?.let { return it }
        PsiTreeUtil.getParentOfType(element, PsiExpression::class.java)?.let { return it }
        
        // Return the element itself as final fallback
        return element
    }
}
