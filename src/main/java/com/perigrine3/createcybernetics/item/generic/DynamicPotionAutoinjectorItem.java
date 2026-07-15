package com.perigrine3.createcybernetics.item.generic;

import com.perigrine3.createcybernetics.component.ModDataComponents;
import com.perigrine3.createcybernetics.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class DynamicPotionAutoinjectorItem extends BaseAutoinjectorItem {

    public DynamicPotionAutoinjectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public List<MobEffectInstance> getSpinalInjectionEffects(ItemStack stack) {
        PotionContents contents = getStoredPotionContents(stack);

        if (!hasUsablePotionContents(contents)) {
            return List.of();
        }

        List<MobEffectInstance> effects = new ArrayList<>();

        for (MobEffectInstance instance : contents.getAllEffects()) {
            effects.add(copyAndAmplifyEffect(instance));
        }

        return effects;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        List<MobEffectInstance> effects = getSpinalInjectionEffects(stack);

        PotionContents.addPotionTooltip(
                effects,
                tooltipComponents::add,
                1.0F,
                context.tickRate()
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!isValidDynamicAutoinjector(stack)) {
            ItemStack empty = convertStackToEmptyAutoinjectors(stack);

            if (!level.isClientSide) {
                player.setItemInHand(hand, empty);
            }

            return InteractionResultHolder.fail(empty);
        }

        return super.use(level, player, hand);
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int timeLeft) {
        if (!(living instanceof Player player)) {
            return;
        }

        if (!isValidDynamicAutoinjector(stack)) {
            player.stopUsingItem();

            if (!level.isClientSide) {
                InteractionHand usedHand = player.getUsedItemHand();
                player.setItemInHand(usedHand, convertStackToEmptyAutoinjectors(stack));
            }

            return;
        }

        super.onUseTick(level, living, stack, timeLeft);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide) {
            return;
        }

        if (!(level instanceof ServerLevel)) {
            return;
        }

        if (!(entity instanceof Player player)) {
            return;
        }

        if (isValidDynamicAutoinjector(stack)) {
            return;
        }

        if (slotId < 0 || slotId >= player.getInventory().getContainerSize()) {
            return;
        }

        player.getInventory().setItem(slotId, convertStackToEmptyAutoinjectors(stack));
    }

    @Override
    protected boolean shouldManualInject(Player user, LivingEntity target, ItemStack stack) {
        return isValidDynamicAutoinjector(stack);
    }

    public static int getAutoinjectorColor(ItemStack stack) {
        PotionContents contents = getStoredPotionContents(stack);

        if (!hasUsablePotionContents(contents)) {
            return 0xFFFFFF;
        }

        List<MobEffectInstance> amplifiedEffects = new ArrayList<>();

        for (MobEffectInstance instance : contents.getAllEffects()) {
            amplifiedEffects.add(copyAndAmplifyEffect(instance));
        }

        if (amplifiedEffects.isEmpty()) {
            return 0xFFFFFF;
        }

        return PotionContents.getColor(amplifiedEffects);
    }

    public static boolean isValidDynamicAutoinjector(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        PotionContents contents = getStoredPotionContents(stack);
        return hasUsablePotionContents(contents);
    }

    private static PotionContents getStoredPotionContents(ItemStack stack) {
        return stack.getOrDefault(
                ModDataComponents.POTION_AUTOINJECTOR_CONTENTS.get(),
                PotionContents.EMPTY
        );
    }

    private static boolean hasUsablePotionContents(PotionContents contents) {
        if (contents == PotionContents.EMPTY) {
            return false;
        }

        for (MobEffectInstance ignored : contents.getAllEffects()) {
            return true;
        }

        return false;
    }

    private static ItemStack convertStackToEmptyAutoinjectors(ItemStack stack) {
        int count = Math.max(1, stack.getCount());
        return new ItemStack(ModItems.EMPTY_AUTOINJECTOR.get(), count);
    }

    private static MobEffectInstance copyAndAmplifyEffect(MobEffectInstance instance) {
        return new MobEffectInstance(
                instance.getEffect(),
                instance.getDuration(),
                instance.getAmplifier() + 1,
                instance.isAmbient(),
                instance.isVisible(),
                instance.showIcon()
        );
    }
}