/**
 * Various utility functions regarding streaming from OpenAI
 */
package com.igrium.aivillagers.gpt

import com.aallam.openai.api.chat.*
import com.aallam.openai.api.core.Role
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import java.util.concurrent.Flow.Publisher
import java.util.concurrent.SubmissionPublisher

/**
 * Process all the incoming chunks of a streamed chat completion and suspend until they're all handled.
 * @param response A flow with all the completion chunks.
 * @param contentHandler Called every time string content is received from the model.
 * @return A compiled `ChatMessage` containing the response content. Identical to what would be received
 * with streaming disabled.
 */
suspend fun handleStreamResponse(
    response: Flow<ChatCompletionChunk>,
    contentHandler: (String) -> Unit = {}
): ChatMessage {
    var role = Role.Assistant
    val toolCalls = mutableMapOf<Int, ToolCallChunk>()
    var content = ""
    var toolCallId: ToolId? = null
    response.collect { chunk ->
        val delta = chunk.choices[0].delta ?: return@collect

        role = delta.role ?: role
        toolCallId = delta.toolCallId ?: toolCallId

        val str = delta.content
        if (!str.isNullOrEmpty()) {
            content += str
            contentHandler(str)
        }

        for (toolCall in delta.toolCalls.orEmpty()) {
            val index = toolCall.index
            val existing = toolCalls[index]
            if (existing != null) {
                toolCalls[index] = existing.append(toolCall)
            } else {
                toolCalls[index] = toolCall
            }
        }
    }
    val toolCallList = toolCalls.values.map { it.toToolCall() }

    return ChatMessage(
        role = role,
        messageContent = TextContent(content),
        toolCallId = toolCallId,
        toolCalls = toolCallList.ifEmpty { null }
    )
}

/**
 * Append arguments from another function call into this function call.
 * @param other Other function call
 * @return The combined function call.
 */
fun FunctionCall.append(other: FunctionCall?): FunctionCall {
    if (other == null) return this
    val args = if (this.argumentsOrNull != null) {
        this.arguments + other.argumentsOrNull.orEmpty()
    } else {
        other.arguments
    }

    return FunctionCall(
        nameOrNull = other.nameOrNull ?: this.nameOrNull,
        argumentsOrNull = args
    )
}

/**
 * Append another tool call chunk to this tool call chunk
 */
fun ToolCallChunk.append(other: ToolCallChunk): ToolCallChunk {
    return ToolCallChunk(
        index = other.index,
        type = other.type ?: this.type,
        id = other.id ?: this.id,
        function = this.function?.append(other.function) ?: other.function
    )
}

/**
 * Convert a tool call chunk into a standard tool call.
 */
fun ToolCallChunk.toToolCall(): ToolCall.Function {
    return ToolCall.Function(
        id = requireNotNull(this.id) { "id is required" },
        function = requireNotNull(this.function) { "function is required" }
    )
}