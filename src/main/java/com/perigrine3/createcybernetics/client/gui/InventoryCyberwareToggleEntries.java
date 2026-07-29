package com.perigrine3.createcybernetics.client.gui;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.cyberware.arm.ArmCannonItem;
import com.perigrine3.createcybernetics.item.cyberware.bone.SpinalInjectorItem;
import com.perigrine3.createcybernetics.item.cyberware.brain.ChipwareSlotsItem;
import com.perigrine3.createcybernetics.item.cyberware.brain.CyberdeckItem;
import com.perigrine3.createcybernetics.item.cyberware.leg.PneumaticCalvesItem;
import com.perigrine3.createcybernetics.item.cyberware.organs.HeatEngineItem;
import com.perigrine3.createcybernetics.network.payload.OpenArmCannonPayload;
import com.perigrine3.createcybernetics.network.payload.OpenChipwareMiniPayload;
import com.perigrine3.createcybernetics.network.payload.OpenCyberdeckPayload;
import com.perigrine3.createcybernetics.network.payload.OpenHeatEnginePayload;
import com.perigrine3.createcybernetics.network.payload.OpenSpinalInjectorPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InventoryCyberwareToggleEntries {

    public record Target(CyberwareSlot slot, int index) {}

    public enum OpenAction {
        SPINAL_INJECTOR,
        ARM_CANNON,
        HEAT_ENGINE,
        CYBERDECK,
        CHIPWARE;

        public void execute() {
            switch (this) {
                case SPINAL_INJECTOR -> PacketDistributor.sendToServer(new OpenSpinalInjectorPayload());
                case ARM_CANNON -> PacketDistributor.sendToServer(new OpenArmCannonPayload());
                case HEAT_ENGINE -> PacketDistributor.sendToServer(new OpenHeatEnginePayload());
                case CYBERDECK -> PacketDistributor.sendToServer(new OpenCyberdeckPayload());
                case CHIPWARE -> PacketDistributor.sendToServer(new OpenChipwareMiniPayload());
            }
        }
    }

    public record Entry(ItemStack icon, List<Target> targets, OpenAction openAction) {
        public boolean canToggle() {
            return !targets.isEmpty();
        }

        public boolean canOpen() {
            return openAction != null;
        }
    }

    private static final class EntryBuilder {
        private final ItemStack icon;
        private final List<Target> targets = new ArrayList<>();
        private OpenAction openAction;

        private EntryBuilder(ItemStack icon) {
            this.icon = icon.copy();
        }

        private Entry build() {
            return new Entry(icon, List.copyOf(targets), openAction);
        }
    }

    private InventoryCyberwareToggleEntries() {}

    public static List<Entry> collect() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.hasData(ModAttachments.CYBERWARE)) {
            return List.of();
        }

        PlayerCyberwareData data = minecraft.player.getData(ModAttachments.CYBERWARE);
        if (data == null) {
            return List.of();
        }

        Map<ResourceLocation, EntryBuilder> entries = new LinkedHashMap<>();

        for (var slotEntry : data.getAll().entrySet()) {
            CyberwareSlot slot = slotEntry.getKey();
            var installed = slotEntry.getValue();
            if (installed == null) continue;

            for (int index = 0; index < installed.length; index++) {
                var cyberware = installed[index];
                if (cyberware == null) continue;

                ItemStack stack = cyberware.getItem();
                if (stack == null || stack.isEmpty()) continue;
                if (!(stack.getItem() instanceof ICyberwareItem item)) continue;

                boolean toggleable = item.isToggleableByWheel(stack, slot);
                OpenAction openAction = openAction(stack);
                if (!toggleable && openAction == null) continue;

                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                EntryBuilder builder = entries.computeIfAbsent(itemId, ignored -> new EntryBuilder(stack));
                if (toggleable) {
                    builder.targets.add(new Target(slot, index));
                }
                if (builder.openAction == null) {
                    builder.openAction = openAction;
                }
            }
        }

        List<Entry> result = new ArrayList<>();
        for (EntryBuilder builder : entries.values()) {
            if (builder.icon.getItem() instanceof PneumaticCalvesItem && builder.targets.size() < 2) {
                continue;
            }
            result.add(builder.build());
        }
        return result;
    }

    private static OpenAction openAction(ItemStack stack) {
        if (stack.getItem() instanceof SpinalInjectorItem) return OpenAction.SPINAL_INJECTOR;
        if (stack.getItem() instanceof ArmCannonItem) return OpenAction.ARM_CANNON;
        if (stack.getItem() instanceof HeatEngineItem) return OpenAction.HEAT_ENGINE;
        if (stack.getItem() instanceof CyberdeckItem) return OpenAction.CYBERDECK;
        if (stack.getItem() instanceof ChipwareSlotsItem) return OpenAction.CHIPWARE;
        return null;
    }

    public static boolean isEnabled(Entry entry) {
        if (!entry.canToggle()) return false;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.hasData(ModAttachments.CYBERWARE)) {
            return false;
        }

        PlayerCyberwareData data = minecraft.player.getData(ModAttachments.CYBERWARE);
        if (data == null) return false;

        for (Target target : entry.targets()) {
            if (data.isEnabled(target.slot(), target.index())) {
                return true;
            }
        }
        return false;
    }
}
