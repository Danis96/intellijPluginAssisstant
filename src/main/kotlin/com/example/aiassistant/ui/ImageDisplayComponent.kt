package com.example.aiassistant.ui

import com.example.aiassistant.utils.uiHelpers.BorderHelper
import com.example.aiassistant.utils.uiHelpers.ColorHelper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URL
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Component for displaying images in the AI assistant chat
 */
class ImageDisplayComponent(
    private val imageContent: ImageContent,
    private val project: Project?
) : JPanel(BorderLayout()) {
    
    private val maxWidth: Int = 400
    private val maxHeight: Int = 300
    private val thumbnailWidth: Int = 200
    private val thumbnailHeight: Int = 150
    
    private var originalImage: BufferedImage? = null
    private var isLoading: Boolean = false
    
    init {
        setupImageDisplay()
    }
    
    private fun setupImageDisplay() {
        background = ColorHelper.surfaceColor
        border = CompoundBorder(
            BorderHelper.createRoundedBackgroundBorder(
                ColorHelper.surfaceColor, 
                ColorHelper.borderColor, 
                1, 
                8
            ),
            EmptyBorder(8, 8, 8, 8)
        )
        
        when (imageContent.type) {
            ImageType.EMBEDDED_BASE64 -> displayBase64Image()
            ImageType.URL_REFERENCE -> displayUrlImage()
            ImageType.LOCAL_FILE -> displayLocalImage()
            ImageType.INLINE_DIAGRAM -> renderDiagram()
            ImageType.AI_GENERATED -> displayAiGeneratedImage()
        }
    }
    
    private fun displayBase64Image() {
        try {
            val base64Data: String = imageContent.source.substringAfter("base64,")
            
            // Check if this is an SVG image
            if (imageContent.source.contains("image/svg+xml")) {
                displaySvgImage(base64Data)
                return
            }
            
            val imageBytes: ByteArray = Base64.getDecoder().decode(base64Data)
            val bufferedImage: BufferedImage? = ImageIO.read(ByteArrayInputStream(imageBytes))
            
            if (bufferedImage != null) {
                originalImage = bufferedImage
                val scaledImage: BufferedImage = scaleImage(bufferedImage, thumbnailWidth, thumbnailHeight)
                
                val imageLabel = JLabel(ImageIcon(scaledImage))
                imageLabel.toolTipText = imageContent.alt.ifEmpty { "Base64 Image" }
                imageLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                imageLabel.addMouseListener(object : java.awt.event.MouseAdapter() {
                    override fun mouseClicked(e: java.awt.event.MouseEvent) {
                        showFullSizeImage()
                    }
                })
                
                add(imageLabel, BorderLayout.CENTER)
                addImageControls()
            } else {
                showImageError("Failed to decode base64 image - format may not be supported")
            }
            
        } catch (e: IllegalArgumentException) {
            showImageError("Invalid base64 data: ${e.message}")
        } catch (e: Exception) {
            showImageError("Failed to load base64 image: ${e.message}")
        }
    }
    
    private fun displaySvgImage(base64Data: String) {
        try {
            // For SVG images, we'll show a placeholder with SVG source
            val svgPanel = JPanel(BorderLayout())
            svgPanel.background = ColorHelper.cardColor
            svgPanel.border = EmptyBorder(8, 8, 8, 8)
            
            val headerLabel = JLabel("🖼️ SVG Image", SwingConstants.CENTER)
            headerLabel.font = Font(Font.SANS_SERIF, Font.BOLD, 12)
            headerLabel.foreground = ColorHelper.primaryColor
            
            val infoLabel = JLabel("SVG rendering not supported yet", SwingConstants.CENTER)
            infoLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 10)
            infoLabel.foreground = ColorHelper.textSecondaryColor
            
            // Decode and show SVG source
            val svgSource: String = String(Base64.getDecoder().decode(base64Data))
            val sourceArea = JTextArea(svgSource)
            sourceArea.font = Font("JetBrains Mono", Font.PLAIN, 10)
            sourceArea.isEditable = false
            sourceArea.background = ColorHelper.backgroundColor
            sourceArea.foreground = JBColor.foreground()
            sourceArea.border = EmptyBorder(4, 4, 4, 4)
            
            val scrollPane = JBScrollPane(sourceArea)
            scrollPane.preferredSize = Dimension(thumbnailWidth, 100)
            scrollPane.border = BorderHelper.createRoundedBackgroundBorder(
                ColorHelper.backgroundColor,
                ColorHelper.borderColor,
                1,
                4
            )
            
            val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER))
            buttonPanel.background = ColorHelper.cardColor
            
            val copyButton = JButton("📋 Copy SVG")
            copyButton.font = Font(Font.SANS_SERIF, Font.PLAIN, 10)
            copyButton.addActionListener { 
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                val stringSelection = java.awt.datatransfer.StringSelection(svgSource)
                clipboard.setContents(stringSelection, null)
                showInfo("SVG source copied to clipboard!")
            }
            
            buttonPanel.add(copyButton)
            
            svgPanel.add(headerLabel, BorderLayout.NORTH)
            svgPanel.add(infoLabel, BorderLayout.CENTER)
            svgPanel.add(scrollPane, BorderLayout.SOUTH)
            
            add(svgPanel, BorderLayout.CENTER)
            add(buttonPanel, BorderLayout.SOUTH)
            
        } catch (e: Exception) {
            showImageError("Failed to process SVG: ${e.message}")
        }
    }
    
    private fun displayUrlImage() {
        // Show loading placeholder first
        val loadingPanel: JPanel = createLoadingPanel()
        add(loadingPanel, BorderLayout.CENTER)
        isLoading = true
        
        // Load image asynchronously
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val url = URL(imageContent.source)
                val bufferedImage: BufferedImage = ImageIO.read(url)
                
                if (bufferedImage != null) {
                    originalImage = bufferedImage
                    val scaledImage: BufferedImage = scaleImage(bufferedImage, thumbnailWidth, thumbnailHeight)
                    
                    SwingUtilities.invokeLater {
                        remove(loadingPanel)
                        val imageLabel = JLabel(ImageIcon(scaledImage))
                        imageLabel.toolTipText = imageContent.alt.ifEmpty { "Image from URL" }
                        imageLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        imageLabel.addMouseListener(object : java.awt.event.MouseAdapter() {
                            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                                showFullSizeImage()
                            }
                        })
                        
                        add(imageLabel, BorderLayout.CENTER)
                        addImageControls()
                        isLoading = false
                        revalidate()
                        repaint()
                    }
                } else {
                    SwingUtilities.invokeLater {
                        remove(loadingPanel)
                        showImageError("Failed to load image from URL")
                        isLoading = false
                        revalidate()
                        repaint()
                    }
                }
                
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    remove(loadingPanel)
                    showImageError("Failed to load image from URL: ${e.message}")
                    isLoading = false
                    revalidate()
                    repaint()
                }
            }
        }
    }
    
    private fun displayLocalImage() {
        try {
            val filePath: String = imageContent.source.removePrefix("file://")
            val file = File(filePath)
            
            if (file.exists()) {
                val bufferedImage: BufferedImage = ImageIO.read(file)
                
                if (bufferedImage != null) {
                    originalImage = bufferedImage
                    val scaledImage: BufferedImage = scaleImage(bufferedImage, thumbnailWidth, thumbnailHeight)
                    
                    val imageLabel = JLabel(ImageIcon(scaledImage))
                    imageLabel.toolTipText = imageContent.alt.ifEmpty { file.name }
                    imageLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    imageLabel.addMouseListener(object : java.awt.event.MouseAdapter() {
                        override fun mouseClicked(e: java.awt.event.MouseEvent) {
                            showFullSizeImage()
                        }
                    })
                    
                    add(imageLabel, BorderLayout.CENTER)
                    addImageControls()
                } else {
                    showImageError("Failed to read image file: ${file.name}")
                }
            } else {
                showImageError("Local file not found: ${file.absolutePath}")
            }
        } catch (e: Exception) {
            showImageError("Failed to load local image: ${e.message}")
        }
    }
    
    private fun renderDiagram() {
        val diagramType: String = imageContent.diagramType ?: "unknown"
        
        when (diagramType.lowercase()) {
            "mermaid" -> renderMermaidDiagram()
            "plantuml" -> renderPlantUMLDiagram()
            else -> renderGenericDiagram()
        }
    }
    
    private fun renderMermaidDiagram() {
        val diagramPanel = JPanel(BorderLayout())
        diagramPanel.background = ColorHelper.cardColor
        diagramPanel.border = EmptyBorder(8, 8, 8, 8)
        
        val headerPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        headerPanel.background = ColorHelper.cardColor
        
        val headerLabel = JLabel("📊 Mermaid Diagram")
        headerLabel.font = Font(Font.SANS_SERIF, Font.BOLD, 12)
        headerLabel.foreground = ColorHelper.primaryColor
        headerPanel.add(headerLabel)
        
        val sourceArea = JTextArea(imageContent.source)
        sourceArea.font = Font("JetBrains Mono", Font.PLAIN, 11)
        sourceArea.isEditable = false
        sourceArea.background = ColorHelper.cardColor
        sourceArea.foreground = JBColor.foreground()
        sourceArea.border = EmptyBorder(8, 8, 8, 8)
        
        val scrollPane = JBScrollPane(sourceArea)
        scrollPane.preferredSize = Dimension(maxWidth, 120)
        scrollPane.border = BorderHelper.createRoundedBackgroundBorder(
            ColorHelper.backgroundColor,
            ColorHelper.borderColor,
            1,
            4
        )
        
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        buttonPanel.background = ColorHelper.cardColor
        
        val renderButton = JButton("🎨 Render")
        renderButton.font = Font(Font.SANS_SERIF, Font.PLAIN, 10)
        renderButton.toolTipText = "Render diagram (future feature)"
        renderButton.addActionListener { 
            showInfo("Diagram rendering will be implemented in a future version")
        }
        
        val copyButton = JButton("📋 Copy")
        copyButton.font = Font(Font.SANS_SERIF, Font.PLAIN, 10)
        copyButton.toolTipText = "Copy diagram source"
        copyButton.addActionListener { 
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val stringSelection = java.awt.datatransfer.StringSelection(imageContent.source)
            clipboard.setContents(stringSelection, null)
            showInfo("Diagram source copied to clipboard!")
        }
        
        buttonPanel.add(copyButton)
        buttonPanel.add(renderButton)
        
        diagramPanel.add(headerPanel, BorderLayout.NORTH)
        diagramPanel.add(scrollPane, BorderLayout.CENTER)
        diagramPanel.add(buttonPanel, BorderLayout.SOUTH)
        
        add(diagramPanel, BorderLayout.CENTER)
    }
    
    private fun renderPlantUMLDiagram() {
        // Similar to Mermaid but with PlantUML specific handling
        val diagramPanel = JPanel(BorderLayout())
        diagramPanel.background = ColorHelper.cardColor
        diagramPanel.border = EmptyBorder(8, 8, 8, 8)
        
        val headerLabel = JLabel("🏗️ PlantUML Diagram")
        headerLabel.font = Font(Font.SANS_SERIF, Font.BOLD, 12)
        headerLabel.foreground = ColorHelper.primaryColor
        
        val sourceArea = JTextArea(imageContent.source)
        sourceArea.font = Font("JetBrains Mono", Font.PLAIN, 11)
        sourceArea.isEditable = false
        sourceArea.background = ColorHelper.cardColor
        sourceArea.foreground = JBColor.foreground()
        
        val scrollPane = JBScrollPane(sourceArea)
        scrollPane.preferredSize = Dimension(maxWidth, 120)
        
        diagramPanel.add(headerLabel, BorderLayout.NORTH)
        diagramPanel.add(scrollPane, BorderLayout.CENTER)
        
        add(diagramPanel, BorderLayout.CENTER)
    }
    
    private fun renderGenericDiagram() {
        val diagramPanel = JPanel(BorderLayout())
        diagramPanel.background = ColorHelper.cardColor
        
        val label = JLabel("📈 Diagram", SwingConstants.CENTER)
        label.font = Font(Font.SANS_SERIF, Font.BOLD, 12)
        
        diagramPanel.add(label, BorderLayout.CENTER)
        add(diagramPanel, BorderLayout.CENTER)
    }
    
    private fun displayAiGeneratedImage() {
        // Placeholder for AI-generated images
        val placeholderPanel = JPanel(BorderLayout())
        placeholderPanel.background = ColorHelper.cardColor
        
        val label = JLabel("🤖 AI Generated Image", SwingConstants.CENTER)
        label.font = Font(Font.SANS_SERIF, Font.BOLD, 12)
        
        placeholderPanel.add(label, BorderLayout.CENTER)
        add(placeholderPanel, BorderLayout.CENTER)
    }
    
    private fun createLoadingPanel(): JPanel {
        val loadingPanel = JPanel(BorderLayout())
        loadingPanel.background = ColorHelper.cardColor
        loadingPanel.preferredSize = Dimension(thumbnailWidth, thumbnailHeight)
        
        val loadingLabel = JLabel("🖼️ Loading image...", SwingConstants.CENTER)
        loadingLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        loadingLabel.foreground = ColorHelper.textSecondaryColor
        
        loadingPanel.add(loadingLabel, BorderLayout.CENTER)
        return loadingPanel
    }
    
    private fun scaleImage(original: BufferedImage, maxWidth: Int, maxHeight: Int): BufferedImage {
        val originalWidth: Int = original.width
        val originalHeight: Int = original.height
        
        // Calculate scaling factor to fit within max dimensions
        val scale: Double = minOf(
            maxWidth.toDouble() / originalWidth,
            maxHeight.toDouble() / originalHeight,
            1.0 // Don't scale up
        )
        
        if (scale >= 1.0) return original
        
        val newWidth: Int = (originalWidth * scale).toInt()
        val newHeight: Int = (originalHeight * scale).toInt()
        
        val scaled = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB)
        val g2d: Graphics2D = scaled.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null)
        g2d.dispose()
        
        return scaled
    }
    
    private fun addImageControls() {
        if (isLoading) return
        
        val controlsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 4))
        controlsPanel.isOpaque = false
        
        // Download/Save button
        val saveButton = JButton("💾")
        saveButton.font = Font(Font.SANS_SERIF, Font.PLAIN, 10)
        saveButton.toolTipText = "Save image"
        saveButton.preferredSize = Dimension(24, 20)
        saveButton.addActionListener { saveImage() }
        
        // Zoom button
        val zoomButton = JButton("🔍")
        zoomButton.font = Font(Font.SANS_SERIF, Font.PLAIN, 10)
        zoomButton.toolTipText = "View full size"
        zoomButton.preferredSize = Dimension(24, 20)
        zoomButton.addActionListener { showFullSizeImage() }
        
        controlsPanel.add(saveButton)
        controlsPanel.add(zoomButton)
        
        add(controlsPanel, BorderLayout.SOUTH)
    }
    
    private fun saveImage() {
        if (originalImage == null) {
            showError("No image to save")
            return
        }
        
        val fileChooser = JFileChooser()
        fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        fileChooser.fileFilter = FileNameExtensionFilter("PNG Images", "png")
        fileChooser.selectedFile = File("image.png")
        
        val result: Int = fileChooser.showSaveDialog(this)
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                val file: File = fileChooser.selectedFile
                ImageIO.write(originalImage!!, "PNG", file)
                showInfo("Image saved successfully: ${file.name}")
            } catch (e: Exception) {
                showError("Failed to save image: ${e.message}")
            }
        }
    }
    
    private fun showFullSizeImage() {
        if (originalImage == null) return
        
        val dialog = JDialog()
        dialog.title = imageContent.alt.ifEmpty { "Image Viewer" }
        dialog.isModal = true
        dialog.defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
        
        val imageLabel = JLabel(ImageIcon(originalImage!!))
        val scrollPane = JScrollPane(imageLabel)
        scrollPane.preferredSize = Dimension(
            minOf(originalImage!!.width + 50, 800),
            minOf(originalImage!!.height + 50, 600)
        )
        
        dialog.add(scrollPane)
        dialog.pack()
        dialog.setLocationRelativeTo(this)
        dialog.isVisible = true
    }
    
    private fun showImageError(message: String) {
        val errorPanel = JPanel(BorderLayout())
        errorPanel.background = ColorHelper.withAlpha(ColorHelper.errorColor, 0.1f)
        errorPanel.border = EmptyBorder(16, 16, 16, 16)
        errorPanel.preferredSize = Dimension(thumbnailWidth, 80)
        
        val errorLabel = JLabel("❌ $message", SwingConstants.CENTER)
        errorLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 11)
        errorLabel.foreground = ColorHelper.errorColor
        
        errorPanel.add(errorLabel, BorderLayout.CENTER)
        add(errorPanel, BorderLayout.CENTER)
    }
    
    private fun showInfo(message: String) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Information",
            JOptionPane.INFORMATION_MESSAGE
        )
    }
    
    private fun showError(message: String) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE
        )
    }
}
