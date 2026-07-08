package com.perigrine3.createcybernetics.event.custom;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.effect.ModEffects;
import com.perigrine3.createcybernetics.item.sculked.SculkedIntestinesItem;
import com.perigrine3.createcybernetics.item.sculked.SculkedMuscleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;

@EventBusSubscriber(modid = CreateCybernetics.MODID)
public final class SculkedCyberwareTickEvents {
    private static final int SCULKED_EFFECT_CHECK_INTERVAL_TICKS = 24000;
    private static final float SCULKED_EFFECT_CHANCE = 0.25f;

    private static final int XP_CHECK_INTERVAL_TICKS = 20;
    private static final int XP_PER_FOOD_POINT = 3;
    private static final float SATURATION_PER_FOOD_POINT = 0.15f;

    private static final int MUSCLE_EFFECT_CHECK_INTERVAL_TICKS = 20;
    private static final int MUSCLE_EFFECT_DURATION_TICKS = 60;

    private static final int STRENGTH_AMPLIFIER = 0;
    private static final int WEAKNESS_AMPLIFIER = 0;

    private SculkedCyberwareTickEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (player.level().isClientSide) {
            return;
        }

        InstalledSculkedCyberware installed = getInstalledSculkedCyberware(player);

        if (!installed.hasAnySculkedCyberware()) {
            return;
        }

        tickSculkedEffect(player);

        if (installed.hasIntestines()) {
            tickExperienceFeeding(player);
        }

        if (installed.hasMuscle()) {
            tickSculkMuscleEffects(player);
        }
    }

    private static InstalledSculkedCyberware getInstalledSculkedCyberware(Player player) {
        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);

        boolean hasIntestines = false;
        boolean hasMuscle = false;

        for (Map.Entry<CyberwareSlot, InstalledCyberware[]> entry : data.getAll().entrySet()) {
            InstalledCyberware[] installedCyberware = entry.getValue();

            if (installedCyberware == null) {
                continue;
            }

            for (InstalledCyberware installed : installedCyberware) {
                if (installed == null) {
                    continue;
                }

                ItemStack stack = installed.getItem();

                if (stack == null || stack.isEmpty()) {
                    continue;
                }

                if (stack.getItem() instanceof SculkedIntestinesItem) {
                    hasIntestines = true;
                }

                if (stack.getItem() instanceof SculkedMuscleItem) {
                    hasMuscle = true;
                }

                if (hasIntestines && hasMuscle) {
                    return new InstalledSculkedCyberware(true, true);
                }
            }
        }

        return new InstalledSculkedCyberware(hasIntestines, hasMuscle);
    }

    private static void tickSculkedEffect(Player player) {
        if (player.hasEffect(ModEffects.SCULKED_EFFECT)) {
            return;
        }

        if ((player.tickCount % SCULKED_EFFECT_CHECK_INTERVAL_TICKS) != 0) {
            return;
        }

        if (player.getRandom().nextFloat() < SCULKED_EFFECT_CHANCE) {
            player.addEffect(new MobEffectInstance(ModEffects.SCULKED_EFFECT, Integer.MAX_VALUE, 0, false, false, true));
        }
    }

    private static void tickExperienceFeeding(Player player) {
        if ((player.tickCount % XP_CHECK_INTERVAL_TICKS) != 0) {
            return;
        }

        FoodData foodData = player.getFoodData();

        if (foodData.getFoodLevel() >= 20) {
            return;
        }

        if (player.totalExperience < XP_PER_FOOD_POINT) {
            return;
        }

        int beforeFood = foodData.getFoodLevel();

        player.giveExperiencePoints(-XP_PER_FOOD_POINT);
        foodData.eat(1, SATURATION_PER_FOOD_POINT);

        if (beforeFood < 20 && foodData.getFoodLevel() >= 20) {
            applyGoldenAppleEffects(player);
        }
    }

    private static void applyGoldenAppleEffects(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 2, false, false, true));
    }

    private static void tickSculkMuscleEffects(Player player) {
        if ((player.tickCount % MUSCLE_EFFECT_CHECK_INTERVAL_TICKS) != 0) {
            return;
        }

        if (isStandingOnSculk(player)) {
            player.removeEffect(MobEffects.WEAKNESS);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, MUSCLE_EFFECT_DURATION_TICKS, STRENGTH_AMPLIFIER, false, false, true));
        } else {
            player.removeEffect(MobEffects.DAMAGE_BOOST);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, MUSCLE_EFFECT_DURATION_TICKS, WEAKNESS_AMPLIFIER, false, false, true));
        }
    }

    private static boolean isStandingOnSculk(Player player) {
        BlockPos below = player.blockPosition().below();
        BlockState state = player.level().getBlockState(below);

        return state.is(Blocks.SCULK);
    }

    private record InstalledSculkedCyberware(boolean hasIntestines, boolean hasMuscle) {
        private boolean hasAnySculkedCyberware() {
            return hasIntestines || hasMuscle;
        }
    }
}