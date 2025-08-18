package com.example.aiassistant.utils.uiHelpers

import java.awt.*
import java.awt.geom.RoundRectangle2D
import javax.swing.border.AbstractBorder

/**
 * Custom rounded border implementation for modern UI components
 */
class RoundedBorder(
    private val color: Color,
    private val thickness: Int = 1,
    private val radius: Int = 8
) : AbstractBorder() {

    override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
        val g2: Graphics2D = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.color = color
        g2.stroke = BasicStroke(thickness.toFloat())
        
        val adjustedX: Int = x + thickness / 2
        val adjustedY: Int = y + thickness / 2
        val adjustedWidth: Int = width - thickness
        val adjustedHeight: Int = height - thickness
        
        g2.draw(RoundRectangle2D.Double(
            adjustedX.toDouble(),
            adjustedY.toDouble(),
            adjustedWidth.toDouble(),
            adjustedHeight.toDouble(),
            radius.toDouble(),
            radius.toDouble()
        ))
        
        g2.dispose()
    }

    override fun getBorderInsets(c: Component): Insets {
        return Insets(thickness, thickness, thickness, thickness)
    }

    override fun isBorderOpaque(): Boolean = false
}

/**
 * Rounded border with background fill
 */
class RoundedBackgroundBorder(
    private val backgroundColor: Color,
    private val borderColor: Color? = null,
    private val thickness: Int = 1,
    private val radius: Int = 8
) : AbstractBorder() {

    override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
        val g2: Graphics2D = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        
        // Fill background
        g2.color = backgroundColor
        g2.fill(RoundRectangle2D.Double(
            x.toDouble(),
            y.toDouble(),
            width.toDouble(),
            height.toDouble(),
            radius.toDouble(),
            radius.toDouble()
        ))
        
        // Draw border if specified
        borderColor?.let { borderCol ->
            g2.color = borderCol
            g2.stroke = BasicStroke(thickness.toFloat())
            
            val adjustedX: Int = x + thickness / 2
            val adjustedY: Int = y + thickness / 2
            val adjustedWidth: Int = width - thickness
            val adjustedHeight: Int = height - thickness
            
            g2.draw(RoundRectangle2D.Double(
                adjustedX.toDouble(),
                adjustedY.toDouble(),
                adjustedWidth.toDouble(),
                adjustedHeight.toDouble(),
                radius.toDouble(),
                radius.toDouble()
            ))
        }
        
        g2.dispose()
    }

    override fun getBorderInsets(c: Component): Insets {
        return Insets(thickness, thickness, thickness, thickness)
    }

    override fun isBorderOpaque(): Boolean = false
}

/**
 * Utility object for creating common rounded borders
 */
object BorderHelper {
    
    /**
     * Creates a simple rounded border with specified color and radius
     */
    fun createRoundedBorder(
        color: Color = ColorHelper.borderColor,
        thickness: Int = 1,
        radius: Int = 8
    ): RoundedBorder {
        return RoundedBorder(color, thickness, radius)
    }
    
    /**
     * Creates a rounded border with background fill
     */
    fun createRoundedBackgroundBorder(
        backgroundColor: Color = ColorHelper.cardColor,
        borderColor: Color? = ColorHelper.borderColor,
        thickness: Int = 1,
        radius: Int = 8
    ): RoundedBackgroundBorder {
        return RoundedBackgroundBorder(backgroundColor, borderColor, thickness, radius)
    }
    
    /**
     * Pre-defined border styles for common UI elements
     */
    object Styles {
        // Card borders
        fun cardBorder(radius: Int = 8): RoundedBorder = 
            createRoundedBorder(ColorHelper.borderColor, 1, radius)
            
        fun cardBorderPrimary(radius: Int = 8): RoundedBorder = 
            createRoundedBorder(ColorHelper.primaryColor, 2, radius)
            
        fun cardBorderAccent(radius: Int = 8): RoundedBorder = 
            createRoundedBorder(ColorHelper.accentColor, 2, radius)
        
        // Input field borders
        fun inputBorder(radius: Int = 6): RoundedBorder = 
            createRoundedBorder(ColorHelper.borderColor, 1, radius)
            
        fun inputBorderFocused(radius: Int = 6): RoundedBorder = 
            createRoundedBorder(ColorHelper.focusColor, 2, radius)
        
        // Button borders
        fun buttonBorder(radius: Int = 6): RoundedBorder = 
            createRoundedBorder(ColorHelper.primaryColor, 1, radius)
            
        fun buttonBackgroundPrimary(radius: Int = 6): RoundedBackgroundBorder = 
            createRoundedBackgroundBorder(
                ColorHelper.Buttons.Primary.background,
                null,
                0,
                radius
            )
            
        fun buttonBackgroundSecondary(radius: Int = 6): RoundedBackgroundBorder = 
            createRoundedBackgroundBorder(
                ColorHelper.Buttons.Secondary.background,
                ColorHelper.Buttons.Secondary.border,
                1,
                radius
            )
        
        // Status borders
        fun successBorder(radius: Int = 8): RoundedBorder = 
            createRoundedBorder(ColorHelper.successColor, 1, radius)
            
        fun errorBorder(radius: Int = 8): RoundedBorder = 
            createRoundedBorder(ColorHelper.errorColor, 1, radius)
            
        fun warningBorder(radius: Int = 8): RoundedBorder = 
            createRoundedBorder(ColorHelper.warningColor, 1, radius)
    }
}
