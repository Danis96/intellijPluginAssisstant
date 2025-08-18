# Image Implementation Documentation

## Overview

This document provides comprehensive documentation for the image implementation system in the AI Assistant plugin. The system provides rich image support for displaying various types of visual content within the chat interface, including embedded images, diagrams, and AI-generated content.

## Architecture

The image system consists of three main components:

1. **ImageParser** - Parses and extracts image content from chat messages
2. **ImageDisplayComponent** - Renders and displays images in the UI
3. **Supporting Data Classes** - Define image content structure and types

## Core Components

### 1. ImageParser Class

**Location**: `src/main/kotlin/com/example/aiassistant/ui/ImageParser.kt`

The `ImageParser` class is responsible for detecting and extracting image content from chat messages using regular expressions.

#### Supported Image Formats:

- **Markdown Images**: `![alt text](image_url)`
- **Base64 Images**: `data:image/png;base64,iVBOR...`
- **URL Images**: `https://example.com/image.png`
- **Diagram Code Blocks**: 
  - Mermaid: ` ```mermaid ... ``` `
  - PlantUML: ` ```plantuml ... ``` `

#### Key Methods:

```kotlin
fun parseImages(content: String): List<ImageContent>
```
- Parses message content and returns list of detected images
- Handles multiple image types in a single message
- Tracks start/end positions for accurate text removal

```kotlin
fun removeImageMarkdown(content: String): String
```
- Removes image markdown from content to get clean text
- Preserves diagram code blocks as they may contain important content
- Returns sanitized text for display

```kotlin
private fun determineImageType(src: String): ImageType
```
- Automatically determines image type based on source URL/format
- Supports data URLs, HTTP URLs, and file paths
- Provides fallback to URL_REFERENCE for unknown formats

#### Regular Expression Patterns:

```kotlin
// Markdown image pattern: ![alt](src)
private val MARKDOWN_IMAGE_PATTERN: Pattern = Pattern.compile(
    "!\\[([^\\]]*)\\]\\(([^\\)]+)\\)",
    Pattern.DOTALL
)

// Base64 image pattern
private val BASE64_IMAGE_PATTERN: Pattern = Pattern.compile(
    "data:image/([^;]+);base64,([A-Za-z0-9+/=]+)",
    Pattern.DOTALL
)

// URL image pattern
private val URL_IMAGE_PATTERN: Pattern = Pattern.compile(
    "https?://[^\\s]+\\.(png|jpg|jpeg|gif|svg|webp)",
    Pattern.CASE_INSENSITIVE
)

// Mermaid diagram pattern
private val MERMAID_PATTERN: Pattern = Pattern.compile(
    "```mermaid\\s*\\n(.*?)\\n```",
    Pattern.DOTALL
)

// PlantUML diagram pattern
private val PLANTUML_PATTERN: Pattern = Pattern.compile(
    "```plantuml\\s*\\n(.*?)\\n```",
    Pattern.DOTALL
)
```

### 2. ImageDisplayComponent Class

**Location**: `src/main/kotlin/com/example/aiassistant/ui/ImageDisplayComponent.kt`

The `ImageDisplayComponent` class is a Swing `JPanel` that renders different types of images within the AI assistant chat interface.

#### Configuration Constants:

```kotlin
private val maxWidth: Int = 400
private val maxHeight: Int = 300
private val thumbnailWidth: Int = 200
private val thumbnailHeight: Int = 150
```

#### Core Features:

- **Thumbnail Display**: Images are displayed as thumbnails with configurable dimensions
- **Click to Expand**: Users can click thumbnails to view full-size images
- **Save Functionality**: Built-in save dialog for downloading images
- **Loading States**: Asynchronous loading with visual feedback
- **Error Handling**: Graceful error display for failed image loads

#### Image Type Handlers:

