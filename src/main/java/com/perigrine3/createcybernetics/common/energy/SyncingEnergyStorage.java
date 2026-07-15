package com.perigrine3.createcybernetics.common.energy;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.energy.EnergyStorage;

public class SyncingEnergyStorage extends EnergyStorage {
    private final BlockEntity owner;

    public SyncingEnergyStorage(BlockEntity owner, int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
        this.owner = owner;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = super.receiveEnergy(maxReceive, simulate);

        if (received > 0 && !simulate) {
            if (owner instanceof ExternalEnergyInputTracker tracker) {
                tracker.markExternalEnergyInput();
            }

            changed();
        }

        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = super.extractEnergy(maxExtract, simulate);

        if (extracted > 0 && !simulate) {
            changed();
        }

        return extracted;
    }

    public boolean consumeEnergyInternally(int amount) {
        if (amount <= 0) {
            return true;
        }

        if (energy < amount) {
            return false;
        }

        energy -= amount;
        owner.setChanged();

        return true;
    }

    public void setEnergyStored(int energy) {
        int clampedEnergy = Math.max(
                0,
                Math.min(energy, getMaxEnergyStored())
        );

        if (this.energy == clampedEnergy) {
            return;
        }

        this.energy = clampedEnergy;
        changed();
    }

    public void setEnergyStoredSilently(int energy) {
        this.energy = Math.max(
                0,
                Math.min(energy, getMaxEnergyStored())
        );
    }

    private void changed() {
        owner.setChanged();

        if (owner.getLevel() != null && !owner.getLevel().isClientSide) {
            owner.getLevel().sendBlockUpdated(
                    owner.getBlockPos(),
                    owner.getBlockState(),
                    owner.getBlockState(),
                    3
            );
        }
    }
}