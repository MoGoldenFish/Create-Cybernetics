package com.perigrine3.createcybernetics.screen.custom.hud;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.cyberware.organs.OregrinderItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = CreateCybernetics.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class OregrinderHudOverlay {

    private OregrinderHudOverlay() {}

    private static final ResourceLocation TITANIUM_HEART_FULL =
            ResourceLocation.fromNamespaceAndPath(
                    CreateCybernetics.MODID,
                    "textures/gui/hud/titanium_heart_full.png"
            );

    private static final ResourceLocation TITANIUM_HEART_HALF =
            ResourceLocation.fromNamespaceAndPath(
                    CreateCybernetics.MODID,
                    "textures/gui/hud/titanium_heart_half.png"
            );

    private static final int ICON_SIZE = 9;
    private static final int ICON_SPACING = 8;
    private static final int HUD_ROW_HEIGHT = 10;

    private static boolean armorPosePushed = false;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();

        if (!hasOregrinderInstalled()) {
            return;
        }

        if (event.getName().equals(VanillaGuiLayers.FOOD_LEVEL)) {
            event.setCanceled(true);
            return;
        }

        if (event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)) {
            event.setCanceled(true);

            renderTitaniumHealth(
                    event.getGuiGraphics(),
                    mc.player.getHealth(),
                    mc.player.getMaxHealth()
            );

            return;
        }

        if (event.getName().equals(VanillaGuiLayers.ARMOR_LEVEL)) {
            int healthRows = getHealthRows(mc.player.getMaxHealth());
            int armorYOffset = healthRows * HUD_ROW_HEIGHT;

            event.getGuiGraphics().pose().pushPose();
            event.getGuiGraphics().pose().translate(0.0F, -armorYOffset, 0.0F);

            armorPosePushed = true;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderGuiLayerPost(RenderGuiLayerEvent.Post event) {
        if (!armorPosePushed) {
            return;
        }

        if (!event.getName().equals(VanillaGuiLayers.ARMOR_LEVEL)) {
            return;
        }

        event.getGuiGraphics().pose().popPose();
        armorPosePushed = false;
    }

    private static boolean hasOregrinderInstalled() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) {
            return false;
        }

        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return false;
        }

        PlayerCyberwareData data = PlayerCyberwareData.getForVisual(
                mc.player,
                mc.player.registryAccess()
        );

        return OregrinderItem.hasOregrinderInstalled(data);
    }

    private static int getHealthRows(float maxHealth) {
        int iconCount = Mth.ceil(maxHealth / 2.0F);

        return Math.max(
                1,
                Mth.ceil(iconCount / 10.0F)
        );
    }

    private static void renderTitaniumHealth(
            GuiGraphics guiGraphics,
            float health,
            float maxHealth
    ) {
        Minecraft mc = Minecraft.getInstance();

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int iconCount = Mth.ceil(maxHealth / 2.0F);
        int startX = screenWidth / 2 - 91;
        int startY = screenHeight - 39;

        for (int iconIndex = 0; iconIndex < iconCount; iconIndex++) {
            int row = iconIndex / 10;
            int column = iconIndex % 10;

            int x = startX + column * ICON_SPACING;
            int y = startY - row * HUD_ROW_HEIGHT;

            float heartStart = iconIndex * 2.0F;
            float remainingHealth = health - heartStart;

            drawHeart(guiGraphics, TITANIUM_HEART_FULL, x, y, 0x55000000);

            if (remainingHealth >= 2.0F) {
                drawHeart(guiGraphics, TITANIUM_HEART_FULL, x, y, 0xFFFFFFFF);
            } else if (remainingHealth > 0.0F) {
                drawHeart(guiGraphics, TITANIUM_HEART_HALF, x, y, 0xFFFFFFFF);
            }
        }
    }

    private static void drawHeart(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            int x,
            int y,
            int color
    ) {
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float alpha = ((color >> 24) & 0xFF) / 255.0F;

        guiGraphics.setColor(red, green, blue, alpha);

        guiGraphics.blit(
                texture,
                x,
                y,
                0.0F,
                0.0F,
                ICON_SIZE,
                ICON_SIZE,
                ICON_SIZE,
                ICON_SIZE
        );

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}