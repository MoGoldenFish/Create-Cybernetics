package com.perigrine3.createcybernetics.client.biomonitor;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public final class BiomonitorTargeting {
    private BiomonitorTargeting() {
    }

    public static LivingEntity findLookedAtLivingEntity(
            LocalPlayer player,
            double range,
            float partialTick
    ) {
        if (player == null || player.level() == null || !player.isAlive()) {
            return null;
        }

        Level level = player.level();

        Vec3 start = player.getEyePosition(partialTick);
        Vec3 lookDirection = player.getViewVector(partialTick);
        Vec3 requestedEnd = start.add(lookDirection.scale(range));

        BlockHitResult blockHit = level.clip(new ClipContext(
                start,
                requestedEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        Vec3 effectiveEnd = requestedEnd;

        if (blockHit.getType() != HitResult.Type.MISS) {
            effectiveEnd = blockHit.getLocation();
        }

        AABB searchBounds = new AABB(start, effectiveEnd).inflate(1.0D);

        List<Entity> candidates = level.getEntities(
                player,
                searchBounds,
                entity -> isValidTarget(player, entity)
        );

        LivingEntity closestTarget = null;
        double closestDistanceSquared = start.distanceToSqr(effectiveEnd);

        for (Entity candidate : candidates) {
            LivingEntity livingEntity = (LivingEntity) candidate;

            AABB targetBounds = livingEntity.getBoundingBox()
                    .inflate(livingEntity.getPickRadius());

            Optional<Vec3> rayHit = targetBounds.clip(start, effectiveEnd);

            if (rayHit.isEmpty()) {
                continue;
            }

            double distanceSquared = start.distanceToSqr(rayHit.get());

            if (distanceSquared >= closestDistanceSquared) {
                continue;
            }

            closestDistanceSquared = distanceSquared;
            closestTarget = livingEntity;
        }

        return closestTarget;
    }

    private static boolean isValidTarget(LocalPlayer player, Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return false;
        }

        if (entity == player) {
            return false;
        }

        if (!entity.isAlive()) {
            return false;
        }

        if (entity.isSpectator()) {
            return false;
        }

        if (entity.isInvisibleTo(player)) {
            return false;
        }

        return true;
    }

    public static Vec3 getInterpolatedPosition(Entity entity, float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()),
                Mth.lerp(partialTick, entity.zo, entity.getZ())
        );
    }

    public static AABB getInterpolatedBoundingBox(Entity entity, float partialTick) {
        Vec3 interpolatedPosition = getInterpolatedPosition(entity, partialTick);

        double xOffset = interpolatedPosition.x - entity.getX();
        double yOffset = interpolatedPosition.y - entity.getY();
        double zOffset = interpolatedPosition.z - entity.getZ();

        return entity.getBoundingBox().move(xOffset, yOffset, zOffset);
    }
}