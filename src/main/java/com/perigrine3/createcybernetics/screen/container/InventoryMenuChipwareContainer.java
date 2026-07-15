package com.perigrine3.createcybernetics.screen.container;

import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class InventoryMenuChipwareContainer implements Container {

    private final Player player;

    public InventoryMenuChipwareContainer(Player player) {
        this.player = player;
    }

    private boolean suppressForCreativeMenuSync() {
        return player != null
                && player.getAbilities() != null
                && player.getAbilities().instabuild;
    }

    private PlayerCyberwareData data() {
        if (player == null) return null;
        if (!player.hasData(ModAttachments.CYBERWARE)) return null;
        return player.getData(ModAttachments.CYBERWARE);
    }

    @Override
    public int getContainerSize() {
        return PlayerCyberwareData.CHIPWARE_SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        if (suppressForCreativeMenuSync()) return true;

        PlayerCyberwareData d = data();
        if (d == null) return true;

        for (int i = 0; i < getContainerSize(); i++) {
            if (!d.getChipwareStack(i).isEmpty()) return false;
        }

        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (suppressForCreativeMenuSync()) {
            return ItemStack.EMPTY;
        }

        PlayerCyberwareData d = data();
        if (d == null) return ItemStack.EMPTY;

        return d.getChipwareStack(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (suppressForCreativeMenuSync()) {
            return ItemStack.EMPTY;
        }

        PlayerCyberwareData d = data();
        if (d == null) return ItemStack.EMPTY;

        ItemStack current = d.getChipwareStack(slot);
        if (current.isEmpty()) return ItemStack.EMPTY;

        d.setChipwareStack(slot, ItemStack.EMPTY);
        setChanged();

        return current;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (suppressForCreativeMenuSync()) {
            return ItemStack.EMPTY;
        }

        PlayerCyberwareData d = data();
        if (d == null) return ItemStack.EMPTY;

        ItemStack current = d.getChipwareStack(slot);
        if (!current.isEmpty()) {
            d.setChipwareStack(slot, ItemStack.EMPTY);
        }

        return current;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (suppressForCreativeMenuSync()) {
            return;
        }

        PlayerCyberwareData d = data();
        if (d == null) return;

        d.setChipwareStack(slot, stack);
        setChanged();
    }

    @Override
    public void setChanged() {
        if (suppressForCreativeMenuSync()) {
            return;
        }

        PlayerCyberwareData d = data();
        if (d == null) return;

        d.setDirty();

        if (!player.level().isClientSide) {
            player.setData(ModAttachments.CYBERWARE, d);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        if (suppressForCreativeMenuSync()) {
            return;
        }

        PlayerCyberwareData d = data();
        if (d == null) return;

        d.clearChipwareInventory();
        setChanged();
    }
}