package com.perigrine3.createcybernetics.screen.custom.computer;

import com.perigrine3.createcybernetics.item.generic.InfologFormatting;
import com.perigrine3.createcybernetics.item.generic.InfologTextData;
import com.perigrine3.createcybernetics.mixin.client.MultiLineEditBoxAccessor;
import com.perigrine3.createcybernetics.network.payload.InfologSaveComputerPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public final class ComputerInfologEditScreen extends Screen {

    private static final int GUI_WIDTH = 390;
    private static final int GUI_HEIGHT = 256;

    private static final int PAD_LEFT = 14;
    private static final int PAD_TOP = 14;
    private static final int PAD_BOTTOM = 42;

    private static final int FORMAT_PANEL_WIDTH = 128;
    private static final int FORMAT_PANEL_GAP = 8;

    private static final int FORMAT_PANEL_BORDER_HEIGHT = 204;

    private static final int PREVIEW_PADDING = 5;
    private static final int PREVIEW_Y_OFFSET = 124;
    private static final int PREVIEW_HEIGHT = 74;
    private static final int PREVIEW_LINE_HEIGHT = 10;

    private static final int MAX_TEXT_LENGTH = 32_000;

    private final Screen parentScreen;
    private final BlockPos computerPos;
    private final int towerSlot;

    private final ItemStack sourceStack;
    private final String initialText;
    private final String initialTitle;

    private int leftPos;
    private int topPos;

    private int editorX;
    private int editorY;
    private int editorWidth;
    private int editorHeight;

    private int formatPanelX;
    private int formatPanelY;

    private int accentColor = 0xFFFFFFFF;
    private int previewScrollOffset;

    private MultiLineEditBox editor;

    public ComputerInfologEditScreen(
            Screen parentScreen,
            BlockPos computerPos,
            int towerSlot,
            ItemStack sourceStack
    ) {
        this(
                parentScreen,
                computerPos,
                towerSlot,
                sourceStack,
                InfologTextData.getText(sourceStack)
        );
    }

    public ComputerInfologEditScreen(
            Screen parentScreen,
            BlockPos computerPos,
            int towerSlot,
            ItemStack sourceStack,
            String initialText
    ) {
        super(
                Component.translatable(
                        "gui.infolog.title"
                )
        );

        this.parentScreen = parentScreen;
        this.computerPos = computerPos.immutable();
        this.towerSlot = towerSlot;
        this.sourceStack = sourceStack.copy();

        this.initialText =
                InfologFormatting.toEditorText(
                        initialText
                );

        this.initialTitle =
                InfologTextData.getTitle(
                        sourceStack
                );
    }

    @Override
    protected void init() {
        leftPos =
                (width - GUI_WIDTH) / 2;

        topPos =
                (height - GUI_HEIGHT) / 2;

        editorX =
                leftPos + PAD_LEFT;

        editorY =
                topPos + PAD_TOP;

        editorWidth =
                GUI_WIDTH
                        - PAD_LEFT
                        - FORMAT_PANEL_WIDTH
                        - FORMAT_PANEL_GAP
                        - PAD_LEFT;

        editorHeight =
                GUI_HEIGHT
                        - PAD_TOP
                        - PAD_BOTTOM;

        formatPanelX =
                editorX
                        + editorWidth
                        + FORMAT_PANEL_GAP;

        formatPanelY =
                editorY;

        accentColor =
                resolveShardColor();

        previewScrollOffset = 0;

        editor = new MultiLineEditBox(
                font,
                editorX,
                editorY,
                editorWidth,
                editorHeight,
                Component.empty(),
                Component.empty()
        );

        editor.setCharacterLimit(
                MAX_TEXT_LENGTH
        );

        editor.setValue(
                initialText
        );

        addRenderableWidget(
                editor
        );

        setInitialFocus(
                editor
        );

        addFormattingButtons();
        addBottomButtons();
    }

    private void addFormattingButtons() {
        int buttonWidth = 25;
        int buttonHeight = 18;
        int gap = 3;

        int firstColumn =
                formatPanelX;

        int secondColumn =
                formatPanelX
                        + buttonWidth
                        + gap;

        int rowY =
                formatPanelY;

        addFormattingButton(
                firstColumn,
                rowY,
                buttonWidth,
                buttonHeight,
                Component.literal("B"),
                "l",
                Component.translatable(
                        "gui.infolog.format.bold"
                )
        );

        addFormattingButton(
                secondColumn,
                rowY,
                buttonWidth,
                buttonHeight,
                Component.literal("I"),
                "o",
                Component.translatable(
                        "gui.infolog.format.italic"
                )
        );

        rowY += buttonHeight + gap;

        addFormattingButton(
                firstColumn,
                rowY,
                buttonWidth,
                buttonHeight,
                Component.literal("U"),
                "n",
                Component.translatable(
                        "gui.infolog.format.underline"
                )
        );

        addFormattingButton(
                secondColumn,
                rowY,
                buttonWidth,
                buttonHeight,
                Component.literal("S"),
                "m",
                Component.translatable(
                        "gui.infolog.format.strikethrough"
                )
        );

        rowY += buttonHeight + gap;

        addFormattingButton(
                firstColumn,
                rowY,
                buttonWidth * 2 + gap,
                buttonHeight,
                Component.literal("RESET"),
                "r",
                Component.translatable(
                        "gui.infolog.format.reset"
                )
        );

        rowY += buttonHeight + 7;

        addColorButtons(
                rowY
        );
    }

    private void addFormattingButton(
            int x,
            int y,
            int width,
            int height,
            Component label,
            String code,
            Component tooltip
    ) {
        FormattingButton button =
                new FormattingButton(
                        x,
                        y,
                        width,
                        height,
                        label,
                        pressed -> insertFormattingCode(code),
                        accentColor
                );

        button.setTooltip(
                Tooltip.create(tooltip)
        );

        addRenderableWidget(
                button
        );
    }

    private void addColorButtons(
            int startY
    ) {
        String[] codes = {
                "0", "1", "2", "3",
                "4", "5", "6", "7",
                "8", "9", "a", "b",
                "c", "d", "e", "f"
        };

        int[] colors = {
                0xFF000000,
                0xFF0000AA,
                0xFF00AA00,
                0xFF00AAAA,
                0xFFAA0000,
                0xFFAA00AA,
                0xFFFFAA00,
                0xFFAAAAAA,
                0xFF555555,
                0xFF5555FF,
                0xFF55FF55,
                0xFF55FFFF,
                0xFFFF5555,
                0xFFFF55FF,
                0xFFFFFF55,
                0xFFFFFFFF
        };

        String[] translationSuffixes = {
                "black",
                "dark_blue",
                "dark_green",
                "dark_aqua",
                "dark_red",
                "dark_purple",
                "gold",
                "gray",
                "dark_gray",
                "blue",
                "green",
                "aqua",
                "red",
                "light_purple",
                "yellow",
                "white"
        };

        int size = 11;
        int gap = 2;

        for (int index = 0;
             index < codes.length;
             index++) {
            int column =
                    index % 4;

            int row =
                    index / 4;

            String formattingCode =
                    codes[index];

            ColorFormattingButton button =
                    new ColorFormattingButton(
                            formatPanelX
                                    + column * (size + gap),
                            startY
                                    + row * (size + gap),
                            size,
                            size,
                            colors[index],
                            pressed -> insertFormattingCode(
                                    formattingCode
                            )
                    );

            button.setTooltip(
                    Tooltip.create(
                            Component.translatable(
                                    "gui.infolog.format.color."
                                            + translationSuffixes[index]
                            )
                    )
            );

            addRenderableWidget(
                    button
            );
        }
    }

    private void addBottomButtons() {
        int buttonY =
                editorY
                        + editorHeight
                        + 10;

        int buttonWidth = 70;
        int gap = 6;

        int totalWidth =
                buttonWidth * 3
                        + gap * 2;

        int startX =
                editorX
                        + (editorWidth - totalWidth) / 2;

        addRenderableWidget(
                new AccentButton(
                        startX,
                        buttonY,
                        buttonWidth,
                        20,
                        Component.translatable(
                                "gui.cancel"
                        ),
                        button -> onClose(),
                        accentColor
                )
        );

        addRenderableWidget(
                new AccentButton(
                        startX + buttonWidth + gap,
                        buttonY,
                        buttonWidth,
                        20,
                        Component.translatable(
                                "gui.done"
                        ),
                        button -> {
                            saveDraft();
                            onClose();
                        },
                        accentColor
                )
        );

        addRenderableWidget(
                new AccentButton(
                        startX + (buttonWidth + gap) * 2,
                        buttonY,
                        buttonWidth,
                        20,
                        Component.translatable(
                                "gui.infolog.save"
                        ),
                        button -> openTitleScreen(),
                        accentColor
                )
        );
    }

    private void insertFormattingCode(
            String code
    ) {
        if (editor == null ||
                code == null ||
                code.isBlank()) {
            return;
        }

        MultilineTextField textField =
                ((MultiLineEditBoxAccessor) editor)
                        .createCybernetics$getTextField();

        textField.insertText(
                Character.toString(
                        InfologFormatting.EDITOR_PREFIX
                ) + code
        );

        editor.setFocused(true);
        setFocused(editor);
    }

    private int resolveShardColor() {
        DyedItemColor dyed =
                sourceStack.get(
                        DataComponents.DYED_COLOR
                );

        if (dyed == null) {
            return 0xFFFFFFFF;
        }

        return 0xFF000000
                | (dyed.rgb() & 0x00FFFFFF);
    }

    private String getEditorText() {
        String text =
                editor.getValue();

        if (text == null) {
            return "";
        }

        text =
                InfologFormatting.toSavedText(
                        text
                );

        if (text.length() > MAX_TEXT_LENGTH) {
            return text.substring(
                    0,
                    MAX_TEXT_LENGTH
            );
        }

        return text;
    }

    private void saveDraft() {
        PacketDistributor.sendToServer(
                new InfologSaveComputerPayload(
                        computerPos,
                        towerSlot,
                        getEditorText(),
                        initialTitle,
                        false
                )
        );
    }

    private void openTitleScreen() {
        Minecraft.getInstance().setScreen(
                new ComputerInfologTitleScreen(
                        parentScreen,
                        computerPos,
                        towerSlot,
                        sourceStack,
                        getEditorText(),
                        accentColor
                )
        );
    }

    private void renderPreview(
            GuiGraphics guiGraphics
    ) {
        int previewX =
                formatPanelX;

        int previewY =
                formatPanelY
                        + PREVIEW_Y_OFFSET;

        int previewWidth =
                FORMAT_PANEL_WIDTH - 6;

        guiGraphics.fill(
                previewX,
                previewY,
                previewX + previewWidth,
                previewY + PREVIEW_HEIGHT,
                0xDD000000
        );

        drawBorder(
                guiGraphics,
                previewX,
                previewY,
                previewWidth,
                PREVIEW_HEIGHT,
                accentColor
        );

        guiGraphics.drawString(
                font,
                Component.translatable(
                        "gui.infolog.format.preview"
                ),
                previewX + PREVIEW_PADDING,
                previewY + PREVIEW_PADDING,
                accentColor,
                false
        );

        String editorText =
                editor == null
                        ? ""
                        : editor.getValue();

        List<FormattedCharSequence> previewLines =
                font.split(
                        InfologFormatting.parseEditorText(
                                editorText
                        ),
                        previewWidth
                                - PREVIEW_PADDING * 2
                                - 3
                );

        int textAreaY =
                previewY + 17;

        int textAreaHeight =
                PREVIEW_HEIGHT - 21;

        int visibleLines =
                Math.max(
                        1,
                        textAreaHeight
                                / PREVIEW_LINE_HEIGHT
                );

        int maximumScroll =
                Math.max(
                        0,
                        previewLines.size()
                                - visibleLines
                );

        previewScrollOffset =
                Mth.clamp(
                        previewScrollOffset,
                        0,
                        maximumScroll
                );

        int endIndex =
                Math.min(
                        previewLines.size(),
                        previewScrollOffset
                                + visibleLines
                );

        guiGraphics.enableScissor(
                previewX + 1,
                textAreaY,
                previewX + previewWidth - 1,
                previewY + PREVIEW_HEIGHT - 2
        );

        int renderY =
                textAreaY;

        for (int index = previewScrollOffset;
             index < endIndex;
             index++) {
            guiGraphics.drawString(
                    font,
                    previewLines.get(index),
                    previewX + PREVIEW_PADDING,
                    renderY,
                    0xFFFFFFFF,
                    false
            );

            renderY += PREVIEW_LINE_HEIGHT;
        }

        guiGraphics.disableScissor();
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

        drawBorder(
                guiGraphics,
                editorX - 1,
                editorY - 1,
                editorWidth + 2,
                editorHeight + 2,
                accentColor
        );

        drawBorder(
                guiGraphics,
                formatPanelX - 3,
                formatPanelY - 3,
                FORMAT_PANEL_WIDTH,
                FORMAT_PANEL_BORDER_HEIGHT,
                accentColor
        );

        renderPreview(
                guiGraphics
        );
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY
    ) {
        int previewX =
                formatPanelX;

        int previewY =
                formatPanelY
                        + PREVIEW_Y_OFFSET;

        int previewWidth =
                FORMAT_PANEL_WIDTH - 6;

        boolean overPreview =
                mouseX >= previewX
                        && mouseX < previewX + previewWidth
                        && mouseY >= previewY
                        && mouseY < previewY + PREVIEW_HEIGHT;

        if (!overPreview) {
            return super.mouseScrolled(
                    mouseX,
                    mouseY,
                    scrollX,
                    scrollY
            );
        }

        int direction =
                scrollY > 0.0D
                        ? -1
                        : 1;

        previewScrollOffset =
                Math.max(
                        0,
                        previewScrollOffset
                                + direction
                );

        return true;
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

        if (editor != null &&
                editor.isFocused() &&
                inventoryKey) {
            return true;
        }

        if (keyCode == 256) {
            onClose();
            return true;
        }

        return super.keyPressed(
                keyCode,
                scanCode,
                modifiers
        );
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(
                parentScreen
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

    private static class AccentButton extends Button {
        protected final int accentColor;

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

            guiGraphics.drawCenteredString(
                    Minecraft.getInstance().font,
                    getMessage(),
                    x + width / 2,
                    y + (height - 8) / 2,
                    accentColor
            );
        }
    }

    private static final class FormattingButton
            extends AccentButton {

        private FormattingButton(
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
                    accentColor
            );
        }
    }

    private static final class ColorFormattingButton
            extends Button {

        private final int color;

        private ColorFormattingButton(
                int x,
                int y,
                int width,
                int height,
                int color,
                OnPress onPress
        ) {
            super(
                    x,
                    y,
                    width,
                    height,
                    Component.empty(),
                    onPress,
                    DEFAULT_NARRATION
            );

            this.color = color;
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
                    0xFFFFFFFF
            );

            guiGraphics.fill(
                    x + 1,
                    y + 1,
                    x + width - 1,
                    y + height - 1,
                    color
            );

            if (isHoveredOrFocused()) {
                drawBorder(
                        guiGraphics,
                        x,
                        y,
                        width,
                        height,
                        0xFFFFFF55
                );
            }
        }
    }
}