package com.perigrine3.createcybernetics.compat.ironsspells;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.block.ironsspells.AnomalyCoreBlockEntity;
import com.perigrine3.createcybernetics.block.ironsspells.HexcoreBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class IronsSpellbooksCompatBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateCybernetics.MODID);

    public static final Supplier<BlockEntityType<HexcoreBlockEntity>> HEXCORE =
            BLOCK_ENTITIES.register(
                    "hexcore",
                    () -> BlockEntityType.Builder.of(
                            HexcoreBlockEntity::new,
                            IronsSpellbooksCompatBlocks.HEXCORE.get()
                    ).build(null)
            );

    public static final Supplier<BlockEntityType<AnomalyCoreBlockEntity>> ANOMALY_CORE =
            BLOCK_ENTITIES.register(
                    "anomaly_core",
                    () -> BlockEntityType.Builder.of(
                            AnomalyCoreBlockEntity::new,
                            IronsSpellbooksCompatBlocks.ANOMALY_CORE.get()
                    ).build(null)
            );

    private IronsSpellbooksCompatBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}