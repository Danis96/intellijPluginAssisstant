package com.example.aiassistant.toolwindow

import com.example.aiassistant.ui.AIAssistantPanel
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import javax.swing.Icon
import com.intellij.openapi.util.IconLoader

class AIAssistantToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel: AIAssistantPanel = AIAssistantPanel(project)
        val contentFactory: ContentFactory = ContentFactory.getInstance()
        val content: Content = contentFactory.createContent(panel, "", false)

        val icon: Icon = IconLoader.getIcon("/icons/ai.png", javaClass)
		content.icon = icon

        toolWindow.contentManager.addContent(content)
    }
}