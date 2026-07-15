package com.perigrine3.createcybernetics.screen.custom.chipware;

import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.generic.InfologFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class InfologReadScreen extends Screen {
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 256;

    private static final int TEXT_X_OFFSET = 14;
    private static final int TEXT_Y_OFFSET = 30;
    private static final int TEXT_WIDTH = 228;
    private static final int TEXT_HEIGHT = 174;

    private static final int TEXT_PADDING_LEFT = 4;
    private static final int TEXT_PADDING_RIGHT = 7;
    private static final int TEXT_PADDING_TOP = 3;
    private static final int TEXT_PADDING_BOTTOM = 3;

    private static final int SCROLLBAR_WIDTH = 2;
    private static final int SCROLLBAR_RIGHT_PADDING = 2;

    private static final int LINE_HEIGHT = 10;

    private static final int CONTENT_WIDTH =
            TEXT_WIDTH
                    - TEXT_PADDING_LEFT
                    - TEXT_PADDING_RIGHT
                    - SCROLLBAR_WIDTH
                    - SCROLLBAR_RIGHT_PADDING;

    private static final int CONTENT_HEIGHT =
            TEXT_HEIGHT
                    - TEXT_PADDING_TOP
                    - TEXT_PADDING_BOTTOM;

    private final int chipwareSlot;
    private final String title;
    private final String text;

    private final @Nullable Screen parentScreen;
    private final ItemStack sourceStack;

    private int leftPos;
    private int topPos;

    private int textX;
    private int textY;

    private int accentColor = 0xFFFFFFFF;
    private int scrollOffset;

    private List<FormattedCharSequence> formattedLines =
            List.of();

    public InfologReadScreen(
            int chipwareSlot,
            String title,
            String text
    ) {
        this(
                null,
                ItemStack.EMPTY,
                chipwareSlot,
                title,
                text
        );
    }

    public InfologReadScreen(
            Screen parentScreen,
            ItemStack sourceStack,
            String title,
            String text
    ) {
        this(
                parentScreen,
                sourceStack,
                -1,
                title,
                text
        );
    }

    private InfologReadScreen(
            @Nullable Screen parentScreen,
            ItemStack sourceStack,
            int chipwareSlot,
            String title,
            String text
    ) {
        super(
                Component.translatable(
                        "gui.infolog.read_title"
                )
        );

        this.parentScreen = parentScreen;
        this.sourceStack = sourceStack.copy();
        this.chipwareSlot = chipwareSlot;

        this.title =
                title == null || title.isBlank()
                        ? Component.translatable(
                        "gui.infolog.untitled"
                ).getString()
                        : title;

        this.text =
                text == null
                        ? ""
                        : text;
    }

    @Override
    protected void init() {
        leftPos =
                (width - GUI_WIDTH) / 2;

        topPos =
                (height - GUI_HEIGHT) / 2;

        textX =
                leftPos + TEXT_X_OFFSET;

        textY =
                topPos + TEXT_Y_OFFSET;

        accentColor =
                resolveAccentColor();

        formattedLines =
                font.split(
                        InfologFormatting.parseSavedText(
                                text
                        ),
                        CONTENT_WIDTH
                );

        scrollOffset = 0;

        int buttonWidth = 90;
        int buttonHeight = 20;

        int buttonX =
                leftPos
                        + (GUI_WIDTH - buttonWidth) / 2;

        int buttonY =
                topPos + GUI_HEIGHT - 30;

        addRenderableWidget(
                new AccentButton(
                        buttonX,
                        buttonY,
                        buttonWidth,
                        buttonHeight,
                        Component.translatable(
                                parentScreen == null
                                        ? "gui.done"
                                        : "gui.createcybernetics.computer.back"
                        ),
                        button -> onClose(),
                        accentColor
                )
        );
    }

    private int resolveAccentColor() {
        if (!sourceStack.isEmpty()) {
            DyedItemColor dyed =
                    sourceStack.get(
                            DataComponents.DYED_COLOR
                    );

            if (dyed != null) {
                return 0xFF000000
                        | (dyed.rgb() & 0x00FFFFFF);
            }
        }

        return resolveChipDyeOrWhite();
    }

    private int resolveChipDyeOrWhite() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return 0xFFFFFFFF;
        }

        if (!minecraft.player.hasData(
                ModAttachments.CYBERWARE
        )) {
            return 0xFFFFFFFF;
        }

        PlayerCyberwareData data =
                minecraft.player.getData(
                        ModAttachments.CYBERWARE
                );

        if (data == null) {
            return 0xFFFFFFFF;
        }

        if (chipwareSlot < 0 ||
                chipwareSlot >=
                        PlayerCyberwareData.CHIPWARE_SLOT_COUNT) {
            return 0xFFFFFFFF;
        }

        ItemStack stack =
                data.getChipwareStack(
                        chipwareSlot
                );

        if (stack.isEmpty()) {
            return 0xFFFFFFFF;
        }

        DyedItemColor dyed =
                stack.get(
                        DataComponents.DYED_COLOR
                );

        if (dyed == null) {
            return 0xFFFFFFFF;
        }

        return 0xFF000000
                | (dyed.rgb() & 0x00FFFFFF);
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

        guiGraphics.drawCenteredString(
                font,
                Component.literal(title),
                leftPos + GUI_WIDTH / 2,
                topPos + 12,
                accentColor
        );

        guiGraphics.fill(
                textX,
                textY,
                textX + TEXT_WIDTH,
                textY + TEXT_HEIGHT,
                0xDD000000
        );

        drawBorder(
                guiGraphics,
                textX - 1,
                textY - 1,
                TEXT_WIDTH + 2,
                TEXT_HEIGHT + 2,
                accentColor
        );

        renderFormattedText(
                guiGraphics
        );

        renderScrollbar(
                guiGraphics
        );
    }

    private void renderFormattedText(
            GuiGraphics guiGraphics
    ) {
        int visibleLines =
                getVisibleLineCount();

        int endIndex =
                Math.min(
                        formattedLines.size(),
                        scrollOffset + visibleLines
                );

        int contentX =
                textX + TEXT_PADDING_LEFT;

        int contentY =
                textY + TEXT_PADDING_TOP;

        guiGraphics.enableScissor(
                contentX,
                contentY,
                contentX + CONTENT_WIDTH,
                contentY + CONTENT_HEIGHT
        );

        int renderY =
                contentY;

        for (int index = scrollOffset;
             index < endIndex;
             index++) {
            guiGraphics.drawString(
                    font,
                    formattedLines.get(index),
                    contentX,
                    renderY,
                    0xFFFFFFFF,
                    false
            );

            renderY += LINE_HEIGHT;
        }

        guiGraphics.disableScissor();
    }

    private void renderScrollbar(
            GuiGraphics guiGraphics
    ) {
        int visibleLines =
                getVisibleLineCount();

        if (formattedLines.size() <= visibleLines) {
            return;
        }

        int scrollbarX =
                textX
                        + TEXT_WIDTH
                        - SCROLLBAR_RIGHT_PADDING
                        - SCROLLBAR_WIDTH;

        int scrollbarY =
                textY + TEXT_PADDING_TOP;

        int scrollbarHeight =
                CONTENT_HEIGHT;

        guiGraphics.fill(
                scrollbarX,
                scrollbarY,
                scrollbarX + SCROLLBAR_WIDTH,
                scrollbarY + scrollbarHeight,
                0xFF222222
        );

        int handleHeight =
                Math.max(
                        8,
                        scrollbarHeight
                                * visibleLines
                                / formattedLines.size()
                );

        int maximumScroll =
                formattedLines.size()
                        - visibleLines;

        int travel =
                scrollbarHeight
                        - handleHeight;

        int handleY =
                scrollbarY
                        + travel
                        * scrollOffset
                        / maximumScroll;

        guiGraphics.fill(
                scrollbarX,
                handleY,
                scrollbarX + SCROLLBAR_WIDTH,
                handleY + handleHeight,
                accentColor
        );
    }

    private int getVisibleLineCount() {
        return Math.max(
                1,
                CONTENT_HEIGHT / LINE_HEIGHT
        );
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY
    ) {
        if (mouseX < textX ||
                mouseX >= textX + TEXT_WIDTH ||
                mouseY < textY ||
                mouseY >= textY + TEXT_HEIGHT) {
            return super.mouseScrolled(
                    mouseX,
                    mouseY,
                    scrollX,
                    scrollY
            );
        }

        int visibleLines =
                getVisibleLineCount();

        int maximumScroll =
                Math.max(
                        0,
                        formattedLines.size()
                                - visibleLines
                );

        int direction =
                scrollY > 0.0D
                        ? -1
                        : 1;

        scrollOffset =
                Mth.clamp(
                        scrollOffset + direction,
                        0,
                        maximumScroll
                );

        return true;
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
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

    private static final class AccentButton
            extends Button {
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

            guiGraphics.drawCenteredString(
                    Minecraft.getInstance().font,
                    getMessage(),
                    x + width / 2,
                    y + (height - 8) / 2,
                    accentColor
            );
        }
    }
}