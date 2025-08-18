package com.example.aiassistant.utils.uiHelpers

import com.intellij.ui.JBColor
import java.awt.Color

/**
 * Centralized color palette for the Genie AI Assistant plugin.
 * Provides consistent theme-aware colors for both light and dark modes.
 */
object ColorHelper {

    // White and black
    val whiteColor: Color = JBColor(Color(255, 255, 255), Color(255, 255, 255))
    val blackColor: Color = JBColor(Color(0, 0, 0), Color(0, 0, 0))

    // Primary brand colors
    val primaryColor: Color = JBColor(Color(74, 144, 226), Color(88, 166, 255))
    val accentColor: Color = JBColor(Color(52, 168, 83), Color(67, 195, 103))
    
    // Status colors
    val successColor: Color = JBColor(Color(52, 168, 83), Color(67, 195, 103))
    val errorColor: Color = JBColor(Color(217, 48, 37), Color(244, 67, 54))
    val warningColor: Color = JBColor(Color(255, 152, 0), Color(255, 193, 7))
    val infoColor: Color = JBColor(Color(33, 150, 243), Color(100, 181, 246))
    
    // Background colors
    val backgroundColor: Color = JBColor(Color(248, 249, 250), Color(43, 43, 43))
    val cardColor: Color = JBColor(Color.WHITE, Color(60, 63, 65))
    val surfaceColor: Color = JBColor(Color(250, 250, 250), Color(48, 48, 48))
    
    // Border and separator colors
    val borderColor: Color = JBColor(Color(218, 220, 224), Color(85, 85, 85))
    val borderColorLight: Color = JBColor(Color(232, 234, 237), Color(70, 70, 70))
    val borderColorDark: Color = JBColor(Color(189, 193, 198), Color(100, 100, 100))
    
    // Text colors
    val textPrimaryColor: Color = JBColor.foreground()
    val textSecondaryColor: Color = JBColor(Color(95, 99, 104), Color(187, 187, 187))
    val textDisabledColor: Color = JBColor(Color(154, 160, 166), Color(128, 128, 128))
    val textLinkColor: Color = JBColor(Color(26, 115, 232), Color(138, 180, 248))
    
    // Interactive element colors
    val hoverColor: Color = JBColor(Color(240, 242, 245), Color(55, 55, 55))
    val activeColor: Color = JBColor(Color(232, 240, 254), Color(25, 45, 75))
    val focusColor: Color = JBColor(Color(26, 115, 232), Color(138, 180, 248))
    
    // Special utility colors
    val overlayColor: Color = JBColor(Color(0, 0, 0, 50), Color(0, 0, 0, 80))
    val highlightColor: Color = JBColor(Color(255, 249, 196), Color(64, 54, 32))
    
    /**
     * Returns a darker variant of the given color for hover effects
     */
    fun getDarkerVariant(color: Color): Color {
        return Color(
            (color.red * 0.8).toInt().coerceIn(0, 255),
            (color.green * 0.8).toInt().coerceIn(0, 255),
            (color.blue * 0.8).toInt().coerceIn(0, 255),
            color.alpha
        )
    }
    
    /**
     * Returns a lighter variant of the given color for subtle backgrounds
     */
    fun getLighterVariant(color: Color): Color {
        return Color(
            (color.red + (255 - color.red) * 0.3).toInt().coerceIn(0, 255),
            (color.green + (255 - color.green) * 0.3).toInt().coerceIn(0, 255),
            (color.blue + (255 - color.blue) * 0.3).toInt().coerceIn(0, 255),
            color.alpha
        )
    }
    
    /**
     * Returns a color with specified alpha transparency
     */
    fun withAlpha(color: Color, alpha: Int): Color {
        return Color(color.red, color.green, color.blue, alpha.coerceIn(0, 255))
    }
    
    /**
     * Returns a color with specified alpha transparency (0.0 to 1.0)
     */
    fun withAlpha(color: Color, alpha: Float): Color {
        return withAlpha(color, (alpha * 255).toInt())
    }
    
    /**
     * Gets the appropriate text color for the given background color
     */
    fun getContrastingTextColor(backgroundColor: Color): Color {
        val luminance: Double = (0.299 * backgroundColor.red + 
                                0.587 * backgroundColor.green + 
                                0.114 * backgroundColor.blue) / 255
        return if (luminance > 0.5) Color.BLACK else Color.WHITE
    }
    
    /**
     * Color scheme for different UI states
     */
    object States {
        val neutral: Color = textSecondaryColor
        val loading: Color = primaryColor
        val success: Color = successColor
        val error: Color = errorColor
        val warning: Color = warningColor
        val info: Color = infoColor
    }
    
    /**
     * Pre-defined color combinations for buttons
     */
    object Buttons {
        object Primary {
            val background: Color = accentColor
            val foreground: Color = Color.WHITE
            val hover: Color = getDarkerVariant(accentColor)
            val disabled: Color = textDisabledColor
        }
        
        object Secondary {
            val background: Color = backgroundColor
            val foreground: Color = primaryColor
            val hover: Color = hoverColor
            val border: Color = primaryColor
        }
        
        object Danger {
            val background: Color = errorColor
            val foreground: Color = Color.WHITE
            val hover: Color = getDarkerVariant(errorColor)
        }
        
        object Success {
            val background: Color = successColor
            val foreground: Color = Color.WHITE
            val hover: Color = getDarkerVariant(successColor)
        }
    }
    
    /**
     * Color palette for message types in chat
     */
    object Messages {
        val userPrefix: Color = primaryColor
        val assistantPrefix: Color = accentColor
        val errorPrefix: Color = errorColor
        val infoPrefix: Color = infoColor
        val systemPrefix: Color = warningColor
    }
}
