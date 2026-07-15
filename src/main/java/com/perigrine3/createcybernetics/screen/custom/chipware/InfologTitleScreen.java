package com.perigrine3.createcybernetics.screen.custom.chipware;

import com.perigrine3.createcybernetics.network.payload.InfologSaveChipwarePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class InfologTitleScreen extends Screen {

    private static final int GUI_W = 256;
    private static final int GUI_H = 128;
    private static final int MAX_TITLE_LENGTH = 32;

    private final int chipwareSlot;
    private final String text;
    private final int accentColor;

    private int leftPos;
    private int topPos;

    private EditBox titleBox;
    private Button saveButton;

    public InfologTitleScreen(int chipwareSlot, String text, int accentColor) {
        super(Component.translatable("gui.infolog.save_title"));
        this.chipwareSlot = chipwareSlot;
        this.text = text == null ? "" : text;
        this.accentColor = accentColor;
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - GUI_W) / 2;
        this.topPos = (this.height - GUI_H) / 2;

        int inputX = leftPos + 18;
        int inputY = topPos + 49;
        int inputW = GUI_W - 36;

        this.titleBox = new EditBox(
                this.font,
                inputX,
                inputY,
                inputW,
                20,
                Component.translatable("gui.infolog.title_hint")
        );

        this.titleBox.setMaxLength(MAX_TITLE_LENGTH);
        this.titleBox.setValue("");
        this.titleBox.setResponder(value -> {
            if (saveButton != null) {
                saveButton.active = !value.trim().isEmpty();
            }
        });

        this.addRenderableWidget(this.titleBox);
        this.setInitialFocus(this.titleBox);

        int buttonY = topPos + 86;
        int buttonW = 90;
        int gap = 10;
        int totalW = buttonW + gap + buttonW;
        int startX = leftPos + (GUI_W - totalW) / 2;

        this.addRenderableWidget(new AccentButton(
                startX,
                buttonY,
                buttonW,
                20,
                Component.translatable("gui.cancel"),
                b -> returnToEditor(),
                accentColor
        ));

        this.saveButton = this.addRenderableWidget(new AccentButton(
                startX + buttonW + gap,
                buttonY,
                buttonW,
                20,
                Component.translatable("gui.infolog.save"),
                b -> saveAndLock(),
                accentColor
        ));

        this.saveButton.active = false;
    }

    private void saveAndLock() {
        String title = this.titleBox.getValue().trim();
        if (title.isEmpty()) return;

        PacketDistributor.sendToServer(new InfologSaveChipwarePayload(
                this.chipwareSlot,
                this.text,
                title,
                true
        ));

        Minecraft.getInstance().setScreen(null);
    }

    private void returnToEditor() {
        Minecraft.getInstance().setScreen(new InfologEditScreen(
                this.chipwareSlot,
                this.text
        ));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.render(gg, mouseX, mouseY, partialTick);

        gg.fill(leftPos, topPos, leftPos + GUI_W, topPos + GUI_H, 0xEE000000);
        drawBorder(gg, leftPos, topPos, GUI_W, GUI_H, accentColor);

        gg.drawCenteredString(
                this.font,
                Component.translatable("gui.infolog.save_title"),
                leftPos + GUI_W / 2,
                topPos + 18,
                accentColor
        );

        gg.drawCenteredString(
                this.font,
                Component.translatable("gui.infolog.save_warning"),
                leftPos + GUI_W / 2,
                topPos + 33,
                0xFFFFFFFF
        );

        drawBorder(
                gg,
                this.titleBox.getX() - 1,
                this.titleBox.getY() - 1,
                this.titleBox.getWidth() + 2,
                this.titleBox.getHeight() + 2,
                accentColor
        );
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            returnToEditor();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static void drawBorder(GuiGraphics gg, int x, int y, int w, int h, int argb) {
        gg.fill(x, y, x + w, y + 1, argb);
        gg.fill(x, y + h - 1, x + w, y + h, argb);
        gg.fill(x, y, x + 1, y + h, argb);
        gg.fill(x + w - 1, y, x + w, y + h, argb);
    }

    private static final class AccentButton extends Button {
        private final int accent;

        private AccentButton(int x, int y, int w, int h, Component msg, OnPress onPress, int accent) {
            super(x, y, w, h, msg, onPress, DEFAULT_NARRATION);
            this.accent = accent;
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();

            gg.fill(x, y, x + w, y + h, 0xFF000000);
            drawBorder(gg, x, y, w, h, accent);

            if (this.isHoveredOrFocused()) {
                gg.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0x22000000);
            }

            int textColor = this.active ? accent : 0xFF666666;

            gg.drawCenteredString(
                    Minecraft.getInstance().font,
                    getMessage(),
                    x + w / 2,
                    y + (h - 8) / 2,
                    textColor
            );
        }
    }
}