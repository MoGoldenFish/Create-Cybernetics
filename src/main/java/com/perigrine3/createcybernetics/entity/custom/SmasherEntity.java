package com.perigrine3.createcybernetics.entity.custom;

import com.perigrine3.createcybernetics.common.attributes.ModAttributes;
import com.perigrine3.createcybernetics.entity.ai.goal.CyberentityFirestarterAttackGoal;
import com.perigrine3.createcybernetics.entity.ai.goal.CyberentityPneumaticCalvesJumpGoal;
import com.perigrine3.createcybernetics.entity.ai.goal.CyberentitySandevistanGoal;
import com.perigrine3.createcybernetics.entity.ai.goal.SmasherAttackGoal;
import com.perigrine3.createcybernetics.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;

public class SmasherEntity extends AbstractIllager {
    private static final EntityDataAccessor<Integer> ATTACK_ANIMATION =
            SynchedEntityData.defineId(SmasherEntity.class, EntityDataSerializers.INT);

    private static final int GRAB_START_TICK = 12;
    private static final int GRAB_SLAM_TICK = 18;

    private static final double GRAB_FORWARD_OFFSET = 0.85D;
    private static final double GRAB_ARM1_SIDE_OFFSET = 1.45D;
    private static final double GRAB_HEIGHT_OFFSET = 1.15D;

    private static final double SLAM_FORWARD_OFFSET = 1.10D;

    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState smashAttackAnimationState = new AnimationState();
    public final AnimationState knockbackAnimationState = new AnimationState();
    public final AnimationState kickAttackAnimationState = new AnimationState();
    public final AnimationState grabAndSlamAnimationState = new AnimationState();

    private int idleAnimationTimeout = 0;

    private int clientAttackAnimation = -1;

    private AttackAnimation activeAttackAnimation = AttackAnimation.NONE;
    private LivingEntity attackTarget;
    private LivingEntity grabbedTarget;
    private int attackAnimationTicks = 0;

    public SmasherEntity(EntityType<? extends AbstractIllager> entityType, Level level) {
        super(entityType, level);
        this.setCanJoinRaid(true);
        this.xpReward = 50;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(ATTACK_ANIMATION, AttackAnimation.NONE.id());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        this.goalSelector.addGoal(1, new SmasherAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(1, new CyberentityPneumaticCalvesJumpGoal(this));
        this.goalSelector.addGoal(1, new CyberentityFirestarterAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(1, new CyberentitySandevistanGoal(this));

        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 25.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 150.0D)
                .add(Attributes.ATTACK_DAMAGE, 18.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0D)
                .add(Attributes.OXYGEN_BONUS, 0.0D)
                .add(Attributes.JUMP_STRENGTH, 0.42D)
                .add(Attributes.ATTACK_SPEED, 4.0D)
                .add(Attributes.LUCK, 0.0D)
                .add(Attributes.BLOCK_INTERACTION_RANGE, 4.5D)
                .add(Attributes.ENTITY_INTERACTION_RANGE, 3.0D)
                .add(Attributes.STEP_HEIGHT, 0.6D)
                .add(Attributes.GRAVITY, 0.08D)
                .add(Attributes.SCALE, 1.0D)
                .add(Attributes.FLYING_SPEED, 0.4D)
                .add(Attributes.BLOCK_BREAK_SPEED, 1.0D)
                .add(Attributes.SAFE_FALL_DISTANCE, 3.0D)
                .add(Attributes.BURNING_TIME, 1.0D)
                .add(Attributes.SUBMERGED_MINING_SPEED, 0.2D)
                .add(Attributes.WATER_MOVEMENT_EFFICIENCY, 0.0D)
                .add(Attributes.MINING_EFFICIENCY, 0.0D)
                .add(Attributes.SNEAKING_SPEED, 0.3D)

                .add(NeoForgeMod.SWIM_SPEED, 1.0D)

                .add(ModAttributes.XP_GAIN_MULTIPLIER, 0.0D)
                .add(ModAttributes.ORE_DROP_MULTIPLIER, 0.0D)
                .add(ModAttributes.HAGGLING, 0.0D)
                .add(ModAttributes.ARROW_INACCURACY, 0.0D)
                .add(ModAttributes.BREEDING_MULTIPLIER, 0.0D)
                .add(ModAttributes.CROP_MULTIPLIER, 0.0D)
                .add(ModAttributes.ELYTRA_SPEED, 0.0D)
                .add(ModAttributes.ELYTRA_HANDLING, 0.0D)
                .add(ModAttributes.INSOMNIA, 0.0D)
                .add(ModAttributes.ENDER_PEARL_DAMAGE, 0.0D);
    }

    @Override
    public void applyRaidBuffs(ServerLevel serverLevel, int wave, boolean unused) {
    }

    @Override
    public SoundEvent getCelebrateSound() {
        return net.minecraft.sounds.SoundEvents.VINDICATOR_CELEBRATE;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SMASHER_IDLE.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.SMASHER_STEP.get(), 1F, 1F + this.random.nextFloat() * 0.15F);
    }

