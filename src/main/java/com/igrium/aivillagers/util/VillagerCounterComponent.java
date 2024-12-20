package com.igrium.aivillagers.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;

/**
 * Keeps track of villager numbers
 */
public class VillagerCounterComponent implements Component {

    public static final ComponentKey<VillagerCounterComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of("ai-villagers:villager_counter"), VillagerCounterComponent.class);

    public VillagerCounterComponent(Scoreboard scoreboard, @Nullable MinecraftServer minecraftServer) {

    }

    public static VillagerCounterComponent get(Scoreboard scoreboard) {
        return KEY.get(scoreboard);
    }

    public static VillagerCounterComponent get(MinecraftServer server) {
        return KEY.get(server.getScoreboard());
    }

    // Start at Villager #5
    int currentIndex = 5;

    @Override
    public void readFromNbt(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup) {
        nbtCompound.putInt("currentIndex", currentIndex);
    }

    @Override
    public void writeToNbt(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup) {
        this.currentIndex = nbtCompound.getShort("currentIndex");
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public String getNextName() {
        return "Villager #" + currentIndex;
    }

    public String getNextNameAndIncrement() {
        int index = currentIndex;
        currentIndex++;
        return "Villager #" + index;
    }
}
