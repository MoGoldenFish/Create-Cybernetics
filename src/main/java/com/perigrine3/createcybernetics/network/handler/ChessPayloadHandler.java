package com.perigrine3.createcybernetics.network.handler;

import com.perigrine3.createcybernetics.block.ComputerBlock;
import com.perigrine3.createcybernetics.block.entity.ComputerBlockEntity;
import com.perigrine3.createcybernetics.common.computer.chess.ChessSavedData;
import com.perigrine3.createcybernetics.network.payload.ChessCreateInvitePayload;
import com.perigrine3.createcybernetics.network.payload.ChessMakeMovePayload;
import com.perigrine3.createcybernetics.network.payload.ChessRequestSyncPayload;
import com.perigrine3.createcybernetics.network.payload.ChessRespondInvitePayload;
import com.perigrine3.createcybernetics.network.payload.ChessResignPayload;
import com.perigrine3.createcybernetics.network.payload.ChessSyncPayload;
import com.perigrine3.createcybernetics.screen.custom.computer.ComputerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ChessPayloadHandler {
    private ChessPayloadHandler() {
    }

    public static void handleRequestSync(
            ChessRequestSyncPayload payload,
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

    public static void handleCreateInvite(
            ChessCreateInvitePayload payload,
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

        ChessSavedData data =
                ChessSavedData.get(
                        player.server
                );

        data.createInvite(
                player.server,
                computer.getComputerCode(),
                payload.receiverCode()
        );

        sendSnapshot(
                player,
                computer
        );
    }

    public static void handleRespondInvite(
            ChessRespondInvitePayload payload,
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

        ChessSavedData data =
                ChessSavedData.get(
                        player.server
                );

        data.respondToInvite(
                player.server,
                payload.inviteId(),
                computer.getComputerCode(),
                payload.accepted()
        );

        sendSnapshot(
                player,
                computer
        );
    }

    public static void handleMakeMove(
            ChessMakeMovePayload payload,
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

        ChessSavedData data =
                ChessSavedData.get(
                        player.server
                );

        data.makeMove(
                payload.sessionId(),
                computer.getComputerCode(),
                payload.toMove()
        );

        sendSnapshot(
                player,
                computer
        );
    }

    public static void handleResign(
            ChessResignPayload payload,
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

        ChessSavedData data =
                ChessSavedData.get(
                        player.server
                );

        data.resign(
                payload.sessionId(),
                computer.getComputerCode()
        );

        sendSnapshot(
                player,
                computer
        );
    }

    public static void sendSnapshot(
            ServerPlayer player,
            ComputerBlockEntity computer
    ) {
        ChessSavedData data =
                ChessSavedData.get(
                        player.server
                );

        PacketDistributor.sendToPlayer(
                player,
                new ChessSyncPayload(
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

        if (computer.getComputerCode()
                .isBlank()) {
            return null;
        }

        return computer;
    }
}