    @Override
    public float getVoicePitch() {
        return 0.75F + (this.random.nextFloat() - 0.5F) * 0.05F;
    }

    @Override
    protected float getSoundVolume() {
        return 25F;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean flag = super.doHurtTarget(target);

        if (flag && target instanceof LivingEntity living) {
            living.hurt(this.level().damageSources().mobAttack(this), 6.0F);
        }

        return flag;
    }

    public boolean isPerformingAttack() {
        return this.activeAttackAnimation != AttackAnimation.NONE;
    }

    public void startAttack(LivingEntity target) {
        if (this.level().isClientSide) {
            return;
        }

        if (this.isPerformingAttack()) {
            return;
        }

        this.activeAttackAnimation = AttackAnimation.random(this.random.nextInt(4));
        this.attackTarget = target;
        this.grabbedTarget = null;
        this.attackAnimationTicks = 0;

        this.entityData.set(ATTACK_ANIMATION, this.activeAttackAnimation.id());
    }

    private void tickAttackAnimation() {
        if (this.activeAttackAnimation == AttackAnimation.NONE) {
            return;
        }

        ++this.attackAnimationTicks;

        if (this.activeAttackAnimation == AttackAnimation.GRAB_AND_SLAM) {
            this.tickGrabAndSlamAttack();
        } else if (this.attackAnimationTicks == this.activeAttackAnimation.hitTick()) {
            this.performAttackHit();
        }

        if (this.attackAnimationTicks >= this.activeAttackAnimation.durationTicks()) {
            this.stopAttack();
        }
    }

    private void tickGrabAndSlamAttack() {
        if (this.attackAnimationTicks == GRAB_START_TICK) {
            this.beginGrab();
        }

        if (this.attackAnimationTicks >= GRAB_START_TICK && this.attackAnimationTicks < GRAB_SLAM_TICK) {
            this.holdGrabbedTarget();
        }

        if (this.attackAnimationTicks == GRAB_SLAM_TICK) {
            this.slamGrabbedTarget();
        }
    }

    private void beginGrab() {
        if (this.attackTarget == null || !this.attackTarget.isAlive()) {
            return;
        }

        if (!this.canAttack(this.attackTarget)) {
            return;
        }

        if (this.distanceToSqr(this.attackTarget) > this.getSmasherAttackReachSqr(this.attackTarget)) {
            return;
        }

        this.grabbedTarget = this.attackTarget;
        this.holdGrabbedTarget();
    }

    private void holdGrabbedTarget() {
        if (this.grabbedTarget == null || !this.grabbedTarget.isAlive()) {
            this.releaseGrabbedTarget();
            return;
        }

        Vec3 heldPosition = this.getGrabbedTargetPosition();

        this.grabbedTarget.setDeltaMovement(Vec3.ZERO);
        this.grabbedTarget.resetFallDistance();
        this.grabbedTarget.setNoGravity(true);

        this.setGrabbedTargetPosition(
                this.grabbedTarget,
                heldPosition.x,
                heldPosition.y,
                heldPosition.z
        );
    }

