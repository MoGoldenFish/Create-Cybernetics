package com.perigrine3.createcybernetics.network.handler;

import com.perigrine3.createcybernetics.block.ComputerBlock;
import com.perigrine3.createcybernetics.block.entity.ComputerBlockEntity;
import com.perigrine3.createcybernetics.common.computer.ChatSpaceSavedData;
import com.perigrine3.createcybernetics.network.payload.ChatSpaceAddContactPayload;
import com.perigrine3.createcybernetics.network.payload.ChatSpaceMarkReadPayload;
import com.perigrine3.createcybernetics.network.payload.ChatSpaceRequestSyncPayload;
import com.perigrine3.createcybernetics.network.payload.ChatSpaceSendMessagePayload;
import com.perigrine3.createcybernetics.network.payload.ChatSpaceSyncPayload;
import com.perigrine3.createcybernetics.screen.custom.computer.ComputerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ChatSpacePayloadHandler {
    private ChatSpacePayloadHandler() {
    }

    public static void handleRequestSync(
            ChatSpaceRequestSyncPayload payload,
            ServerPlayer player
    ) {
        ComputerBlockEntity computer =
                getValidComputer(
                        player,
                        payload.computerPos()
                );

        if (computer == null) {
            return;
        }

        sendSnapshot(
                player,
                computer
        );
    }

    public static void handleAddContact(
            ChatSpaceAddContactPayload payload,
            ServerPlayer player
    ) {
        ComputerBlockEntity computer =
                getValidComputer(
                        player,
                        payload.computerPos()
                );

        if (computer == null) {
            return;
        }

        ChatSpaceSavedData data =
                ChatSpaceSavedData.get(
                        player.server
                );

        data.addContact(
                computer.getComputerCode(),
                payload.remoteCode(),
                payload.displayName()
        );

        sendSnapshot(
                player,
                computer
        );
    }

    public static void handleSendMessage(
            ChatSpaceSendMessagePayload payload,
            ServerPlayer player
    ) {
        ComputerBlockEntity computer =
                getValidComputer(
                        player,
                        payload.computerPos()
                );

        if (computer == null) {
            return;
        }

        ChatSpaceSavedData data =
                ChatSpaceSavedData.get(
                        player.server
                );

        data.sendMessage(
                computer.getComputerCode(),
                payload.remoteCode(),
                payload.message(),
                System.currentTimeMillis()
        );

        sendSnapshot(
                player,
                computer
        );
    }

    public static void handleMarkRead(
            ChatSpaceMarkReadPayload payload,
            ServerPlayer player
    ) {
        ComputerBlockEntity computer =
                getValidComputer(
                        player,
                        payload.computerPos()
                );

        if (computer == null) {
            return;
        }

        ChatSpaceSavedData data =
                ChatSpaceSavedData.get(
                        player.server
                );

        data.markRead(
                computer.getComputerCode(),
                payload.remoteCode()
        );

        sendSnapshot(
                player,
                computer
        );
    }

    private static void sendSnapshot(
            ServerPlayer player,
            ComputerBlockEntity computer
    ) {
        ChatSpaceSavedData data =
                ChatSpaceSavedData.get(
                        player.server
                );

        PacketDistributor.sendToPlayer(
                player,
                new ChatSpaceSyncPayload(
                        data.createClientSnapshot(
                                computer.getComputerCode()
                        )
                )
        );
    }

    private static ComputerBlockEntity getValidComputer(
            ServerPlayer player,
            BlockPos requestedPos
    ) {
        if (!(player.containerMenu
                instanceof ComputerMenu menu)) {
            return null;
        }

        if (!menu.getComputerPos().equals(
                requestedPos
        )) {
            return null;
        }

        if (player.distanceToSqr(
                requestedPos.getX() + 0.5D,
                requestedPos.getY() + 0.5D,
                requestedPos.getZ() + 0.5D
        ) > 64.0D) {
            return null;
        }

        BlockState state =
                player.level().getBlockState(
                        requestedPos
                );

        if (!(state.getBlock()
                instanceof ComputerBlock)) {
            return null;
        }

        if (!state.getValue(
                ComputerBlock.POWERED
        )) {
            return null;
        }

        BlockEntity blockEntity =
                player.level().getBlockEntity(
                        requestedPos
                );

        if (!(blockEntity
                instanceof ComputerBlockEntity computer)) {
            return null;
        }

        if (computer.getComputerCode().isBlank()) {
            return null;
        }

        return computer;
    }
}