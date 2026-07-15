package com.perigrine3.createcybernetics.network;

import com.perigrine3.createcybernetics.network.handler.ChessPayloadHandler;
import com.perigrine3.createcybernetics.network.payload.ChessCreateInvitePayload;
import com.perigrine3.createcybernetics.network.payload.ChessMakeMovePayload;
import com.perigrine3.createcybernetics.network.payload.ChessRequestSyncPayload;
import com.perigrine3.createcybernetics.network.payload.ChessRespondInvitePayload;
import com.perigrine3.createcybernetics.network.payload.ChessResignPayload;
import com.perigrine3.createcybernetics.network.payload.ChessSyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ChessPayloads {
    private ChessPayloads() {
    }

    public static void register(
            PayloadRegistrar registrar
    ) {
        registrar.playToServer(
                ChessRequestSyncPayload.TYPE,
                ChessRequestSyncPayload.STREAM_CODEC,
                (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player()
                                    instanceof ServerPlayer player) {
                                ChessPayloadHandler.handleRequestSync(
                                        payload,
                                        player
                                );
                            }
                        })
        );

        registrar.playToServer(
                ChessCreateInvitePayload.TYPE,
                ChessCreateInvitePayload.STREAM_CODEC,
                (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player()
                                    instanceof ServerPlayer player) {
                                ChessPayloadHandler.handleCreateInvite(
                                        payload,
                                        player
                                );
                            }
                        })
        );

        registrar.playToServer(
                ChessRespondInvitePayload.TYPE,
                ChessRespondInvitePayload.STREAM_CODEC,
                (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player()
                                    instanceof ServerPlayer player) {
                                ChessPayloadHandler.handleRespondInvite(
                                        payload,
                                        player
                                );
                            }
                        })
        );

        registrar.playToServer(
                ChessMakeMovePayload.TYPE,
                ChessMakeMovePayload.STREAM_CODEC,
                (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player()
                                    instanceof ServerPlayer player) {
                                ChessPayloadHandler.handleMakeMove(
                                        payload,
                                        player
                                );
                            }
                        })
        );

        registrar.playToServer(
                ChessResignPayload.TYPE,
                ChessResignPayload.STREAM_CODEC,
                (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player()
                                    instanceof ServerPlayer player) {
                                ChessPayloadHandler.handleResign(
                                        payload,
                                        player
                                );
                            }
                        })
        );

        registrar.playToClient(
                ChessSyncPayload.TYPE,
                ChessSyncPayload.STREAM_CODEC,
                ChessSyncPayload::handle
        );
    }
}