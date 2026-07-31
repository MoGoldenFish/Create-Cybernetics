package com.perigrine3.createcybernetics.compat.jei;

import com.perigrine3.createcybernetics.api.CyberwareRepairType;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import net.minecraft.world.item.ItemStack;

public record CyberwareAnvilRepairJeiRecipe(ItemStack brokenCyberware, ItemStack repairMaterial, ItemStack repairedCyberware, CyberwareSlot slot, CyberwareRepairType repairType, int repairAmount, int maximumDurability) {
}