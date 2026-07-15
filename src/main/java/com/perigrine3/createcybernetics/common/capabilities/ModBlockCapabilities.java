package com.perigrine3.createcybernetics.common.capabilities;

import com.perigrine3.createcybernetics.block.entity.ChargingBlockEntity;
import com.perigrine3.createcybernetics.block.entity.ComputerBlockEntity;
import com.perigrine3.createcybernetics.block.entity.ModBlockEntities;
import com.perigrine3.createcybernetics.block.entity.RobosurgeonBlockEntity;
import com.perigrine3.createcybernetics.common.energy.ConditionalBlockPower;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class ModBlockCapabilities {
    private ModBlockCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.CHARGING_BLOCK.get(),
                ModBlockCapabilities::getChargingBlockEnergy
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.ROBOSURGEON_BLOCKENTITY.get(),
                ModBlockCapabilities::getRobosurgeonEnergy
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.COMPUTER.get(),
                ModBlockCapabilities::getComputerEnergy
        );
    }

    private static IEnergyStorage getChargingBlockEnergy(
            ChargingBlockEntity blockEntity,
            Direction side
    ) {
        if (!ConditionalBlockPower.shouldUseEnergyInsteadOfRedstone()) {
            return null;
        }

        return blockEntity.getEnergyStorage();
    }

    private static IEnergyStorage getRobosurgeonEnergy(
            RobosurgeonBlockEntity blockEntity,
            Direction side
    ) {
        if (!ConditionalBlockPower.shouldUseEnergyInsteadOfRedstone()) {
            return null;
        }

        return blockEntity.getEnergyStorage();
    }

    private static IEnergyStorage getComputerEnergy(
            ComputerBlockEntity blockEntity,
            Direction side
    ) {
        if (!ConditionalBlockPower.shouldUseEnergyInsteadOfRedstone()) {
            return null;
        }

        return blockEntity.getEnergyStorage();
    }
}