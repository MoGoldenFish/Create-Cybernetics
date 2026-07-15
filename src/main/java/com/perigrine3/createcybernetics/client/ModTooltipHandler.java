package com.perigrine3.createcybernetics.client;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.block.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class ModTooltipHandler {

    private ModTooltipHandler() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (stack.getItem() == ModBlocks.CHARGING_BLOCK.asItem()) {
            event.getToolTip().add(Component.translatable("tooltip.createcybernetics.charging_block").withStyle(ChatFormatting.AQUA));
            event.getToolTip().add(Component.translatable("tooltip.createcybernetics.charging_block.energy").withStyle(ChatFormatting.DARK_RED));
        }
        if (stack.getItem() == ModBlocks.ROBOSURGEON.asItem()) {
            event.getToolTip().add(Component.translatable("tooltip.createcybernetics.robosurgeon").withStyle(ChatFormatting.AQUA));
            event.getToolTip().add(Component.translatable("tooltip.createcybernetics.robosurgeon.energy").withStyle(ChatFormatting.DARK_RED));
        }
        if (stack.getItem() == ModBlocks.SURGERY_TABLE.asItem()) {
            event.getToolTip().add(Component.translatable("tooltip.createcybernetics.surgery_table").withStyle(ChatFormatting.AQUA));
        }
        if (stack.getItem() == ModBlocks.ENGINEERING_TABLE.asItem()) {
            event.getToolTip().add(Component.translatable("tooltip.createcybernetics.engineering_table").withStyle(ChatFormatting.AQUA));
        }
        if (stack.getItem() == ModBlocks.GRAFTING_TABLE.asItem()) {
            event.getToolTip().add(Component.translatable("tooltip.createcybernetics.grafting_table").withStyle(ChatFormatting.AQUA));
        }

    }
}