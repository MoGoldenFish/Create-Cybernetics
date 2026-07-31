package com.perigrine3.createcybernetics.item.generic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.UUID;

public class SharedNavigationShardItem extends DataShardItem {

    public SharedNavigationShardItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        UUID networkId = SharedNavigationShardData.getNetworkId(stack);

        if (networkId == null) {
            tooltip.add(Component.translatable("item.createcybernetics.data_shard_shared_navigation.uninitialized").withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltip.add(Component.translatable("item.createcybernetics.data_shard_shared_navigation.linked").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.createcybernetics.data_shard_shared_navigation.network", shortNetworkId(networkId)).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.createcybernetics.data_shard_shared_navigation.shared_map").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.createcybernetics.data_shard_shared_navigation.tracking").withStyle(ChatFormatting.GRAY));

        if (SharedNavigationShardData.getPendingMergeSource(stack) != null) {
            tooltip.add(Component.translatable("item.createcybernetics.data_shard_shared_navigation.merge_pending").withStyle(ChatFormatting.YELLOW));
        }
    }

    private static String shortNetworkId(UUID networkId) {
        String value = networkId.toString();
        return value.substring(0, 8);
    }
}