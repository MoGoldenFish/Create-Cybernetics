package com.perigrine3.createcybernetics.block;

import com.mojang.serialization.MapCodec;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.block.entity.ModBlockEntities;
import com.perigrine3.createcybernetics.block.entity.RobosurgeonBlockEntity;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.common.energy.ConditionalBlockPower;
import com.perigrine3.createcybernetics.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class RobosurgeonBlock extends BaseEntityBlock {
    public static final MapCodec<RobosurgeonBlock> CODEC = simpleCodec(RobosurgeonBlock::new);

    public static final int ENERGY_REQUIRED_TO_OPEN = 100;
    public static final int ENERGY_USED_PER_GUI_TICK = 100;

    public RobosurgeonBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RobosurgeonBlockEntity(pos, state);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof RobosurgeonBlockEntity blockEntity)) {
            return 0;
        }

        return blockEntity.getComparatorOutput();
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }

        return createTickerHelper(
                blockEntityType,
                ModBlockEntities.ROBOSURGEON_BLOCKENTITY.get(),
                RobosurgeonBlockEntity::serverTick
        );
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!(context instanceof EntityCollisionContext entityContext)) {
            return super.getCollisionShape(state, level, pos, context);
        }

        if (!(entityContext.getEntity() instanceof Player player)) {
            return super.getCollisionShape(state, level, pos, context);
        }

        if (shouldBypassCollision(player)) {
            return Shapes.empty();
        }

        return super.getCollisionShape(state, level, pos, context);
    }

    private static boolean shouldBypassCollision(Player player) {
        if (!player.isCrouching()) {
            return false;
        }

        if (!canSafelyReadCyberware(player)) {
            return false;
        }

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);

        return data.hasSpecificItem(ModItems.BASECYBERWARE_RIGHTARM.get(), CyberwareSlot.RARM)
                && data.hasSpecificItem(ModItems.BASECYBERWARE_LEFTARM.get(), CyberwareSlot.LARM)
                && data.hasSpecificItem(ModItems.BASECYBERWARE_RIGHTLEG.get(), CyberwareSlot.RLEG)
                && data.hasSpecificItem(ModItems.BASECYBERWARE_LEFTLEG.get(), CyberwareSlot.LLEG)
                && data.hasSpecificItem(ModItems.SKINUPGRADES_METALPLATING.get(), CyberwareSlot.SKIN)
                && data.hasSpecificItem(ModItems.MUSCLEUPGRADES_SYNTHMUSCLE.get(), CyberwareSlot.MUSCLE)
                && data.hasSpecificItem(ModItems.HEARTUPGRADES_CYBERHEART.get(), CyberwareSlot.HEART)
                && data.hasSpecificItem(ModItems.BASECYBERWARE_LINEARFRAME.get(), CyberwareSlot.BONE)
                && data.hasSpecificItem(ModItems.BASECYBERWARE_CYBEREYES.get(), CyberwareSlot.EYES)
                && data.hasMultipleSpecificItem(ModItems.BONEUPGRADES_BONELACING.get(), CyberwareSlot.BONE, 3)
                && data.hasMultipleSpecificItem(ModItems.ARMUPGRADES_PNEUMATICWRIST.get(), 2, CyberwareSlot.RARM, CyberwareSlot.LARM)
                && data.hasMultipleSpecificItem(ModItems.LEGUPGRADES_ANKLEBRACERS.get(), 2, CyberwareSlot.RLEG, CyberwareSlot.LLEG)
                && data.hasMultipleSpecificItem(ModItems.LEGUPGRADES_JUMPBOOST.get(), 2, CyberwareSlot.RLEG, CyberwareSlot.LLEG)
                && data.hasSpecificItem(ModItems.ARMUPGRADES_ARMCANNON.get(), CyberwareSlot.RARM, CyberwareSlot.LARM)
                && data.hasSpecificItem(ModItems.EYEUPGRADES_TARGETING.get(), CyberwareSlot.EYES)
                && data.hasSpecificItem(ModItems.BRAINUPGRADES_MATRIX.get(), CyberwareSlot.BRAIN)
                && data.hasSpecificItem(ModItems.BONEUPGRADES_SANDEVISTAN.get(), CyberwareSlot.BONE);
    }

    private static boolean canSafelyReadCyberware(Player player) {
        if (player.level().isClientSide()) {
            return true;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        return serverPlayer.connection != null;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof RobosurgeonBlockEntity robosurgeonBlockEntity) {
                robosurgeonBlockEntity.drops();
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (tryOpenPoweredMenu(level, pos, player)) {
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (tryOpenPoweredMenu(level, pos, player)) {
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.CONSUME;
    }

    private boolean tryOpenPoweredMenu(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return true;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }

        if (!(level.getBlockEntity(pos) instanceof RobosurgeonBlockEntity robosurgeonBlockEntity)) {
            return true;
        }

        boolean powered = ConditionalBlockPower.hasRequiredPower(
                level,
                pos,
                robosurgeonBlockEntity.getMutableEnergyStorage(),
                ENERGY_REQUIRED_TO_OPEN
        );

        if (!powered) {
            if (ConditionalBlockPower.shouldUseEnergyInsteadOfRedstone()) {
                player.displayClientMessage(Component.translatable("message.createcybernetics.block.requires_energy"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.createcybernetics.block.requires_redstone"), true);
            }

            return false;
        }

        serverPlayer.openMenu(
                new SimpleMenuProvider(robosurgeonBlockEntity, Component.literal("_" + player.getName().getString() + ".exe")),
                pos
        );

        return true;
    }
}