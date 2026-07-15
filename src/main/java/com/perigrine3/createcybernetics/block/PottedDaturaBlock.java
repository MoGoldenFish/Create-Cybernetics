package com.perigrine3.createcybernetics.block;

import com.perigrine3.createcybernetics.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

public class PottedDaturaBlock extends FlowerPotBlock {

    public PottedDaturaBlock(
            Supplier<FlowerPotBlock> emptyPot,
            Properties properties
    ) {
        super(emptyPot, () -> Blocks.AIR, properties);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!level.isClientSide) {
            ItemStack flowerStack =
                    new ItemStack(ModItems.DATURA_FLOWER.get());

            if (!player.addItem(flowerStack)) {
                player.drop(flowerStack, false);
            }

            level.setBlock(
                    pos,
                    getEmptyPot().defaultBlockState(),
                    3
            );

            level.gameEvent(
                    player,
                    GameEvent.BLOCK_CHANGE,
                    pos
            );
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public ItemStack getCloneItemStack(
            LevelReader level,
            BlockPos pos,
            BlockState state
    ) {
        return new ItemStack(ModItems.DATURA_FLOWER.get());
    }
}