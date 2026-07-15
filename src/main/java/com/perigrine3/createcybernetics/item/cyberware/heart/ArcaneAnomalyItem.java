package com.perigrine3.createcybernetics.item.cyberware.heart;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.compat.ironsspells.IronsSpellbooksManaCompat;
import com.perigrine3.createcybernetics.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Set;

public class ArcaneAnomalyItem extends Item implements ICyberwareItem {
    private final int humanityCost;

    public ArcaneAnomalyItem(Properties props, int humanityCost) {
        super(props);
        this.humanityCost = humanityCost;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.createcybernetics.humanity", humanityCost).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.createcybernetics.heartupgrades_anomaly.energy").withStyle(ChatFormatting.DARK_GREEN));
        }
    }

    @Override
    public boolean surgeryInstallable() {
        return false;
    }

    @Override
    public int getHumanityCost() {
        return humanityCost;
    }

    @Override
    public Set<CyberwareSlot> getSupportedSlots() {
        return Set.of(CyberwareSlot.HEART);
    }

    @Override
    public boolean replacesOrgan() {
        return true;
    }

    @Override
    public Set<CyberwareSlot> getReplacedOrgans() {
        return Set.of(CyberwareSlot.HEART);
    }

    @Override
    public Set<TagKey<Item>> incompatibleCyberwareTags(ItemStack installedStack, CyberwareSlot slot) {
        return Set.of(ModTags.Items.HEART_ITEMS);
    }

    @Override
    public int getEnergyGeneratedPerTick(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot) {
        return 75;
    }

    @Override
    public int consumeGeneratedEnergySurplus(Player player, ItemStack installedStack, CyberwareSlot slot, int availableSurplus) {
        if (player == null || availableSurplus <= 0) return 0;
        if (!IronsSpellbooksManaCompat.isLoaded()) return 0;

        float mana = IronsSpellbooksManaCompat.getMana(player);
        float maxMana = IronsSpellbooksManaCompat.getMaxMana(player);

        if (maxMana <= 0.0F) return 0;
        if (mana >= maxMana) return 0;

        int missingMana = Math.max(0, (int) Math.ceil(maxMana - mana));
        int attemptedConversion = Math.min(availableSurplus, missingMana);

        return IronsSpellbooksManaCompat.addMana(player, attemptedConversion);
    }

    @Override
    public void onInstalled(LivingEntity entity) {
    }

    @Override
    public void onRemoved(LivingEntity entity) {
    }

    @Override
    public void onTick(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot, int index) {
    }

    @Override
    public void onTick(LivingEntity entity) {
    }
}