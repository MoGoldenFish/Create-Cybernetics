package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.network.handler.VampyresBiteHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record VampyresBiteHeldPayload(boolean held, int targetEntityId) implements CustomPacketPayload {
    public static final Type<VampyresBiteHeldPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "vampyres_bite_held"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VampyresBiteHeldPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            VampyresBiteHeldPayload::held,
            ByteBufCodecs.VAR_INT,
            VampyresBiteHeldPayload::targetEntityId,
            VampyresBiteHeldPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(VampyresBiteHeldPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;

        VampyresBiteHandler.setHeld(player, payload.held(), payload.targetEntityId());
    }
}