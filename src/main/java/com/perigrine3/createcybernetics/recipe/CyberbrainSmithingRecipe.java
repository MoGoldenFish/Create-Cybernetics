package com.perigrine3.createcybernetics.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.perigrine3.createcybernetics.item.generic.BiochipDataShardItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;

public final class CyberbrainSmithingRecipe extends SmithingTransformRecipe {

    private final Ingredient templateIngredient;
    private final Ingredient baseIngredient;
    private final Ingredient additionIngredient;
    private final ItemStack resultStack;

    public CyberbrainSmithingRecipe(
            Ingredient template,
            Ingredient base,
            Ingredient addition,
            ItemStack result
    ) {
        super(template, base, addition, result);

        this.templateIngredient = template;
        this.baseIngredient = base;
        this.additionIngredient = addition;
        this.resultStack = result;
    }

    public Ingredient getTemplateIngredient() {
        return templateIngredient;
    }

    public Ingredient getBaseIngredient() {
        return baseIngredient;
    }

    public Ingredient getAdditionIngredient() {
        return additionIngredient;
    }

    public ItemStack getResultStack() {
        return resultStack;
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        ItemStack result = resultStack.copy();

        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack biochip = input.base();

        if (biochip.isEmpty()) {
            return result;
        }

        CompoundTag biochipTag = BiochipDataShardItem.getTagOrNull(biochip);

        if (biochipTag == null || biochipTag.isEmpty()) {
            return result;
        }

        CompoundTag copiedTag = biochipTag.copy();

        result.set(
                DataComponents.CUSTOM_DATA,
                CustomData.of(copiedTag)
        );

        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.CYBERBRAIN_SMITHING.get();
    }

    public static final class Serializer implements RecipeSerializer<CyberbrainSmithingRecipe> {

        public static final MapCodec<CyberbrainSmithingRecipe> CODEC =
                RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Ingredient.CODEC_NONEMPTY
                                .fieldOf("template")
                                .forGetter(CyberbrainSmithingRecipe::getTemplateIngredient),

                        Ingredient.CODEC_NONEMPTY
                                .fieldOf("base")
                                .forGetter(CyberbrainSmithingRecipe::getBaseIngredient),

                        Ingredient.CODEC_NONEMPTY
                                .fieldOf("addition")
                                .forGetter(CyberbrainSmithingRecipe::getAdditionIngredient),

                        ItemStack.CODEC
                                .fieldOf("result")
                                .forGetter(CyberbrainSmithingRecipe::getResultStack)
                ).apply(instance, CyberbrainSmithingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, CyberbrainSmithingRecipe> STREAM_CODEC = StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC,
                        CyberbrainSmithingRecipe::getTemplateIngredient,

                        Ingredient.CONTENTS_STREAM_CODEC,
                        CyberbrainSmithingRecipe::getBaseIngredient,

                        Ingredient.CONTENTS_STREAM_CODEC,
                        CyberbrainSmithingRecipe::getAdditionIngredient,

                        ItemStack.STREAM_CODEC,
                        CyberbrainSmithingRecipe::getResultStack,

                        CyberbrainSmithingRecipe::new
                );

        @Override
        public MapCodec<CyberbrainSmithingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CyberbrainSmithingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}