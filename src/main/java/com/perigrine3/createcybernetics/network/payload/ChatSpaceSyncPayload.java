package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.computer.ChatSpaceClientData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ChatSpaceSyncPayload(
        CompoundTag snapshot
) implements CustomPacketPayload {
    public static final Type<ChatSpaceSyncPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateCybernetics.MODID,
                            "chatspace_sync"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            ChatSpaceSyncPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) ->
                            buffer.writeNbt(
                                    payload.snapshot
                            ),
                    buffer -> {
                        CompoundTag tag =
                                buffer.readNbt();

                        return new ChatSpaceSyncPayload(
                                tag == null
                                        ? new CompoundTag()
                                        : tag
                        );
                    }
            );

    public static void handle(
            ChatSpaceSyncPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> ChatSpaceClientData.acceptSnapshot(
                        payload.snapshot
                )
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}