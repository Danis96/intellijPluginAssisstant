package com.example.aiassistant.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class AIAssistantConfigurable : Configurable {

    private var panel: JPanel? = null
    private val tokenField: JBPasswordField = JBPasswordField()
    private val noteField: JBTextField = JBTextField()

    override fun getDisplayName(): String {
        return "AI Assistant"
    }

    override fun createComponent(): JComponent {
        val currentToken: String? = AccessTokenState.getInstance().accessToken
        tokenField.text = currentToken ?: ""
        noteField.text = "Enter your access token"

        val form: JPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Access Token:", tokenField, 1, false)
            .addComponent(noteField, 1)
            .panel

        panel = form
        return form
    }

    override fun isModified(): Boolean {
        val saved: String = AccessTokenState.getInstance().accessToken ?: ""
        val current: String = String(tokenField.password)
        return saved != current
    }

    override fun apply() {
        val newToken: String = String(tokenField.password)
        AccessTokenState.getInstance().accessToken = newToken.ifBlank { null }
    }

    override fun reset() {
        val saved: String = AccessTokenState.getInstance().accessToken ?: ""
        tokenField.text = saved
    }

    override fun disposeUIResources() {
        panel = null
    }
}