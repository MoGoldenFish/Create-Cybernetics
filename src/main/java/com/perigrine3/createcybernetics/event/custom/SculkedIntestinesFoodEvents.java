package com.perigrine3.createcybernetics.event.custom;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.sculked.SculkedIntestinesItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Map;

@EventBusSubscriber(modid = CreateCybernetics.MODID)
public final class SculkedIntestinesFoodEvents {
    private SculkedIntestinesFoodEvents() {}

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (!shouldBlockFood(player, stack)) {
            return;
        }

        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onStartUsingItem(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!shouldBlockFood(player, event.getItem())) {
            return;
        }

        event.setDuration(0);
        event.setCanceled(true);
    }

    private static boolean shouldBlockFood(Player player, ItemStack stack) {
        if (player == null) {
            return false;
        }

        if (stack == null || stack.isEmpty()) {
            return false;
        }

        if (stack.getFoodProperties(player) == null) {
            return false;
        }

        return hasSculkedIntestines(player);
    }

    private static boolean hasSculkedIntestines(Player player) {
        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);

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
                    return true;
                }
            }
        }

        return false;
    }
}