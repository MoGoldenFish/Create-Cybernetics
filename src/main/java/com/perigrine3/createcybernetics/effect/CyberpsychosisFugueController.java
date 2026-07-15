package com.perigrine3.createcybernetics.effect;

import com.perigrine3.createcybernetics.common.damage.ModDamageSources;
import com.perigrine3.createcybernetics.common.toggle.CyberwareToggleController;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CyberpsychosisFugueController {

    private static final Map<UUID, FugueState> STATES = new HashMap<>();

    private static final double TARGET_SEARCH_RADIUS = 24.0D;
    private static final double TARGET_KEEP_RADIUS = 32.0D;

    private static final double ATTACK_RANGE = 3.15D;
    private static final int ATTACK_COOLDOWN_TICKS = 12;

    private static final int RETARGET_INTERVAL_TICKS = 10;
    private static final int FUGUE_SYNC_DURATION_TICKS = 12;

    private CyberpsychosisFugueController() {
    }

    public static boolean isInFugue(Player player) {
        if (player == null) {
            return false;
        }

        FugueState state = STATES.get(player.getUUID());
        return state != null && state.inFugue;
    }

    public static void clear(Player player) {
        if (player == null) {
            return;
        }

        FugueState removed = STATES.remove(player.getUUID());

        if (removed != null) {
            player.setSprinting(false);
        }

        player.removeEffect(ModEffects.CYBERPSYCHOSIS_FUGUE);
    }

    public static void tick(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (serverPlayer.isCreative() || serverPlayer.isSpectator() || !serverPlayer.isAlive()) {
            clear(serverPlayer);
            return;
        }

        CyberpsychosisSeverity severity = CyberpsychosisSeverity.fromPlayer(serverPlayer);
        if (severity != CyberpsychosisSeverity.LEVEL_3) {
            clear(serverPlayer);
            return;
        }

        float negativeProgress = CyberpsychosisSeverity.getNegativeProgress(serverPlayer);
        FugueState state = STATES.computeIfAbsent(serverPlayer.getUUID(), id -> new FugueState());

        state.tick(serverPlayer, negativeProgress);

        if (!state.inFugue) {
            serverPlayer.setSprinting(false);
            serverPlayer.removeEffect(ModEffects.CYBERPSYCHOSIS_FUGUE);
            return;
        }

        CyberwareToggleController.forceAllToggleablesActiveForFugue(serverPlayer);

        syncFugueEffect(serverPlayer, negativeProgress);
        forceCloseScreens(serverPlayer);

        if (serverPlayer.tickCount % RETARGET_INTERVAL_TICKS == 0 || !isValidTarget(serverPlayer, state.target)) {
            state.target = findTarget(serverPlayer);
        }

        if (isValidTarget(serverPlayer, state.target)) {
            tickAttackTarget(serverPlayer, state, state.target);
        }
    }

    private static void syncFugueEffect(ServerPlayer player, float negativeProgress) {
        int amplifier = negativeProgress >= 0.66F ? 2 : negativeProgress >= 0.33F ? 1 : 0;

        MobEffectInstance existing = player.getEffect(ModEffects.CYBERPSYCHOSIS_FUGUE);
        if (existing != null && existing.getDuration() > 5 && existing.getAmplifier() == amplifier) {
            return;
        }

        player.addEffect(new MobEffectInstance(
                ModEffects.CYBERPSYCHOSIS_FUGUE,
                FUGUE_SYNC_DURATION_TICKS,
                amplifier,
                false,
                false,
                false
        ));
    }

    private static void forceCloseScreens(ServerPlayer player) {
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
    }

    private static void tickAttackTarget(ServerPlayer player, FugueState state, LivingEntity target) {
        Vec3 targetLookPos = target.position().add(0.0D, target.getBbHeight() * 0.65D, 0.0D);
        lookAt(player, targetLookPos, 35.0F, 22.0F);

        double distanceSqr = player.distanceToSqr(target);
        double attackRangeSqr = ATTACK_RANGE * ATTACK_RANGE;

        player.setSprinting(distanceSqr > attackRangeSqr);

        if (distanceSqr <= attackRangeSqr && state.attackCooldown <= 0 && player.hasLineOfSight(target)) {
            forceAttack(player, target);
            state.attackCooldown = ATTACK_COOLDOWN_TICKS;
        }

        if (state.attackCooldown > 0) {
            state.attackCooldown--;
        }
    }

    private static void forceAttack(ServerPlayer player, LivingEntity target) {
        if (!target.isAlive()) {
            return;
        }

        player.swing(InteractionHand.MAIN_HAND, true);
        player.attack(target);
        player.resetAttackStrengthTicker();

        if (target.isAlive() && player.getRandom().nextFloat() < 0.10F) {
            target.hurt(ModDamageSources.cyberwareRejection(player.level(), player, null), 1.0F);
        }
    }

    private static LivingEntity findTarget(ServerPlayer player) {
        Level level = player.level();

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(TARGET_SEARCH_RADIUS),
                entity -> isCandidateTarget(player, entity)
        );

        if (candidates.isEmpty()) {
            return null;
        }

        return candidates.stream()
                .min(Comparator
                        .comparingInt(CyberpsychosisFugueController::targetPriority)
                        .thenComparingDouble(player::distanceToSqr))
                .orElse(null);
    }

    private static boolean isCandidateTarget(ServerPlayer player, LivingEntity entity) {
        if (entity == player) {
            return false;
        }

        if (!entity.isAlive()) {
            return false;
        }

        if (!EntitySelector.NO_SPECTATORS.test(entity)) {
            return false;
        }

        if (entity instanceof Player otherPlayer) {
            return !otherPlayer.isCreative() && !otherPlayer.isSpectator();
        }

        return player.distanceToSqr(entity) <= TARGET_SEARCH_RADIUS * TARGET_SEARCH_RADIUS;
    }

    private static boolean isValidTarget(ServerPlayer player, LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }

        if (target == player) {
            return false;
        }

        if (target.level() != player.level()) {
            return false;
        }

        if (target instanceof Player otherPlayer && (otherPlayer.isCreative() || otherPlayer.isSpectator())) {
            return false;
        }

        return player.distanceToSqr(target) <= TARGET_KEEP_RADIUS * TARGET_KEEP_RADIUS;
    }

    private static int targetPriority(LivingEntity entity) {
        if (entity instanceof Player) {
            return 0;
        }

        if (entity instanceof AbstractVillager) {
            return 1;
        }

        if (entity instanceof Animal) {
            return 2;
        }

        if (entity instanceof Enemy) {
            return 3;
        }

        return 4;
    }

    private static void lookAt(ServerPlayer player, Vec3 targetPos, float maxYawChange, float maxPitchChange) {
        Vec3 eye = player.getEyePosition();
        Vec3 delta = targetPos.subtract(eye);

        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

        float wantedYaw = (float) (Mth.atan2(delta.z, delta.x) * (180.0D / Math.PI)) - 90.0F;
        float wantedPitch = (float) (-(Mth.atan2(delta.y, horizontal) * (180.0D / Math.PI)));

        float yaw = rotateToward(player.getYRot(), wantedYaw, maxYawChange);
        float pitch = rotateToward(player.getXRot(), wantedPitch, maxPitchChange);

        pitch = Mth.clamp(pitch, -89.0F, 89.0F);

        player.setYRot(yaw);
        player.setXRot(pitch);

        player.yHeadRot = yaw;
        player.yBodyRot = yaw;
        player.yHeadRotO = yaw;
        player.yBodyRotO = yaw;
    }

    private static float rotateToward(float current, float target, float maxChange) {
        float delta = Mth.wrapDegrees(target - current);
        delta = Mth.clamp(delta, -maxChange, maxChange);
        return current + delta;
    }

    private static final class FugueState {
        private boolean inFugue = false;

        private int fugueTicksLeft = 0;
        private int controlTicksLeft = 0;

        private int attackCooldown = 0;

        private LivingEntity target = null;

        private void tick(ServerPlayer player, float negativeProgress) {
            if (inFugue) {
                fugueTicksLeft--;

                if (fugueTicksLeft <= 0) {
                    inFugue = false;
                    target = null;
                    attackCooldown = 0;
                    controlTicksLeft = rollControlTicks(player, negativeProgress);
                }

                return;
            }

            if (controlTicksLeft > 0) {
                controlTicksLeft--;
                return;
            }

            inFugue = true;
            fugueTicksLeft = rollFugueTicks(player, negativeProgress);
            target = null;
            attackCooldown = 0;
        }

        private static int rollFugueTicks(ServerPlayer player, float negativeProgress) {
            int min = Mth.floor(Mth.lerp(negativeProgress, 80.0F, 240.0F));
            int max = Mth.floor(Mth.lerp(negativeProgress, 160.0F, 500.0F));
            return Mth.nextInt(player.getRandom(), min, max);
        }

        private static int rollControlTicks(ServerPlayer player, float negativeProgress) {
            int min = Mth.floor(Mth.lerp(negativeProgress, 120.0F, 25.0F));
            int max = Mth.floor(Mth.lerp(negativeProgress, 300.0F, 80.0F));
            return Mth.nextInt(player.getRandom(), min, max);
        }
    }
}