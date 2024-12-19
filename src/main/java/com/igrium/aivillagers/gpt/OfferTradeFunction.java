package com.igrium.aivillagers.gpt;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.igrium.aivillagers.gpt.GptUtil.AIContext;

import io.github.sashirestela.openai.common.function.Functional;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;

public class OfferTradeFunction implements Functional {

    @JsonPropertyDescription("minecraft internal name for item given to the player, ex: minecraft:stone")
    @JsonProperty(required = true)
    public String sellItemID;

    @JsonPropertyDescription("amount of the item to give to the player.")
    @JsonProperty(required = true)
    public int sellAmount;

    @JsonPropertyDescription("minecraft internal name for the item to take from the player. ex: minecraft:stone")
    @JsonProperty(required = true)
    public String buyItemID;

    @JsonPropertyDescription("amount of the item to take from the player.")
    @JsonProperty(required = true)
    public int buyAmount;

    @Override
    public Object execute() {
        AIContext context = GptUtil.AI_CONTEXT.get();

        Item sellItem = Registries.ITEM.get(Identifier.tryParse(sellItemID));
        Item buyItem = Registries.ITEM.get(Identifier.tryParse(buyItemID));

        if (sellItem == Items.AIR || buyItem == Items.AIR) {
            return "FAILED: invalid item";
        }

        if (!(context.villager() instanceof MerchantEntity villager)) {
            return "FAILED: not villager";
        }

        ItemStack sellStack = sellItem.getDefaultStack();
        sellStack.setCount(sellAmount);

        villager.getOffers().add(new TradeOffer(new TradedItem(buyItem, buyAmount), sellStack, 1, 1, 1));

        LoggerFactory.getLogger(getClass()).info("{} is offering to trade {} {} for {} {}",
                context.villager().getDisplayName().getString(), sellAmount, sellItemID, buyAmount, buyAmount);

        return "DONE";
    }
    
}
