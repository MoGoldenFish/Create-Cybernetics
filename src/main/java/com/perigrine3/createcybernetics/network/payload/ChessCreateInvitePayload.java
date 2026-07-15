package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ChessCreateInvitePayload(
        BlockPos computerPos,
        String receiverCode
) implements CustomPacketPayload {

    public static final Type<ChessCreateInvitePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateCybernetics.MODID,
                            "chess_create_invite"
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, ChessCreateInvitePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeBlockPos(
                                payload.computerPos()
                        );

                        buffer.writeUtf(
                                payload.receiverCode(),
                                5
                        );
                    },
                    buffer ->
                            new ChessCreateInvitePayload(
                                    buffer.readBlockPos(),
                                    buffer.readUtf(5)
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}