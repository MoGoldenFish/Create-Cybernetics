package com.perigrine3.createcybernetics.block.entity;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.common.energy.ConditionalBlockPower;
import com.perigrine3.createcybernetics.common.energy.ExternalEnergyInputTracker;
import com.perigrine3.createcybernetics.common.energy.SyncingEnergyStorage;
import com.perigrine3.createcybernetics.common.surgery.DefaultOrgans;
import com.perigrine3.createcybernetics.common.surgery.RobosurgeonSlotMap;
import com.perigrine3.createcybernetics.screen.custom.surgery.robosurgeon.RobosurgeonMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class RobosurgeonBlockEntity extends BlockEntity implements MenuProvider, ExternalEnergyInputTracker {
    public final ItemStackHandler inventory = new ItemStackHandler(65) {

        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);

            ItemStack stack = getStackInSlot(slot);

            if (stack.isEmpty()) {
                staged[slot] = false;
                markedForRemoval[slot] = false;

                setChanged();
                updateComparatorOutput();
                return;
            }

            if (surgeryInProgress) {
                setChanged();
                updateComparatorOutput();
                return;
            }

            if (!installed[slot]) {
                staged[slot] = true;
                markedForRemoval[slot] = false;
            }

            setChanged();
            updateComparatorOutput();

            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(
                        getBlockPos(),
                        getBlockState(),
                        getBlockState(),
                        3
                );
            }
        }
    };

    private static final String TAG_ENERGY = "Energy";
    private static final int ENERGY_PULL_PER_TICK = 4_000;

    private final SyncingEnergyStorage energyStorage = new SyncingEnergyStorage(
            this,
            250_000,
            4_000,
            4_000
    );

    private boolean surgeryInProgress = false;

    public final boolean[] installed = new boolean[65];
    public final boolean[] staged = new boolean[65];
    public final boolean[] markedForRemoval = new boolean[65];

    public RobosurgeonBlockEntity(BlockPos pos, BlockState blockState) {
        super(
                ModBlockEntities.ROBOSURGEON_BLOCKENTITY.get(),
                pos,
                blockState
        );
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public SyncingEnergyStorage getMutableEnergyStorage() {
        return energyStorage;
    }

    @Override
    public void markExternalEnergyInput() {
        setChanged();
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            RobosurgeonBlockEntity blockEntity
    ) {
        if (level.isClientSide) {
            return;
        }

        if (!ConditionalBlockPower.shouldUseEnergyInsteadOfRedstone()) {
            return;
        }

        blockEntity.pullEnergyFromNeighbors();
    }

    private void pullEnergyFromNeighbors() {
        if (level == null || level.isClientSide) {
            return;
        }

        int remainingCapacity =
                energyStorage.getMaxEnergyStored()
                        - energyStorage.getEnergyStored();

        int remainingInput = Math.min(
                ENERGY_PULL_PER_TICK,
                remainingCapacity
        );

        if (remainingInput <= 0) {
            return;
        }

        for (Direction direction : Direction.values()) {
            if (remainingInput <= 0) {
                return;
            }

            BlockPos neighborPos = worldPosition.relative(direction);

            IEnergyStorage sidedNeighborEnergy = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK,
                    neighborPos,
                    direction.getOpposite()
            );

            int receivedFromSided = pullEnergyFromHandler(
                    sidedNeighborEnergy,
                    remainingInput
            );

            remainingInput -= receivedFromSided;

            if (remainingInput <= 0) {
                return;
            }

            IEnergyStorage unsidedNeighborEnergy = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK,
                    neighborPos,
                    null
            );

            if (unsidedNeighborEnergy == sidedNeighborEnergy) {
                continue;
            }

            int receivedFromUnsided = pullEnergyFromHandler(
                    unsidedNeighborEnergy,
                    remainingInput
            );

            remainingInput -= receivedFromUnsided;
        }
    }

    private int pullEnergyFromHandler(
            IEnergyStorage neighborEnergy,
            int maxAmount
    ) {
        if (neighborEnergy == null) {
            return 0;
        }

        if (maxAmount <= 0) {
            return 0;
        }

        if (!neighborEnergy.canExtract()) {
            return 0;
        }

        if (!energyStorage.canReceive()) {
            return 0;
        }

        int simulatedExtract = neighborEnergy.extractEnergy(
                maxAmount,
                true
        );

        if (simulatedExtract <= 0) {
            return 0;
        }

        int simulatedReceive = energyStorage.receiveEnergy(
                simulatedExtract,
                true
        );

        if (simulatedReceive <= 0) {
            return 0;
        }

        int extracted = neighborEnergy.extractEnergy(
                simulatedReceive,
                false
        );

        if (extracted <= 0) {
            return 0;
        }

        int received = energyStorage.receiveEnergy(
                extracted,
                false
        );

        if (received < extracted && neighborEnergy.canReceive()) {
            neighborEnergy.receiveEnergy(
                    extracted - received,
                    false
            );
        }

        return received;
    }

    public boolean isInstalled(int index) {
        return index >= 0
                && index < installed.length
                && installed[index];
    }

    public boolean isStaged(int index) {
        return index >= 0
                && index < staged.length
                && staged[index];
    }

    public boolean isMarkedForRemoval(int index) {
        return index >= 0
                && index < markedForRemoval.length
                && markedForRemoval[index];
    }

    public void setInstalled(int index, boolean value) {
        if (index < 0 || index >= installed.length) {
            return;
        }

        installed[index] = value;

        if (!value) {
            markedForRemoval[index] = false;
        }

        setChanged();
        updateComparatorOutput();
    }

    public void setStaged(int index, boolean value) {
        if (index < 0 || index >= staged.length) {
            return;
        }

        staged[index] = value;

        if (!value) {
            markedForRemoval[index] = false;
        }

        setChanged();
        updateComparatorOutput();
    }

    public void toggleMarkedForRemoval(int index) {
        if (index < 0 || index >= markedForRemoval.length) {
            return;
        }

        if (!installed[index]) {
            return;
        }

        markedForRemoval[index] = !markedForRemoval[index];

        setChanged();
        updateComparatorOutput();
    }

    public void clearSlotStates() {
        for (int index = 0; index < inventory.getSlots(); index++) {
            staged[index] = false;
            markedForRemoval[index] = false;
        }

        setChanged();
        updateComparatorOutput();
    }

    public void beginSurgery() {
        surgeryInProgress = true;
        setChanged();
    }

    public void endSurgery() {
        surgeryInProgress = false;
        setChanged();
    }

    public void clearContents() {
        for (int index = 0; index < inventory.getSlots(); index++) {
            inventory.setStackInSlot(
                    index,
                    ItemStack.EMPTY
            );
        }
    }

    public void drops() {
        if (level == null || level.isClientSide) {
            return;
        }

        for (int index = 0; index < inventory.getSlots(); index++) {
            if (!staged[index]) {
                continue;
            }

            ItemStack stack = inventory.getStackInSlot(index);

            if (stack.isEmpty()) {
                continue;
            }

            Containers.dropItemStack(
                    level,
                    worldPosition.getX(),
                    worldPosition.getY(),
                    worldPosition.getZ(),
                    stack
            );

            inventory.setStackInSlot(
                    index,
                    ItemStack.EMPTY
            );

            staged[index] = false;
        }

        setChanged();
        updateComparatorOutput();
    }

    public int getComparatorOutput() {
        return Math.min(
                15,
                countNonDefaultImplants()
        );
    }

    private int countNonDefaultImplants() {
        int count = 0;

        for (CyberwareSlot slot : CyberwareSlot.values()) {
            int mappedSize = RobosurgeonSlotMap.mappedSize(slot);

            for (int slotIndex = 0; slotIndex < mappedSize; slotIndex++) {
                int inventoryIndex = RobosurgeonSlotMap.toInventoryIndex(
                        slot,
                        slotIndex
                );

                if (inventoryIndex < 0
                        || inventoryIndex >= inventory.getSlots()) {
                    continue;
                }

                ItemStack stack =
                        inventory.getStackInSlot(inventoryIndex);

                if (stack.isEmpty()) {
                    continue;
                }

                ItemStack defaultStack =
                        DefaultOrgans.get(slot, slotIndex);

                if (defaultStack == null || defaultStack.isEmpty()) {
                    count++;
                    continue;
                }

                if (!ItemStack.isSameItemSameComponents(
                        stack,
                        defaultStack
                )) {
                    count++;
                }
            }
        }

        return count;
    }

    private void updateComparatorOutput() {
        if (level == null || level.isClientSide) {
            return;
        }

        level.updateNeighbourForOutputSignal(
                worldPosition,
                getBlockState().getBlock()
        );
    }

    private static byte[] encode(boolean[] data) {
        byte[] encoded = new byte[data.length];

        for (int index = 0; index < data.length; index++) {
            encoded[index] = (byte) (data[index] ? 1 : 0);
        }

        return encoded;
    }

    private static void decode(
            byte[] encoded,
            boolean[] target
    ) {
        int length = Math.min(
                encoded.length,
                target.length
        );

        for (int index = 0; index < length; index++) {
            target[index] = encoded[index] != 0;
        }
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(tag, registries);

        tag.put(
                "inventory",
                inventory.serializeNBT(registries)
        );

        tag.putByteArray(
                "Installed",
                encode(installed)
        );

        tag.putByteArray(
                "Staged",
                encode(staged)
        );

        tag.putByteArray(
                "Marked",
                encode(markedForRemoval)
        );

        tag.putInt(
                TAG_ENERGY,
                energyStorage.getEnergyStored()
        );
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(tag, registries);

        if (tag.contains("inventory")) {
            inventory.deserializeNBT(
                    registries,
                    tag.getCompound("inventory")
            );
        }

        if (tag.contains("Installed")) {
            decode(
                    tag.getByteArray("Installed"),
                    installed
            );
        }

        if (tag.contains("Staged")) {
            decode(
                    tag.getByteArray("Staged"),
                    staged
            );
        }

        if (tag.contains("Marked")) {
            decode(
                    tag.getByteArray("Marked"),
                    markedForRemoval
            );
        }

        if (tag.contains(TAG_ENERGY)) {
            energyStorage.setEnergyStoredSilently(
                    tag.getInt(TAG_ENERGY)
            );
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "gui.robosurgeon.title"
        );
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int containerId,
            Inventory inventory,
            Player player
    ) {
        return new RobosurgeonMenu(
                containerId,
                inventory,
                this
        );
    }

    public AbstractContainerMenu getMenu(
            int containerId,
            Inventory inventory,
            Player player
    ) {
        return new RobosurgeonMenu(
                containerId,
                inventory,
                this
        );
    }
}