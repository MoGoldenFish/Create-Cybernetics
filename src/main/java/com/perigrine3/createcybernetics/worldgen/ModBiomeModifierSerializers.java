package com.perigrine3.createcybernetics.worldgen;

import com.mojang.serialization.MapCodec;
import com.perigrine3.createcybernetics.CreateCybernetics;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModBiomeModifierSerializers {
    private ModBiomeModifierSerializers() {
    }

    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, CreateCybernetics.MODID);

    public static final Supplier<MapCodec<? extends BiomeModifier>> CONFIGURABLE_ENTITY_SPAWN =
            BIOME_MODIFIER_SERIALIZERS.register(
                    "configurable_entity_spawn",
                    () -> ConfigurableEntitySpawnBiomeModifier.CODEC
            );

    public static void register(IEventBus modEventBus) {
        BIOME_MODIFIER_SERIALIZERS.register(modEventBus);
    }
}