package com.perigrine3.createcybernetics.client.skin;

import com.mojang.blaze3d.platform.NativeImage;
import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.player.Player;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CybereyeSecondaryOverlayHandler {

    private CybereyeSecondaryOverlayHandler() {}

    private static final int DEFAULT_LEFT_X = 10;
    private static final int DEFAULT_LEFT_Y = 12;
    private static final int DEFAULT_RIGHT_X = 13;
    private static final int DEFAULT_RIGHT_Y = 12;

    private static final ResourceLocation COMBINED_SOURCE = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/entity/cybereyes_dyed.png");
    private static final ResourceLocation PRIMARY_SOURCE = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/entity/cybereyes_dye_primary.png");
    private static final ResourceLocation SECONDARY_SOURCE = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/entity/cybereyes_dye_secondary.png");

    private static final Map<UUID, Entry> CACHE = new ConcurrentHashMap<>();

    private static NativeImage combinedSource;
    private static NativeImage primarySource;
    private static NativeImage secondarySource;

    private static boolean templatesLoaded = false;
    private static boolean templatesFailed = false;

    private static final class Entry {
        final ResourceLocation combinedHighlightId;
        final ResourceLocation primaryHighlightId;
        final ResourceLocation secondaryOverlayId;

        final DynamicTexture combinedHighlight;
        final DynamicTexture primaryHighlight;
        final DynamicTexture secondaryOverlay;

        int lastHash;

        Entry(ResourceLocation combinedHighlightId, ResourceLocation primaryHighlightId, ResourceLocation secondaryOverlayId, DynamicTexture combinedHighlight, DynamicTexture primaryHighlight, DynamicTexture secondaryOverlay, int lastHash) {
            this.combinedHighlightId = combinedHighlightId;
            this.primaryHighlightId = primaryHighlightId;
            this.secondaryOverlayId = secondaryOverlayId;
            this.combinedHighlight = combinedHighlight;
            this.primaryHighlight = primaryHighlight;
            this.secondaryOverlay = secondaryOverlay;
            this.lastHash = lastHash;
        }
    }

    public static ResourceLocation getOrBuildCombinedHighlight(Player player) {
        Entry entry = getOrBuildEntry(player);
        return entry == null ? null : entry.combinedHighlightId;
    }

    public static ResourceLocation getOrBuildPrimaryHighlight(Player player) {
        Entry entry = getOrBuildEntry(player);
        return entry == null ? null : entry.primaryHighlightId;
    }

    public static ResourceLocation getOrBuildSecondaryOverlay(Player player) {
        Entry entry = getOrBuildEntry(player);
        return entry == null ? null : entry.secondaryOverlayId;
    }

    private static Entry getOrBuildEntry(Player player) {
        if (player == null) return null;

        CybereyeOverlayHandler.ResolvedPlacements placements = CybereyeOverlayHandler.getResolvedPlacements(player);
        if (placements == null) return null;

        ensureTemplatesLoaded();
        if (templatesFailed) return null;

        CybereyeOverlayHandler.EyePlacement left = placements.left();
        CybereyeOverlayHandler.EyePlacement right = placements.right();

        int hash = hash(left, right);
        UUID playerId = player.getUUID();

        Entry entry = CACHE.get(playerId);
        if (entry != null && entry.lastHash == hash) {
            return entry;
        }

        Minecraft mc = Minecraft.getInstance();

        if (entry == null) {
            ResourceLocation combinedHighlightId = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "dynamic/cybereyes/combined_highlight/" + playerId);
            ResourceLocation primaryHighlightId = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "dynamic/cybereyes/primary_highlight/" + playerId);
            ResourceLocation secondaryOverlayId = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "dynamic/cybereyes/secondary/" + playerId);

            DynamicTexture combinedHighlight = new DynamicTexture(64, 64, true);
            DynamicTexture primaryHighlight = new DynamicTexture(64, 64, true);
            DynamicTexture secondaryOverlay = new DynamicTexture(64, 64, true);

            mc.getTextureManager().register(combinedHighlightId, combinedHighlight);
            mc.getTextureManager().register(primaryHighlightId, primaryHighlight);
            mc.getTextureManager().register(secondaryOverlayId, secondaryOverlay);

            entry = new Entry(combinedHighlightId, primaryHighlightId, secondaryOverlayId, combinedHighlight, primaryHighlight, secondaryOverlay, -1);

            CACHE.put(playerId, entry);
        }

        NativeImage combinedOutput = entry.combinedHighlight.getPixels();
        NativeImage primaryOutput = entry.primaryHighlight.getPixels();
        NativeImage secondaryOutput = entry.secondaryOverlay.getPixels();

        if (combinedOutput == null || primaryOutput == null || secondaryOutput == null) {
            return null;
        }

        clear(combinedOutput);
        clear(primaryOutput);
        clear(secondaryOutput);

        stampCombined(combinedOutput, combinedSource, left, right);
        stampShifted(primaryOutput, primarySource, DEFAULT_LEFT_X, DEFAULT_LEFT_Y, left);
        stampShifted(secondaryOutput, secondarySource, DEFAULT_RIGHT_X, DEFAULT_RIGHT_Y, right);

        entry.combinedHighlight.upload();
        entry.primaryHighlight.upload();
        entry.secondaryOverlay.upload();

        entry.lastHash = hash;

        return entry;
    }

    public static void invalidate(Player player) {
        if (player == null) return;
        invalidate(player.getUUID());
    }

    public static void invalidate(UUID playerId) {
        if (playerId == null) return;

        Entry entry = CACHE.get(playerId);
        if (entry != null) {
            entry.lastHash = -1;
        }
    }

    public static void clearAll() {
        CACHE.clear();

        if (combinedSource != null) {
            combinedSource.close();
            combinedSource = null;
        }

        if (primarySource != null) {
            primarySource.close();
            primarySource = null;
        }

        if (secondarySource != null) {
            secondarySource.close();
            secondarySource = null;
        }

        templatesLoaded = false;
        templatesFailed = false;
    }

    private static int hash(CybereyeOverlayHandler.EyePlacement left, CybereyeOverlayHandler.EyePlacement right) {
        int hash = 17;

        hash = 31 * hash + left.x();
        hash = 31 * hash + left.y();
        hash = 31 * hash + left.variant().ordinal();

        hash = 31 * hash + right.x();
        hash = 31 * hash + right.y();
        hash = 31 * hash + right.variant().ordinal();

        return hash;
    }

    private static void ensureTemplatesLoaded() {
        if (templatesLoaded || templatesFailed) return;

        try {
            combinedSource = loadTemplate(COMBINED_SOURCE);
            primarySource = loadTemplate(PRIMARY_SOURCE);
            secondarySource = loadTemplate(SECONDARY_SOURCE);

            templatesLoaded = true;
        } catch (Throwable throwable) {
            CreateCybernetics.LOGGER.error("Failed to load dynamic cybereye overlay templates", throwable);
            templatesFailed = true;
        }
    }

    private static NativeImage loadTemplate(ResourceLocation texture) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        Resource resource = mc.getResourceManager().getResourceOrThrow(texture);

        try (InputStream stream = resource.open()) {
            return NativeImage.read(stream);
        }
    }

    private static void clear(NativeImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setPixelRGBA(x, y, 0x00000000);
            }
        }
    }

    private static void stampCombined(NativeImage output, NativeImage source, CybereyeOverlayHandler.EyePlacement left, CybereyeOverlayHandler.EyePlacement right) {
        if (source == null) return;

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int rgba = source.getPixelRGBA(x, y);
                int alpha = (rgba >>> 24) & 0xFF;

                if (alpha == 0) continue;

                int leftDistance = Math.abs(x - DEFAULT_LEFT_X) + Math.abs(y - DEFAULT_LEFT_Y);
                int rightDistance = Math.abs(x - DEFAULT_RIGHT_X) + Math.abs(y - DEFAULT_RIGHT_Y);

                if (leftDistance <= rightDistance) {
                    writeShiftedPixel(output, x, y, alpha, DEFAULT_LEFT_X, DEFAULT_LEFT_Y, left);
                } else {
                    writeShiftedPixel(output, x, y, alpha, DEFAULT_RIGHT_X, DEFAULT_RIGHT_Y, right);
                }
            }
        }
    }

    private static void stampShifted(NativeImage output, NativeImage source, int defaultX, int defaultY, CybereyeOverlayHandler.EyePlacement placement) {
        if (source == null) return;

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int rgba = source.getPixelRGBA(x, y);
                int alpha = (rgba >>> 24) & 0xFF;

                if (alpha == 0) continue;

                writeShiftedPixel(output, x, y, alpha, defaultX, defaultY, placement);
            }
        }
    }

    private static void writeShiftedPixel(NativeImage output, int sourceX, int sourceY, int alpha, int defaultX, int defaultY, CybereyeOverlayHandler.EyePlacement placement) {
        int offsetX = placement.x() - defaultX;
        int offsetY = placement.y() - defaultY;

        int targetX = sourceX + offsetX;
        int targetY = sourceY + offsetY;

        if (sourceX == defaultX && sourceY == defaultY) {
            int width = variantWidth(placement.variant());
            int height = variantHeight(placement.variant());

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    writePixel(output, targetX + x, targetY + y, alpha);
                }
            }

            return;
        }

        writePixel(output, targetX, targetY, alpha);
    }

    private static void writePixel(NativeImage output, int x, int y, int alpha) {
        if (x < 0 || x >= output.getWidth()) return;
        if (y < 0 || y >= output.getHeight()) return;

        output.setPixelRGBA(x, y, (alpha << 24) | 0x00FFFFFF);
    }

    private static int variantWidth(CybereyeOverlayHandler.Variant variant) {
        return variant == CybereyeOverlayHandler.Variant.V2x2 ? 2 : 1;
    }

    private static int variantHeight(CybereyeOverlayHandler.Variant variant) {
        return variant == CybereyeOverlayHandler.Variant.V1x2 || variant == CybereyeOverlayHandler.Variant.V2x2 ? 2 : 1;
    }
}