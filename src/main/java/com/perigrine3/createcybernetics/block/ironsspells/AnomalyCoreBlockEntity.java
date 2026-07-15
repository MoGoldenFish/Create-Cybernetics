package com.perigrine3.createcybernetics.block.ironsspells;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.compat.ironsspells.IronsSpellbooksCompatBlockEntities;
import com.perigrine3.createcybernetics.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class AnomalyCoreBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    private static final int RITUAL_DURATION_TICKS = 100;

    private static final int DAMAGE_INTERVAL_TICKS = 10;
    private static final float DAMAGE_PER_INTERVAL = 1.0F;

    private static final double ENTITY_PULL_RADIUS = 8.0D;
    private static final double ENTITY_PULL_STRENGTH = 0.18D;

    private static final double PLAYER_CAPTURE_HEIGHT = 1.15D;

    private static final double PLAYER_THROW_STRENGTH = 1.45D;
    private static final double PLAYER_THROW_HEIGHT = 0.65D;

    /*
     * The visible Anomaly Core shape is:
     *
     * Block.box(4, 4, 4, 12, 12, 12)
     *
     * which occupies 0.25D through 0.75D on every block axis.
     *
     * Entities collide against the outside edge of that shape and will usually
     * never enter the exact 0.25D -> 0.75D volume. This expands the contact
     * area enough that touching the outer collision surface counts as contact.
     */
    private static final double CORE_CONTACT_MIN = 0.05D;
    private static final double CORE_CONTACT_MAX = 0.95D;

    /*
     * Extra bounding-box padding is needed because an entity can be resting
     * exactly against a collision face without mathematically intersecting it.
     */
    private static final double CONTACT_PADDING = 0.08D;

    private static final float ENTITY_CONSUME_DAMAGE = 1000.0F;

    private static final String NBT_ACTIVE_PLAYER = "ActivePlayer";
    private static final String NBT_REMAINING_TICKS = "RemainingTicks";
    private static final String NBT_LAUNCH_X = "LaunchX";
    private static final String NBT_LAUNCH_Y = "LaunchY";
    private static final String NBT_LAUNCH_Z = "LaunchZ";
    private static final String NBT_RITUAL_USED = "RitualUsed";

    @Nullable
    private UUID activePlayerUuid;

    private int remainingTicks;

    private Vec3 launchDirection = Vec3.ZERO;

    /*
     * This becomes true as soon as the ritual begins.
     *
     * The core remains permanently spent even if the player dies, disconnects,
     * leaves the dimension, or the ritual otherwise fails to finish.
     */
    private boolean ritualUsed;

    public AnomalyCoreBlockEntity(BlockPos pos, BlockState blockState) {
        super(IronsSpellbooksCompatBlockEntities.ANOMALY_CORE.get(), pos, blockState);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            AnomalyCoreBlockEntity blockEntity
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        blockEntity.tickRitual(serverLevel);
    }

    public boolean tryStartRitual(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        if (!(level instanceof ServerLevel)) {
            return false;
        }

        if (ritualUsed || isRitualActive()) {
            return false;
        }

        if (!canReceiveAnomalyHeart(serverPlayer)) {
            return false;
        }

        ritualUsed = true;
        activePlayerUuid = serverPlayer.getUUID();
        remainingTicks = RITUAL_DURATION_TICKS;
        launchDirection = getLaunchDirection(serverPlayer);

        serverPlayer.closeContainer();

        setChanged();
        return true;
    }

    private void tickRitual(ServerLevel level) {
        if (!isRitualActive()) {
            return;
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(activePlayerUuid);

        if (player == null || !player.isAlive() || player.level() != level) {
            clearActiveRitual();
            return;
        }

        Vec3 center = Vec3.atCenterOf(worldPosition);

        consumeEntitiesTouchingCore(level, player);
        holdPlayerAtCore(player, center);
        pullNearbyEntities(level, player, center);
        spawnCaptureParticles(level, player, center);

        if (remainingTicks % DAMAGE_INTERVAL_TICKS == 0) {
            player.hurt(level.damageSources().magic(), DAMAGE_PER_INTERVAL);
        }

        remainingTicks--;

        if (remainingTicks > 0) {
            setChanged();
            return;
        }

        finishRitual(level, player, center);
    }

    private void holdPlayerAtCore(ServerPlayer player, Vec3 center) {
        Vec3 capturePosition = new Vec3(
                center.x,
                worldPosition.getY() + PLAYER_CAPTURE_HEIGHT,
                center.z
        );

        player.connection.teleport(
                capturePosition.x,
                capturePosition.y,
                capturePosition.z,
                player.getYRot(),
                player.getXRot()
        );

        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.hurtMarked = true;
    }

    private void pullNearbyEntities(
            ServerLevel level,
            ServerPlayer activePlayer,
            Vec3 center
    ) {
        List<Entity> nearbyEntities = level.getEntities(
                activePlayer,
                getEntityPullArea(),
                entity -> entity != activePlayer
                        && !entity.isRemoved()
                        && !entity.isSpectator()
        );

        for (Entity entity : nearbyEntities) {
            pullEntityTowardCore(entity, center);

            if (entity instanceof ItemEntity itemEntity) {
                itemEntity.setPickUpDelay(20);
            }

            spawnInwardParticle(
                    level,
                    entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D),
                    center
            );
        }
    }

    private void pullEntityTowardCore(Entity entity, Vec3 center) {
        Vec3 entityCenter = entity.getBoundingBox().getCenter();
        Vec3 difference = center.subtract(entityCenter);

        double distance = difference.length();

        if (distance <= 0.05D) {
            entity.setDeltaMovement(Vec3.ZERO);
            entity.hurtMarked = true;
            return;
        }

        double pullStrength = ENTITY_PULL_STRENGTH * Mth.clamp(
                1.4D - (distance / ENTITY_PULL_RADIUS),
                0.35D,
                1.4D
        );

        Vec3 pull = difference.normalize().scale(pullStrength);

        entity.setDeltaMovement(
                entity.getDeltaMovement()
                        .scale(0.45D)
                        .add(pull)
        );

        entity.hurtMarked = true;
    }

    private void consumeEntitiesTouchingCore(
            ServerLevel level,
            ServerPlayer activePlayer
    ) {
        AABB coreContactArea = getCoreContactArea();

        List<Entity> nearbyEntities = level.getEntities(
                activePlayer,
                coreContactArea.inflate(1.0D),
                entity -> entity != activePlayer
                        && !entity.isRemoved()
                        && !entity.isSpectator()
        );

        for (Entity entity : nearbyEntities) {
            /*
             * Use the expanded bounding box. This catches entities that are
             * physically pressed against the Anomaly Core collision shape.
             */
            if (!entity.getBoundingBox()
                    .inflate(CONTACT_PADDING)
                    .intersects(coreContactArea)) {
                continue;
            }

            /*
             * Dropped item stacks are destroyed without producing another
             * item entity or allowing the stack to be recovered.
             */
            if (entity instanceof ItemEntity) {
                entity.discard();
                continue;
            }

            /*
             * Deal real magic damage before using removal as a fallback.
             *
             * This allows normal living entities to die, fire death events,
             * invoke death handling, and respect their standard hurt logic.
             */
            entity.hurt(level.damageSources().magic(), ENTITY_CONSUME_DAMAGE);

            /*
             * Projectiles, armor stands, invulnerable entities, or entities
             * with custom damage handling can survive or ignore hurt(...).
             * They must still be destroyed when they contact the core.
             */
            if (!entity.isRemoved() && entity.isAlive()) {
                entity.discard();
            }
        }
    }

    private void spawnCaptureParticles(
            ServerLevel level,
            ServerPlayer player,
            Vec3 center
    ) {
        Vec3 playerPosition = player.position().add(0.0D, 0.9D, 0.0D);

        for (int i = 0; i < 5; i++) {
            double offsetX = (level.random.nextDouble() - 0.5D) * 1.2D;
            double offsetY = (level.random.nextDouble() - 0.5D) * 1.2D;
            double offsetZ = (level.random.nextDouble() - 0.5D) * 1.2D;

            spawnInwardParticle(
                    level,
                    playerPosition.add(offsetX, offsetY, offsetZ),
                    center
            );
        }

        level.sendParticles(
                ParticleTypes.ENCHANT,
                center.x,
                center.y,
                center.z,
                2,
                0.12D,
                0.12D,
                0.12D,
                0.0D
        );
    }

    private void spawnInwardParticle(
            ServerLevel level,
            Vec3 start,
            Vec3 center
    ) {
        Vec3 velocity = center.subtract(start);

        if (velocity.lengthSqr() > 0.0001D) {
            velocity = velocity.normalize().scale(0.18D);
        } else {
            velocity = Vec3.ZERO;
        }

        level.sendParticles(
                ParticleTypes.ENCHANT,
                start.x,
                start.y,
                start.z,
                0,
                velocity.x,
                velocity.y,
                velocity.z,
                1.0D
        );
    }

    private void finishRitual(
            ServerLevel level,
            ServerPlayer player,
            Vec3 center
    ) {
        boolean installed = installAnomalyHeart(player);

        spawnReleaseParticles(level, center);

        Vec3 releasePosition = new Vec3(
                center.x + (launchDirection.x * 0.55D),
                worldPosition.getY() + PLAYER_CAPTURE_HEIGHT,
                center.z + (launchDirection.z * 0.55D)
        );

        player.connection.teleport(
                releasePosition.x,
                releasePosition.y,
                releasePosition.z,
                player.getYRot(),
                player.getXRot()
        );

        player.setDeltaMovement(
                launchDirection.scale(PLAYER_THROW_STRENGTH)
                        .add(0.0D, PLAYER_THROW_HEIGHT, 0.0D)
        );

        player.hurtMarked = true;

        if (!installed) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "The Anomaly Core rejects your current heart configuration."
                    ),
                    true
            );
        }

        clearActiveRitual();
    }

    private void spawnReleaseParticles(ServerLevel level, Vec3 center) {
        level.sendParticles(
                ParticleTypes.WITCH,
                center.x,
                center.y,
                center.z,
                70,
                0.9D,
                0.9D,
                0.9D,
                0.14D
        );

        level.sendParticles(
                ParticleTypes.ENCHANT,
                center.x,
                center.y,
                center.z,
                120,
                1.25D,
                1.25D,
                1.25D,
                0.20D
        );
    }

    private boolean canReceiveAnomalyHeart(ServerPlayer player) {
        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);

        if (data == null) {
            return false;
        }

        Item anomalyHeart = ModItems.HEARTUPGRADES_ANOMALY.get();

        if (data.hasSpecificItem(anomalyHeart, CyberwareSlot.HEART)) {
            return false;
        }

        for (int i = 0; i < CyberwareSlot.HEART.size; i++) {
            InstalledCyberware installed = data.get(CyberwareSlot.HEART, i);

            if (installed == null
                    || installed.getItem() == null
                    || installed.getItem().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private boolean installAnomalyHeart(ServerPlayer player) {
        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);

        if (data == null) {
            return false;
        }

        Item anomalyHeart = ModItems.HEARTUPGRADES_ANOMALY.get();

        if (!(anomalyHeart instanceof ICyberwareItem cyberwareItem)) {
            return false;
        }

        if (data.hasSpecificItem(anomalyHeart, CyberwareSlot.HEART)) {
            return true;
        }

        for (int i = 0; i < CyberwareSlot.HEART.size; i++) {
            InstalledCyberware existing = data.get(CyberwareSlot.HEART, i);

            if (existing != null
                    && existing.getItem() != null
                    && !existing.getItem().isEmpty()) {
                continue;
            }

            ItemStack stack = new ItemStack(anomalyHeart);

            InstalledCyberware installed = new InstalledCyberware(
                    stack,
                    CyberwareSlot.HEART,
                    i,
                    cyberwareItem.getHumanityCost()
            );

            installed.setPowered(true);

            data.set(CyberwareSlot.HEART, i, installed);
            cyberwareItem.onInstalled(player);

            data.recomputeHumanityBaseFromInstalled();
            data.setDirty();

            ModAttachments.syncCyberware(player);
            player.syncData(ModAttachments.CYBERWARE);

            return true;
        }

        return false;
    }

    private Vec3 getLaunchDirection(ServerPlayer player) {
        Vec3 center = Vec3.atCenterOf(worldPosition);

        Vec3 awayFromCore = player.position()
                .subtract(center)
                .multiply(1.0D, 0.0D, 1.0D);

        if (awayFromCore.lengthSqr() > 0.001D) {
            return awayFromCore.normalize();
        }

        Vec3 fallback = player.getLookAngle()
                .multiply(-1.0D, 0.0D, -1.0D);

        if (fallback.lengthSqr() > 0.001D) {
            return fallback.normalize();
        }

        return new Vec3(0.0D, 0.0D, 1.0D);
    }

    private AABB getEntityPullArea() {
        return new AABB(
                worldPosition.getX() - ENTITY_PULL_RADIUS,
                worldPosition.getY() - ENTITY_PULL_RADIUS,
                worldPosition.getZ() - ENTITY_PULL_RADIUS,
                worldPosition.getX() + ENTITY_PULL_RADIUS + 1.0D,
                worldPosition.getY() + ENTITY_PULL_RADIUS + 1.0D,
                worldPosition.getZ() + ENTITY_PULL_RADIUS + 1.0D
        );
    }

    private AABB getCoreContactArea() {
        return new AABB(
                worldPosition.getX() + CORE_CONTACT_MIN,
                worldPosition.getY() + CORE_CONTACT_MIN,
                worldPosition.getZ() + CORE_CONTACT_MIN,
                worldPosition.getX() + CORE_CONTACT_MAX,
                worldPosition.getY() + CORE_CONTACT_MAX,
                worldPosition.getZ() + CORE_CONTACT_MAX
        );
    }

    private boolean isRitualActive() {
        return activePlayerUuid != null && remainingTicks > 0;
    }

    private void clearActiveRitual() {
        activePlayerUuid = null;
        remainingTicks = 0;
        launchDirection = Vec3.ZERO;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        if (activePlayerUuid != null) {
            tag.putUUID(NBT_ACTIVE_PLAYER, activePlayerUuid);
        }

        tag.putInt(NBT_REMAINING_TICKS, remainingTicks);
        tag.putDouble(NBT_LAUNCH_X, launchDirection.x);
        tag.putDouble(NBT_LAUNCH_Y, launchDirection.y);
        tag.putDouble(NBT_LAUNCH_Z, launchDirection.z);
        tag.putBoolean(NBT_RITUAL_USED, ritualUsed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        activePlayerUuid = tag.hasUUID(NBT_ACTIVE_PLAYER)
                ? tag.getUUID(NBT_ACTIVE_PLAYER)
                : null;

        remainingTicks = Math.max(0, tag.getInt(NBT_REMAINING_TICKS));

        launchDirection = new Vec3(
                tag.getDouble(NBT_LAUNCH_X),
                tag.getDouble(NBT_LAUNCH_Y),
                tag.getDouble(NBT_LAUNCH_Z)
        );

        ritualUsed = tag.getBoolean(NBT_RITUAL_USED);

        if (activePlayerUuid == null || remainingTicks <= 0) {
            activePlayerUuid = null;
            remainingTicks = 0;
            launchDirection = Vec3.ZERO;
        }
    }
}