##### Base64 Images (`displayBase64Image()`)
- Decodes Base64 data and creates `BufferedImage`
- Special handling for SVG images (shows source code with copy functionality)
- Supports all standard image formats (PNG, JPG, GIF, etc.)
- Includes error handling for invalid Base64 data

```kotlin
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
            // ... display logic
        }
    } catch (e: Exception) {
        showImageError("Failed to load base64 image: ${e.message}")
    }
}
```

##### URL Images (`displayUrlImage()`)
- Asynchronous loading using `ApplicationManager.executeOnPooledThread`
- Shows loading placeholder during download
- Handles network timeouts and connection errors
- Updates UI on EDT thread after loading

```kotlin
private fun displayUrlImage() {
    val loadingPanel: JPanel = createLoadingPanel()
    add(loadingPanel, BorderLayout.CENTER)
    isLoading = true
    
    ApplicationManager.getApplication().executeOnPooledThread {
        try {
            val url = URL(imageContent.source)
            val bufferedImage: BufferedImage = ImageIO.read(url)
            
            SwingUtilities.invokeLater {
                // Update UI on EDT
                remove(loadingPanel)
                // ... display image
                isLoading = false
                revalidate()
                repaint()
            }
        } catch (e: Exception) {
            // Handle errors on EDT
        }
    }
}
```

##### Local File Images (`displayLocalImage()`)
- Supports file:// URLs and absolute paths
- File existence validation
- Cross-platform path handling
- Standard image format support via `ImageIO`

##### Diagram Rendering (`renderDiagram()`)
- **Mermaid Diagrams**: Shows source code with syntax highlighting
- **PlantUML Diagrams**: Similar to Mermaid with PlantUML-specific styling
- **Copy Functionality**: One-click copying of diagram source to clipboard
- **Future Extensibility**: Ready for actual diagram rendering implementation

```kotlin
private fun renderMermaidDiagram() {
    val diagramPanel = JPanel(BorderLayout())
    diagramPanel.background = ColorHelper.cardColor
    
    val headerLabel = JLabel("📊 Mermaid Diagram")
    val sourceArea = JTextArea(imageContent.source)
    sourceArea.font = Font("JetBrains Mono", Font.PLAIN, 11)
    
    val copyButton = JButton("📋 Copy")
    copyButton.addActionListener { 
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val stringSelection = StringSelection(imageContent.source)
        clipboard.setContents(stringSelection, null)
        showInfo("Diagram source copied to clipboard!")
    }
    
    // Layout components...
}
```

#### Image Scaling and Quality

The component includes sophisticated image scaling with quality preservation:

```kotlin
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
```

#### User Interaction Features

##### Image Controls (`addImageControls()`)
- **Save Button** (💾): Opens file dialog for saving images
- **Zoom Button** (🔍): Opens full-size image viewer
- Positioned in bottom-right corner with consistent styling

##### Full-Size Image Viewer (`showFullSizeImage()`)
- Modal dialog with scrollable image view
- Automatic sizing based on image dimensions
- Maximum size constraints for very large images
- Centered positioning relative to parent component

```kotlin
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
```

### 3. Data Classes and Enums

#### ImageContent Data Class

```kotlin
data class ImageContent(
    val type: ImageType,
    val source: String,
    val alt: String = "",
    val width: Int? = null,
    val height: Int? = null,
    val startIndex: Int = 0,
    val endIndex: Int = 0,
    val diagramType: String? = null
)
```

**Properties**:
- `type: ImageType` - The type of image (Base64, URL, etc.)
- `source: String` - The image source (URL, Base64 data, file path, or diagram code)
- `alt: String` - Alternative text for accessibility
- `width/height: Int?` - Optional dimensions (future use)
- `startIndex/endIndex: Int` - Position in original text for accurate removal
- `diagramType: String?` - Specific diagram type for INLINE_DIAGRAM images

#### ImageType Enum

