package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ChatSpaceAddContactPayload(
        BlockPos computerPos,
        String remoteCode,
        String displayName
) implements CustomPacketPayload {
    public static final Type<ChatSpaceAddContactPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateCybernetics.MODID,
                            "chatspace_add_contact"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            ChatSpaceAddContactPayload
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
                                payload.displayName,
                                32
                        );
                    },
                    buffer ->
                            new ChatSpaceAddContactPayload(
                                    buffer.readBlockPos(),
                                    buffer.readUtf(5),
                                    buffer.readUtf(32)
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}