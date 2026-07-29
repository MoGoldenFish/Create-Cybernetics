package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.item.cyberware.lungs.VampyresItem;
import com.perigrine3.createcybernetics.screen.custom.vampyres.VampyresMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenVampyresPayload() implements CustomPacketPayload {
    public static final Type<OpenVampyresPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "open_vampyres"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenVampyresPayload> STREAM_CODEC = StreamCodec.unit(new OpenVampyresPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenVampyresPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!VampyresItem.isInstalled(player)) return;

        player.openMenu(new SimpleMenuProvider((containerId, inventory, menuPlayer) -> new VampyresMenu(containerId, inventory), Component.translatable("gui.createcybernetics.vampyres")));
    }
}