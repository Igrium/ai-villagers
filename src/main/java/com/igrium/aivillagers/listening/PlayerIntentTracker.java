package com.igrium.aivillagers.listening;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Predicate;

/**
 * Watches a player's movements and tries to determine which villager they're likely talking to.
 */
public class PlayerIntentTracker {
    // Use weak reference so we don't break WeakHashMap used for tracking these
    private final WeakReference<PlayerEntity> player;
    private final double radius;
    private final Predicate<? super Entity> shouldTrack;

    private int trackedTicks = 0;

    /**
     * A map of all tracked entities and the sum of their dot products over all ticks.
     */
    private final Map<Entity, Double> dots = new HashMap<>();

    /**
     * Create a player intent tracker.
     *
     * @param player      Target player.
     * @param radius      Radius around to the player to look for entities.
     * @param shouldTrack A predicate dictating whether a given entity should be considered.
     */
    public PlayerIntentTracker(PlayerEntity player, double radius, Predicate<? super Entity> shouldTrack) {
        this.player = new WeakReference<>(player);
        this.radius = radius;
        this.shouldTrack = shouldTrack;
    }

    @NotNull
    public PlayerEntity getPlayer() {
        return Objects.requireNonNull(player.get());
    }

    /**
     * Call every tick while the tracker is active.
     */
    public void tick() {
        PlayerEntity player = this.player.get();
        if (player == null) return;

        Set<Entity> ents = new HashSet<>(dots.keySet());
        Box box = getPlayer().getBoundingBox().expand(radius);

        ents.addAll(player.getWorld().getOtherEntities(player, box, shouldTrack));

        Vec3d facing = player.getRotationVector();
        Vec3d playerPos = player.getEyePos();

        for (var ent : ents) {
            Vec3d normal = ent.getEyePos().subtract(playerPos).normalize();

            double dot = dots.getOrDefault(ent, 0d);
            dot += normal.dotProduct(facing);
            dots.put(ent, dot);
        }

        trackedTicks++;
    }

    /**
     * Return the entity the player was most likely talking to in the duration of this tracker.
     *
     * @param threshold The minimum average dot product between the player and the entity's relative normal.
     *                  In layman's terms, the amount the player must be looking at the entity for it to count.
     *                  <code>-1</code>: not looking at all; <code>0</code>: looking at max 90 degrees away;
     *                  <code>1</code>: must be looking <em>exactly</em> at entity (not recommended).
     * @return The target entity, or <code>null</code> if there was no target meeting the criteria.
     */
    @Nullable
    public Entity getLikelyTarget(double threshold) {
        Entity maxEntity = null;
        double maxDot = 0;

        for (var entry : dots.entrySet()) {
            double dot = entry.getValue() / trackedTicks;
            if (maxEntity == null || dot > maxDot) {
                maxEntity = entry.getKey();
                maxDot = dot;
            }
        }
        return maxDot >= threshold ? maxEntity : null;
    }
}
