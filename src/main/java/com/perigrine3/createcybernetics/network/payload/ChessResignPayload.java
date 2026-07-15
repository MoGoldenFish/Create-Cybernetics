package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record ChessResignPayload(
        BlockPos computerPos,
        UUID sessionId
) implements CustomPacketPayload {

    public static final Type<ChessResignPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateCybernetics.MODID,
                            "chess_resign"
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, ChessResignPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeBlockPos(
                                payload.computerPos()
                        );

                        buffer.writeUUID(
                                payload.sessionId()
                        );
                    },
                    buffer ->
                            new ChessResignPayload(
                                    buffer.readBlockPos(),
                                    buffer.readUUID()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}