package com.example.aiassistant.actions

import com.example.aiassistant.ui.AIAssistantPanel
import com.example.aiassistant.utils.TextHelper
import com.example.aiassistant.utils.notifyWarn
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager

// NOTE Action does the same as CheckCodeInAssistantAction, but it's for undersection1.

class Undersection1Action : AnAction(TextHelper.Prompts.UNDERSECTION1_ACTION_TEXT) {

	override fun update(e: AnActionEvent) {
		val editor = e.getData(CommonDataKeys.EDITOR)
		val hasSelection: Boolean = editor?.selectionModel?.hasSelection() == true
		e.presentation.isEnabledAndVisible = hasSelection
	}

	override fun actionPerformed(e: AnActionEvent) {
		val project: Project = e.project ?: return
		val editor = e.getData(CommonDataKeys.EDITOR)
		val selected: String? = editor?.selectionModel?.selectedText?.trim()
		if (selected.isNullOrEmpty()) {
			notifyWarn(project, TextHelper.Notifications.WARNING_TITLE, TextHelper.Prompts.NO_SELECTION_WARNING)
			return
		}

		val vFile: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)
		val languageId: String = languageHintFromExtension(vFile?.extension)

		val codeBlock: String = if (languageId.isNotEmpty()) {
			"```$languageId\n$selected\n```"
		} else {
			"```\n$selected\n```"
		}

		val promptText: String = TextHelper.format(TextHelper.Prompts.UNDERSECTION1_TEMPLATE, codeBlock)

        val toolWindowManager: ToolWindowManager = ToolWindowManager.getInstance(project)
		val toolWindow: ToolWindow = toolWindowManager.getToolWindow("Genie AI Assistant") ?: return

		toolWindow.activate({
			val content = toolWindow.contentManager.selectedContent ?: toolWindow.contentManager.contents.firstOrNull()
			val component = content?.component
			if (component is AIAssistantPanel) {
				component.insertPreparedPrompt(promptText, true)
			} else {
				notifyWarn(project, TextHelper.Notifications.WARNING_TITLE, TextHelper.Prompts.UNABLE_TO_FIND_PANEL_WARNING)
			}
		}, true)
	}

	private fun languageHintFromExtension(ext: String?): String {
		return when (ext?.lowercase()) {
			"kt" -> "kotlin"
			"kts" -> "kotlin"
			"java" -> "java"
			"py" -> "python"
			"js" -> "javascript"
			"ts" -> "typescript"
			"tsx" -> "tsx"
			"jsx" -> "jsx"
			"go" -> "go"
			"rb" -> "ruby"
			"php" -> "php"
			"cs" -> "csharp"
			"cpp", "cc", "cxx", "hpp", "hh", "hxx" -> "cpp"
			"c", "h" -> "c"
			"rs" -> "rust"
			"swift" -> "swift"
			"scala" -> "scala"
			"sh", "bash" -> "bash"
			"json" -> "json"
			"yaml", "yml" -> "yaml"
			"xml" -> "xml"
			else -> ""
		}
	}
}