package com.perigrine3.createcybernetics.block.ironsspells;

import com.perigrine3.createcybernetics.compat.ironsspells.IronsSpellbooksCompatBlockEntities;
import com.perigrine3.createcybernetics.compat.ironsspells.IronsSpellbooksCompatBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public class HexcoreBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    public static final int MAX_EXPERIENCE_LEVELS = 500;
    public static final int MAX_EXPERIENCE = getExperienceForLevel(MAX_EXPERIENCE_LEVELS);

    /*
     * Five complete levels are removed every second from each eligible player.
     *
     * This is intentionally level-based rather than a flat XP-point amount, so
     * the Hexcore remains aggressively draining at higher player levels.
     */
    private static final int PLAYER_LEVELS_DRAINED_PER_SECOND = 5;

    /*
     * XP orbs are checked every server tick before player draining occurs.
     * This makes the Hexcore immediately absorb an orb that has entered range.
     */
    private static final long PLAYER_DRAIN_INTERVAL = 20L;

    /*
     * XP orbs inside the collection cube are pulled toward the center before
     * they are absorbed. This visually reinforces that the Hexcore is claiming
     * the orb rather than the player.
     */
    private static final double ORB_PULL_SPEED = 0.22D;

    private int storedExperience;

    public HexcoreBlockEntity(BlockPos pos, BlockState blockState) {
        super(IronsSpellbooksCompatBlockEntities.HEXCORE.get(), pos, blockState);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            HexcoreBlockEntity blockEntity
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (blockEntity.isFull()) {
            blockEntity.transformIntoAnomalyCore();
            return;
        }

        /*
         * Orbs always have priority over direct player draining.
         */
        blockEntity.collectNearbyExperienceOrbs(serverLevel);

        if (blockEntity.isFull()) {
            blockEntity.transformIntoAnomalyCore();
            return;
        }

        if (serverLevel.getGameTime() % PLAYER_DRAIN_INTERVAL == 0L) {
            blockEntity.drainNearbyPlayers(serverLevel);
        }

        if (blockEntity.isFull()) {
            blockEntity.transformIntoAnomalyCore();
        }
    }

    public int getStoredExperience() {
        return storedExperience;
    }

    public int getStoredExperienceLevels() {
        return getLevelForExperience(storedExperience);
    }

    public int getMaxExperience() {
        return MAX_EXPERIENCE;
    }

    public int getMaxExperienceLevels() {
        return MAX_EXPERIENCE_LEVELS;
    }

    public boolean isFull() {
        return storedExperience >= MAX_EXPERIENCE;
    }

    public void releaseStoredExperience() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (storedExperience <= 0) {
            return;
        }

        ExperienceOrb.award(
                serverLevel,
                Vec3.atCenterOf(worldPosition),
                storedExperience
        );

        storedExperience = 0;
        setChanged();
    }

    public boolean tryAbsorbExperienceOrb(ExperienceOrb experienceOrb) {
        if (experienceOrb == null || experienceOrb.isRemoved() || isFull()) {
            return false;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        if (!getCollectionArea().contains(experienceOrb.position())) {
            return false;
        }

        int orbValue = experienceOrb.getValue();

        if (orbValue <= 0) {
            return false;
        }

        int acceptedExperience = addExperience(orbValue);

        if (acceptedExperience <= 0) {
            return false;
        }

        experienceOrb.discard();

        int remainingExperience = orbValue - acceptedExperience;

        if (remainingExperience > 0) {
            ExperienceOrb.award(
                    serverLevel,
                    experienceOrb.position(),
                    remainingExperience
            );
        }

        return true;
    }

    private void collectNearbyExperienceOrbs(ServerLevel level) {
        if (isFull()) {
            return;
        }

        List<ExperienceOrb> experienceOrbs = level.getEntitiesOfClass(
                ExperienceOrb.class,
                getCollectionArea(),
                experienceOrb -> !experienceOrb.isRemoved()
        );

        Vec3 center = Vec3.atCenterOf(worldPosition);

        for (ExperienceOrb experienceOrb : experienceOrbs) {
            if (isFull()) {
                return;
            }

            pullExperienceOrbTowardCore(experienceOrb, center);
            tryAbsorbExperienceOrb(experienceOrb);
        }
    }

    private void pullExperienceOrbTowardCore(ExperienceOrb experienceOrb, Vec3 center) {
        Vec3 difference = center.subtract(experienceOrb.position());
        double distance = difference.length();

        if (distance <= 0.05D) {
            return;
        }

        Vec3 pull = difference.normalize().scale(ORB_PULL_SPEED);

        experienceOrb.setDeltaMovement(
                experienceOrb.getDeltaMovement().scale(0.55D).add(pull)
        );
        experienceOrb.hasImpulse = true;
    }

    private void drainNearbyPlayers(ServerLevel level) {
        if (isFull()) {
            return;
        }

        List<ServerPlayer> players = level.getEntitiesOfClass(
                ServerPlayer.class,
                getCollectionArea(),
                this::canDrainExperienceFrom
        );

        for (ServerPlayer player : players) {
            if (isFull()) {
                return;
            }

            drainExperienceLevelsFromPlayer(player);
        }
    }

    private void drainExperienceLevelsFromPlayer(ServerPlayer player) {
        if (player.experienceLevel <= 0 && player.experienceProgress <= 0.0F) {
            return;
        }

        int currentExperience = getPlayerExperience(player);

        if (currentExperience <= 0) {
            return;
        }

        int targetLevel = Math.max(
                0,
                player.experienceLevel - PLAYER_LEVELS_DRAINED_PER_SECOND
        );

        int currentLevelExperience = getExperienceForLevel(player.experienceLevel);
        int currentLevelProgressExperience = Math.max(
                0,
                currentExperience - currentLevelExperience
        );

        int targetLevelCapacity = getExperienceNeededForNextLevel(targetLevel);

        int retainedProgressExperience = Math.min(
                currentLevelProgressExperience,
                targetLevelCapacity
        );

        int targetExperience = getExperienceForLevel(targetLevel) + retainedProgressExperience;
        int requestedExperience = Math.max(0, currentExperience - targetExperience);

        if (requestedExperience <= 0) {
            return;
        }

        int acceptedExperience = Math.min(
                requestedExperience,
                MAX_EXPERIENCE - storedExperience
        );

        if (acceptedExperience <= 0) {
            return;
        }

        player.giveExperiencePoints(-acceptedExperience);
        addExperience(acceptedExperience);
    }

    private boolean canDrainExperienceFrom(ServerPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && !player.getAbilities().instabuild;
    }

    private int addExperience(int amount) {
        if (amount <= 0 || isFull()) {
            return 0;
        }

        int acceptedExperience = Math.min(
                amount,
                MAX_EXPERIENCE - storedExperience
        );

        storedExperience += acceptedExperience;
        setChanged();

        return acceptedExperience;
    }

    private void transformIntoAnomalyCore() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!isFull()) {
            return;
        }

        /*
         * HexcoreBlock#onRemove releases stored XP when this block is replaced.
         * Clear the storage first so the 500 stored levels are consumed during
         * conversion rather than dropped into the world.
         */
        storedExperience = 0;
        setChanged();

        serverLevel.setBlock(
                worldPosition,
                IronsSpellbooksCompatBlocks.ANOMALY_CORE.get().defaultBlockState(),
                3
        );
    }

    private AABB getCollectionArea() {
        return new AABB(
                worldPosition.getX() - 1.0D,
                worldPosition.getY() - 1.0D,
                worldPosition.getZ() - 1.0D,
                worldPosition.getX() + 2.0D,
                worldPosition.getY() + 2.0D,
                worldPosition.getZ() + 2.0D
        );
    }

    public static HexcoreBlockEntity findNearestEligibleHexcore(
            ServerLevel level,
            ExperienceOrb experienceOrb
    ) {
        BlockPos orbPos = experienceOrb.blockPosition();

        HexcoreBlockEntity nearestHexcore = null;
        double nearestDistanceSqr = Double.MAX_VALUE;

        for (BlockPos candidatePos : BlockPos.betweenClosed(
                orbPos.offset(-1, -1, -1),
                orbPos.offset(1, 1, 1)
        )) {
            if (!(level.getBlockEntity(candidatePos) instanceof HexcoreBlockEntity hexcoreBlockEntity)) {
                continue;
            }

            if (hexcoreBlockEntity.isFull()) {
                continue;
            }

            if (!hexcoreBlockEntity.getCollectionArea().contains(experienceOrb.position())) {
                continue;
            }

            double distanceSqr = hexcoreBlockEntity.getBlockPos()
                    .distToCenterSqr(experienceOrb.position());

            if (distanceSqr < nearestDistanceSqr) {
                nearestDistanceSqr = distanceSqr;
                nearestHexcore = hexcoreBlockEntity;
            }
        }

        return nearestHexcore;
    }

    private static int getPlayerExperience(ServerPlayer player) {
        int experienceAtCurrentLevel = getExperienceForLevel(player.experienceLevel);

        int progressExperience = Mth.floor(
                player.experienceProgress * getExperienceNeededForNextLevel(player.experienceLevel)
        );

        return experienceAtCurrentLevel + progressExperience;
    }

    public static int getExperienceForLevel(int level) {
        if (level <= 0) {
            return 0;
        }

        if (level <= 16) {
            return level * level + (6 * level);
        }

        if (level <= 31) {
            return (5 * level * level - 81 * level + 720) / 2;
        }

        return (9 * level * level - 325 * level + 4440) / 2;
    }

    public static int getExperienceNeededForNextLevel(int level) {
        if (level >= 30) {
            return 9 * level - 158;
        }

        if (level >= 15) {
            return 5 * level - 38;
        }

        return 2 * level + 7;
    }

    public static int getLevelForExperience(int experience) {
        if (experience <= 0) {
            return 0;
        }

        int level = 0;

        while (getExperienceForLevel(level + 1) <= experience) {
            level++;
        }

        return level;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.putInt("StoredExperience", storedExperience);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        storedExperience = Mth.clamp(
                tag.getInt("StoredExperience"),
                0,
                MAX_EXPERIENCE
        );
    }
}