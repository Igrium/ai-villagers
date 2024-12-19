package com.igrium.aivillagers.gpt

import com.aallam.openai.api.core.Parameters
import kotlinx.serialization.json.*
import net.minecraft.entity.Entity
import org.slf4j.LoggerFactory

private fun offerTrade(aiInterface: VillagerAIInterface, villager: Entity, target: Entity?, params: JsonObject): String {
    val sellItem = params.getValue("sellItem").jsonPrimitive.content;
    val sellAmount = params.getValue("sellAmount").jsonPrimitive.int;
    val buyItem = params.getValue("buyItem").jsonPrimitive.content;
    val buyAmount = params.getValue("buyAmount").jsonPrimitive.int;

    return aiInterface.offerTrade(villager, sellItem, sellAmount, buyItem, buyAmount);
}

val offerTradeFunction = ToolCallFunction(
    description = "Make a trade with the player",
    parameters = Parameters.buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("sellItem") {
                put("type", "string")
                put("description", "The minecraft internal name for item sold to the player, ex: minecraft:stone")
            }
            putJsonObject("sellAmount") {
                put("type", "number")
                put("description", "amount of the item to sell to the player.")
            }
            putJsonObject("buyItem") {
                put("type", "string")
                put("description", "The minecraft internal name for item bought to the player, ex: minecraft:stone")
            }
            putJsonObject("buyAmount") {
                put("type", "number")
                put("description", "The amount of the item to buy from the player")
            }
        }
    },
    impl = ::offerTrade
)