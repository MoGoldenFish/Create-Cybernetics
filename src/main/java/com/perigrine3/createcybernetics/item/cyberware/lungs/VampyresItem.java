package com.perigrine3.createcybernetics.item.cyberware.lungs;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.Tags;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class VampyresItem extends Item implements ICyberwareItem {
    public static final int SLOT_COUNT = 2;
    public static final int SLOT_STACK_LIMIT = 4;

    public static final int DORMANT_ENERGY_COST = 10;
    public static final int INJECTION_ENERGY_COST = 75;

    private final int humanityCost;

    public VampyresItem(Properties properties, int humanityCost) {
        super(properties);
        this.humanityCost = humanityCost;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.createcybernetics.humanity", humanityCost).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.createcybernetics.lungsupgrades_vampyres.energy").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public Map<Item, Integer> getAdditionalAnvilRepairMaterials(ItemStack cyberwareStack) {
        return Map.of(
                Items.GLASS_BOTTLE, 150,
                ModItems.TITANIUMINGOT.get(), 350,
                ModItems.COMPONENT_ACTUATOR.get(), 500
        );
    }

    @Override
    public int getHumanityCost() {
        return humanityCost;
    }

    @Override
    public boolean isToggleableByWheel(ItemStack installedStack, CyberwareSlot slot) {
        return true;
    }

    @Override
    public Set<TagKey<Item>> requiresCyberwareTags(ItemStack installedStack, CyberwareSlot slot) {
        return Set.of(ModTags.Items.LUNGS_ITEMS);
    }

    @Override
    public Set<CyberwareSlot> getSupportedSlots() {
        return Set.of(CyberwareSlot.LUNGS);
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
    public int maxStacksPerSlotType(ItemStack stack, CyberwareSlot slotType) {
        return 1;
    }

    @Override
    public int getEnergyUsedPerTick(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot) {
        if (slot != CyberwareSlot.LUNGS) return 0;
        if (!(entity instanceof Player player)) return 0;

        return isEnabled(player) ? DORMANT_ENERGY_COST : 0;
    }

    public static boolean isInjectable(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(Tags.Items.POTIONS);
    }

    public static boolean isInstalled(Player player) {
        if (player == null) return false;
        if (!player.hasData(ModAttachments.CYBERWARE)) return false;

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
        if (data == null) return false;

        return findInstalledIndex(data) >= 0;
    }

    public static boolean isEnabled(Player player) {
        if (player == null) return false;
        if (!player.hasData(ModAttachments.CYBERWARE)) return false;

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
        if (data == null) return false;

        int index = findInstalledIndex(data);
        return index >= 0 && data.isEnabled(CyberwareSlot.LUNGS, index);
    }

    public static boolean isPowered(Player player) {
        if (player == null) return false;
        if (!player.hasData(ModAttachments.CYBERWARE)) return false;

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
        if (data == null) return false;

        int index = findInstalledIndex(data);
        if (index < 0) return false;

        InstalledCyberware installed = data.get(CyberwareSlot.LUNGS, index);
        if (installed == null) return false;

        return installed.isPowered();
    }

    public static int findInstalledIndex(PlayerCyberwareData data) {
        if (data == null) return -1;

        InstalledCyberware[] installed = data.getAll().get(CyberwareSlot.LUNGS);
        if (installed == null) return -1;

        for (int i = 0; i < installed.length; i++) {
            InstalledCyberware cyberware = installed[i];
            if (cyberware == null) continue;

            ItemStack stack = cyberware.getItem();
            if (stack == null || stack.isEmpty()) continue;

            if (stack.getItem() instanceof VampyresItem) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public void onInstalled(LivingEntity entity) {
    }

    @Override
    public void onRemoved(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!player.hasData(ModAttachments.CYBERWARE)) return;

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
        if (data == null) return;

        data.ejectVampyresInventory(player);
        player.syncData(ModAttachments.CYBERWARE);
    }

    @Override
    public void onTick(LivingEntity entity) {
    }
}