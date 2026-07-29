package com.perigrine3.createcybernetics.compat.vampirism;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class UltraViolentFlashLightManager {
    private static final List<TemporaryLight> ACTIVE_LIGHTS = new ArrayList<>();

    private UltraViolentFlashLightManager() {}

    public static void createFlash(ServerLevel level, BlockPos playerPos, int durationTicks) {
        BlockPos lightPos = findLightPosition(level, playerPos);
        if (lightPos == null) return;

        BlockState previousState = level.getBlockState(lightPos);
        BlockState lightState = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);

        level.setBlock(lightPos, lightState, 3);
        level.getChunkSource().getLightEngine().checkBlock(lightPos);
        level.sendBlockUpdated(lightPos, previousState, lightState, 3);

        ACTIVE_LIGHTS.removeIf(activeLight -> {
            if (activeLight.level != level) return false;
            if (!activeLight.pos.equals(lightPos)) return false;

            activeLight.removalGameTime = level.getGameTime() + durationTicks;
            return true;
        });

        ACTIVE_LIGHTS.add(new TemporaryLight(level, lightPos, previousState, level.getGameTime() + durationTicks));
    }

    private static BlockPos findLightPosition(ServerLevel level, BlockPos playerPos) {
        BlockPos[] positions = {
                playerPos.above(),
                playerPos,
                playerPos.above(2)
        };

        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);

            if (state.isAir() || state.is(Blocks.LIGHT)) {
                return pos.immutable();
            }
        }

        return null;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Iterator<TemporaryLight> iterator = ACTIVE_LIGHTS.iterator();

        while (iterator.hasNext()) {
            TemporaryLight activeLight = iterator.next();

            if (activeLight.level.getGameTime() < activeLight.removalGameTime) continue;

            restorePreviousState(activeLight);
            iterator.remove();
        }
    }

    private static void restorePreviousState(TemporaryLight activeLight) {
        if (!activeLight.level.isLoaded(activeLight.pos)) return;

        BlockState currentState = activeLight.level.getBlockState(activeLight.pos);
        if (!currentState.is(Blocks.LIGHT)) return;

        activeLight.level.setBlock(activeLight.pos, activeLight.previousState, 3);
        activeLight.level.getChunkSource().getLightEngine().checkBlock(activeLight.pos);
        activeLight.level.sendBlockUpdated(activeLight.pos, currentState, activeLight.previousState, 3);
    }

    private static final class TemporaryLight {
        private final ServerLevel level;
        private final BlockPos pos;
        private final BlockState previousState;
        private long removalGameTime;

        private TemporaryLight(ServerLevel level, BlockPos pos, BlockState previousState, long removalGameTime) {
            this.level = level;
            this.pos = pos;
            this.previousState = previousState;
            this.removalGameTime = removalGameTime;
        }
    }
}