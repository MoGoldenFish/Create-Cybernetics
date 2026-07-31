package com.perigrine3.createcybernetics.common.durability;

import com.perigrine3.createcybernetics.ConfigValues;
import com.perigrine3.createcybernetics.api.CyberwareRepairType;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.util.ModTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CyberwareRepairManager {

    private CyberwareRepairManager() {}

    public static int getRepairAmount(InstalledCyberware installed, ItemStack repairMaterial) {
        if (installed == null) return 0;
        if (repairMaterial == null || repairMaterial.isEmpty()) return 0;

        ItemStack installedStack = installed.getItem();
        if (installedStack == null || installedStack.isEmpty()) return 0;

        if (!(installedStack.getItem() instanceof com.perigrine3.createcybernetics.api.ICyberwareItem cyberwareItem)) return 0;

        CyberwareRepairType repairType = cyberwareItem.getRepairType(installedStack, installed.getSlot());

        if (repairType == CyberwareRepairType.BIOLOGICAL && repairMaterial.is(Items.GHAST_TEAR)) {
            return installed.getMaxDurability();
        }

        if (repairType == CyberwareRepairType.CYBERLIMB) {
            if (repairMaterial.is(ModItems.TITANIUMSHEET)) return ConfigValues.TITANIUM_SHEET_REPAIR;
            if (repairMaterial.is(ModItems.TITANIUMINGOT)) return ConfigValues.TITANIUM_INGOT_REPAIR;
            if (repairMaterial.is(ModItems.COMPONENT_PLATING)) return ConfigValues.PLATING_COMPONENT_REPAIR;
        }

        if (repairType == CyberwareRepairType.CYBERNETIC && repairMaterial.is(ModItems.COMPONENT_PLATING)) {
            return ConfigValues.PLATING_COMPONENT_REPAIR;
        }

        if (repairType == CyberwareRepairType.BATTERY && repairMaterial.is(ModTags.Items.BATTERY_REPAIR_MATERIALS)) {
            return ConfigValues.BATTERY_REPAIR_AMOUNT;
        }

        return 0;
    }

    public static boolean canRepair(InstalledCyberware installed, ItemStack repairMaterial) {
        return installed != null && !installed.isAtMaxDurability() && getRepairAmount(installed, repairMaterial) > 0;
    }

    public static int repair(LivingEntity entity, PlayerCyberwareData data, InstalledCyberware installed, ItemStack repairMaterial) {
        int amount = getRepairAmount(installed, repairMaterial);
        if (amount <= 0) return 0;

        ItemStack installedStack = installed.getItem();
        if (!(installedStack.getItem() instanceof com.perigrine3.createcybernetics.api.ICyberwareItem cyberwareItem)) return 0;

        CyberwareRepairType repairType = cyberwareItem.getRepairType(installedStack, installed.getSlot());
        boolean clearFatigue = repairType == CyberwareRepairType.BIOLOGICAL && repairMaterial.is(Items.GHAST_TEAR);

        return CyberwareDurabilityManager.repairByType(entity, data, installed, repairType, amount, clearFatigue);
    }
}