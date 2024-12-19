package com.igrium.aivillagers.gpt;

import com.aallam.openai.api.chat.ChatMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class VillagerAIInterfaceImpl implements VillagerAIInterface {
    public static final Logger LOGGER = LoggerFactory.getLogger("Villager AI Interface");
    private static final Gson GSON = new GsonBuilder().create();


    @Override
    public List<ChatMessage> getHistory(Entity villager) {
        return ChatHistoryComponent.get(villager).getMessageHistory();
    }

    public static record TradeValidationResult(boolean success, @Nullable String reason) {
        public String getResponsePrompt() {
            if (success) {
                return "SUCCESS";
            } else {
                return "FAILED: " + reason;
            }
        }
    };

    public static TradeValidationResult ValidateTrade(Item sellItem, int sellAmount, Item buyItem, int buyAmount) {
        if (sellItem == Items.AIR) {
            return new TradeValidationResult(false, "invalid sell item");
        }
        if (buyItem == Items.AIR) {
            return new TradeValidationResult(false, "invalid buy item");
        }

        if (sellAmount > sellItem.getMaxCount()) {
            return new TradeValidationResult(false, "max sell amount: " + sellItem.getMaxCount());
        }
        if (sellAmount < 0) {
            return new TradeValidationResult(false, "sell amount negative");
        }

        if (buyAmount > buyItem.getMaxCount()) {
            return new TradeValidationResult(false, "max buy amount: " + buyItem.getMaxCount());
        }
        if (buyAmount < 0) {
            return new TradeValidationResult(false, "buy amount negative");
        }

        return new TradeValidationResult(true, null);
    }


    @Override
    public String offerTrade(Entity entity, String sellItemID, int sellAmount, String buyItemID, int buyAmount) {
        LOGGER.info("{} is offering to trade {} {} for {} {}",
                entity.getDisplayName().getString(), sellAmount, sellItemID, buyAmount, buyItemID);

        Item sellItem = Registries.ITEM.get(Identifier.tryParse(sellItemID));
        Item buyItem = Registries.ITEM.get(Identifier.tryParse(buyItemID));

        var validation = ValidateTrade(sellItem, sellAmount, buyItem, buyAmount);
        if (!validation.success) {
            LOGGER.warn(validation.reason);
            return validation.getResponsePrompt();
        }

        if (!(entity instanceof MerchantEntity villager)) {
            return "FAILED: not villager";
        }

        ItemStack sellStack = sellItem.getDefaultStack();
        sellStack.setCount(sellAmount);

        TradedItem buyStack = new TradedItem(buyItem, buyAmount);

        Objects.requireNonNull(entity.getServer()).execute(() -> {
            villager.getOffers().add(
                    new TradeOffer(buyStack, sellStack, 10, 1, 1)
            );

        });

        return "DONE";
    }
}
