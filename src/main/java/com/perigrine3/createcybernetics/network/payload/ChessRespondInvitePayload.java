package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record ChessRespondInvitePayload(
        BlockPos computerPos,
        UUID inviteId,
        boolean accepted
) implements CustomPacketPayload {

    public static final Type<ChessRespondInvitePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateCybernetics.MODID,
                            "chess_respond_invite"
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, ChessRespondInvitePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeBlockPos(
                                payload.computerPos()
                        );

                        buffer.writeUUID(
                                payload.inviteId()
                        );

                        buffer.writeBoolean(
                                payload.accepted()
                        );
                    },
                    buffer ->
                            new ChessRespondInvitePayload(
                                    buffer.readBlockPos(),
                                    buffer.readUUID(),
                                    buffer.readBoolean()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}