package com.perigrine3.createcybernetics.client.vampyres;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.cyberware.lungs.VampyresItem;
import com.perigrine3.createcybernetics.network.payload.OpenVampyresPayload;
import com.perigrine3.createcybernetics.network.payload.VampyresBiteHeldPayload;
import com.perigrine3.createcybernetics.screen.custom.crafting.ExpandedInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class VampyresClientHandler {
    private static final ResourceLocation INDICATOR_EMPTY =
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/gui/vampyres_biteindicator_empty.png");

    private static final ResourceLocation INDICATOR_1 =
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/gui/vampyres_biteindicator_1.png");

    private static final ResourceLocation INDICATOR_2 =
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/gui/vampyres_biteindicator_2.png");

    private static final ResourceLocation INDICATOR_3 =
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/gui/vampyres_biteindicator_3.png");

    private static final ResourceLocation INDICATOR_FULL =
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/gui/vampyres_biteindicator_full.png");

    private static final double MAX_BITE_DISTANCE = 3.5D;

    private static final int CLIENT_CHARGE_TICKS_REQUIRED = 10;
    private static final int COMPLETE_BAR_VISIBLE_TICKS = 8;

    private static final int PROGRESS_BAR_WIDTH = 24;
    private static final int PROGRESS_BAR_HEIGHT = 4;
    private static final int PROGRESS_BAR_Y_OFFSET = 2;

    private static final int PROGRESS_BAR_BACKGROUND = 0xD0000000;
    private static final int PROGRESS_BAR_BORDER = 0xFF4A0A0A;
    private static final int PROGRESS_BAR_FILL = 0xFFE62020;
    private static final int PROGRESS_BAR_COMPLETE = 0xFFFF6060;

    private static final int SURVIVAL_BUTTON_X_OFFSET = 59;
    private static final int SURVIVAL_BUTTON_Y_OFFSET = 8;

    private static boolean lastHeld;
    private static int lastTargetId = -1;

    private static int clientChargeTicks;
    private static int clientChargeTargetId = -1;
    private static int completeBarTicks;

    private VampyresClientHandler() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) return;
        if (!VampyresItem.isInstalled(minecraft.player)) return;

        if (!(event.getScreen() instanceof InventoryScreen) && !(event.getScreen() instanceof ExpandedInventoryScreen)) {
            return;
        }

        int leftPos = (event.getScreen().width - 176) / 2;
        int topPos = (event.getScreen().height - 166) / 2;

        VampyresInventoryButton button = new VampyresInventoryButton(leftPos + SURVIVAL_BUTTON_X_OFFSET, topPos + SURVIVAL_BUTTON_Y_OFFSET, () -> PacketDistributor.sendToServer(new OpenVampyresPayload()));

        event.addListener(button);
    }

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen) && !(event.getScreen() instanceof ExpandedInventoryScreen)) {
            return;
        }

        for (var child : event.getScreen().children()) {
            if (!(child instanceof VampyresInventoryButton button)) continue;

            button.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            button.renderTooltip(event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
        }
    }

    @SubscribeEvent
    public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) return;
        if (minecraft.level == null) return;
        if (minecraft.screen != null) return;
        if (!VampyresItem.isEnabled(minecraft.player)) return;

        LivingEntity target = getLookedAtTarget(minecraft);
        if (target == null) return;

        event.setCanceled(true);
        event.setSwingHand(false);

        beginClientCharge(target.getId());
        sendHeldState(true, target.getId());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (completeBarTicks > 0) {
            completeBarTicks--;
        }

        if (minecraft.player == null || minecraft.level == null) {
            sendReleased();
            return;
        }

        if (minecraft.screen != null) {
            sendReleased();
            return;
        }

        if (!VampyresItem.isEnabled(minecraft.player)) {
            sendReleased();
            return;
        }

        LivingEntity target = getLookedAtTarget(minecraft);

        boolean held = target != null && minecraft.options.keyUse.isDown();
        int targetId = target == null ? -1 : target.getId();

        if (held) {
            tickClientCharge(targetId);
        } else if (completeBarTicks <= 0) {
            resetClientCharge();
        }

        sendHeldState(held, targetId);
    }

    @SubscribeEvent
    public static void onRenderCrosshair(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.CROSSHAIR)) return;

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) return;
        if (!VampyresItem.isEnabled(minecraft.player)) return;

        LivingEntity target = getLookedAtTarget(minecraft);
        if (target == null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        ResourceLocation texture = getIndicatorTexture(minecraft);

        int crosshairX = (graphics.guiWidth() - 16) / 2;
        int crosshairY = (graphics.guiHeight() - 16) / 2;

        graphics.blit(texture, crosshairX, crosshairY, 0, 0, 16, 16, 16, 16);

        if (shouldRenderProgressBar(target.getId())) {
            renderProgressBar(graphics, target.getId(), crosshairX, crosshairY);
        }

        event.setCanceled(true);
    }

    private static boolean shouldRenderProgressBar(int targetId) {
        if (completeBarTicks > 0) return true;
        if (!lastHeld) return false;
        if (clientChargeTicks <= 0) return false;

        return clientChargeTargetId == targetId;
    }

    private static void renderProgressBar(GuiGraphics graphics, int targetId, int crosshairX, int crosshairY) {
        int progressTicks = targetId == clientChargeTargetId ? clientChargeTicks : CLIENT_CHARGE_TICKS_REQUIRED;

        float progress = Math.min(1.0F, progressTicks / (float) CLIENT_CHARGE_TICKS_REQUIRED);
        int filledWidth = Math.round((PROGRESS_BAR_WIDTH - 2) * progress);

        int barX = crosshairX + 8 - PROGRESS_BAR_WIDTH / 2;
        int barY = crosshairY + 16 + PROGRESS_BAR_Y_OFFSET;

        graphics.fill(barX, barY, barX + PROGRESS_BAR_WIDTH, barY + PROGRESS_BAR_HEIGHT, PROGRESS_BAR_BORDER);
        graphics.fill(barX + 1, barY + 1, barX + PROGRESS_BAR_WIDTH - 1, barY + PROGRESS_BAR_HEIGHT - 1, PROGRESS_BAR_BACKGROUND);

        if (filledWidth <= 0) return;

        int fillColor = progress >= 1.0F ? PROGRESS_BAR_COMPLETE : PROGRESS_BAR_FILL;
        graphics.fill(barX + 1, barY + 1, barX + 1 + filledWidth, barY + PROGRESS_BAR_HEIGHT - 1, fillColor);
    }

    private static void beginClientCharge(int targetId) {
        if (clientChargeTargetId == targetId && clientChargeTicks > 0) return;

        clientChargeTargetId = targetId;
        clientChargeTicks = 0;
        completeBarTicks = 0;
    }

    private static void tickClientCharge(int targetId) {
        if (clientChargeTargetId != targetId) {
            clientChargeTargetId = targetId;
            clientChargeTicks = 0;
            completeBarTicks = 0;
        }

        if (clientChargeTicks < CLIENT_CHARGE_TICKS_REQUIRED) {
            clientChargeTicks++;

            if (clientChargeTicks >= CLIENT_CHARGE_TICKS_REQUIRED) {
                clientChargeTicks = CLIENT_CHARGE_TICKS_REQUIRED;
                completeBarTicks = COMPLETE_BAR_VISIBLE_TICKS;
            }
        }
    }

    private static void resetClientCharge() {
        clientChargeTicks = 0;
        clientChargeTargetId = -1;
        completeBarTicks = 0;
    }

    private static LivingEntity getLookedAtTarget(Minecraft minecraft) {
        if (!(minecraft.hitResult instanceof EntityHitResult entityHitResult)) return null;
        if (!(entityHitResult.getEntity() instanceof LivingEntity target)) return null;

        if (target == minecraft.player) return null;
        if (!target.isAlive()) return null;
        if (minecraft.player.distanceToSqr(target) > MAX_BITE_DISTANCE * MAX_BITE_DISTANCE) return null;

        return target;
    }

    private static ResourceLocation getIndicatorTexture(Minecraft minecraft) {
        if (!minecraft.player.hasData(ModAttachments.CYBERWARE)) {
            return INDICATOR_EMPTY;
        }

        PlayerCyberwareData data = minecraft.player.getData(ModAttachments.CYBERWARE);
        if (data == null) return INDICATOR_EMPTY;

        int highestCount = 0;

        for (int slot = 0; slot < PlayerCyberwareData.VAMPYRES_SLOT_COUNT; slot++) {
            ItemStack stack = data.getVampyresStack(slot);

            if (stack == null || stack.isEmpty()) continue;

            highestCount = Math.max(highestCount, stack.getCount());
        }

        return switch (Math.min(VampyresItem.SLOT_STACK_LIMIT, highestCount)) {
            case 4 -> INDICATOR_FULL;
            case 3 -> INDICATOR_3;
            case 2 -> INDICATOR_2;
            case 1 -> INDICATOR_1;
            default -> INDICATOR_EMPTY;
        };
    }

    private static void sendHeldState(boolean held, int targetId) {
        if (held == lastHeld && targetId == lastTargetId) return;

        PacketDistributor.sendToServer(new VampyresBiteHeldPayload(held, targetId));

        lastHeld = held;
        lastTargetId = targetId;
    }

    private static void sendReleased() {
        if (completeBarTicks <= 0) {
            resetClientCharge();
        }

        sendHeldState(false, -1);
    }
}