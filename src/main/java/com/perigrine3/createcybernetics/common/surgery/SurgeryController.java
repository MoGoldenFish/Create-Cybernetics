package com.perigrine3.createcybernetics.common.surgery;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.block.RobosurgeonBlock;
import com.perigrine3.createcybernetics.block.entity.RobosurgeonBlockEntity;
import com.perigrine3.createcybernetics.block.entity.SurgeryTableBlockEntity;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.common.energy.ConditionalBlockPower;
import com.perigrine3.createcybernetics.event.custom.CyberwareSurgeryEvent;
import com.perigrine3.createcybernetics.item.cyberware.brain.CerebralProcessingUnitItem;
import com.perigrine3.createcybernetics.item.cyberware.organs.OregrinderItem;
import com.perigrine3.createcybernetics.util.ModTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SurgeryController {

    private SurgeryController() {}

    public static float performSurgery(Player player, RobosurgeonBlockEntity surgeon) {
        if (!canRobosurgeonPerformSurgery(player, surgeon)) {
            return 0.0F;
        }

        return performSurgery(player, surgeon, surgeon.staged, surgeon.markedForRemoval);
    }

    public static float performSurgery(Player player, SurgeryTableBlockEntity surgeon) {
        return performSurgery(player, surgeon, surgeon.getStagedFlags(), surgeon.getMarkedForRemovalFlags());
    }

    public static float performSurgery(Player player, RobosurgeonBlockEntity surgeon, boolean[] staged, boolean[] markedForRemoval) {
        if (player.level().isClientSide) return 0.0F;

        if (!canRobosurgeonPerformSurgery(player, surgeon)) {
            return 0.0F;
        }

        surgeon.beginSurgery();

        try {
            PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
            if (data == null) return 0.0F;

            boolean didWork = false;
            int installs = 0;
            int removals = 0;

            List<CyberwareSurgeryEvent.Change> installedChanges = new ArrayList<>();
            List<CyberwareSurgeryEvent.Change> removedChanges = new ArrayList<>();

            boolean[] wasInstalledBefore = new boolean[surgeon.inventory.getSlots()];
            for (CyberwareSlot slot : CyberwareSlot.values()) {
                for (int i = 0; i < slot.size; i++) {
                    int invIndex = RobosurgeonSlotMap.toInventoryIndex(slot, i);
                    if (invIndex < 0 || invIndex >= surgeon.inventory.getSlots()) continue;

                    InstalledCyberware inst = data.get(slot, i);
                    wasInstalledBefore[invIndex] = inst != null && inst.getItem() != null && !inst.getItem().isEmpty();
                }
            }

            Set<Item> removedItemsThisSurgery = new HashSet<>();

            for (CyberwareSlot slot : CyberwareSlot.values()) {
                for (int i = 0; i < slot.size; i++) {
                    int invIndex = RobosurgeonSlotMap.toInventoryIndex(slot, i);
                    if (invIndex < 0 || invIndex >= surgeon.inventory.getSlots()) continue;

                    ItemStack stackInGui = surgeon.inventory.getStackInSlot(invIndex);

                    boolean isStaged = staged != null && invIndex < staged.length && staged[invIndex];
                    boolean isMarked = markedForRemoval != null && invIndex < markedForRemoval.length && markedForRemoval[invIndex];

                    boolean willRemove = isMarked;
                    boolean willInstall = isStaged && !stackInGui.isEmpty();

                    if (willRemove && willInstall) {
                        InstalledCyberware current = data.get(slot, i);
                        ItemStack installedStack = current != null && current.getItem() != null ? current.getItem() : ItemStack.EMPTY;

                        if (!installedStack.isEmpty() && ItemStack.isSameItemSameComponents(stackInGui, installedStack)) {
                            willInstall = false;

                            surgeon.inventory.setStackInSlot(invIndex, ItemStack.EMPTY);
                            surgeon.installed[invIndex] = false;
                            surgeon.staged[invIndex] = false;
                            stackInGui = ItemStack.EMPTY;
                        }
                    }

                    if (willRemove) {
                        InstalledCyberware removed = data.remove(slot, i);

                        if (removed != null && removed.getItem() != null && !removed.getItem().isEmpty()) {
                            didWork = true;
                            removals++;
                            removedChanges.add(new CyberwareSurgeryEvent.Change(slot, i, removed.getItem().copy()));
                            removedItemsThisSurgery.add(removed.getItem().getItem());

                            data.ejectInventoryForRemovedCyberware(
                                    player,
                                    removed.getItem()
                            );

                            if (removed.getItem().getItem() instanceof ICyberwareItem cw) {
                                cw.onRemoved(player);
                            }

                            ItemStack giveBack = removed.getItem().copy();
                            if (!player.getInventory().add(giveBack)) {
                                player.drop(giveBack, false);
                            }
                        }

                        if (!willInstall) {
                            clearRobosurgeonSlot(surgeon, invIndex);
                            continue;
                        }
                    }

                    if (!willInstall) {
                        if (isStaged && stackInGui.isEmpty()) {
                            surgeon.staged[invIndex] = false;
                        }
                        continue;
                    }

                    if (stackInGui.getItem() instanceof CerebralProcessingUnitItem
                            && !CerebralProcessingUnitItem.hasCompletedBiochipBinding(stackInGui)) {
                        rejectRobosurgeonInstall(player, surgeon, invIndex, stackInGui);
                        continue;
                    }

                    if (stackInGui.getItem() instanceof ICyberwareItem cw && !cw.surgeryInstallable()) {
                        rejectRobosurgeonInstall(player, surgeon, invIndex, stackInGui);
                        continue;
                    }

                    if (stackInGui.getItem() instanceof ICyberwareItem cw) {
                        int cap = Math.max(1, cw.maxStacksPerSlotType(stackInGui, slot));
                        int currentlyInstalledSame = countInstalledSameInSlotType(data, slot, stackInGui);
                        int plannedRemovedSame = countPlannedRemovalsSameInSlotType(data, slot, surgeon, markedForRemoval, stackInGui);
                        int effectiveSameAfterAllRemovals = currentlyInstalledSame - plannedRemovedSame;

                        if (effectiveSameAfterAllRemovals >= cap) {
                            rejectRobosurgeonInstall(player, surgeon, invIndex, stackInGui);
                            continue;
                        }
                    }

                    if (stackInGui.getItem() instanceof ICyberwareItem cwReq) {
                        Set<Item> requiredItems = cwReq.requiresCyberware(stackInGui, slot);
                        Set<TagKey<Item>> requiredTags = cwReq.requiresCyberwareTags(stackInGui, slot);

                        boolean needItems = requiredItems != null && !requiredItems.isEmpty();
                        boolean needTags = requiredTags != null && !requiredTags.isEmpty();

                        boolean ok = true;

                        if (needItems) {
                            ok = hasAnyRequiredCyberwareItems(data, surgeon, staged, requiredItems, slot);
                        }
                        if (ok && needTags) {
                            ok = hasAnyRequiredCyberwareTags(data, surgeon, staged, requiredTags, slot);
                        }

                        if (!ok) {
                            rejectRobosurgeonInstall(player, surgeon, invIndex, stackInGui);
                            continue;
                        }
                    }

                    if (stackInGui.getItem() instanceof ICyberwareItem) {
                        if (stackInGui.getItem() instanceof OregrinderItem
                                && hasAnyInstalledWetware(data, surgeon, markedForRemoval)) {
                            rejectRobosurgeonInstall(player, surgeon, invIndex, stackInGui);
                            continue;
                        }

                        if (hasAnyIncompatibleCyberware(data, surgeon, staged, markedForRemoval, stackInGui, slot, i)) {
                            rejectRobosurgeonInstall(player, surgeon, invIndex, stackInGui);
                            continue;
                        }

                        if (hasAnySameSlotIncompatibleCyberware(data, surgeon, staged, markedForRemoval, stackInGui, slot, i)) {
                            rejectRobosurgeonInstall(player, surgeon, invIndex, stackInGui);
                            continue;
                        }
                    }

                    int humanityCost = 0;
                    if (stackInGui.getItem() instanceof ICyberwareItem cyberwareItem) {
                        humanityCost = cyberwareItem.getHumanityCost();
                    }

                    InstalledCyberware installed = new InstalledCyberware(stackInGui.copy(), slot, i, humanityCost);
                    installed.setPowered(true);
                    data.set(slot, i, installed);

                    didWork = true;
                    installs++;
                    installedChanges.add(new CyberwareSurgeryEvent.Change(slot, i, installed.getItem().copy()));

                    if (stackInGui.getItem() instanceof ICyberwareItem cw) {
                        cw.onInstalled(player, installed.getItem());
                        surgeon.inventory.setStackInSlot(invIndex, installed.getItem().copy());
                    }

                    surgeon.installed[invIndex] = true;
                    surgeon.staged[invIndex] = false;
                    surgeon.markedForRemoval[invIndex] = false;
                }
            }

            if (!removedItemsThisSurgery.isEmpty()) {
                int forced = forceRemoveDependents(player, surgeon, data, wasInstalledBefore, removedItemsThisSurgery, removedChanges);
                if (forced > 0) {
                    didWork = true;
                    removals += forced;
                }
            }

            data.recomputeHumanityBaseFromInstalled(player);
            float damageApplied = 0.0F;

            if (didWork) {
                if (player instanceof ServerPlayer sp) {
                    NeoForge.EVENT_BUS.post(new CyberwareSurgeryEvent(sp, installs, removals, installedChanges, removedChanges));
                }

                if (player.level() instanceof ServerLevel sl) {
                    sl.sendParticles(
                            ParticleTypes.HAPPY_VILLAGER,
                            player.getX(),
                            player.getY() + 1.0,
                            player.getZ(),
                            18,
                            0.35,
                            0.55,
                            0.35,
                            0.0
                    );
                }
            }

            data.setDirty();
            if (player instanceof ServerPlayer sp) {
                ModAttachments.syncCyberware(sp);
            }

            surgeon.clearSlotStates();
            surgeon.setChanged();

            Level surgeonLevel = surgeon.getLevel();
            if (surgeonLevel != null) {
                surgeonLevel.sendBlockUpdated(surgeon.getBlockPos(), surgeon.getBlockState(), surgeon.getBlockState(), 3);
            }

            return damageApplied;
        } finally {
            surgeon.endSurgery();
        }
    }

    private static boolean canRobosurgeonPerformSurgery(Player player, RobosurgeonBlockEntity surgeon) {
        if (player == null || surgeon == null) {
            return false;
        }

        if (player.level().isClientSide) {
            return false;
        }

        Level level = surgeon.getLevel();

        if (level == null || level.isClientSide) {
            return false;
        }

        boolean powered = ConditionalBlockPower.consumeRequiredPower(
                level,
                surgeon.getBlockPos(),
                surgeon.getMutableEnergyStorage(),
                RobosurgeonBlock.ENERGY_USED_PER_GUI_TICK
        );

        if (powered) {
            return true;
        }

        if (ConditionalBlockPower.shouldUseEnergyInsteadOfRedstone()) {
            player.displayClientMessage(Component.translatable("message.createcybernetics.block.requires_energy"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.createcybernetics.block.requires_redstone"), true);
        }

        return false;
    }

    public static float performSurgery(Player player, SurgeryTableBlockEntity surgeon, boolean[] staged, boolean[] markedForRemoval) {
        if (player.level().isClientSide) return 0.0F;

        surgeon.beginSurgery();

        try {
            PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
            if (data == null) return 0.0F;

            boolean didWork = false;
            int installs = 0;
            int removals = 0;

            List<CyberwareSurgeryEvent.Change> installedChanges = new ArrayList<>();
            List<CyberwareSurgeryEvent.Change> removedChanges = new ArrayList<>();

            boolean[] wasInstalledBefore = new boolean[surgeon.inventory.getSlots()];
            for (CyberwareSlot slot : CyberwareSlot.values()) {
                for (int i = 0; i < slot.size; i++) {
                    int invIndex = RobosurgeonSlotMap.toInventoryIndex(slot, i);
                    if (invIndex < 0 || invIndex >= surgeon.inventory.getSlots()) continue;

                    InstalledCyberware inst = data.get(slot, i);
                    wasInstalledBefore[invIndex] = inst != null && inst.getItem() != null && !inst.getItem().isEmpty();
                }
            }

            Set<Item> removedItemsThisSurgery = new HashSet<>();

            for (CyberwareSlot slot : CyberwareSlot.values()) {
                for (int i = 0; i < slot.size; i++) {
                    int invIndex = RobosurgeonSlotMap.toInventoryIndex(slot, i);
                    if (invIndex < 0 || invIndex >= surgeon.inventory.getSlots()) continue;

                    ItemStack stackInGui = surgeon.inventory.getStackInSlot(invIndex);

                    boolean isStaged = staged != null && invIndex < staged.length && staged[invIndex];
                    boolean isMarked = markedForRemoval != null && invIndex < markedForRemoval.length && markedForRemoval[invIndex];

                    boolean willRemove = isMarked;
                    boolean willInstall = isStaged && !stackInGui.isEmpty();

                    if (willRemove && willInstall) {
                        InstalledCyberware current = data.get(slot, i);
                        ItemStack installedStack = current != null && current.getItem() != null ? current.getItem() : ItemStack.EMPTY;

                        if (!installedStack.isEmpty() && ItemStack.isSameItemSameComponents(stackInGui, installedStack)) {
                            willInstall = false;

                            surgeon.inventory.setStackInSlot(invIndex, ItemStack.EMPTY);
                            surgeon.setInstalled(invIndex, false);
                            surgeon.setStaged(invIndex, false);
                            stackInGui = ItemStack.EMPTY;
                        }
                    }

                    if (willRemove) {
                        InstalledCyberware removed = data.remove(slot, i);

                        if (removed != null && removed.getItem() != null && !removed.getItem().isEmpty()) {
                            didWork = true;
                            removals++;
                            removedChanges.add(new CyberwareSurgeryEvent.Change(slot, i, removed.getItem().copy()));
                            removedItemsThisSurgery.add(removed.getItem().getItem());

                            data.ejectInventoryForRemovedCyberware(
                                    player,
                                    removed.getItem()
                            );

                            if (removed.getItem().getItem() instanceof ICyberwareItem cw) {
                                cw.onRemoved(player);
                            }

                            ItemStack giveBack = removed.getItem().copy();
                            if (!player.getInventory().add(giveBack)) {
                                player.drop(giveBack, false);
                            }
                        }

                        if (!willInstall) {
                            clearSurgeryTableSlot(surgeon, invIndex);
                            continue;
                        }
                    }

                    if (!willInstall) {
                        if (isStaged && stackInGui.isEmpty()) {
                            surgeon.setStaged(invIndex, false);
                        }
                        continue;
                    }

                    if (stackInGui.getItem() instanceof CerebralProcessingUnitItem
                            && !CerebralProcessingUnitItem.hasCompletedBiochipBinding(stackInGui)) {
                        rejectSurgeryTableInstall(player, surgeon, invIndex, stackInGui);
                        continue;
                    }

                    if (stackInGui.getItem() instanceof ICyberwareItem cw && !cw.surgeryInstallable()) {
                        rejectSurgeryTableInstall(player, surgeon, invIndex, stackInGui);
                        continue;
                    }

                    if (stackInGui.getItem() instanceof ICyberwareItem cw) {
                        int cap = Math.max(1, cw.maxStacksPerSlotType(stackInGui, slot));
                        int currentlyInstalledSame = countInstalledSameInSlotType(data, slot, stackInGui);
                        int plannedRemovedSame = countPlannedRemovalsSameInSlotType(data, slot, surgeon, markedForRemoval, stackInGui);
                        int effectiveSameAfterAllRemovals = currentlyInstalledSame - plannedRemovedSame;

                        if (effectiveSameAfterAllRemovals >= cap) {
                            rejectSurgeryTableInstall(player, surgeon, invIndex, stackInGui);
                            continue;
                        }
                    }

                    if (stackInGui.getItem() instanceof ICyberwareItem cwReq) {
                        Set<Item> requiredItems = cwReq.requiresCyberware(stackInGui, slot);
                        Set<TagKey<Item>> requiredTags = cwReq.requiresCyberwareTags(stackInGui, slot);

                        boolean needItems = requiredItems != null && !requiredItems.isEmpty();
                        boolean needTags = requiredTags != null && !requiredTags.isEmpty();

                        boolean ok = true;

                        if (needItems) {
                            ok = hasAnyRequiredCyberwareItems(data, surgeon, staged, requiredItems, slot);
                        }
                        if (ok && needTags) {
                            ok = hasAnyRequiredCyberwareTags(data, surgeon, staged, requiredTags, slot);
                        }

                        if (!ok) {
                            rejectSurgeryTableInstall(player, surgeon, invIndex, stackInGui);
                            continue;
                        }
                    }

                    if (stackInGui.getItem() instanceof ICyberwareItem) {
                        if (stackInGui.getItem() instanceof OregrinderItem
                                && hasAnyInstalledWetware(data, surgeon, markedForRemoval)) {
                            rejectSurgeryTableInstall(player, surgeon, invIndex, stackInGui);
                            continue;
                        }

                        if (hasAnyIncompatibleCyberware(data, surgeon, staged, markedForRemoval, stackInGui, slot, i)) {
                            rejectSurgeryTableInstall(player, surgeon, invIndex, stackInGui);
                            continue;
                        }

                        if (hasAnySameSlotIncompatibleCyberware(data, surgeon, staged, markedForRemoval, stackInGui, slot, i)) {
                            rejectSurgeryTableInstall(player, surgeon, invIndex, stackInGui);
                            continue;
                        }
                    }

                    int humanityCost = 0;
                    if (stackInGui.getItem() instanceof ICyberwareItem cyberwareItem) {
                        humanityCost = cyberwareItem.getHumanityCost();
                    }

                    InstalledCyberware installed = new InstalledCyberware(stackInGui.copy(), slot, i, humanityCost);
                    installed.setPowered(true);
                    data.set(slot, i, installed);

                    didWork = true;
                    installs++;
                    installedChanges.add(new CyberwareSurgeryEvent.Change(slot, i, installed.getItem().copy()));

                    if (stackInGui.getItem() instanceof ICyberwareItem cw) {
                        cw.onInstalled(player, installed.getItem());
                        surgeon.inventory.setStackInSlot(invIndex, installed.getItem().copy());
                    }

                    surgeon.setInstalled(invIndex, true);
                    surgeon.setStaged(invIndex, false);
                    surgeon.setMarkedForRemoval(invIndex, false);
                }
            }

            if (!removedItemsThisSurgery.isEmpty()) {
                int forced = forceRemoveDependents(player, surgeon, data, wasInstalledBefore, removedItemsThisSurgery, removedChanges);
                if (forced > 0) {
                    didWork = true;
                    removals += forced;
                }
            }

            data.recomputeHumanityBaseFromInstalled(player);
            float damageApplied = 0.0F;

            if (didWork) {
                if (player instanceof ServerPlayer sp) {
                    NeoForge.EVENT_BUS.post(new CyberwareSurgeryEvent(sp, installs, removals, installedChanges, removedChanges));
                }

                if (player.level() instanceof ServerLevel sl) {
                    sl.sendParticles(
                            ParticleTypes.HAPPY_VILLAGER,
                            player.getX(),
                            player.getY() + 1.0,
                            player.getZ(),
                            18,
                            0.35,
                            0.55,
                            0.35,
                            0.0
                    );
                }
            }

            data.setDirty();
            if (player instanceof ServerPlayer sp) {
                ModAttachments.syncCyberware(sp);
            }

            surgeon.clearSlotStates();
            surgeon.setChanged();
            player.level().sendBlockUpdated(surgeon.getBlockPos(), surgeon.getBlockState(), surgeon.getBlockState(), 3);

            return damageApplied;
        } finally {
            surgeon.endSurgery();
        }
    }

    private static void rejectRobosurgeonInstall(Player player, RobosurgeonBlockEntity surgeon, int invIndex, ItemStack stack) {
        giveOrDrop(player, stack);
        clearRobosurgeonSlot(surgeon, invIndex);
    }

    private static void rejectSurgeryTableInstall(Player player, SurgeryTableBlockEntity surgeon, int invIndex, ItemStack stack) {
        giveOrDrop(player, stack);
        clearSurgeryTableSlot(surgeon, invIndex);
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        ItemStack giveBack = stack.copy();
        if (!player.getInventory().add(giveBack)) {
            player.drop(giveBack, false);
        }
    }

    private static void clearRobosurgeonSlot(RobosurgeonBlockEntity surgeon, int invIndex) {
        surgeon.inventory.setStackInSlot(invIndex, ItemStack.EMPTY);
        surgeon.installed[invIndex] = false;
        surgeon.staged[invIndex] = false;
        surgeon.markedForRemoval[invIndex] = false;
    }

    private static void clearSurgeryTableSlot(SurgeryTableBlockEntity surgeon, int invIndex) {
        surgeon.inventory.setStackInSlot(invIndex, ItemStack.EMPTY);
        surgeon.setInstalled(invIndex, false);
        surgeon.setStaged(invIndex, false);
        surgeon.setMarkedForRemoval(invIndex, false);
    }



    private static boolean hasAnyInstalledWetware(PlayerCyberwareData data, RobosurgeonBlockEntity surgeon, boolean[] markedForRemoval) {
        for (CyberwareSlot slot : CyberwareSlot.values()) {
            for (int index = 0; index < slot.size; index++) {
                int invIndex = RobosurgeonSlotMap.toInventoryIndex(slot, index);

                if (invIndex < 0 || invIndex >= surgeon.inventory.getSlots()) {
                    continue;
                }

                if (markedForRemoval != null
                        && invIndex < markedForRemoval.length
                        && markedForRemoval[invIndex]) {
                    continue;
                }

                InstalledCyberware installed = data.get(slot, index);
                if (installed == null || installed.getItem() == null || installed.getItem().isEmpty()) {
                    continue;
                }

                if (installed.getItem().is(ModTags.Items.WETWARE_ITEM)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean hasAnyInstalledWetware(PlayerCyberwareData data, SurgeryTableBlockEntity surgeon, boolean[] markedForRemoval) {
        for (CyberwareSlot slot : CyberwareSlot.values()) {
            for (int index = 0; index < slot.size; index++) {
                int invIndex = RobosurgeonSlotMap.toInventoryIndex(slot, index);

                if (invIndex < 0 || invIndex >= surgeon.inventory.getSlots()) {
                    continue;
                }

                if (markedForRemoval != null
                        && invIndex < markedForRemoval.length
                        && markedForRemoval[invIndex]) {
                    continue;
                }

                InstalledCyberware installed = data.get(slot, index);
                if (installed == null || installed.getItem() == null || installed.getItem().isEmpty()) {
                    continue;
                }

                if (installed.getItem().is(ModTags.Items.WETWARE_ITEM)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static int forceRemoveDependents(
            Player player,
            RobosurgeonBlockEntity surgeon,
            PlayerCyberwareData data,
            boolean[] wasInstalledBefore,
            Set<Item> removedItemsThisSurgery,
            List<CyberwareSurgeryEvent.Change> removedChanges
    ) {
        int forcedRemovals = 0;

        boolean changed;
        do {
            changed = false;

            for (CyberwareSlot slot : CyberwareSlot.values()) {
                for (int i = 0; i < slot.size; i++) {
                    int invIndex = RobosurgeonSlotMap.toInventoryIndex(slot, i);
                    if (invIndex < 0 || invIndex >= surgeon.inventory.getSlots()) continue;
                    if (invIndex >= wasInstalledBefore.length) continue;
                    if (!wasInstalledBefore[invIndex]) continue;

                    InstalledCyberware inst = data.get(slot, i);
                    if (inst == null || inst.getItem() == null || inst.getItem().isEmpty()) continue;

                    ItemStack installedStack = inst.getItem();
                    if (!(installedStack.getItem() instanceof ICyberwareItem cw)) continue;

                    Set<Item> requiredItems = cw.requiresCyberware(installedStack, slot);
                    Set<TagKey<Item>> requiredTags = cw.requiresCyberwareTags(installedStack, slot);

                    boolean hasReqItems = requiredItems != null && !requiredItems.isEmpty();
                    boolean hasReqTags = requiredTags != null && !requiredTags.isEmpty();

                    if (!hasReqItems && !hasReqTags) continue;

                    boolean intersectsRemoved = removedRequirementMatches(requiredItems, requiredTags, removedItemsThisSurgery);
                    if (!intersectsRemoved) continue;

                    boolean itemsSatisfied = hasReqItems && hasAnyInstalledItem(data, requiredItems);
                    boolean tagsSatisfied = hasReqTags && hasAnyInstalledItemWithAnyTag(data, requiredTags);

                    if (itemsSatisfied || tagsSatisfied) continue;

                    InstalledCyberware removed = data.remove(slot, i);
                    if (removed == null || removed.getItem() == null || removed.getItem().isEmpty()) {
                        surgeon.inventory.setStackInSlot(invIndex, ItemStack.EMPTY);
                        surgeon.installed[invIndex] = false;
                        continue;
                    }

                    forcedRemovals++;
                    changed = true;

                    Item removedItem = removed.getItem().getItem();
                    if (removedItem != null) {
                        removedItemsThisSurgery.add(removedItem);
                    }

                    removedChanges.add(new CyberwareSurgeryEvent.Change(slot, i, removed.getItem().copy()));

                    data.ejectInventoryForRemovedCyberware(
                            player,
                            removed.getItem()
                    );

                    if (removedItem instanceof ICyberwareItem cwRemoved) {
                        cwRemoved.onRemoved(player);
                    }

                    giveOrDrop(player, removed.getItem());
                    clearRobosurgeonSlot(surgeon, invIndex);
                }
            }
        } while (changed);

        return forcedRemovals;
    }

    private static int forceRemoveDependents(
            Player player,
            SurgeryTableBlockEntity surgeon,
            PlayerCyberwareData data,
            boolean[] wasInstalledBefore,
            Set<Item> removedItemsThisSurgery,
            List<CyberwareSurgeryEvent.Change> removedChanges
    ) {
        int forcedRemovals = 0;

        boolean changed;
        do {
            changed = false;

            for (CyberwareSlot slot : CyberwareSlot.values()) {
                for (int i = 0; i < slot.size; i++) {
                    int invIndex = RobosurgeonSlotMap.toInventoryIndex(slot, i);
                    if (invIndex < 0 || invIndex >= surgeon.inventory.getSlots()) continue;
                    if (invIndex >= wasInstalledBefore.length) continue;
                    if (!wasInstalledBefore[invIndex]) continue;

                    InstalledCyberware inst = data.get(slot, i);
                    if (inst == null || inst.getItem() == null || inst.getItem().isEmpty()) continue;

                    ItemStack installedStack = inst.getItem();
                    if (!(installedStack.getItem() instanceof ICyberwareItem cw)) continue;

                    Set<Item> requiredItems = cw.requiresCyberware(installedStack, slot);
                    Set<TagKey<Item>> requiredTags = cw.requiresCyberwareTags(installedStack, slot);

                    boolean hasReqItems = requiredItems != null && !requiredItems.isEmpty();
                    boolean hasReqTags = requiredTags != null && !requiredTags.isEmpty();

                    if (!hasReqItems && !hasReqTags) continue;

                    boolean intersectsRemoved = removedRequirementMatches(requiredItems, requiredTags, removedItemsThisSurgery);
                    if (!intersectsRemoved) continue;

                    boolean itemsSatisfied = hasReqItems && hasAnyInstalledItem(data, requiredItems);
                    boolean tagsSatisfied = hasReqTags && hasAnyInstalledItemWithAnyTag(data, requiredTags);

                    if (itemsSatisfied || tagsSatisfied) continue;

                    InstalledCyberware removed = data.remove(slot, i);
                    if (removed == null || removed.getItem() == null || removed.getItem().isEmpty()) {
                        surgeon.inventory.setStackInSlot(invIndex, ItemStack.EMPTY);
                        surgeon.setInstalled(invIndex, false);
                        continue;
                    }

                    forcedRemovals++;
                    changed = true;

                    Item removedItem = removed.getItem().getItem();
                    if (removedItem != null) {
                        removedItemsThisSurgery.add(removedItem);
                    }

                    removedChanges.add(new CyberwareSurgeryEvent.Change(slot, i, removed.getItem().copy()));

                    data.ejectInventoryForRemovedCyberware(
                            player,
                            removed.getItem()
                    );

                    if (removedItem instanceof ICyberwareItem cwRemoved) {
                        cwRemoved.onRemoved(player);
                    }

                    giveOrDrop(player, removed.getItem());
                    clearSurgeryTableSlot(surgeon, invIndex);
                }
            }
        } while (changed);

        return forcedRemovals;
    }

    private static boolean removedRequirementMatches(
            Set<Item> requiredItems,
            Set<TagKey<Item>> requiredTags,
            Set<Item> removedItemsThisSurgery
    ) {
        if (requiredItems != null) {
            for (Item req : requiredItems) {
                if (req != null && removedItemsThisSurgery.contains(req)) {
                    return true;
                }
            }
        }

        if (requiredTags != null) {
            for (TagKey<Item> tag : requiredTags) {
                if (tag != null && anyRemovedItemMatchesTag(removedItemsThisSurgery, tag)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean hasAnyInstalledItem(PlayerCyberwareData data, Set<Item> items) {
        if (items == null || items.isEmpty()) return true;

        for (var entry : data.getAll().entrySet()) {
            InstalledCyberware[] arr = entry.getValue();
            if (arr == null) continue;

            for (InstalledCyberware inst : arr) {
                if (inst == null || inst.getItem() == null) continue;
                ItemStack st = inst.getItem();
                if (st.isEmpty()) continue;

                if (items.contains(st.getItem())) return true;
            }
        }

        return false;
    }

    private static boolean hasAnyInstalledItemWithAnyTag(PlayerCyberwareData data, Set<TagKey<Item>> tags) {
        if (tags == null || tags.isEmpty()) return true;

        for (var entry : data.getAll().entrySet()) {
            InstalledCyberware[] arr = entry.getValue();
            if (arr == null) continue;

            for (InstalledCyberware inst : arr) {
                if (inst == null || inst.getItem() == null) continue;
                ItemStack st = inst.getItem();
                if (st.isEmpty()) continue;

                for (TagKey<Item> tag : tags) {
                    if (tag != null && st.is(tag)) return true;
                }
            }
        }

        return false;
    }

    private static boolean anyRemovedItemMatchesTag(Set<Item> removedItems, TagKey<Item> tag) {
        if (removedItems == null || removedItems.isEmpty()) return false;
        if (tag == null) return false;

        for (Item item : removedItems) {
            if (item == null) continue;
            if (new ItemStack(item).is(tag)) return true;
        }
        return false;
    }

    private static boolean hasAnyRequiredCyberwareItems(
            PlayerCyberwareData data,
            RobosurgeonBlockEntity surgeon,
            boolean[] staged,
            Set<Item> required,
            CyberwareSlot installSlotType
    ) {
        if (required == null || required.isEmpty()) return true;

        if (staged != null) {
            int mappedSize = RobosurgeonSlotMap.mappedSize(installSlotType);
            for (int i = 0; i < mappedSize; i++) {
                int invIndex = RobosurgeonSlotMap.toInventoryIndex(installSlotType, i);
                if (invIndex < 0 || invIndex >= surgeon.inventory.getSlots()) continue;
                if (invIndex >= staged.length) continue;
                if (!staged[invIndex]) continue;

                ItemStack st = surgeon.inventory.getStackInSlot(invIndex);
                if (st.isEmpty()) continue;

                if (required.contains(st.getItem())) return true;
            }
        }

        int mappedSize = RobosurgeonSlotMap.mappedSize(installSlotType);
        for (int i = 0; i < mappedSize; i++) {
            InstalledCyberware inst = data.get(installSlotType, i);
            if (inst == null || inst.getItem() == null || inst.getItem().isEmpty()) continue;
            if (required.contains(inst.getItem().getItem())) return true;
        }

        return false;
    }

    private static boolean hasAnyRequiredCyberwareItems(
            PlayerCyberwareData data,
            SurgeryTableBlockEntity surgeon,
            boolean[] staged,
            Set<Item> required,
            CyberwareSlot installSlotType
    ) {
        if (required == null || required.isEmpty()) return true;

        if (staged != null) {
            int mappedSize = RobosurgeonSlotMap.mappedSize(installSlotType);
            for (int i = 0; i < mappedSize; i++) {
                int invIndex = RobosurgeonSlotMap.toInventoryIndex(installSlotType, i);
                if (invIndex < 0 || invIndex >= surgeon.inventory.getSlots()) continue;
                if (invIndex >= staged.length) continue;
                if (!staged[invIndex]) continue;

                ItemStack st = surgeon.inventory.getStackInSlot(invIndex);
                if (st.isEmpty()) continue;

                if (required.contains(st.getItem())) return true;
            }
        }

        int mappedSize = RobosurgeonSlotMap.mappedSize(installSlotType);
        for (int i = 0; i < mappedSize; i++) {
            InstalledCyberware inst = data.get(installSlotType, i);
            if (inst == null || inst.getItem() == null || inst.getItem().isEmpty()) continue;
            if (required.contains(inst.getItem().getItem())) return true;
        }

        return false;
    }

    private static boolean hasAnyRequiredCyberwareTags(
            PlayerCyberwareData data,
            RobosurgeonBlockEntity surgeon,
            boolean[] staged,
            Set<TagKey<Item>> required,
            CyberwareSlot installSlotType
    ) {
        if (required == null || required.isEmpty()) return true;

        if (staged != null) {
            int mappedSize = RobosurgeonSlotMap.mappedSize(installSlotType);
            for (int i = 0; i < mappedSize; i++) {
                int invIndex = RobosurgeonSlotMap.toInventoryIndex(installSlotType, i);
                if (invIndex < 0 || invIndex >= surgeon.inventory.getSlots()) continue;
                if (invIndex >= staged.length) continue;
                if (!staged[invIndex]) continue;

                ItemStack st = surgeon.inventory.getStackInSlot(invIndex);
                if (st.isEmpty()) continue;

                for (TagKey<Item> tag : required) {
                    if (tag != null && st.is(tag)) return true;
                }
            }
        }

        int mappedSize = RobosurgeonSlotMap.mappedSize(installSlotType);
        for (int i = 0; i < mappedSize; i++) {
            InstalledCyberware inst = data.get(installSlotType, i);
            if (inst == null || inst.getItem() == null || inst.getItem().isEmpty()) continue;

            ItemStack st = inst.getItem();
            for (TagKey<Item> tag : required) {
                if (tag != null && st.is(tag)) return true;
            }
        }

        return false;
    }

    private static boolean hasAnyRequiredCyberwareTags(
            PlayerCyberwareData data,
            SurgeryTableBlockEntity surgeon,
            boolean[] staged,
            Set<TagKey<Item>> required,
            CyberwareSlot installSlotType
    ) {
        if (required == null || required.isEmpty()) return true;

        if (staged != null) {
            int mappedSize = RobosurgeonSlotMap.mappedSize(installSlotType);
            for (int i = 0; i < mappedSize; i++) {
                int invIndex = RobosurgeonSlotMap.toInventoryIndex(installSlotType, i);
                if (invIndex < 0 || invIndex >= surgeon.inventory.getSlots()) continue;
                if (invIndex >= staged.length) continue;
                if (!staged[invIndex]) continue;

                ItemStack st = surgeon.inventory.getStackInSlot(invIndex);
                if (st.isEmpty()) continue;

                for (TagKey<Item> tag : required) {
                    if (tag != null && st.is(tag)) return true;
                }
            }
        }

        int mappedSize = RobosurgeonSlotMap.mappedSize(installSlotType);
        for (int i = 0; i < mappedSize; i++) {
            InstalledCyberware inst = data.get(installSlotType, i);
            if (inst == null || inst.getItem() == null || inst.getItem().isEmpty()) continue;

            ItemStack st = inst.getItem();
            for (TagKey<Item> tag : required) {
                if (tag != null && st.is(tag)) return true;
            }
        }

        return false;
    }

    private static boolean hasAnyIncompatibleCyberware(
            PlayerCyberwareData data,
            RobosurgeonBlockEntity surgeon,
            boolean[] staged,
            boolean[] markedForRemoval,
            ItemStack installingStack,
            CyberwareSlot installingSlotType,
            int installingIndex
    ) {
        if (!(installingStack.getItem() instanceof ICyberwareItem installingItem)) return false;

        Set<Item> badItems = installingItem.incompatibleCyberware(installingStack, installingSlotType);
        Set<TagKey<Item>> badTags = installingItem.incompatibleCyberwareTags(installingStack, installingSlotType);

        if (badItems == null) badItems = Set.of();
        if (badTags == null) badTags = Set.of();

        int currentInvIndex = RobosurgeonSlotMap.toInventoryIndex(installingSlotType, installingIndex);

        for (CyberwareSlot otherSlot : CyberwareSlot.values()) {
            for (int otherIndex = 0; otherIndex < otherSlot.size; otherIndex++) {
                int otherInvIndex = RobosurgeonSlotMap.toInventoryIndex(otherSlot, otherIndex);
                if (otherInvIndex < 0 || otherInvIndex >= surgeon.inventory.getSlots()) continue;
                if (otherInvIndex == currentInvIndex) continue;

                if (markedForRemoval != null && otherInvIndex < markedForRemoval.length && markedForRemoval[otherInvIndex]) {
                    continue;
                }

                ItemStack otherStack;

                boolean otherIsStaged = staged != null && otherInvIndex < staged.length && staged[otherInvIndex];

                if (otherIsStaged) {
                    otherStack = surgeon.inventory.getStackInSlot(otherInvIndex);
                } else {
                    InstalledCyberware inst = data.get(otherSlot, otherIndex);
                    otherStack = inst != null && inst.getItem() != null ? inst.getItem() : ItemStack.EMPTY;
                }

                if (otherStack.isEmpty()) continue;

                if (badItems.contains(otherStack.getItem())) return true;

                for (TagKey<Item> tag : badTags) {
                    if (tag != null && otherStack.is(tag)) return true;
                }

                if (otherStack.getItem() instanceof ICyberwareItem otherCyberware) {
                    Set<Item> otherBadItems = otherCyberware.incompatibleCyberware(otherStack, otherSlot);
                    if (otherBadItems != null && otherBadItems.contains(installingStack.getItem())) return true;

                    Set<TagKey<Item>> otherBadTags = otherCyberware.incompatibleCyberwareTags(otherStack, otherSlot);
                    if (otherBadTags != null) {
                        for (TagKey<Item> tag : otherBadTags) {
                            if (tag != null && installingStack.is(tag)) return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static boolean hasAnyIncompatibleCyberware(
            PlayerCyberwareData data,
            SurgeryTableBlockEntity surgeon,
            boolean[] staged,
            boolean[] markedForRemoval,
            ItemStack installingStack,
            CyberwareSlot installingSlotType,
            int installingIndex
    ) {
        if (!(installingStack.getItem() instanceof ICyberwareItem installingItem)) return false;

        Set<Item> badItems = installingItem.incompatibleCyberware(installingStack, installingSlotType);
        Set<TagKey<Item>> badTags = installingItem.incompatibleCyberwareTags(installingStack, installingSlotType);

        if (badItems == null) badItems = Set.of();
        if (badTags == null) badTags = Set.of();

        int currentInvIndex = RobosurgeonSlotMap.toInventoryIndex(installingSlotType, installingIndex);

        for (CyberwareSlot otherSlot : CyberwareSlot.values()) {
            for (int otherIndex = 0; otherIndex < otherSlot.size; otherIndex++) {
                int otherInvIndex = RobosurgeonSlotMap.toInventoryIndex(otherSlot, otherIndex);
                if (otherInvIndex < 0 || otherInvIndex >= surgeon.inventory.getSlots()) continue;
                if (otherInvIndex == currentInvIndex) continue;

                if (markedForRemoval != null
                        && otherInvIndex < markedForRemoval.length
                        && markedForRemoval[otherInvIndex]) {
                    continue;
                }

                ItemStack otherStack;

                boolean otherIsStaged = staged != null
                        && otherInvIndex < staged.length
                        && staged[otherInvIndex];

                if (otherIsStaged) {
                    otherStack = surgeon.inventory.getStackInSlot(otherInvIndex);
                } else {
                    InstalledCyberware inst = data.get(otherSlot, otherIndex);
                    otherStack = (inst != null && inst.getItem() != null) ? inst.getItem() : ItemStack.EMPTY;
                }

                if (otherStack.isEmpty()) continue;

                if (badItems.contains(otherStack.getItem())) return true;

                for (TagKey<Item> tag : badTags) {
                    if (tag != null && otherStack.is(tag)) return true;
                }

                if (otherStack.getItem() instanceof ICyberwareItem otherCyberware) {
                    Set<Item> otherBadItems = otherCyberware.incompatibleCyberware(otherStack, otherSlot);
                    if (otherBadItems != null && otherBadItems.contains(installingStack.getItem())) return true;

                    Set<TagKey<Item>> otherBadTags = otherCyberware.incompatibleCyberwareTags(otherStack, otherSlot);
                    if (otherBadTags != null) {
                        for (TagKey<Item> tag : otherBadTags) {
                            if (tag != null && installingStack.is(tag)) return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static boolean hasAnySameSlotIncompatibleCyberware(
            PlayerCyberwareData data,
            RobosurgeonBlockEntity surgeon,
            boolean[] staged,
            boolean[] markedForRemoval,
            ItemStack installingStack,
            CyberwareSlot installingSlotType,
            int installingIndex
    ) {
        if (!(installingStack.getItem() instanceof ICyberwareItem installingItem)) {
            return false;
        }

        Set<TagKey<Item>> badTags =
                installingItem.sameSlotIncompatibleCyberwareTags(installingStack, installingSlotType);

        if (badTags == null || badTags.isEmpty()) {
            return false;
        }

        int currentInvIndex = RobosurgeonSlotMap.toInventoryIndex(installingSlotType, installingIndex);
        int mappedSize = RobosurgeonSlotMap.mappedSize(installingSlotType);

        for (int otherIndex = 0; otherIndex < mappedSize; otherIndex++) {
            int otherInvIndex = RobosurgeonSlotMap.toInventoryIndex(installingSlotType, otherIndex);

            if (otherInvIndex < 0 || otherInvIndex >= surgeon.inventory.getSlots()) {
                continue;
            }

            if (otherInvIndex == currentInvIndex) {
                continue;
            }

            if (markedForRemoval != null
                    && otherInvIndex < markedForRemoval.length
                    && markedForRemoval[otherInvIndex]) {
                continue;
            }

            ItemStack otherStack;

            boolean otherIsStaged =
                    staged != null
                            && otherInvIndex < staged.length
                            && staged[otherInvIndex];

            if (otherIsStaged) {
                otherStack = surgeon.inventory.getStackInSlot(otherInvIndex);
            } else {
                InstalledCyberware installed = data.get(installingSlotType, otherIndex);
                otherStack = installed != null && installed.getItem() != null
                        ? installed.getItem()
                        : ItemStack.EMPTY;
            }

            if (otherStack.isEmpty()) {
                continue;
            }

            for (TagKey<Item> tag : badTags) {
                if (tag != null && otherStack.is(tag)) {
                    return true;
                }
            }

            if (otherStack.getItem() instanceof ICyberwareItem otherCyberware) {
                Set<TagKey<Item>> otherBadTags =
                        otherCyberware.sameSlotIncompatibleCyberwareTags(otherStack, installingSlotType);

                if (otherBadTags != null) {
                    for (TagKey<Item> tag : otherBadTags) {
                        if (tag != null && installingStack.is(tag)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static boolean hasAnySameSlotIncompatibleCyberware(
            PlayerCyberwareData data,
            SurgeryTableBlockEntity surgeon,
            boolean[] staged,
            boolean[] markedForRemoval,
            ItemStack installingStack,
            CyberwareSlot installingSlotType,
            int installingIndex
    ) {
        if (!(installingStack.getItem() instanceof ICyberwareItem installingItem)) {
            return false;
        }

        Set<TagKey<Item>> badTags =
                installingItem.sameSlotIncompatibleCyberwareTags(installingStack, installingSlotType);

        if (badTags == null || badTags.isEmpty()) {
            return false;
        }

        int currentInvIndex = RobosurgeonSlotMap.toInventoryIndex(installingSlotType, installingIndex);
        int mappedSize = RobosurgeonSlotMap.mappedSize(installingSlotType);

        for (int otherIndex = 0; otherIndex < mappedSize; otherIndex++) {
            int otherInvIndex = RobosurgeonSlotMap.toInventoryIndex(installingSlotType, otherIndex);

            if (otherInvIndex < 0 || otherInvIndex >= surgeon.inventory.getSlots()) {
                continue;
            }

            if (otherInvIndex == currentInvIndex) {
                continue;
            }

            if (markedForRemoval != null
                    && otherInvIndex < markedForRemoval.length
                    && markedForRemoval[otherInvIndex]) {
                continue;
            }

            ItemStack otherStack;

            boolean otherIsStaged =
                    staged != null
                            && otherInvIndex < staged.length
                            && staged[otherInvIndex];

            if (otherIsStaged) {
                otherStack = surgeon.inventory.getStackInSlot(otherInvIndex);
            } else {
                InstalledCyberware installed = data.get(installingSlotType, otherIndex);
                otherStack = installed != null && installed.getItem() != null
                        ? installed.getItem()
                        : ItemStack.EMPTY;
            }

            if (otherStack.isEmpty()) {
                continue;
            }

            for (TagKey<Item> tag : badTags) {
                if (tag != null && otherStack.is(tag)) {
                    return true;
                }
            }

            if (otherStack.getItem() instanceof ICyberwareItem otherCyberware) {
                Set<TagKey<Item>> otherBadTags =
                        otherCyberware.sameSlotIncompatibleCyberwareTags(otherStack, installingSlotType);

                if (otherBadTags != null) {
                    for (TagKey<Item> tag : otherBadTags) {
                        if (tag != null && installingStack.is(tag)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static int countInstalledSameInSlotType(PlayerCyberwareData data, CyberwareSlot slotType, ItemStack needle) {
        if (needle == null || needle.isEmpty()) return 0;

        int count = 0;

        for (int i = 0; i < slotType.size; i++) {
            InstalledCyberware inst = data.get(slotType, i);
            if (inst == null || inst.getItem() == null || inst.getItem().isEmpty()) continue;

            if (inst.getItem().is(needle.getItem())) {
                count++;
            }
        }

        return count;
    }

    private static int countPlannedRemovalsSameInSlotType(PlayerCyberwareData data, CyberwareSlot slotType, RobosurgeonBlockEntity surgeon, boolean[] markedForRemoval, ItemStack needle) {
        if (markedForRemoval == null) return 0;
        if (needle == null || needle.isEmpty()) return 0;

        int count = 0;

        for (int i = 0; i < slotType.size; i++) {
            int invIndex = RobosurgeonSlotMap.toInventoryIndex(slotType, i);
            if (invIndex < 0 || invIndex >= surgeon.inventory.getSlots()) continue;
            if (invIndex >= markedForRemoval.length || !markedForRemoval[invIndex]) continue;

            InstalledCyberware inst = data.get(slotType, i);
            if (inst == null || inst.getItem() == null || inst.getItem().isEmpty()) continue;

            if (inst.getItem().is(needle.getItem())) {
                count++;
            }
        }

        return count;
    }

    private static int countPlannedRemovalsSameInSlotType(PlayerCyberwareData data, CyberwareSlot slotType, SurgeryTableBlockEntity surgeon, boolean[] markedForRemoval, ItemStack needle) {
        if (markedForRemoval == null) return 0;
        if (needle == null || needle.isEmpty()) return 0;

        int count = 0;

        for (int i = 0; i < slotType.size; i++) {
            int invIndex = RobosurgeonSlotMap.toInventoryIndex(slotType, i);
            if (invIndex < 0 || invIndex >= surgeon.inventory.getSlots()) continue;
            if (invIndex >= markedForRemoval.length || !markedForRemoval[invIndex]) continue;

            InstalledCyberware inst = data.get(slotType, i);
            if (inst == null || inst.getItem() == null || inst.getItem().isEmpty()) continue;

            if (inst.getItem().is(needle.getItem())) {
                count++;
            }
        }

        return count;
    }
}