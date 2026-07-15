package com.perigrine3.createcybernetics.block;

import com.mojang.serialization.MapCodec;
import com.perigrine3.createcybernetics.block.entity.ChargingBlockEntity;
import com.perigrine3.createcybernetics.common.energy.ConditionalBlockPower;
import com.perigrine3.createcybernetics.common.energy.EnergyController;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ChargingBlock extends BaseEntityBlock {
    public static final MapCodec<ChargingBlock> CODEC = simpleCodec(ChargingBlock::new);

    public ChargingBlock(Properties properties) {
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
        return new ChargingBlockEntity(pos, state);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);

        if (level.isClientSide) {
            return;
        }

        if (!(entity instanceof Player player)) {
            return;
        }

        if (!(level.getBlockEntity(pos) instanceof ChargingBlockEntity chargingBlockEntity)) {
            return;
        }

        if (ConditionalBlockPower.shouldUseEnergyInsteadOfRedstone()) {
            int requestedEnergy = EnergyController.getRequestedChargerEnergy(player);

            if (requestedEnergy <= 0) {
                return;
            }

            int extractedEnergy = chargingBlockEntity.getMutableEnergyStorage().extractEnergy(requestedEnergy, false);

            if (extractedEnergy <= 0) {
                return;
            }

            EnergyController.markOnChargingBlock(player, extractedEnergy);
            return;
        }

        if (!level.hasNeighborSignal(pos)) {
            return;
        }

        EnergyController.markOnChargingBlock(player);
    }
}