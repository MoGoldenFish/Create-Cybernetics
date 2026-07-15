package com.perigrine3.createcybernetics.item.generic;

import com.perigrine3.createcybernetics.effect.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public class NeuropozyneAutoinjector extends BaseAutoinjectorItem {

    private static final int EFFECT_DURATION = 24000;
    private static final int EFFECT_AMPLIFIER = 0;

    public NeuropozyneAutoinjector(Properties properties) {
        super(properties);
    }

    @Override
    protected Optional<String> getDurationTranslationKey() {
        return Optional.of("item.createcybernetics.neuropozyne_autoinjector.duration");
    }

    @Override
    protected Optional<String> getDescriptionTranslationKey() {
        return Optional.of("item.createcybernetics.neuropozyne_autoinjector.desc");
    }

    @Override
    public List<MobEffectInstance> getSpinalInjectionEffects(ItemStack stack) {
        return List.of(new MobEffectInstance(ModEffects.NEUROPOZYNE, EFFECT_DURATION, EFFECT_AMPLIFIER));
    }
}