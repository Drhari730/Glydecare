package com.diabeticcare.app.network

data class ClaudeRequest(
    val model: String = "claude-haiku-4-5",
    val max_tokens: Int = 150,
    val messages: List<ClaudeMessage>
)

data class ClaudeMessage(val role: String, val content: String)

data class ClaudeResponse(val content: List<ClaudeContent>)
data class ClaudeContent(val type: String, val text: String)
