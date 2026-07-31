package com.perigrine3.createcybernetics.screen.custom.vampyres;

import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.cyberware.lungs.VampyresItem;
import com.perigrine3.createcybernetics.screen.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class VampyresMenu extends AbstractContainerMenu {
    public static final int VAMPYRES_SLOT_START = 0;
    public static final int VAMPYRES_SLOT_END = 2;

    public static final int PLAYER_INVENTORY_START = 2;
    public static final int PLAYER_INVENTORY_END = 29;

    public static final int HOTBAR_START = 29;
    public static final int HOTBAR_END = 38;

    public static final int VAMPYRES_SLOT_0_X = 71;
    public static final int VAMPYRES_SLOT_0_Y = 31;

    public static final int VAMPYRES_SLOT_1_X = 89;
    public static final int VAMPYRES_SLOT_1_Y = 31;

    private final Player owner;
    private final Container vampyresInventory;

    private final int[] vampyresCounts = new int[VampyresItem.SLOT_COUNT];

    public VampyresMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf ignored) {
        this(containerId, playerInventory);
    }

    public VampyresMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.VAMPYRES_MENU.get(), containerId);

        this.owner = playerInventory.player;

        this.vampyresInventory = new SimpleContainer(VampyresItem.SLOT_COUNT) {
            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        };

        for (int i = 0; i < VampyresItem.SLOT_COUNT; i++) {
            final int slot = i;

            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return vampyresCounts[slot];
                }

                @Override
                public void set(int value) {
                    vampyresCounts[slot] = value;
                }
            });
        }

        if (owner instanceof ServerPlayer player) {
            loadFromPlayerData(player);
        }

        addSlot(new VampyresPotionSlot(vampyresInventory, 0, VAMPYRES_SLOT_0_X, VAMPYRES_SLOT_0_Y));
        addSlot(new VampyresPotionSlot(vampyresInventory, 1, VAMPYRES_SLOT_1_X, VAMPYRES_SLOT_1_Y));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    public int getVampyresDisplayCount(int slot) {
        if (slot < 0 || slot >= vampyresCounts.length) return 0;
        return vampyresCounts[slot];
    }

    private void loadFromPlayerData(ServerPlayer player) {
        if (!player.hasData(ModAttachments.CYBERWARE)) return;

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
        if (data == null) return;

        for (int slot = 0; slot < VampyresItem.SLOT_COUNT; slot++) {
            ItemStack stored = data.getVampyresStack(slot);

            if (stored == null || stored.isEmpty() || !VampyresItem.isInjectable(stored)) {
                vampyresInventory.setItem(slot, ItemStack.EMPTY);
                vampyresCounts[slot] = 0;
                continue;
            }

            ItemStack representative = stored.copy();
            representative.setCount(1);

            vampyresInventory.setItem(slot, representative);
            vampyresCounts[slot] = Math.min(VampyresItem.SLOT_STACK_LIMIT, stored.getCount());
        }
    }

    private void sanitizeVampyresInventory() {
        for (int slot = 0; slot < VampyresItem.SLOT_COUNT; slot++) {
            ItemStack stack = vampyresInventory.getItem(slot);

            if (stack == null || stack.isEmpty()) {
                vampyresInventory.setItem(slot, ItemStack.EMPTY);
                vampyresCounts[slot] = 0;
                continue;
            }

            if (!VampyresItem.isInjectable(stack)) {
                vampyresInventory.setItem(slot, ItemStack.EMPTY);
                vampyresCounts[slot] = 0;
                continue;
            }

            stack.setCount(1);

            int count = vampyresCounts[slot];

            if (count <= 0) {
                count = 1;
            }

            vampyresCounts[slot] = Math.min(VampyresItem.SLOT_STACK_LIMIT, count);
        }
    }

    private void saveToPlayerData(ServerPlayer player) {
        if (!player.hasData(ModAttachments.CYBERWARE)) return;

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);
        if (data == null) return;

        boolean changed = false;

        for (int slot = 0; slot < VampyresItem.SLOT_COUNT; slot++) {
            ItemStack representative = vampyresInventory.getItem(slot);
            int count = vampyresCounts[slot];

            ItemStack desired;

            if (representative == null || representative.isEmpty() || count <= 0) {
                desired = ItemStack.EMPTY;
            } else {
                desired = representative.copy();
                desired.setCount(Math.min(VampyresItem.SLOT_STACK_LIMIT, count));
            }

            ItemStack current = data.getVampyresStack(slot);

            boolean same = current.isEmpty() && desired.isEmpty();

            if (!same && !current.isEmpty() && !desired.isEmpty()) {
                same = ItemStack.isSameItemSameComponents(current, desired)
                        && current.getCount() == desired.getCount();
            }

            if (same) continue;

            data.setVampyresStack(slot, desired);
            changed = true;
        }

        if (changed) {
            data.setDirty();
            player.syncData(ModAttachments.CYBERWARE);
        }
    }

    @Override
    public void broadcastChanges() {
        if (owner instanceof ServerPlayer player) {
            sanitizeVampyresInventory();
            saveToPlayerData(player);
        }

        super.broadcastChanges();
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);

        if (container == vampyresInventory && owner instanceof ServerPlayer) {
            broadcastChanges();
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (player.level().isClientSide) {
            super.clicked(slotId, button, clickType, player);
            return;
        }

        if (clickType == ClickType.PICKUP && slotId >= VAMPYRES_SLOT_START && slotId < VAMPYRES_SLOT_END) {
            Slot targetSlot = slots.get(slotId);

            ItemStack carried = getCarried();
            ItemStack stored = targetSlot.getItem();

            if (carried.isEmpty()) {
                if (stored.isEmpty()) return;

                int storedCount = vampyresCounts[slotId];

                if (storedCount <= 0) {
                    storedCount = 1;
                }

                ItemStack removed = stored.copy();
                removed.setCount(1);

                setCarried(removed);

                storedCount--;
                vampyresCounts[slotId] = storedCount;

                if (storedCount <= 0) {
                    vampyresCounts[slotId] = 0;
                    targetSlot.set(ItemStack.EMPTY);
                }

                targetSlot.setChanged();
                broadcastChanges();
                return;
            }

            if (!VampyresItem.isInjectable(carried)) {
                return;
            }

            if (stored.isEmpty()) {
                ItemStack representative = carried.copy();
                representative.setCount(1);

                targetSlot.set(representative);
                vampyresCounts[slotId] = 1;

                carried.shrink(1);
                setCarried(carried);

                targetSlot.setChanged();
                broadcastChanges();
                return;
            }

            if (!ItemStack.isSameItemSameComponents(stored, carried)) {
                return;
            }

            int storedCount = vampyresCounts[slotId];

            if (storedCount <= 0) {
                storedCount = 1;
            }

            if (storedCount >= VampyresItem.SLOT_STACK_LIMIT) {
                return;
            }

            vampyresCounts[slotId] = storedCount + 1;

            carried.shrink(1);
            setCarried(carried);

            targetSlot.setChanged();
            broadcastChanges();
            return;
        }

        super.clicked(slotId, button, clickType, player);
    }

    private boolean movePotionIntoVampyres(ItemStack source) {
        if (source.isEmpty()) return false;
        if (!VampyresItem.isInjectable(source)) return false;

        for (int slot = 0; slot < VampyresItem.SLOT_COUNT; slot++) {
            ItemStack stored = vampyresInventory.getItem(slot);

            if (stored.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(stored, source)) continue;

            int storedCount = vampyresCounts[slot];

            if (storedCount <= 0) {
                storedCount = 1;
            }

            if (storedCount >= VampyresItem.SLOT_STACK_LIMIT) continue;

            vampyresCounts[slot] = storedCount + 1;
            source.shrink(1);

            slots.get(slot).setChanged();
            broadcastChanges();
            return true;
        }

        for (int slot = 0; slot < VampyresItem.SLOT_COUNT; slot++) {
            ItemStack stored = vampyresInventory.getItem(slot);

            if (!stored.isEmpty()) continue;

            ItemStack representative = source.copy();
            representative.setCount(1);

            slots.get(slot).set(representative);
            vampyresCounts[slot] = 1;

            source.shrink(1);

            slots.get(slot).setChanged();
            broadcastChanges();
            return true;
        }

        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (player.level().isClientSide) return ItemStack.EMPTY;
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;

        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        if (index >= VAMPYRES_SLOT_START && index < VAMPYRES_SLOT_END) {
            ItemStack stored = slot.getItem();

            int storedCount = vampyresCounts[index];

            if (storedCount <= 0) {
                storedCount = 1;
            }

            int moved = 0;

            for (int i = 0; i < storedCount; i++) {
                ItemStack one = stored.copy();
                one.setCount(1);

                if (!moveItemStackTo(one, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                    break;
                }

                moved++;
            }

            if (moved <= 0) return ItemStack.EMPTY;

            vampyresCounts[index] = storedCount - moved;

            if (vampyresCounts[index] <= 0) {
                vampyresCounts[index] = 0;
                slot.set(ItemStack.EMPTY);
            }

            slot.setChanged();
            broadcastChanges();

            ItemStack result = stored.copy();
            result.setCount(moved);
            return result;
        }

        ItemStack source = slot.getItem();

        if (!VampyresItem.isInjectable(source)) {
            return ItemStack.EMPTY;
        }

        ItemStack original = source.copy();

        if (!movePotionIntoVampyres(source)) {
            return ItemStack.EMPTY;
        }

        if (source.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        broadcastChanges();
        return original;
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inventory) {
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    @Override
    public void removed(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            sanitizeVampyresInventory();
            saveToPlayerData(serverPlayer);
        }

        super.removed(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return VampyresItem.isInstalled(player);
    }
}