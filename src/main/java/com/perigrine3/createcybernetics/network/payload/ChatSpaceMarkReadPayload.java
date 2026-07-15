package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ChatSpaceMarkReadPayload(
        BlockPos computerPos,
        String remoteCode
) implements CustomPacketPayload {
    public static final Type<ChatSpaceMarkReadPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateCybernetics.MODID,
                            "chatspace_mark_read"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            ChatSpaceMarkReadPayload
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
                    },
                    buffer ->
                            new ChatSpaceMarkReadPayload(
                                    buffer.readBlockPos(),
                                    buffer.readUtf(5)
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}