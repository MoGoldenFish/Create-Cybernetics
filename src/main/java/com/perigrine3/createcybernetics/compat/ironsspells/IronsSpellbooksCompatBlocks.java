package com.perigrine3.createcybernetics.compat.ironsspells;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.block.ironsspells.AnomalyCoreBlock;
import com.perigrine3.createcybernetics.block.ironsspells.HexcoreBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class IronsSpellbooksCompatBlocks {
    public static final String IRONS_SPELLBOOKS_MODID = "irons_spellbooks";

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CreateCybernetics.MODID);

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CreateCybernetics.MODID);

    public static final Supplier<Block> ANOMALY_CORE = BLOCKS.register(
            "anomaly_core",
            () -> new AnomalyCoreBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 8)
                    .noOcclusion()
                    .sound(SoundType.AMETHYST))
    );

    public static final Supplier<Block> HEXCORE = BLOCKS.register(
            "hexcore",
            () -> new HexcoreBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 8)
                    .noOcclusion()
                    .sound(SoundType.METAL))
    );

    public static final Supplier<Item> ANOMALY_CORE_ITEM = ITEMS.register(
            "anomaly_core",
            () -> new BlockItem(ANOMALY_CORE.get(), new Item.Properties())
    );

    public static final Supplier<Item> HEXCORE_ITEM = ITEMS.register(
            "hexcore",
            () -> new BlockItem(HEXCORE.get(), new Item.Properties())
    );

    private IronsSpellbooksCompatBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        if (!isLoaded()) {
            CreateCybernetics.LOGGER.info("[Iron's Spells compat] Skipping Anomaly Core registration because Iron's Spells is not installed.");
            return;
        }

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        IronsSpellbooksCompatBlockEntities.register(modEventBus);

        CreateCybernetics.LOGGER.info("[Iron's Spells compat] Registered Anomaly Core.");
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(IRONS_SPELLBOOKS_MODID);
    }
}