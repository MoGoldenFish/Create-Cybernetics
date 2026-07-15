package com.perigrine3.createcybernetics.block.entity;

import com.perigrine3.createcybernetics.common.energy.SyncingEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

public class ChargingBlockEntity extends BlockEntity {
    private static final String TAG_ENERGY = "Energy";

    private final SyncingEnergyStorage energyStorage = new SyncingEnergyStorage(
            this,
            100_000,
            2_000,
            2_000
    );

    public ChargingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGING_BLOCK.get(), pos, state);
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public SyncingEnergyStorage getMutableEnergyStorage() {
        return energyStorage;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt(TAG_ENERGY, energyStorage.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);

        if (tag.contains(TAG_ENERGY)) {
            energyStorage.setEnergyStoredSilently(tag.getInt(TAG_ENERGY));
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }
}