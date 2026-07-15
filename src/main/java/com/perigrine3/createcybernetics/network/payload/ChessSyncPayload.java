package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.computer.ChessClientData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ChessSyncPayload(
        CompoundTag snapshot
) implements CustomPacketPayload {

    public static final Type<ChessSyncPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateCybernetics.MODID,
                            "chess_sync"
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, ChessSyncPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) ->
                            buffer.writeNbt(
                                    payload.snapshot()
                            ),
                    buffer -> {
                        CompoundTag snapshot =
                                buffer.readNbt();

                        return new ChessSyncPayload(
                                snapshot == null
                                        ? new CompoundTag()
                                        : snapshot
                        );
                    }
            );

    public static void handle(
            ChessSyncPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> ChessClientData.acceptSnapshot(
                        payload.snapshot()
                )
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}