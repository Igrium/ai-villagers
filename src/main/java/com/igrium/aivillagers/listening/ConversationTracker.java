package com.igrium.aivillagers.listening;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * Keeps track of what villager a player most recently spoke to.
 * Should only be manipulated from the server thread.
 */
public class ConversationTracker {

    // Don't keep strong reference to player because we're relying on WeakHashMap to clean this up.
    private final WeakReference<ServerPlayerEntity> player;

    // Remember a player has been in an interaction for this many ticks.
    private static final int INTERACTION_CACHE_TIMEOUT = 400; // 20 seconds
    private static final double MAX_DISTANCE = 6;

    @Nullable
    private PlayerIntentTracker intentTracker;

    private Entity mostRecentInteraction;
    private int mostRecentInteractionTime;

    public ConversationTracker(ServerPlayerEntity player) {
        this.player = new WeakReference<>(player);
    }

    public void tick() {
        if (player.refersTo(null)) {
            return;
        }

        if (intentTracker != null) {
            intentTracker.tick();
        } else if (getPlayer().age - mostRecentInteractionTime > INTERACTION_CACHE_TIMEOUT) {
            mostRecentInteraction = null; // Let entity get garbage collected
        }
    }

    /**
     * Called when the player begins talking to initialize an intent tracker.
     * @param force Always create a new intent tracker even if there already is one.
     */
    public void startTalking(boolean force) {
        // TODO: customizable predicate
        if (force || intentTracker == null)
            intentTracker = new PlayerIntentTracker(getPlayer(), MAX_DISTANCE, v -> v instanceof VillagerEntity);
    }

    /**
     * Called when the player stopped talking.
     * @return The entity we were talking to, if any.
     */
    @Nullable
    public Entity stopTalking() {
        if (intentTracker == null) {
            LoggerFactory.getLogger(getClass()).warn("Player did not have an intent tracker.");
            return null;
        }
        Entity prev = mostRecentInteraction;
        if (prev != null && prev.squaredDistanceTo(getPlayer()) > MAX_DISTANCE * MAX_DISTANCE) {
            prev = null;
        }

        // If we have a recent interaction, use slightly smaller cone to confirm player actually wants to switch targets.
        Entity target = intentTracker.getLikelyTarget(prev != null ? .9 : .7);

        if (target == null) {
            target = prev;
        }

        mostRecentInteraction = target;
        mostRecentInteractionTime = getPlayer().age;
        intentTracker = null;

        return target;
    }

    @NotNull
    public ServerPlayerEntity getPlayer() {
        return Objects.requireNonNull(player.get());
    }
}
