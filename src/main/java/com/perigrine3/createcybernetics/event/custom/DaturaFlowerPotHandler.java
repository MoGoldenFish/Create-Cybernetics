package com.perigrine3.createcybernetics.event.custom;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.block.ModBlocks;
import com.perigrine3.createcybernetics.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID)
public final class DaturaFlowerPotHandler {

    private DaturaFlowerPotHandler() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Player player = event.getEntity();
        ItemStack heldStack = event.getItemStack();
        BlockState state = level.getBlockState(pos);

        if (!state.is(Blocks.FLOWER_POT)) {
            return;
        }

        if (!heldStack.is(ModItems.DATURA_FLOWER.get())) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(
                InteractionResult.sidedSuccess(level.isClientSide)
        );

        if (level.isClientSide) {
            return;
        }

        BlockState pottedState =
                ModBlocks.POTTED_DATURA.get().defaultBlockState();

        level.setBlock(
                pos,
                pottedState,
                3
        );

        level.playSound(
                null,
                pos,
                SoundEvents.GRASS_PLACE,
                SoundSource.BLOCKS,
                1.0F,
                1.0F
        );

        level.gameEvent(
                player,
                GameEvent.BLOCK_CHANGE,
                pos
        );

        if (!player.getAbilities().instabuild) {
            heldStack.shrink(1);
        }
    }
}