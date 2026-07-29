package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.MinimapWaypointClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.*;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public final class NavigationMapPayloads {

    private NavigationMapPayloads() {}

    public record RequestMapImportPayload(boolean mainHand) implements CustomPacketPayload {
        public static final Type<RequestMapImportPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "request_navigation_map_import"));
        public static final StreamCodec<ByteBuf, RequestMapImportPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, RequestMapImportPayload::mainHand, RequestMapImportPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(RequestMapImportPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) return;

                InteractionHand hand = payload.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                ItemStack stack = player.getItemInHand(hand);

                if (!stack.is(Items.FILLED_MAP)) return;

                MapId mapId = stack.get(DataComponents.MAP_ID);

                if (mapId == null) return;

                MapItemSavedData mapData = MapItem.getSavedData(stack, player.level());

                if (mapData == null) return;

                String dimension = mapData.dimension.location().toString();
                List<BannerMarker> banners = new ArrayList<>();
                List<ExplorerMarker> explorerMarkers = new ArrayList<>();

                for (MapBanner banner : mapData.getBanners()) {
                    String name = banner.name().map(Component::getString).orElseGet(() -> Component.translatable("gui.createcybernetics.navigation.default_banner", Component.translatable("color.minecraft." + banner.color().getName())).getString());
                    banners.add(new BannerMarker(banner.pos().getX(), banner.pos().getY(), banner.pos().getZ(), name, getWaypointColor(banner.color())));
                }

                int blocksPerPixel = 1 << mapData.scale;

                for (MapDecoration decoration : mapData.getDecorations()) {
                    Holder<MapDecorationType> type = decoration.type();
                    boolean buriedTreasure = type.equals(MapDecorationTypes.RED_X);

                    if (!buriedTreasure && !type.value().explorationMapElement()) continue;

                    double worldX = mapData.centerX + decoration.x() * 0.5D * blocksPerPixel;
                    double worldZ = mapData.centerZ + decoration.y() * 0.5D * blocksPerPixel;
                    String typeId = type.value().assetId().toString();
                    int mapColor = type.value().mapColor();
                    int waypointColor = buriedTreasure ? 0xFFFF3B55 : mapColor < 0 ? 0xFFFF3B55 : 0xFF000000 | mapColor;

                    explorerMarkers.add(new ExplorerMarker((int) Math.floor(worldX), (int) Math.floor(worldZ), typeId, waypointColor));
                }

                PacketDistributor.sendToPlayer(player, new ImportMapPayload(Integer.toString(mapId.id()), dimension, mapData.centerX, mapData.centerZ, mapData.scale, mapData.colors.clone(), banners, explorerMarkers));
            });
        }
    }

    public record BannerMarker(int x, int y, int z, String name, int color) {
        private static final StreamCodec<RegistryFriendlyByteBuf, BannerMarker> STREAM_CODEC = StreamCodec.of(
                (buffer, marker) -> {
                    buffer.writeInt(marker.x());
                    buffer.writeInt(marker.y());
                    buffer.writeInt(marker.z());
                    buffer.writeUtf(marker.name());
                    buffer.writeInt(marker.color());
                },
                buffer -> new BannerMarker(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readUtf(), buffer.readInt())
        );
    }

    public record ExplorerMarker(int x, int z, String typeId, int color) {
        private static final StreamCodec<RegistryFriendlyByteBuf, ExplorerMarker> STREAM_CODEC = StreamCodec.of(
                (buffer, marker) -> {
                    buffer.writeInt(marker.x());
                    buffer.writeInt(marker.z());
                    buffer.writeUtf(marker.typeId());
                    buffer.writeInt(marker.color());
                },
                buffer -> new ExplorerMarker(buffer.readInt(), buffer.readInt(), buffer.readUtf(), buffer.readInt())
        );
    }

    public record ImportMapPayload(String mapId, String dimension, int centerX, int centerZ, byte scale, byte[] colors, List<BannerMarker> banners, List<ExplorerMarker> explorerMarkers) implements CustomPacketPayload {
        public static final Type<ImportMapPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "import_navigation_map"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ImportMapPayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.mapId());
                    buffer.writeUtf(payload.dimension());
                    buffer.writeInt(payload.centerX());
                    buffer.writeInt(payload.centerZ());
                    buffer.writeByte(payload.scale());
                    buffer.writeByteArray(payload.colors());

                    buffer.writeVarInt(payload.banners().size());

                    for (BannerMarker banner : payload.banners()) {
                        BannerMarker.STREAM_CODEC.encode(buffer, banner);
                    }

                    buffer.writeVarInt(payload.explorerMarkers().size());

                    for (ExplorerMarker explorerMarker : payload.explorerMarkers()) {
                        ExplorerMarker.STREAM_CODEC.encode(buffer, explorerMarker);
                    }
                },
                buffer -> {
                    String mapId = buffer.readUtf();
                    String dimension = buffer.readUtf();
                    int centerX = buffer.readInt();
                    int centerZ = buffer.readInt();
                    byte scale = buffer.readByte();
                    byte[] colors = buffer.readByteArray();

                    int bannerCount = buffer.readVarInt();
                    List<BannerMarker> banners = new ArrayList<>(bannerCount);

                    for (int index = 0; index < bannerCount; index++) {
                        banners.add(BannerMarker.STREAM_CODEC.decode(buffer));
                    }

                    int explorerMarkerCount = buffer.readVarInt();
                    List<ExplorerMarker> explorerMarkers = new ArrayList<>(explorerMarkerCount);

                    for (int index = 0; index < explorerMarkerCount; index++) {
                        explorerMarkers.add(ExplorerMarker.STREAM_CODEC.decode(buffer));
                    }

                    return new ImportMapPayload(mapId, dimension, centerX, centerZ, scale, colors, banners, explorerMarkers);
                }
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(ImportMapPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                Minecraft minecraft = Minecraft.getInstance();
                LocalPlayer player = minecraft.player;

                if (player == null) return;
                if (payload.colors().length != 128 * 128) return;

                boolean newMap = MinimapWaypointClient.importMap(player, payload.mapId(), payload.dimension(), payload.centerX(), payload.centerZ(), payload.scale(), payload.colors());
                int importedBanners = 0;
                int importedExplorerMarkers = 0;

                for (BannerMarker banner : payload.banners()) {
                    if (MinimapWaypointClient.addMapBannerWaypoint(player, payload.dimension(), new net.minecraft.core.BlockPos(banner.x(), banner.y(), banner.z()), banner.name(), banner.color())) {
                        importedBanners++;
                    }
                }

                for (ExplorerMarker explorerMarker : payload.explorerMarkers()) {
                    if (MinimapWaypointClient.addExplorerWaypoint(player, payload.dimension(), explorerMarker.x(), explorerMarker.z(), payload.mapId(), explorerMarker.typeId(), explorerMarker.color())) {
                        importedExplorerMarkers++;
                    }
                }

                MinimapWaypointClient.saveIfDirty(player.getUUID());

                Component status = Component.translatable(newMap ? "gui.createcybernetics.navigation.map_imported" : "gui.createcybernetics.navigation.map_updated");
                Component message = Component.translatable("gui.createcybernetics.navigation.map_import_summary", status, importedBanners, importedExplorerMarkers);

                player.displayClientMessage(message, true);
            });
        }
    }

    private static int getWaypointColor(DyeColor color) {
        return switch (color) {
            case WHITE -> 0xFFFFFFFF;
            case ORANGE -> 0xFFFFA500;
            case MAGENTA -> 0xFFFF55FF;
            case LIGHT_BLUE -> 0xFF55AAFF;
            case YELLOW -> 0xFFFFFF55;
            case LIME -> 0xFF55FF55;
            case PINK -> 0xFFFF88AA;
            case GRAY -> 0xFF777777;
            case LIGHT_GRAY -> 0xFFAAAAAA;
            case CYAN -> 0xFF00FFFF;
            case PURPLE -> 0xFFAA55FF;
            case BLUE -> 0xFF5555FF;
            case BROWN -> 0xFFAA6633;
            case GREEN -> 0xFF00AA55;
            case RED -> 0xFFFF5555;
            case BLACK -> 0xFF333333;
        };
    }
}