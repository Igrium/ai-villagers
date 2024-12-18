package com.igrium.aivillagers.gpt

import com.aallam.openai.api.core.Parameters
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import net.minecraft.entity.Entity
import org.slf4j.LoggerFactory

private suspend fun offerTrade(aiInterface: VillagerAIInterface, villager: Entity, target: Entity?, params: JsonObject): String {
    LoggerFactory.getLogger("Gpt Functions")
        .info("Villager {} is offering to trade {} {}", villager.nameForScoreboard, target?.nameForScoreboard, params);
    return "success"
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