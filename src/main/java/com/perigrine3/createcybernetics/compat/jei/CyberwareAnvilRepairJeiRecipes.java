package com.perigrine3.createcybernetics.compat.jei;

import com.perigrine3.createcybernetics.api.CyberwareDurabilityCategory;
import com.perigrine3.createcybernetics.api.CyberwareRepairType;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.common.durability.CyberwareDurabilityData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class CyberwareAnvilRepairJeiRecipes {

    private CyberwareAnvilRepairJeiRecipes() {}

    public static List<CyberwareAnvilRepairJeiRecipe> createRecipes() {
        List<CyberwareAnvilRepairJeiRecipe> recipes = new ArrayList<>();

        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof ICyberwareItem cyberwareItem)) continue;

            ItemStack cyberwareStack = new ItemStack(item);
            CyberwareSlot slot = cyberwareItem.getDurabilityDataSlot(cyberwareStack);

            if (slot == null) continue;
            if (!cyberwareItem.canRepairInAnvil(cyberwareStack)) continue;

            CyberwareDurabilityCategory durabilityCategory = cyberwareItem.getDurabilityCategory(cyberwareStack, slot);
            if (durabilityCategory == CyberwareDurabilityCategory.DEFAULT_ORGAN || durabilityCategory == CyberwareDurabilityCategory.WETWARE) continue;

            CyberwareRepairType repairType = cyberwareItem.getRepairType(cyberwareStack, slot);
            if (repairType == CyberwareRepairType.NONE || repairType == CyberwareRepairType.BIOLOGICAL) continue;

            int maximumDurability = CyberwareDurabilityData.getMaxDurability(cyberwareStack, slot);
            if (maximumDurability <= 0) continue;

            Map<Item, Integer> repairMaterials = cyberwareItem.getAnvilRepairMaterials(cyberwareStack);
            if (repairMaterials == null || repairMaterials.isEmpty()) continue;

            for (Map.Entry<Item, Integer> entry : repairMaterials.entrySet()) {
                Item repairItem = entry.getKey();
                int repairAmount = Math.max(0, entry.getValue());

                if (repairItem == null) continue;
                if (repairAmount <= 0) continue;

                ItemStack brokenCyberware = cyberwareStack.copy();
                CyberwareDurabilityData.setDurability(brokenCyberware, slot, 0);

                ItemStack repairMaterial = new ItemStack(repairItem);

                ItemStack repairedCyberware = cyberwareStack.copy();
                CyberwareDurabilityData.setDurability(repairedCyberware, slot, Math.min(maximumDurability, repairAmount));

                recipes.add(new CyberwareAnvilRepairJeiRecipe(brokenCyberware, repairMaterial, repairedCyberware, slot, repairType, repairAmount, maximumDurability));
            }
        }

        recipes.sort(Comparator
                .comparing((CyberwareAnvilRepairJeiRecipe recipe) -> BuiltInRegistries.ITEM.getKey(recipe.brokenCyberware().getItem()).toString())
                .thenComparing(recipe -> BuiltInRegistries.ITEM.getKey(recipe.repairMaterial().getItem()).toString()));

        return recipes;
    }
}