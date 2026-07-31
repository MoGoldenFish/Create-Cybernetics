package com.perigrine3.createcybernetics.item.cyberware.eyes;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.ModItems;
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

public class BiomonitorModuleItem extends Item implements ICyberwareItem {
    private final int humanityCost;
    private boolean nightVisionModule;
    private boolean underwaterVisionModule;

    public BiomonitorModuleItem(Properties props, int humanityCost) {
        super(props);
        this.humanityCost = humanityCost;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.createcybernetics.humanity", humanityCost).withStyle(ChatFormatting.GOLD));

            tooltip.add(Component.translatable("tooltip.createcybernetics.eyeupgrades_biomonitor.energy").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public int getHumanityCost() {
        return humanityCost;
    }

    @Override
    public Set<CyberwareSlot> getSupportedSlots() {
        return Set.of(CyberwareSlot.EYES);
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
    public boolean meetsCyberwareRequirements(PlayerCyberwareData data, ItemStack installedStack, CyberwareSlot slot) {
        boolean hasMonovision = data.hasSpecificItem(ModItems.EYEUPGRADES_MONOVISION.get(), CyberwareSlot.EYES);
        boolean hasCybereyes = data.hasSpecificItem(ModItems.BASECYBERWARE_CYBEREYES.get(), CyberwareSlot.EYES)
                || data.hasSpecificItem(ModItems.EYEUPGRADES_MULTIOPTICS1.get(), CyberwareSlot.EYES)
                || data.hasSpecificItem(ModItems.EYEUPGRADES_MULTIOPTICS2.get(), CyberwareSlot.EYES)
                || data.hasSpecificItem(ModItems.EYEUPGRADES_MULTIOPTICS3.get(), CyberwareSlot.EYES)
                || data.hasSpecificItem(ModItems.EYEUPGRADES_MULTIOPTICS4.get(), CyberwareSlot.EYES);
        boolean hasHudjack = data.hasSpecificItem(ModItems.EYEUPGRADES_HUDJACK.get(), CyberwareSlot.EYES);

        return hasMonovision || (hasCybereyes && hasHudjack);
    }

    @Override
    public int getEnergyUsedPerTick(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot) {
        return 4;
    }

    @Override
    public boolean requiresEnergyToFunction(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot) {
        return true;
    }

    @Override
    public void onInstalled(Player player) {

    }

    @Override
    public void onRemoved(Player player) {
        // remove strength modifier
    }

    @Override
    public void onTick(LivingEntity entity) {

    }
}