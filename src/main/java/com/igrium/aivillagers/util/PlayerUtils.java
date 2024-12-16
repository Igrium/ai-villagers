package com.igrium.aivillagers.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class PlayerUtils {
    public static HitResult findCrosshairTarget(Entity camera, double blockInteractionRange,
            double entityInteractionRange) {
        double maxRange = Math.max(blockInteractionRange, entityInteractionRange);
        double maxRangeSquared = MathHelper.square(maxRange);
        Vec3d cameraPos = camera.getCameraPosVec(0);

        HitResult hitResult = camera.raycast(maxRange, 0, false);
        double blockHitDistSquared = hitResult.getPos().squaredDistanceTo(cameraPos);

        if (hitResult.getType() != HitResult.Type.MISS) {
            maxRangeSquared = blockHitDistSquared;
            maxRange = Math.sqrt(blockHitDistSquared);
        }

        Vec3d cameraRot = camera.getRotationVec(0);
        Vec3d raycastTarget = cameraPos.add(cameraRot.x * maxRange, cameraRot.y * maxRange, cameraRot.z * maxRange);
        Box box = camera.getBoundingBox().stretch(cameraRot.multiply(maxRange)).expand(1.0, 1.0, 1.0);

        EntityHitResult entityHitResult = ProjectileUtil.raycast(camera, cameraPos, raycastTarget, box, EntityPredicates.CAN_HIT,
                maxRangeSquared);

        return entityHitResult != null && entityHitResult.getPos().squaredDistanceTo(cameraPos) < blockHitDistSquared
                ? ensureTargetInRange(entityHitResult, cameraPos, entityInteractionRange)
                : ensureTargetInRange(hitResult, cameraPos, blockInteractionRange);
    }

    private static HitResult ensureTargetInRange(HitResult hitResult, Vec3d cameraPos, double interactionRange) {
        Vec3d hitPos = hitResult.getPos();
        if (!hitPos.isInRange(cameraPos, interactionRange)) {
            Vec3d hitpos2 = hitResult.getPos();
            Direction direction = Direction.getFacing(hitpos2.x - cameraPos.x,
                    hitpos2.y - cameraPos.y,
                    hitpos2.z - cameraPos.z);
            return BlockHitResult.createMissed(hitpos2, direction, BlockPos.ofFloored(hitpos2));
        } else {
            return hitResult;
        }
    }
}
