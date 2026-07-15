package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.common.computer.chess.ChessMove;
import com.perigrine3.createcybernetics.common.computer.chess.ChessPieceType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record ChessMakeMovePayload(
        BlockPos computerPos,
        UUID sessionId,
        int fromX,
        int fromY,
        int toX,
        int toY,
        ChessPieceType promotion
) implements CustomPacketPayload {

    public static final Type<ChessMakeMovePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateCybernetics.MODID,
                            "chess_make_move"
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, ChessMakeMovePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeBlockPos(
                                payload.computerPos()
                        );

                        buffer.writeUUID(
                                payload.sessionId()
                        );

                        buffer.writeByte(
                                payload.fromX()
                        );

                        buffer.writeByte(
                                payload.fromY()
                        );

                        buffer.writeByte(
                                payload.toX()
                        );

                        buffer.writeByte(
                                payload.toY()
                        );

                        buffer.writeUtf(
                                payload.promotion() == null
                                        ? ""
                                        : payload.promotion()
                                        .name(),
                                16
                        );
                    },
                    buffer ->
                            new ChessMakeMovePayload(
                                    buffer.readBlockPos(),
                                    buffer.readUUID(),
                                    buffer.readByte(),
                                    buffer.readByte(),
                                    buffer.readByte(),
                                    buffer.readByte(),
                                    parsePromotion(
                                            buffer.readUtf(16)
                                    )
                            )
            );

    public ChessMove toMove() {
        return new ChessMove(
                fromX,
                fromY,
                toX,
                toY,
                promotion
        );
    }

    private static ChessPieceType parsePromotion(
            String value
    ) {
        if (value == null ||
                value.isBlank()) {
            return null;
        }

        try {
            return ChessPieceType.valueOf(
                    value
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}