    private Vec3 getGrabbedTargetPosition() {
        Vec3 forward = this.getLookAngle().multiply(1.0D, 0.0D, 1.0D);

        if (forward.lengthSqr() < 0.0001D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            forward = forward.normalize();
        }

        /*
         * arm1 is the positive-X arm in the Smasher model.
         * This side vector puts the victim at that animated arm.
         */
        Vec3 arm1Side = new Vec3(forward.z, 0.0D, -forward.x);

        return new Vec3(
                this.getX() + forward.x * GRAB_FORWARD_OFFSET + arm1Side.x * GRAB_ARM1_SIDE_OFFSET,
                this.getY() + GRAB_HEIGHT_OFFSET,
                this.getZ() + forward.z * GRAB_FORWARD_OFFSET + arm1Side.z * GRAB_ARM1_SIDE_OFFSET
        );
    }

    private void slamGrabbedTarget() {
        LivingEntity target = this.grabbedTarget;

        if (target == null || !target.isAlive()) {
            this.releaseGrabbedTarget();
            return;
        }

        Vec3 forward = this.getLookAngle().multiply(1.0D, 0.0D, 1.0D);

        if (forward.lengthSqr() < 0.0001D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            forward = forward.normalize();
        }

        double slamX = this.getX() + forward.x * SLAM_FORWARD_OFFSET;
        double slamZ = this.getZ() + forward.z * SLAM_FORWARD_OFFSET;

        BlockPos searchStart = BlockPos.containing(
                slamX,
                Mth.floor(this.getY() + 2.0D),
                slamZ
        );

        BlockPos slamGround = this.findGroundBelow(searchStart, 12);

        if (slamGround != null) {
            this.setGrabbedTargetPosition(
                    target,
                    slamX,
                    slamGround.getY() + 1.0D,
                    slamZ
            );

            target.setNoGravity(false);
            target.setDeltaMovement(0.0D, -1.25D, 0.0D);
            target.resetFallDistance();

            this.spawnSlamBlockParticles(slamGround);
        } else {
            this.setGrabbedTargetPosition(
                    target,
                    slamX,
                    this.getY(),
                    slamZ
            );

            target.setNoGravity(false);
            target.setDeltaMovement(0.0D, -1.25D, 0.0D);
            target.resetFallDistance();
        }

        if (this.canAttack(target)) {
            this.doHurtTarget(target);
        }

        this.releaseGrabbedTarget();
    }

    private void setGrabbedTargetPosition(LivingEntity target, double x, double y, double z) {
        if (target instanceof ServerPlayer serverPlayer) {
            serverPlayer.setDeltaMovement(Vec3.ZERO);
            serverPlayer.hurtMarked = true;

            serverPlayer.connection.teleport(
                    x,
                    y,
                    z,
                    serverPlayer.getYRot(),
                    serverPlayer.getXRot()
            );

            return;
        }

        target.setPos(x, y, z);
        target.hurtMarked = true;
    }

    private void releaseGrabbedTarget() {
        if (this.grabbedTarget != null) {
            this.grabbedTarget.setNoGravity(false);
            this.grabbedTarget.setDeltaMovement(Vec3.ZERO);
            this.grabbedTarget.resetFallDistance();
        }

        this.grabbedTarget = null;
    }

    private BlockPos findGroundBelow(BlockPos start, int maxDistance) {
        BlockPos.MutableBlockPos cursor = start.mutable();

        for (int i = 0; i <= maxDistance; i++) {
            BlockState state = this.level().getBlockState(cursor);

            if (!state.isAir() && !state.getCollisionShape(this.level(), cursor).isEmpty()) {
                return cursor.immutable();
            }

            cursor.move(Direction.DOWN);
        }

        return null;
    }

