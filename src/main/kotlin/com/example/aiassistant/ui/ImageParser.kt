package com.example.aiassistant.ui

import java.util.regex.Pattern

/**
 * Parser for detecting and extracting image content from chat messages
 */
class ImageParser {
    
    companion object {
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
        
        // Diagram patterns (Mermaid, PlantUML)
        private val MERMAID_PATTERN: Pattern = Pattern.compile(
            "```mermaid\\s*\\n(.*?)\\n```",
            Pattern.DOTALL
        )
        
        private val PLANTUML_PATTERN: Pattern = Pattern.compile(
            "```plantuml\\s*\\n(.*?)\\n```",
            Pattern.DOTALL
        )
    }
    
    /**
     * Parse images from message content
     */
    fun parseImages(content: String): List<ImageContent> {
        val images: MutableList<ImageContent> = mutableListOf()
        
        // Parse markdown images
        val markdownMatcher = MARKDOWN_IMAGE_PATTERN.matcher(content)
        while (markdownMatcher.find()) {
            val alt: String = markdownMatcher.group(1) ?: ""
            val src: String = markdownMatcher.group(2) ?: ""
            images.add(ImageContent(
                type = determineImageType(src),
                source = src,
                alt = alt,
                startIndex = markdownMatcher.start(),
                endIndex = markdownMatcher.end()
            ))
        }
        
        // Parse base64 images
        val base64Matcher = BASE64_IMAGE_PATTERN.matcher(content)
        while (base64Matcher.find()) {
            images.add(ImageContent(
                type = ImageType.EMBEDDED_BASE64,
                source = base64Matcher.group(0),
                alt = "Base64 Image",
                startIndex = base64Matcher.start(),
                endIndex = base64Matcher.end()
            ))
        }
        
        // Parse URL images
        val urlMatcher = URL_IMAGE_PATTERN.matcher(content)
        while (urlMatcher.find()) {
            images.add(ImageContent(
                type = ImageType.URL_REFERENCE,
                source = urlMatcher.group(0),
                alt = "Image from URL",
                startIndex = urlMatcher.start(),
                endIndex = urlMatcher.end()
            ))
        }
        
        // Parse diagram code blocks
        val mermaidMatcher = MERMAID_PATTERN.matcher(content)
        while (mermaidMatcher.find()) {
            images.add(ImageContent(
                type = ImageType.INLINE_DIAGRAM,
                source = mermaidMatcher.group(1) ?: "",
                alt = "Mermaid Diagram",
                startIndex = mermaidMatcher.start(),
                endIndex = mermaidMatcher.end(),
                diagramType = "mermaid"
            ))
        }
        
        val plantumlMatcher = PLANTUML_PATTERN.matcher(content)
        while (plantumlMatcher.find()) {
            images.add(ImageContent(
                type = ImageType.INLINE_DIAGRAM,
                source = plantumlMatcher.group(1) ?: "",
                alt = "PlantUML Diagram",
                startIndex = plantumlMatcher.start(),
                endIndex = plantumlMatcher.end(),
                diagramType = "plantuml"
            ))
        }
        
        return images
    }
    
    /**
     * Remove image markdown from content to get clean text
     */
    fun removeImageMarkdown(content: String): String {
        var cleanContent: String = content
        
        // Remove markdown images
        cleanContent = MARKDOWN_IMAGE_PATTERN.matcher(cleanContent).replaceAll("")
        
        // Remove base64 images
        cleanContent = BASE64_IMAGE_PATTERN.matcher(cleanContent).replaceAll("")
        
        // Remove URL images (but keep them as text links)
        // cleanContent = URL_IMAGE_PATTERN.matcher(cleanContent).replaceAll("")
        
        // Keep diagram code blocks as they might contain important text content
        
        return cleanContent.trim()
    }
    
    private fun determineImageType(src: String): ImageType {
        return when {
            src.startsWith("data:image/") -> ImageType.EMBEDDED_BASE64
            src.startsWith("http://") || src.startsWith("https://") -> ImageType.URL_REFERENCE
            src.startsWith("file://") || src.startsWith("/") || src.contains(":\\") -> ImageType.LOCAL_FILE
            else -> ImageType.URL_REFERENCE // Default fallback
        }
    }
}

/**
 * Data class representing image content in a message
 */
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

/**
 * Types of images that can be displayed
 */
enum class ImageType {
    EMBEDDED_BASE64,    // data:image/png;base64,iVBOR...
    URL_REFERENCE,      // https://example.com/image.png
    LOCAL_FILE,         // file://path/to/image.png or /path/to/image.png
    INLINE_DIAGRAM,     // Mermaid, PlantUML, etc.
    AI_GENERATED        // Images generated by AI (future use)
}

/**
 * Enhanced message content with image support
 */
data class MessageContent(
    val text: String,
    val images: List<ImageContent> = emptyList(),
    val codeBlocks: List<CodeDiffViewer.CodeBlock> = emptyList()
)
