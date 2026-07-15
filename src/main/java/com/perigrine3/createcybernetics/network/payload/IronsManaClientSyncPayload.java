package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.compat.ironsspells.IronsSpellbooksClientManaCompat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record IronsManaClientSyncPayload(int mana) implements CustomPacketPayload {

    public static final Type<IronsManaClientSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "irons_mana_client_sync")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, IronsManaClientSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    IronsManaClientSyncPayload::mana,
                    IronsManaClientSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(IronsManaClientSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> IronsSpellbooksClientManaCompat.setClientMana(payload.mana()));
    }
}