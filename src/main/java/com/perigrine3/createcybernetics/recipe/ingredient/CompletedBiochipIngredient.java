package com.perigrine3.createcybernetics.recipe.ingredient;

import com.mojang.serialization.MapCodec;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.item.generic.BiochipDataShardItem;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.stream.Stream;

public final class CompletedBiochipIngredient implements ICustomIngredient {
    public static final MapCodec<CompletedBiochipIngredient> CODEC =
            MapCodec.unit(new CompletedBiochipIngredient());

    @Override
    public boolean test(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(ModItems.DATA_SHARD_BIOCHIP.get())) {
            return false;
        }

        CompoundTag tag = BiochipDataShardItem.getTagOrNull(stack);
        if (tag == null) {
            return false;
        }

        if (!tag.getBoolean(BiochipDataShardItem.TAG_DONE)) {
            return false;
        }

        String ownerName = tag.getString(BiochipDataShardItem.TAG_OWNER_NAME);
        String ownerUuid = tag.getString(BiochipDataShardItem.TAG_OWNER_UUID);

        return !ownerName.isBlank() && !ownerUuid.isBlank();
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public Stream<ItemStack> getItems() {
        return Stream.of(new ItemStack(ModItems.DATA_SHARD_BIOCHIP.get()));
    }

    @Override
    public IngredientType<?> getType() {
        return ModIngredientTypes.COMPLETED_BIOCHIP.get();
    }
}