package com.igrium.aivillagers.gpt

import com.aallam.openai.api.chat.*
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString

fun ChatMessage.toNbt(): NbtCompound {
    val nbt = NbtCompound()

    nbt.putString("role", this.role.role)

    val content = this.messageContent
    if (content != null) nbt.put("content", content.toNbt())

    val name = this.name
    if (name != null) nbt.putString("name", name)

    val toolCalls = this.toolCalls
    if (!toolCalls.isNullOrEmpty()) {
        val list = NbtList()
        for (call in toolCalls) {
            list.add(call.toNbt())
        }
        nbt.put("toolCalls", list)
    }

    val toolCallId = this.toolCallId
    if (toolCallId != null) nbt.putString("toolCallId", toolCallId.id)

    return nbt
}

fun chatMessageFromNbt(nbt: NbtCompound): ChatMessage {
    val role = nbt.getString("role")
    require(!role.isNullOrBlank()) { "role must be defined" }

    val contentNbt = nbt.get("content")
    val content = if (contentNbt != null) contentFromNbt(contentNbt) else null;

    var name = nbt.getString("name")
    if (name.isNullOrBlank()) name = null

    val toolCallsNbt = nbt.getList("toolCalls", NbtElement.COMPOUND_TYPE.toInt())
    val toolCalls = if (!toolCallsNbt.isNullOrEmpty()) {
        toolCallsNbt.map { toolCallFromNbt(it as NbtCompound) }
    } else null

    val toolCallIdStr = nbt.getString("toolCallId")
    val toolCallId = if (!toolCallIdStr.isNullOrBlank()) ToolId(toolCallIdStr) else null

    return ChatMessage(
        role = ChatRole(role),
        messageContent = content,
        name = name,
        toolCalls = toolCalls,
        toolCallId = toolCallId
    )
}

fun ToolCall.toNbt(): NbtCompound {
    require (this is ToolCall.Function) {"Tool call must be a function!"}

    val nbt = NbtCompound()
    nbt.put("function", this.function.toNbt())
    nbt.putString("id", this.id.id)

    return nbt
}

fun toolCallFromNbt(nbt: NbtCompound): ToolCall.Function {
    val function = nbt.get("function")
    requireNotNull(function) { "function must be defined" }

    val id = nbt.getString("id")
    require (!id.isNullOrBlank()) { "id must be defined" }

    return ToolCall.Function(ToolId(id), functionCallFromNbt(function as NbtCompound))
}

fun FunctionCall.toNbt(): NbtCompound {
    val nbt = NbtCompound()

    val name = this.nameOrNull
    if (name != null) nbt.putString("name", name)

    val args = this.argumentsOrNull
    if (args != null) nbt.putString("arguments", args)

    return nbt
}

fun functionCallFromNbt(nbt: NbtCompound): FunctionCall {
    var name: String? = nbt.getString("name")
    if (name?.isBlank() == true) name = null
    var args: String? = nbt.getString("arguments")
    if (args?.isBlank() == true) args = null

    return FunctionCall(name, args)
}

fun Content.toNbt(): NbtElement {
    if (this is TextContent) {
        return NbtString.of(this.content)
    } else if (this is ListContent) {
        val list = NbtList()
        for (part in this.content) {
            list.add(part.toNbt())
        }
        return list
    } else {
        throw IllegalArgumentException("Content must be of TextContent or ListContent")
    }
}

fun contentFromNbt(nbt: NbtElement): Content {
    if (nbt is NbtString) {
        return TextContent(nbt.asString())
    } else if (nbt is NbtList) {
        return ListContent(nbt.map { contentPartFromNbt(it) })
    } else {
        throw IllegalArgumentException("Unsupported tag: " + nbt.nbtType.crashReportName)
    }
}

fun ContentPart.toNbt(): NbtElement {
    if (this is TextPart) {
        return NbtString.of(this.text)
    } else {
        throw IllegalArgumentException("For now, only text content is supported.")
    }
}

fun contentPartFromNbt(nbt: NbtElement): ContentPart {
    if (nbt is NbtString) {
        return TextPart(nbt.asString())
    } else {
        throw IllegalArgumentException("Nbt must be string element")
    }
}