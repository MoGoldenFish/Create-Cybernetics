package com.perigrine3.createcybernetics.common.durability;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.CyberwareDurabilityCategory;
import com.perigrine3.createcybernetics.api.CyberwareRepairType;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class CyberwareAnvilRepairEvents {

    private CyberwareAnvilRepairEvents() {}

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack cyberwareStack = event.getLeft();
        ItemStack repairStack = event.getRight();

        if (cyberwareStack.isEmpty()) return;
        if (repairStack.isEmpty()) return;
        if (!(cyberwareStack.getItem() instanceof ICyberwareItem cyberwareItem)) return;
        if (!cyberwareItem.canRepairInAnvil(cyberwareStack)) return;
        if (!cyberwareItem.isAnvilRepairMaterial(cyberwareStack, repairStack)) return;

        CyberwareSlot slot = cyberwareItem.getDurabilityDataSlot(cyberwareStack);
        if (slot == null) return;

        CyberwareDurabilityCategory durabilityCategory = cyberwareItem.getDurabilityCategory(cyberwareStack, slot);
        if (durabilityCategory == CyberwareDurabilityCategory.DEFAULT_ORGAN || durabilityCategory == CyberwareDurabilityCategory.WETWARE) return;

        CyberwareRepairType repairType = cyberwareItem.getRepairType(cyberwareStack, slot);
        if (repairType == CyberwareRepairType.NONE || repairType == CyberwareRepairType.BIOLOGICAL) return;

        int maximumDurability = CyberwareDurabilityData.getMaxDurability(cyberwareStack, slot);
        int currentDurability = CyberwareDurabilityData.getDurability(cyberwareStack, slot);

        if (maximumDurability <= 0) return;
        if (currentDurability >= maximumDurability) return;

        int repairPerMaterial = cyberwareItem.getAnvilRepairAmount(cyberwareStack, repairStack);
        if (repairPerMaterial <= 0) return;

        int missingDurability = maximumDurability - currentDurability;
        int materialsNeeded = Mth.ceil((float) missingDurability / (float) repairPerMaterial);
        int materialsConsumed = Math.min(materialsNeeded, repairStack.getCount());
        int durabilityRestored = Math.min(missingDurability, repairPerMaterial * materialsConsumed);

        if (materialsConsumed <= 0) return;
        if (durabilityRestored <= 0) return;

        ItemStack output = cyberwareStack.copy();
        output.setCount(1);

        CyberwareDurabilityData.setDurability(output, slot, currentDurability + durabilityRestored);
        applyRequestedName(event, output);

        int levelCost = cyberwareItem.getAnvilRepairLevelCost(cyberwareStack, repairStack, materialsConsumed, durabilityRestored);

        event.setOutput(output);
        event.setMaterialCost(materialsConsumed);
        event.setCost(Math.max(1, levelCost));
    }

    private static void applyRequestedName(AnvilUpdateEvent event, ItemStack output) {
        String requestedName = event.getName();
        if (requestedName == null) return;

        if (requestedName.isBlank()) {
            output.remove(DataComponents.CUSTOM_NAME);
            return;
        }

        output.set(DataComponents.CUSTOM_NAME, Component.literal(requestedName));
    }
}