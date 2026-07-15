package com.perigrine3.createcybernetics.item.food;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class BrainStewItem extends Item {

    public BrainStewItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        ItemStack result = super.finishUsingItem(stack, level, livingEntity);
        ItemStack bowl = new ItemStack(Items.BOWL);

        if (result.isEmpty()) {
            return bowl;
        }

        if (!level.isClientSide && livingEntity instanceof ServerPlayer player) {
            if (!player.getInventory().add(bowl)) {
                player.drop(bowl, false);
            }
        }

        return result;
    }
}