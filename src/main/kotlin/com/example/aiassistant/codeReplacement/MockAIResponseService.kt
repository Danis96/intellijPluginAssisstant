package com.example.aiassistant.codeReplacement

/**
 * Mock AI response service for testing code replacement functionality.
 * This simulates real AI responses to demonstrate the code replacement system.
 */
object MockAIResponseService {

    /**
     * Sample code snippets for testing
     */
    object SampleCode {
        const val SIMPLE_FUNCTION: String = """
fun calculateArea(radius: Double): Double {
    return 3.14 * radius * radius
}
"""

        const val IMPROVED_FUNCTION: String = """
fun calculateArea(radius: Double): Double {
    require(radius >= 0) { "Radius must be non-negative" }
    return kotlin.math.PI * radius * radius
}
"""

        const val BUGGY_LOOP: String = """
for (i in 0..10) {
    println(i)
    if (i == 5) break
}
"""

        const val FIXED_LOOP: String = """
for (i in 0..10) {
    println("Value: "$/i")
    if (i == 5) {
        println("Breaking at 5")
        break
    }
}
"""

        const val POOR_ERROR_HANDLING: String = """
fun divide(a: Int, b: Int): Int {
    return a / b
}
"""

        const val IMPROVED_ERROR_HANDLING: String = """
fun divide(a: Int, b: Int): Result<Double> {
    return if (b == 0) {
        Result.failure(IllegalArgumentException("Division by zero"))
    } else {
        Result.success(a.toDouble() / b.toDouble())
    }
}
"""
    }

    /**
     * Generates a mock AI response with code improvements
     */
    fun generateMockResponse(originalCode: String): String {
        return when {
            originalCode.contains("calculateArea") -> generateAreaFunctionResponse()
            originalCode.contains("for (i in 0..10)") -> generateLoopResponse()
            originalCode.contains("fun divide") -> generateErrorHandlingResponse()
            originalCode.contains("val x = foo(bar)") -> generateSimpleDiffResponse()
            else -> generateGenericResponse(originalCode)
        }
    }

    private fun generateAreaFunctionResponse(): String {
        return """
I can see several improvements for this function:

1. **Input validation**: Should check for negative radius
2. **Use precise PI**: Use `kotlin.math.PI` instead of hardcoded 3.14
3. **Better naming**: Function name is clear but could be more specific

Here's the improved version:

```kotlin
fun calculateArea(radius: Double): Double {
    require(radius >= 0) { "Radius must be non-negative" }
    return kotlin.math.PI * radius * radius
}
```

**Key improvements:**
- Added input validation with `require()`
- Using `kotlin.math.PI` for better precision
- Clear error message for invalid input

This makes the function more robust and follows Kotlin best practices.
"""
    }

    private fun generateLoopResponse(): String {
        return """
This loop can be improved for better readability and debugging:

```diff
 for (i in 0..10) {
-    println(i)
-    if (i == 5) break
+    println("Value: ${'$'}{'i'}")
+    if (i == 5) {
+        println("Breaking at 5")
+        break
+    }
 }
```

**Improvements:**
- More descriptive output with string templates
- Better formatting with braces for the if statement
- Added debug message when breaking

This makes the code more maintainable and easier to debug.
"""
    }

    private fun generateErrorHandlingResponse(): String {
        return """
This function has a critical issue - division by zero is not handled. Here's a safer approach:

```kotlin
fun divide(a: Int, b: Int): Result<Double> {
    return if (b == 0) {
        Result.failure(IllegalArgumentException("Division by zero"))
    } else {
        Result.success(a.toDouble() / b.toDouble())
    }
}
```

**Key improvements:**
- Uses `Result<T>` for safe error handling
- Prevents division by zero crashes
- Returns Double for more precise results
- Follows functional programming principles

Usage example:
```kotlin
val result = divide(10, 2)
result.fold(
    onSuccess = { println("Result: ${'$'}{'it'}") },
    onFailure = { println("Error: ${'$'}{it.message}") }
)
```
"""
    }

    private fun generateSimpleDiffResponse(): String {
        return """
I notice this line could be improved by adding null safety and string trimming:

```diff
-    val x = foo(bar)
+    val x = foo(bar)?.trim()
```

This ensures that if `foo(bar)` returns a string, any leading/trailing whitespace is removed.
"""
    }

    private fun generateGenericResponse(originalCode: String): String {
        return """
I've analyzed your code and here are some general improvements:

**Original Code:**
```kotlin
$originalCode
```

**Suggested Improvements:**
1. Consider adding input validation
2. Use more descriptive variable names
3. Add proper error handling
4. Follow Kotlin coding conventions

**Improved Version:**
```kotlin
// Improved version with better practices
${improveCodeGeneric(originalCode)}
```

Would you like me to explain any specific improvements or apply these changes?
"""
    }

    private fun improveCodeGeneric(code: String): String {
        // Simple generic improvements
        return code
            .replace("val x", "val result")
            .replace("val y", "val value")
            .replace("fun test", "fun processData")
            .trim()
    }

