package com.perigrine3.createcybernetics.block.ironsspells;

import com.mojang.serialization.MapCodec;
import com.perigrine3.createcybernetics.compat.ironsspells.IronsSpellbooksCompatBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class HexcoreBlock extends BaseEntityBlock {
    public static final MapCodec<HexcoreBlock> CODEC = simpleCodec(HexcoreBlock::new);

    private static final VoxelShape SHAPE = Block.box(
            4.0D, 4.0D, 4.0D,
            12.0D, 12.0D, 12.0D
    );

    public HexcoreBlock(BlockBehaviour.Properties properties) {
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HexcoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {
        if (level.isClientSide) {
            return null;
        }

        return createTickerHelper(
                blockEntityType,
                IronsSpellbooksCompatBlockEntities.HEXCORE.get(),
                HexcoreBlockEntity::serverTick
        );
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!(blockEntity instanceof HexcoreBlockEntity hexcoreBlockEntity)) {
            return InteractionResult.PASS;
        }

        int remainingLevels = Math.max(
                0,
                hexcoreBlockEntity.getMaxExperienceLevels()
                        - hexcoreBlockEntity.getStoredExperienceLevels()
        );

        player.displayClientMessage(
                Component.translatable(
                        "message.createcybernetics.hexcore.experience_required",
                        remainingLevels
                ),
                true
        );

        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof HexcoreBlockEntity hexcoreBlockEntity) {
                hexcoreBlockEntity.releaseStoredExperience();
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    protected boolean propagatesSkylightDown(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return true;
    }

    @Override
    protected float getShadeBrightness(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return 1.0F;
    }
}