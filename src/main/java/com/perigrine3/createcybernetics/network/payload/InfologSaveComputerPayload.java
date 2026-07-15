package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record InfologSaveComputerPayload(
        BlockPos computerPos,
        int towerSlot,
        String text,
        String title,
        boolean locked
) implements CustomPacketPayload {

    public static final Type<InfologSaveComputerPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateCybernetics.MODID,
                            "infolog_save_computer"
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, InfologSaveComputerPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeBlockPos(
                                payload.computerPos
                        );

                        buffer.writeVarInt(
                                payload.towerSlot
                        );

                        buffer.writeUtf(
                                payload.text,
                                32_000
                        );

                        buffer.writeUtf(
                                payload.title,
                                32
                        );

                        buffer.writeBoolean(
                                payload.locked
                        );
                    },
                    buffer ->
                            new InfologSaveComputerPayload(
                                    buffer.readBlockPos(),
                                    buffer.readVarInt(),
                                    buffer.readUtf(32_000),
                                    buffer.readUtf(32),
                                    buffer.readBoolean()
                            )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}