package com.perigrine3.createcybernetics.item.cyberware.heart;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.EntityCyberwareData;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.ModMobAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Set;

public class StemCellsItem extends Item implements ICyberwareItem {
    private final int humanityCost;

    private static final String NBT_REGEN_NEXT_TICK = "cc_stemcells_nextTick";

    private static final int REGEN_TICKS = 20 * 30;
    private static final int REGEN_COOLDOWN_TICKS = 20 * 180;

    private static final int ENERGY_ON_TRIGGER = 5;
    private static final float TRIGGER_HEALTH = 5.0F;

    public StemCellsItem(Properties props, int humanityCost) {
        super(props);
        this.humanityCost = humanityCost;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.createcybernetics.humanity", humanityCost)
                    .withStyle(ChatFormatting.GOLD));

            tooltip.add(Component.translatable("tooltip.createcybernetics.heartupgrades_stemcell.energy")
                    .withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public int getHumanityCost() {
        return humanityCost;
    }

    @Override
    public Set<TagKey<Item>> requiresCyberwareTags(ItemStack installedStack, CyberwareSlot slot) {
        return Set.of(ModTags.Items.HEART_ITEMS);
    }

    @Override
    public Set<CyberwareSlot> getSupportedSlots() {
        return Set.of(CyberwareSlot.HEART);
    }

    @Override
    public boolean replacesOrgan() {
        return false;
    }

    @Override
    public Set<CyberwareSlot> getReplacedOrgans() {
        return Set.of();
    }

    @Override
    public boolean requiresEnergyToFunction(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot) {
        return true;
    }

    @Override
    public int getEnergyUsedPerTick(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot) {
        if (!shouldAttemptTrigger(entity)) return 0;
        return ENERGY_ON_TRIGGER;
    }

    @Override
    public void onTick(LivingEntity entity) {
    }

    @Override
    public void onTick(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot, int index) {
        if (entity.level().isClientSide) return;
        if (!entity.isAlive()) return;

        InstalledCyberware cw = getInstalledCyberware(entity, slot, index);
        if (cw == null) return;

        if (!shouldAttemptTrigger(entity)) return;

        if (!cw.isPowered()) return;

        CompoundTag tag = entity.getPersistentData();
        long now = entity.level().getGameTime();

        entity.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION,
                REGEN_TICKS,
                2,
                false,
                true,
                true
        ));

        tag.putLong(NBT_REGEN_NEXT_TICK, now + REGEN_COOLDOWN_TICKS);
    }

    private static boolean shouldAttemptTrigger(LivingEntity entity) {
        if (entity == null) return false;
        if (entity.level().isClientSide) return false;
        if (!entity.isAlive()) return false;

        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }

        if (entity.getHealth() > TRIGGER_HEALTH) return false;

        CompoundTag tag = entity.getPersistentData();
        long now = entity.level().getGameTime();

        long next = tag.getLong(NBT_REGEN_NEXT_TICK);
        return next == 0L || now >= next;
    }

    private static InstalledCyberware getInstalledCyberware(LivingEntity entity, CyberwareSlot slot, int index) {
        if (entity instanceof Player player) {
            if (!player.hasData(ModAttachments.CYBERWARE)) return null;

            PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
            if (data == null) return null;

            return data.get(slot, index);
        }

        if (!entity.hasData(ModMobAttachments.CYBERENTITY_CYBERWARE)) return null;

        EntityCyberwareData data = entity.getData(ModMobAttachments.CYBERENTITY_CYBERWARE);
        if (data == null) return null;

        return data.get(slot, index);
    }
}