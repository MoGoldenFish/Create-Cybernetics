package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.navigation.SharedNavigationClientState;
import com.perigrine3.createcybernetics.client.navigation.SharedNavigationClientSync;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SharedNavigationClearPayload() implements CustomPacketPayload {

    public static final Type<SharedNavigationClearPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "shared_navigation_clear"));
    public static final StreamCodec<ByteBuf, SharedNavigationClearPayload> STREAM_CODEC = StreamCodec.unit(new SharedNavigationClearPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SharedNavigationClearPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            SharedNavigationClientState.clear();
            SharedNavigationClientSync.clearQueues();
        });
    }
}