    /**
     * Generates a response that includes multiple code replacements
     */
    fun generateMultipleReplacementsResponse(): String {
        return """
I found several issues in your code that need fixing:

**Issue 1: Unsafe null handling**
```diff
-    val name = user.getName()
+    val name = user.getName() ?: "Unknown"
```

**Issue 2: Poor error handling**  
```diff
-    return result.value
+    return result.getOrElse { 
+        throw IllegalStateException("Failed to get result")
+    }
```

**Issue 3: Missing validation**
```kotlin
fun validateInput(input: String): Boolean {
    require(input.isNotBlank()) { "Input cannot be blank" }
    return input.length >= 3
}
```

These changes will make your code more robust and follow Kotlin best practices.
"""
    }

        /**
     * Generates a response that includes images for demonstration
     */
    fun generateImageResponse(prompt: String): String {
        return when {
            prompt.contains("diagram", ignoreCase = true) -> generateDiagramResponse()
            prompt.contains("chart", ignoreCase = true) -> generateChartResponse()
            prompt.contains("flowchart", ignoreCase = true) -> generateFlowchartResponse()
            prompt.contains("image", ignoreCase = true) -> generateImageExampleResponse()
            else -> generateMockResponse(prompt)
        }
    }
    
    private fun generateDiagramResponse(): String {
        return """
Here's a flowchart showing the user authentication flow:

```mermaid
flowchart TD
    A[User Login] --> B{Valid Credentials?}
    B -->|Yes| C[Generate JWT Token]
    B -->|No| D[Show Error Message]
    C --> E[Store Token in Session]
    E --> F[Redirect to Dashboard]
    D --> A
    F --> G[Access Protected Resources]
```

This diagram illustrates the complete authentication process:

1. **User Input**: Login credentials are submitted
2. **Validation**: System checks credentials against database  
3. **Token Generation**: JWT token created for valid users
4. **Session Management**: Token stored securely
5. **Redirection**: User sent to protected area
6. **Access Control**: Token validates resource access

The flow ensures secure authentication while providing clear error feedback for invalid attempts.
"""
    }
    
    private fun generateChartResponse(): String {
        return """
Based on your performance data, here's the analysis with a system architecture diagram:

```mermaid
graph LR
    A[Frontend] --> B[Load Balancer]
    B --> C[API Gateway]
    C --> D[Auth Service]
    C --> E[User Service]
    C --> F[Data Service]
    D --> G[(Auth DB)]
    E --> H[(User DB)]
    F --> I[(Main DB)]
```

**Performance Insights:**
- Response times improved by 40% after optimization
- Memory usage decreased by 25%  
- User satisfaction increased significantly

**Key Improvements Made:**
1. Added load balancing for better distribution
2. Implemented API gateway for centralized routing
3. Separated services for better scalability
4. Optimized database queries

**Recommendations:**
- Continue monitoring performance metrics
- Implement Redis caching for frequently accessed data
- Consider horizontal scaling for peak loads
"""
    }
    
    private fun generateFlowchartResponse(): String {
        return """
Here's the complete code review process flowchart:

```mermaid
flowchart TD
    A[Code Submitted] --> B[Automated Checks]
    B --> C{Tests Pass?}
    C -->|No| D[Notify Author]
    C -->|Yes| E[Assign Reviewer]
    D --> A
    E --> F[Code Review]
    F --> G{Approved?}
    G -->|No| H[Request Changes]
    G -->|Yes| I[Merge to Main]
    H --> A
    I --> J[Deploy to Staging]
    J --> K[QA Testing]
    K --> L{QA Pass?}
    L -->|No| M[Rollback]
    L -->|Yes| N[Deploy to Production]
    M --> A
```

This workflow ensures:
- **Quality Control**: Multiple checkpoints prevent bugs
- **Automation**: Reduces manual overhead
- **Feedback Loops**: Quick iteration on issues
- **Safety**: Staging environment testing before production

The process typically takes 1-3 days depending on complexity and reviewer availability.
"""
    }
    
    private fun generateImageExampleResponse(): String {
        return """
Here's an example of how images can be displayed in the chat:

![Sample Icon](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==)

You can also reference external images by URL:
![IntelliJ IDEA](https://resources.jetbrains.com/storage/products/intellij-idea/img/meta/intellij-idea_logo_300x300.png)

**Image Support Features:**
- 📱 Base64 embedded images
- 🌐 URL referenced images  
- 📁 Local file references
- 📊 Diagram rendering (Mermaid, PlantUML)
- 🖼️ Thumbnail with full-size view
- 💾 Save functionality
- 🔍 Zoom capabilities

Images are automatically detected and displayed inline with the conversation!
"""
    }

    /**
     * Predefined test scenarios
     */
    object TestScenarios {
        val AREA_CALCULATION = Pair(SampleCode.SIMPLE_FUNCTION, ::generateAreaFunctionResponse)
        val LOOP_IMPROVEMENT = Pair(SampleCode.BUGGY_LOOP, ::generateLoopResponse)
        val ERROR_HANDLING = Pair(SampleCode.POOR_ERROR_HANDLING, ::generateErrorHandlingResponse)
    }
}