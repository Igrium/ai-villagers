package com.igrium.aivillagers.gpt

import com.aallam.openai.api.chat.ToolBuilder
import com.aallam.openai.api.core.Parameters
import com.igrium.aivillagers.util.ToolCallImplementation
import kotlinx.coroutines.future.await
import kotlinx.serialization.json.JsonObject
import net.minecraft.entity.Entity
import org.apache.commons.lang3.function.TriFunction
import java.util.concurrent.CompletableFuture


/**
 * The implementation of a function tool call.
 * @param description The description to send to the LLM.
 * @param parameters The parameters to use.
 * @param impl The implementation of the function.
 */
data class ToolCallFunction(
    val description: String,
    val parameters: Parameters,
    val impl: suspend (ai: VillagerAIInterface, villager: Entity, target: Entity?, params: JsonObject) -> String
) {
    companion object {

        /**
         * An alternative constructor for `ToolCallFunction` which is java-compatible.
         * @param name The name of the function
         * @param description The description to send to the LLM.
         * @param parameters The parameters to use.
         * @param impl The implementation of the function.
         */
        @JvmStatic
        fun create(
            name: String,
            description: String,
            parameters: Parameters,
            impl: ToolCallImplementation
        ): ToolCallFunction {
            return ToolCallFunction(description, parameters, { ai, ent, target, params ->
                impl.execute(ai, ent, target, params).await()
            })
        }
    }
}

fun ToolBuilder.function(name: String, function: ToolCallFunction) {
    return function(
        name,
        function.description,
        function.parameters
    )
}