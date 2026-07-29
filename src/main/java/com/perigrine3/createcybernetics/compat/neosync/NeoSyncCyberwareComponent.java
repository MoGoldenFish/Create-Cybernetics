package com.perigrine3.createcybernetics.compat.neosync;

import com.breakinblocks.neosync.api.shell.ShellStateComponent;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.util.CyberwareAttributeHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class NeoSyncCyberwareComponent extends ShellStateComponent {

    public static final String ID = "createcybernetics:cyberware";
    private static final String NBT_CYBERWARE_DATA = "CyberwareData";

    private final @Nullable ServerPlayer player;
    private CompoundTag cyberwareData = new CompoundTag();

    public NeoSyncCyberwareComponent(@Nullable ServerPlayer player) {
        this.player = player;

        if (player != null) {
            PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
            cyberwareData = data.serializeNBT(player.registryAccess());
        }
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void clone(ShellStateComponent component) {
        NeoSyncCyberwareComponent source = component.as(NeoSyncCyberwareComponent.class);

        if (source == null) {
            return;
        }

        cyberwareData = source.cyberwareData.copy();

        if (player != null) {
            applyToPlayer(player);
        }
    }

    @Override
    protected void readComponentNbt(CompoundTag nbt) {
        if (nbt.contains(NBT_CYBERWARE_DATA, Tag.TAG_COMPOUND)) {
            cyberwareData = nbt.getCompound(NBT_CYBERWARE_DATA).copy();
        } else {
            cyberwareData = new CompoundTag();
        }
    }

    @Override
    protected CompoundTag writeComponentNbt(CompoundTag nbt) {
        nbt.put(NBT_CYBERWARE_DATA, cyberwareData.copy());
        return nbt;
    }

    public void initializeDefaultOrgans(HolderLookup.Provider provider) {
        PlayerCyberwareData data = new PlayerCyberwareData();
        data.resetToDefaultOrgans();
        cyberwareData = data.serializeNBT(provider);
    }

    public PlayerCyberwareData createVisualData(HolderLookup.Provider provider) {
        PlayerCyberwareData data = new PlayerCyberwareData();

        if (!cyberwareData.isEmpty()) {
            data.deserializeNBT(cyberwareData.copy(), provider);
        } else {
            data.resetToDefaultOrgans();
        }

        return data;
    }

    private void applyToPlayer(ServerPlayer player) {
        CyberwareAttributeHelper.removeAllRegisteredModifiers(player);

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);

        if (cyberwareData.isEmpty()) {
            data.clear();
            data.resetToDefaultOrgans();
        } else {
            data.deserializeNBT(cyberwareData.copy(), player.registryAccess());
        }

        reapplyInstalledCyberware(player, data);

        data.recomputeHumanityBaseFromInstalled(player);
        data.setDirty();
        player.syncData(ModAttachments.CYBERWARE);
        player.refreshDimensions();
    }

    private static void reapplyInstalledCyberware(ServerPlayer player, PlayerCyberwareData data) {
        for (CyberwareSlot slot : CyberwareSlot.values()) {
            InstalledCyberware[] installedCyberware = data.getAll().get(slot);
            if (installedCyberware == null) {
                continue;
            }

            for (InstalledCyberware installed : installedCyberware) {
                if (installed == null) {
                    continue;
                }

                ItemStack stack = installed.getItem();
                if (stack == null || stack.isEmpty()) {
                    continue;
                }

                if (stack.getItem() instanceof ICyberwareItem cyberwareItem) {
                    cyberwareItem.onInstalled(player, stack);
                }
            }
        }
    }
}