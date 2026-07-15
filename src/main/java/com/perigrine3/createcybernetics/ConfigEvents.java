package com.perigrine3.createcybernetics;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ConfigEvents {
    private ConfigEvents() {}

    @SubscribeEvent
    public static void onConfigLoading(ModConfigEvent.Loading e) {
        bake(e.getConfig());
    }

    @SubscribeEvent
    public static void onConfigReloading(ModConfigEvent.Reloading e) {
        bake(e.getConfig());
    }

    private static void bake(ModConfig config) {
        if (config.getSpec() != Config.SPEC) return;

        ConfigValues.BASE_HUMANITY = Config.HUMANITY.get();
        ConfigValues.KEEP_CYBERWARE = Config.KEEP_CYBERWARE.get();
        ConfigValues.SURGERY_DAMAGE_SCALING = Config.SURGERY_DAMAGE_SCALING.get();
        ConfigValues.EPILEPSY_MODE = Config.EPILEPSY_MODE.get();
        ConfigValues.TATTOO_UPLOAD_MODE = Config.TATTOO_UPLOAD_MODE.get();

        ConfigValues.CYBERZOMBIE_SPAWN_WEIGHT = Config.CYBERZOMBIE_SPAWN_WEIGHT.get();
        ConfigValues.CYBERZOMBIE_MIN_GROUP = Config.CYBERZOMBIE_MIN_GROUP.get();
        ConfigValues.CYBERZOMBIE_MAX_GROUP = Math.max(
                Config.CYBERZOMBIE_MIN_GROUP.get(),
                Config.CYBERZOMBIE_MAX_GROUP.get()
        );

        ConfigValues.CYBERSKELETON_SPAWN_WEIGHT = Config.CYBERSKELETON_SPAWN_WEIGHT.get();
        ConfigValues.CYBERSKELETON_MIN_GROUP = Config.CYBERSKELETON_MIN_GROUP.get();
        ConfigValues.CYBERSKELETON_MAX_GROUP = Math.max(
                Config.CYBERSKELETON_MIN_GROUP.get(),
                Config.CYBERSKELETON_MAX_GROUP.get()
        );

        ConfigValues.HOGBOY_SPAWN_WEIGHT = Config.HOGBOY_SPAWN_WEIGHT.get();
        ConfigValues.HOGBOY_MIN_GROUP = Config.HOGBOY_MIN_GROUP.get();
        ConfigValues.HOGBOY_MAX_GROUP = Math.max(
                Config.HOGBOY_MIN_GROUP.get(),
                Config.HOGBOY_MAX_GROUP.get()
        );

        ConfigValues.PUNKLIN_SPAWN_WEIGHT = Config.PUNKLIN_SPAWN_WEIGHT.get();
        ConfigValues.PUNKLIN_MIN_GROUP = Config.PUNKLIN_MIN_GROUP.get();
        ConfigValues.PUNKLIN_MAX_GROUP = Math.max(
                Config.PUNKLIN_MIN_GROUP.get(),
                Config.PUNKLIN_MAX_GROUP.get()
        );

        ConfigValues.PIGSTROM_SPAWN_WEIGHT = Config.PIGSTROM_SPAWN_WEIGHT.get();
        ConfigValues.PIGSTROM_MIN_GROUP = Config.PIGSTROM_MIN_GROUP.get();
        ConfigValues.PIGSTROM_MAX_GROUP = Math.max(
                Config.PIGSTROM_MIN_GROUP.get(),
                Config.PIGSTROM_MAX_GROUP.get()
        );
    }
}