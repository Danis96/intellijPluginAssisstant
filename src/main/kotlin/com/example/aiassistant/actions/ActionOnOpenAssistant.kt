package com.example.aiassistant.actions

import com.example.aiassistant.settings.AccessTokenState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager

class OpenAIAssistantAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val toolWindowManager: ToolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow: ToolWindow = toolWindowManager.getToolWindow("Genie AI Assistant")
            ?: return
        toolWindow.activate(null)

        val token: String? = AccessTokenState.getInstance().accessToken
        if (token.isNullOrBlank()) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "Genie AI Assistant")
        }
    }
}