```kotlin
enum class ImageType {
    EMBEDDED_BASE64,    // data:image/png;base64,iVBOR...
    URL_REFERENCE,      // https://example.com/image.png
    LOCAL_FILE,         // file://path/to/image.png or /path/to/image.png
    INLINE_DIAGRAM,     // Mermaid, PlantUML, etc.
    AI_GENERATED        // Images generated by AI (future use)
}
```

#### MessageContent Data Class

```kotlin
data class MessageContent(
    val text: String,
    val images: List<ImageContent> = emptyList(),
    val codeBlocks: List<CodeDiffViewer.CodeBlock> = emptyList()
)
```

Represents enhanced message content with support for multiple media types.

## Integration with AI Assistant Panel

### Usage in AIAssistantPanel

The image system integrates seamlessly with the main chat interface in `AIAssistantPanel.kt`:

```kotlin
private fun addMessage(
    content: String,
    role: String,
    timestamp: String = SimpleDateFormat("HH:mm").format(Date())
): Unit {
    // Parse content for images
    val imageParser = ImageParser()
    val images: List<ImageContent> = imageParser.parseImages(content)
    val textContent: String = if (images.isNotEmpty()) {
        imageParser.removeImageMarkdown(content)
    } else {
        content
    }
    
    // Create enhanced content component with image support
    val contentComponent: JComponent = if (images.isNotEmpty()) {
        createMultiMediaContentPanel(textContent, images, role)
    } else if (role == "assistant" && textContent.contains("```")) {
        CodeDiffViewer(textContent, project, lastSentCode)
    } else {
        // Regular text area for non-code content
        // ...
    }
}
```

### Multi-Media Content Panel

```kotlin
private fun createMultiMediaContentPanel(
    text: String, 
    images: List<ImageContent>, 
    role: String
): JPanel {
    val mainPanel = JPanel()
    mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)
    mainPanel.isOpaque = false
    
    // Add text content if present
    if (text.isNotBlank()) {
        val textComponent: JComponent = if (role == "assistant" && text.contains("```")) {
            CodeDiffViewer(text, project, lastSentCode)
        } else {
            // Regular text area
        }
        mainPanel.add(textComponent)
        
        if (images.isNotEmpty()) {
            mainPanel.add(Box.createVerticalStrut(12))
        }
    }
    
    // Add images
    images.forEach { imageContent: ImageContent ->
        val imageComponent: ImageDisplayComponent = ImageDisplayComponent(imageContent, project)
        imageComponent.maximumSize = Dimension(Int.MAX_VALUE, imageComponent.preferredSize.height)
        mainPanel.add(imageComponent)
        mainPanel.add(Box.createVerticalStrut(8))
    }
    
    return mainPanel
}
```

## UI Design and Styling

### Color System Integration

The image components use the centralized `ColorHelper` for consistent theming:

- `ColorHelper.surfaceColor` - Background color for image containers
- `ColorHelper.borderColor` - Border color for image frames
- `ColorHelper.cardColor` - Background for diagram panels
- `ColorHelper.primaryColor` - Accent color for headers and buttons
- `ColorHelper.textSecondaryColor` - Secondary text color
- `ColorHelper.errorColor` - Error message color

### Border and Layout

Images use rounded borders with consistent padding:

```kotlin
border = CompoundBorder(
    BorderHelper.createRoundedBackgroundBorder(
        ColorHelper.surfaceColor, 
        ColorHelper.borderColor, 
        1, 
        8
    ),
    EmptyBorder(8, 8, 8, 8)
)
```

### Responsive Design

- Images scale proportionally to fit within defined constraints
- Minimum and maximum sizes prevent UI layout issues
- Thumbnail view with click-to-expand functionality
- Scrollable full-size viewer for large images

## Error Handling

### Comprehensive Error Management

The system includes robust error handling for common failure scenarios:

1. **Invalid Base64 Data**: Graceful handling of malformed Base64 strings
2. **Network Timeouts**: Timeout handling for URL-based images
3. **File Not Found**: Clear error messages for missing local files
4. **Unsupported Formats**: Fallback handling for unsupported image types
5. **Memory Constraints**: Proper disposal of graphics resources

### Error Display

```kotlin
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
```

## Performance Considerations

### Asynchronous Loading

- URL images load asynchronously on background threads
- UI updates occur only on the Event Dispatch Thread (EDT)
- Loading indicators provide user feedback during long operations

### Memory Management

- Original images stored only when needed for full-size viewing
- Thumbnail images use optimized scaling algorithms
- Graphics2D resources properly disposed after use
- Base64 decoding optimized for large images

### Caching Strategy

The current implementation does not include caching, but provides foundation for future enhancements:

- Image thumbnails could be cached for repeated display
- Network images could be cached locally
- Diagram renderings could be cached after generation

## Future Enhancements

### Planned Features

1. **SVG Rendering**: Native SVG support instead of source code display
2. **Diagram Rendering**: Actual rendering of Mermaid and PlantUML diagrams
3. **Image Caching**: Local caching for improved performance
4. **Additional Formats**: Support for WebP, AVIF, and other modern formats
5. **Image Editing**: Basic editing capabilities (crop, resize, filters)
6. **Drag & Drop**: Direct image upload via drag and drop
7. **Clipboard Integration**: Paste images directly from clipboard
8. **AI Image Generation**: Integration with AI image generation services

### Extension Points

The architecture provides several extension points for future development:

- New image types can be added to the `ImageType` enum
- Additional parsers can be added to `ImageParser`
- Custom renderers can be implemented for specific image types
- New display modes can be added to `ImageDisplayComponent`

## Testing Considerations

### Unit Testing

Key areas for unit testing:

1. **ImageParser Regular Expressions**: Verify pattern matching accuracy
2. **Image Type Detection**: Test all supported URL/path formats
3. **Base64 Decoding**: Test valid and invalid Base64 inputs
4. **Scaling Algorithms**: Verify proper image scaling and quality

### Integration Testing

1. **UI Integration**: Test image display within chat interface
2. **Asynchronous Loading**: Verify proper thread handling
3. **Error Scenarios**: Test network failures and invalid inputs
4. **User Interactions**: Test click handlers and dialog functionality

### Performance Testing

1. **Large Images**: Test memory usage with high-resolution images
2. **Multiple Images**: Test performance with many images in chat
3. **Network Latency**: Test behavior with slow network connections
4. **Concurrent Loading**: Test multiple simultaneous image loads

## Dependencies

### Required Libraries

- **Java AWT/Swing**: Core UI components and image handling
- **IntelliJ Platform SDK**: Integration with IDE theme system
- **ImageIO**: Standard Java image reading/writing
- **Java Regex**: Pattern matching for image detection

### Optional Dependencies

Future enhancements may require additional dependencies:

- **Apache Batik**: For SVG rendering
- **PlantUML**: For diagram generation
- **Mermaid CLI**: For Mermaid diagram rendering
- **Image Processing Libraries**: For advanced image manipulation

## Configuration

### Customizable Settings

The system includes several configurable parameters:

```kotlin
// Image display dimensions
private val maxWidth: Int = 400
private val maxHeight: Int = 300
private val thumbnailWidth: Int = 200
private val thumbnailHeight: Int = 150
```

These could be made configurable through plugin settings in future versions.

### Theme Integration

The image system automatically adapts to IntelliJ IDEA theme changes through:

- `JBColor` usage for dynamic color adaptation
- `ColorHelper` integration for consistent theming
- Font selection compatible with IDE font settings

## Conclusion

The image implementation provides a robust, extensible foundation for rich media support in the AI Assistant plugin. The modular architecture allows for easy extension and maintenance, while the comprehensive error handling ensures a smooth user experience across various image formats and network conditions.

The system successfully integrates with the existing chat interface while maintaining performance and usability standards expected in IntelliJ IDEA plugins.