    private void spawnSlamBlockParticles(BlockPos center) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        for (int xOffset = -1; xOffset <= 1; xOffset++) {
            for (int zOffset = -1; zOffset <= 1; zOffset++) {
                BlockPos particleSearchStart = center.offset(xOffset, 2, zOffset);
                BlockPos particleGround = this.findGroundBelow(particleSearchStart, 5);

                if (particleGround == null) {
                    continue;
                }

                BlockState groundState = serverLevel.getBlockState(particleGround);

                if (groundState.isAir()) {
                    continue;
                }

                serverLevel.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, groundState),
                        particleGround.getX() + 0.5D,
                        particleGround.getY() + 1.05D,
                        particleGround.getZ() + 0.5D,
                        18,
                        0.42D,
                        0.24D,
                        0.42D,
                        0.15D
                );
            }
        }
    }

    private void performAttackHit() {
        if (this.attackTarget == null) {
            return;
        }

        if (!this.attackTarget.isAlive()) {
            return;
        }

        if (!this.canAttack(this.attackTarget)) {
            return;
        }

        if (this.distanceToSqr(this.attackTarget) > this.getSmasherAttackReachSqr(this.attackTarget)) {
            return;
        }

        boolean hit = this.doHurtTarget(this.attackTarget);

        if (!hit) {
            return;
        }

        if (this.activeAttackAnimation == AttackAnimation.KNOCKBACK) {
            this.attackTarget.knockback(
                    1.25F,
                    this.attackTarget.getX() - this.getX(),
                    this.attackTarget.getZ() - this.getZ()
            );
        }

        if (this.activeAttackAnimation == AttackAnimation.KICKATTACK) {
            this.attackTarget.knockback(
                    1.75F,
                    this.attackTarget.getX() - this.getX(),
                    this.attackTarget.getZ() - this.getZ()
            );
        }
    }

    private double getSmasherAttackReachSqr(LivingEntity target) {
        float reach = this.getBbWidth() * 2.0F;

        return reach * reach + target.getBbWidth();
    }

    private void stopAttack() {
        this.activeAttackAnimation = AttackAnimation.NONE;
        this.attackTarget = null;
        this.releaseGrabbedTarget();
        this.attackAnimationTicks = 0;

        this.entityData.set(ATTACK_ANIMATION, AttackAnimation.NONE.id());
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        if (entity instanceof net.minecraft.world.entity.raid.Raider) return true;
        if (entity instanceof net.minecraft.world.entity.monster.Ravager) return true;
        return super.isAlliedTo(entity);
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return true;
    }

    private void setupAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = 80;
            idleAnimationState.start(this.tickCount);
        } else {
            --this.idleAnimationTimeout;
        }

        int syncedAnimation = this.entityData.get(ATTACK_ANIMATION);

        if (syncedAnimation == this.clientAttackAnimation) {
            return;
        }

        this.stopAttackAnimationStates();
        this.clientAttackAnimation = syncedAnimation;

        AttackAnimation animation = AttackAnimation.byId(syncedAnimation);

        switch (animation) {
            case SMASHATTACK -> this.smashAttackAnimationState.start(this.tickCount);
            case KNOCKBACK -> this.knockbackAnimationState.start(this.tickCount);
            case KICKATTACK -> this.kickAttackAnimationState.start(this.tickCount);
            case GRAB_AND_SLAM -> this.grabAndSlamAnimationState.start(this.tickCount);
            case NONE -> {
            }
        }
    }

    private void stopAttackAnimationStates() {
        this.smashAttackAnimationState.stop();
        this.knockbackAnimationState.stop();
        this.kickAttackAnimationState.stop();
        this.grabAndSlamAnimationState.stop();
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            this.setupAnimationStates();
        } else {
            this.tickAttackAnimation();
        }
    }

    public enum AttackAnimation {
        NONE(0, 0, 0),
        SMASHATTACK(1, 28, 6),
        KNOCKBACK(2, 30, 15),
        KICKATTACK(3, 38, 10),
        GRAB_AND_SLAM(4, 34, GRAB_SLAM_TICK);

        private final int id;
        private final int durationTicks;
        private final int hitTick;

        AttackAnimation(int id, int durationTicks, int hitTick) {
            this.id = id;
            this.durationTicks = durationTicks;
            this.hitTick = hitTick;
        }

        public int id() {
            return id;
        }

        public int durationTicks() {
            return durationTicks;
        }

        public int hitTick() {
            return hitTick;
        }

        public static AttackAnimation byId(int id) {
            for (AttackAnimation animation : values()) {
                if (animation.id == id) {
                    return animation;
                }
            }

            return NONE;
        }

        public static AttackAnimation random(int index) {
            return switch (index) {
                case 0 -> SMASHATTACK;
                case 1 -> KNOCKBACK;
                case 2 -> KICKATTACK;
                case 3 -> GRAB_AND_SLAM;
                default -> SMASHATTACK;
            };
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        this.releaseGrabbedTarget();
        super.remove(reason);
    }
}