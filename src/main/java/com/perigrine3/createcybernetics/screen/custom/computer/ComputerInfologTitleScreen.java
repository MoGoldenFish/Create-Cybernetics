package com.perigrine3.createcybernetics.screen.custom.computer;

import com.perigrine3.createcybernetics.network.payload.InfologSaveComputerPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ComputerInfologTitleScreen extends Screen {
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 128;

    private static final int MAX_TITLE_LENGTH = 32;

    private final Screen parentScreen;
    private final BlockPos computerPos;
    private final int towerSlot;

    private final ItemStack sourceStack;
    private final String text;
    private final int accentColor;

    private int leftPos;
    private int topPos;

    private EditBox titleBox;
    private Button saveButton;

    public ComputerInfologTitleScreen(
            Screen parentScreen,
            BlockPos computerPos,
            int towerSlot,
            ItemStack sourceStack,
            String text,
            int accentColor
    ) {
        super(
                Component.translatable(
                        "gui.infolog.save_title"
                )
        );

        this.parentScreen = parentScreen;
        this.computerPos = computerPos.immutable();
        this.towerSlot = towerSlot;
        this.sourceStack = sourceStack.copy();
        this.text = text == null
                ? ""
                : text;
        this.accentColor = accentColor;
    }

    @Override
    protected void init() {
        leftPos =
                (width - GUI_WIDTH) / 2;

        topPos =
                (height - GUI_HEIGHT) / 2;

        int inputX =
                leftPos + 18;

        int inputY =
                topPos + 49;

        int inputWidth =
                GUI_WIDTH - 36;

        titleBox = new EditBox(
                font,
                inputX,
                inputY,
                inputWidth,
                20,
                Component.translatable(
                        "gui.infolog.title_hint"
                )
        );

        titleBox.setMaxLength(
                MAX_TITLE_LENGTH
        );

        titleBox.setResponder(
                value -> {
                    if (saveButton != null) {
                        saveButton.active =
                                !value.trim().isEmpty();
                    }
                }
        );

        addRenderableWidget(
                titleBox
        );

        setInitialFocus(
                titleBox
        );

        int buttonY =
                topPos + 86;

        int buttonWidth = 90;
        int gap = 10;

        int totalWidth =
                buttonWidth * 2 + gap;

        int startX =
                leftPos
                        + (GUI_WIDTH - totalWidth) / 2;

        addRenderableWidget(
                new AccentButton(
                        startX,
                        buttonY,
                        buttonWidth,
                        20,
                        Component.translatable(
                                "gui.cancel"
                        ),
                        button -> returnToEditor(),
                        accentColor
                )
        );

        saveButton = addRenderableWidget(
                new AccentButton(
                        startX + buttonWidth + gap,
                        buttonY,
                        buttonWidth,
                        20,
                        Component.translatable(
                                "gui.infolog.save"
                        ),
                        button -> saveAndLock(),
                        accentColor
                )
        );

        saveButton.active = false;
    }

    private void saveAndLock() {
        String title =
                titleBox.getValue()
                        .trim();

        if (title.isEmpty()) {
            return;
        }

        PacketDistributor.sendToServer(
                new InfologSaveComputerPayload(
                        computerPos,
                        towerSlot,
                        text,
                        title,
                        true
                )
        );

        Minecraft.getInstance().setScreen(
                parentScreen
        );
    }

    private void returnToEditor() {
        Minecraft.getInstance().setScreen(
                new ComputerInfologEditScreen(
                        parentScreen,
                        computerPos,
                        towerSlot,
                        sourceStack,
                        text
                )
        );
    }

    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        super.render(
                guiGraphics,
                mouseX,
                mouseY,
                partialTick
        );

        guiGraphics.fill(
                leftPos,
                topPos,
                leftPos + GUI_WIDTH,
                topPos + GUI_HEIGHT,
                0xEE000000
        );

        drawBorder(
                guiGraphics,
                leftPos,
                topPos,
                GUI_WIDTH,
                GUI_HEIGHT,
                accentColor
        );

        guiGraphics.drawCenteredString(
                font,
                Component.translatable(
                        "gui.infolog.save_title"
                ),
                leftPos + GUI_WIDTH / 2,
                topPos + 18,
                accentColor
        );

        guiGraphics.drawCenteredString(
                font,
                Component.translatable(
                        "gui.infolog.save_warning"
                ),
                leftPos + GUI_WIDTH / 2,
                topPos + 33,
                0xFFFFFFFF
        );

        drawBorder(
                guiGraphics,
                titleBox.getX() - 1,
                titleBox.getY() - 1,
                titleBox.getWidth() + 2,
                titleBox.getHeight() + 2,
                accentColor
        );
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        boolean inventoryKey =
                Minecraft.getInstance()
                        .options
                        .keyInventory
                        .matches(
                                keyCode,
                                scanCode
                        );

        if (titleBox != null &&
                titleBox.isFocused() &&
                inventoryKey) {
            return true;
        }

        if (keyCode == 256) {
            returnToEditor();
            return true;
        }

        return super.keyPressed(
                keyCode,
                scanCode,
                modifiers
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static void drawBorder(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        guiGraphics.fill(
                x,
                y,
                x + width,
                y + 1,
                color
        );

        guiGraphics.fill(
                x,
                y + height - 1,
                x + width,
                y + height,
                color
        );

        guiGraphics.fill(
                x,
                y,
                x + 1,
                y + height,
                color
        );

        guiGraphics.fill(
                x + width - 1,
                y,
                x + width,
                y + height,
                color
        );
    }

    private static final class AccentButton extends Button {
        private final int accentColor;

        private AccentButton(
                int x,
                int y,
                int width,
                int height,
                Component message,
                OnPress onPress,
                int accentColor
        ) {
            super(
                    x,
                    y,
                    width,
                    height,
                    message,
                    onPress,
                    DEFAULT_NARRATION
            );

            this.accentColor = accentColor;
        }

        @Override
        protected void renderWidget(
                GuiGraphics guiGraphics,
                int mouseX,
                int mouseY,
                float partialTick
        ) {
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();

            guiGraphics.fill(
                    x,
                    y,
                    x + width,
                    y + height,
                    0xFF000000
            );

            drawBorder(
                    guiGraphics,
                    x,
                    y,
                    width,
                    height,
                    accentColor
            );

            if (isHoveredOrFocused()) {
                guiGraphics.fill(
                        x + 1,
                        y + 1,
                        x + width - 1,
                        y + height - 1,
                        0x22000000
                );
            }

            int textColor =
                    active
                            ? accentColor
                            : 0xFF666666;

            guiGraphics.drawCenteredString(
                    Minecraft.getInstance().font,
                    getMessage(),
                    x + width / 2,
                    y + (height - 8) / 2,
                    textColor
            );
        }
    }
}