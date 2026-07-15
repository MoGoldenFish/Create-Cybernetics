package com.perigrine3.createcybernetics.event.custom;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.item.generic.BaseAutoinjectorItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID)
public final class AutoinjectorInteractionEvents {

    private AutoinjectorInteractionEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (!shouldForceAutoinjectorUse(player, stack, event.getTarget())) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);

        player.startUsingItem(event.getHand());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (!shouldForceAutoinjectorUse(player, stack, event.getTarget())) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);

        player.startUsingItem(event.getHand());
    }

    private static boolean shouldForceAutoinjectorUse(Player player, ItemStack stack, Object target) {
        return player.isShiftKeyDown()
                && stack.getItem() instanceof BaseAutoinjectorItem
                && target instanceof LivingEntity livingTarget
                && livingTarget.isAlive();
    }
}