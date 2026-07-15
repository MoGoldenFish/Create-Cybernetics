package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ChatSpaceSendMessagePayload(
        BlockPos computerPos,
        String remoteCode,
        String message
) implements CustomPacketPayload {
    public static final Type<ChatSpaceSendMessagePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateCybernetics.MODID,
                            "chatspace_send_message"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            ChatSpaceSendMessagePayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeBlockPos(
                                payload.computerPos
                        );

                        buffer.writeUtf(
                                payload.remoteCode,
                                5
                        );

                        buffer.writeUtf(
                                payload.message,
                                512
                        );
                    },
                    buffer ->
                            new ChatSpaceSendMessagePayload(
                                    buffer.readBlockPos(),
                                    buffer.readUtf(5),
                                    buffer.readUtf(512)
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}