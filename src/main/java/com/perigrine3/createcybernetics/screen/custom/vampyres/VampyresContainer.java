package com.perigrine3.createcybernetics.screen.custom.vampyres;

import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.cyberware.lungs.VampyresItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class VampyresContainer implements Container {
    private final ServerPlayer player;
    private final PlayerCyberwareData data;

    public VampyresContainer(ServerPlayer player) {
        this.player = player;
        this.data = player.getData(ModAttachments.CYBERWARE);
    }

    @Override
    public int getContainerSize() {
        return VampyresItem.SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            if (!getItem(slot).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (data == null) return ItemStack.EMPTY;
        return data.getVampyresStack(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (data == null) return ItemStack.EMPTY;

        ItemStack removed = data.removeVampyresStack(slot, amount);
        sync();

        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (data == null) return ItemStack.EMPTY;

        ItemStack current = data.getVampyresStack(slot);
        if (current.isEmpty()) return ItemStack.EMPTY;

        ItemStack removed = current.copy();
        data.setVampyresStack(slot, ItemStack.EMPTY);
        sync();

        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (data == null) return;

        data.setVampyresStack(slot, stack);
        sync();
    }

    @Override
    public void setChanged() {
        if (data == null) return;

        data.setDirty();
        sync();
    }

    @Override
    public boolean stillValid(Player player) {
        return player == this.player && VampyresItem.isInstalled(player);
    }

    @Override
    public void clearContent() {
        if (data == null) return;

        data.clearVampyresInventory();
        sync();
    }

    @Override
    public int getMaxStackSize() {
        return VampyresItem.SLOT_STACK_LIMIT;
    }

    private void sync() {
        player.syncData(ModAttachments.CYBERWARE);
    }
}