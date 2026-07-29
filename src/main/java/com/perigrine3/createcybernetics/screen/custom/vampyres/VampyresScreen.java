package com.perigrine3.createcybernetics.screen.custom.vampyres;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.item.cyberware.lungs.VampyresItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class VampyresScreen extends AbstractContainerScreen<VampyresMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "textures/gui/vampyres_gui.png");

    public VampyresScreen(VampyresMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);

        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        renderVampyresCounts(graphics);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderVampyresCounts(GuiGraphics graphics) {
        for (int slot = 0; slot < VampyresItem.SLOT_COUNT; slot++) {
            int count = menu.getVampyresDisplayCount(slot);

            if (count <= 1) continue;

            int slotX = slot == 0
                    ? VampyresMenu.VAMPYRES_SLOT_0_X
                    : VampyresMenu.VAMPYRES_SLOT_1_X;

            int slotY = slot == 0
                    ? VampyresMenu.VAMPYRES_SLOT_0_Y
                    : VampyresMenu.VAMPYRES_SLOT_1_Y;

            String text = Integer.toString(count);

            int x = leftPos + slotX + 17 - font.width(text);
            int y = topPos + slotY + 9;

            graphics.drawString(font, text, x, y, 0xFFFFFFFF, true);
        }
    }
}