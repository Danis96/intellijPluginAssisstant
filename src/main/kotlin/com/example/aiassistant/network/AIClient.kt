package com.example.aiassistant.network

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class AIClient(
    private val apiEndpoint: String
) {

    private val client: HttpClient = HttpClient.newHttpClient()

    fun chat(token: String, prompt: String): String {
        val requestBody: String = """{"prompt": ${escapeJson(prompt)}}"""
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(apiEndpoint))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $token")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        val code: Int = response.statusCode()
        if (code in 200..299) {
            return response.body()
        }
        return "Error: HTTP $code"
    }

    private fun escapeJson(text: String): String {
        val escaped: String = text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
        return "\"$escaped\""
    }
}