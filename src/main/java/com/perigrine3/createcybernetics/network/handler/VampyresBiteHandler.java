package com.perigrine3.createcybernetics.network.handler;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.ISpinalInjectableItem;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.item.cyberware.lungs.VampyresItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class VampyresBiteHandler {
    private static final int CHARGE_TICKS_REQUIRED = 10;
    private static final double MAX_BITE_DISTANCE = 3.5D;

    private static final Map<UUID, BiteState> BITE_STATES = new HashMap<>();

    private VampyresBiteHandler() {
    }

    public static void setHeld(ServerPlayer player, boolean held, int targetEntityId) {
        BiteState state = BITE_STATES.computeIfAbsent(player.getUUID(), ignored -> new BiteState());

        if (!held) {
            state.held = false;
            state.targetEntityId = -1;
            state.chargeTicks = 0;
            state.mustRelease = false;
            return;
        }

        if (state.mustRelease) return;

        if (state.targetEntityId != targetEntityId) {
            state.chargeTicks = 0;
        }

        state.held = true;
        state.targetEntityId = targetEntityId;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        BiteState state = BITE_STATES.get(player.getUUID());
        if (state == null || !state.held || state.mustRelease) return;

        if (!VampyresItem.isEnabled(player)) {
            resetState(state);
            return;
        }

        if (!VampyresItem.isPowered(player)) {
            state.chargeTicks = 0;
            return;
        }

        LivingEntity target = getValidTarget(player, state.targetEntityId);

        if (target == null) {
            state.chargeTicks = 0;
            return;
        }

        state.chargeTicks++;

        if (state.chargeTicks < CHARGE_TICKS_REQUIRED) return;

        performBite(player, target);

        state.chargeTicks = 0;
        state.mustRelease = true;
    }

    private static LivingEntity getValidTarget(ServerPlayer player, int targetEntityId) {
        Entity entity = player.level().getEntity(targetEntityId);
        if (!(entity instanceof LivingEntity target)) return null;

        if (target == player) return null;
        if (!target.isAlive()) return null;
        if (player.distanceToSqr(target) > MAX_BITE_DISTANCE * MAX_BITE_DISTANCE) return null;
        if (!player.hasLineOfSight(target)) return null;

        return target;
    }

    private static void performBite(ServerPlayer player, LivingEntity target) {
        if (!player.hasData(ModAttachments.CYBERWARE)) return;

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
        if (data == null) return;

        boolean hasLoadedInjection = hasAnyLoadedInjection(data);

        if (hasLoadedInjection && !data.tryConsumeEnergy(VampyresItem.INJECTION_ENERGY_COST)) {
            player.displayClientMessage(Component.translatable("message.createcybernetics.vampyres.insufficient_energy").withStyle(ChatFormatting.RED), true);
            return;
        }

        float damage = 2.0F + player.getRandom().nextFloat() * 4.0F;
        target.hurt(player.damageSources().playerAttack(player), damage);

        if (!hasLoadedInjection) {
            return;
        }

        for (int slot = 0; slot < PlayerCyberwareData.VAMPYRES_SLOT_COUNT; slot++) {
            ItemStack stored = data.getVampyresStack(slot);
            if (stored.isEmpty()) continue;
            if (!VampyresItem.isInjectable(stored)) continue;

            ItemStack carrier = stored.copy();
            carrier.setCount(1);

            if (!applyInjectionContents(player, target, carrier)) {
                continue;
            }

            data.removeVampyresStack(slot, 1);
            giveReturnedContainer(player, carrier);
        }

        data.setDirty();
        player.syncData(ModAttachments.CYBERWARE);
    }

    private static boolean hasAnyLoadedInjection(PlayerCyberwareData data) {
        for (int slot = 0; slot < PlayerCyberwareData.VAMPYRES_SLOT_COUNT; slot++) {
            ItemStack stack = data.getVampyresStack(slot);

            if (!stack.isEmpty() && VampyresItem.isInjectable(stack) && hasInjectionContents(stack)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasInjectionContents(ItemStack carrier) {
        if (carrier.getItem() instanceof ISpinalInjectableItem injectable) {
            List<MobEffectInstance> effects = injectable.getSpinalInjectionEffects(carrier);
            return effects != null && !effects.isEmpty();
        }

        PotionContents contents = carrier.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return contents != null && contents.hasEffects();
    }

    private static boolean applyInjectionContents(ServerPlayer attacker, LivingEntity target, ItemStack carrier) {
        if (carrier.getItem() instanceof ISpinalInjectableItem injectable) {
            List<MobEffectInstance> effects = injectable.getSpinalInjectionEffects(carrier);

            if (effects == null || effects.isEmpty()) {
                return false;
            }

            for (MobEffectInstance instance : effects) {
                applyEffect(attacker, target, instance);
            }

            return true;
        }

        PotionContents contents = carrier.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (contents == null || !contents.hasEffects()) return false;

        contents.forEachEffect(instance -> applyEffect(attacker, target, instance));
        return true;
    }

    private static void applyEffect(ServerPlayer attacker, LivingEntity target, MobEffectInstance instance) {
        if (instance == null) return;

        Holder<MobEffect> effectHolder = instance.getEffect();
        MobEffect effect = effectHolder.value();

        if (effect.isInstantenous()) {
            effect.applyInstantenousEffect(attacker, attacker, target, instance.getAmplifier(), 1.0D);
            return;
        }

        target.addEffect(new MobEffectInstance(effectHolder, instance.getDuration(), instance.getAmplifier(), instance.isAmbient(), instance.isVisible(), instance.showIcon()), attacker);
    }

    private static void giveReturnedContainer(ServerPlayer player, ItemStack carrier) {
        Item remainderItem = carrier.getItem().getCraftingRemainingItem();

        ItemStack returned;

        if (remainderItem != null) {
            returned = new ItemStack(remainderItem);
        } else if (carrier.getItem() instanceof PotionItem) {
            returned = new ItemStack(Items.GLASS_BOTTLE);
        } else {
            returned = new ItemStack(ModItems.EMPTY_AUTOINJECTOR.get());
        }

        if (!player.getInventory().add(returned)) {
            player.drop(returned, false);
        }
    }

    private static void resetState(BiteState state) {
        state.held = false;
        state.targetEntityId = -1;
        state.chargeTicks = 0;
        state.mustRelease = false;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        BITE_STATES.remove(event.getEntity().getUUID());
    }

    private static final class BiteState {
        private boolean held;
        private int targetEntityId = -1;
        private int chargeTicks;
        private boolean mustRelease;
    }
}