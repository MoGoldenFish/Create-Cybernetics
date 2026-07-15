package com.perigrine3.createcybernetics.screen.custom.computer.game.chess;

import com.perigrine3.createcybernetics.client.computer.ChatSpaceClientData;
import com.perigrine3.createcybernetics.client.computer.ChessClientData;
import com.perigrine3.createcybernetics.network.payload.ChatSpaceRequestSyncPayload;
import com.perigrine3.createcybernetics.network.payload.ChessRequestSyncPayload;
import com.perigrine3.createcybernetics.screen.custom.computer.ComputerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class ChessLauncherScreen extends Screen {
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 174;

    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;

    private static final int ACCENT_COLOR = 0xFF39FF14;
    private static final int PANEL_BACKGROUND = 0xF0000000;
    private static final int BUTTON_BACKGROUND = 0xFF000000;
    private static final int BUTTON_HOVER_BACKGROUND = 0xFF123B0D;
    private static final int DISABLED_COLOR = 0xFF777777;

    private static final long SYNC_INTERVAL_MILLIS = 750L;

    private final Screen computerScreen;
    private final BlockPos computerPos;

    private int leftPos;
    private int topPos;

    private long nextSyncAt;

    private AccentButton resumeButton;

    public ChessLauncherScreen(
            Screen computerScreen
    ) {
        super(
                Component.translatable(
                        "gui.createcybernetics.chess.title"
                )
        );

        this.computerScreen = computerScreen;
        this.computerPos = resolveComputerPos(
                computerScreen
        );
    }

    private static BlockPos resolveComputerPos(
            Screen computerScreen
    ) {
        if (computerScreen instanceof ComputerScreen screen) {
            return screen.getMenu()
                    .getComputerPos()
                    .immutable();
        }

        return BlockPos.ZERO;
    }

    public Screen getComputerScreen() {
        return computerScreen;
    }

    public BlockPos getComputerPos() {
        return computerPos;
    }

    @Override
    protected void init() {
        leftPos =
                (width - PANEL_WIDTH) / 2;

        topPos =
                (height - PANEL_HEIGHT) / 2;

        nextSyncAt = 0L;

        int buttonX =
                leftPos
                        + (PANEL_WIDTH - BUTTON_WIDTH) / 2;

        addRenderableWidget(
                new AccentButton(
                        buttonX,
                        topPos + 39,
                        BUTTON_WIDTH,
                        BUTTON_HEIGHT,
                        Component.translatable(
                                "gui.createcybernetics.chess.one_player"
                        ),
                        button ->
                                Minecraft.getInstance().setScreen(
                                        new ChessBoardScreen(
                                                this,
                                                computerScreen,
                                                true
                                        )
                                )
                )
        );

        addRenderableWidget(
                new AccentButton(
                        buttonX,
                        topPos + 65,
                        BUTTON_WIDTH,
                        BUTTON_HEIGHT,
                        Component.translatable(
                                "gui.createcybernetics.chess.two_player"
                        ),
                        button ->
                                Minecraft.getInstance().setScreen(
                                        new ChessMultiplayerScreen(
                                                this,
                                                computerScreen,
                                                computerPos
                                        )
                                )
                )
        );

        resumeButton = addRenderableWidget(
                new AccentButton(
                        buttonX,
                        topPos + 91,
                        BUTTON_WIDTH,
                        BUTTON_HEIGHT,
                        Component.translatable(
                                "gui.createcybernetics.chess.resume"
                        ),
                        button -> openActiveSession()
                )
        );

        addRenderableWidget(
                new AccentButton(
                        buttonX,
                        topPos + 128,
                        BUTTON_WIDTH,
                        BUTTON_HEIGHT,
                        Component.translatable(
                                "gui.createcybernetics.computer.back"
                        ),
                        button -> onClose()
                )
        );

        requestAllSync();
        updateResumeButton();
    }

    private void requestAllSync() {
        if (computerPos.equals(
                BlockPos.ZERO
        )) {
            return;
        }

        PacketDistributor.sendToServer(
                new ChessRequestSyncPayload(
                        computerPos
                )
        );

        PacketDistributor.sendToServer(
                new ChatSpaceRequestSyncPayload(
                        computerPos
                )
        );

        nextSyncAt =
                System.currentTimeMillis()
                        + SYNC_INTERVAL_MILLIS;
    }

    private void updatePeriodicSync() {
        if (System.currentTimeMillis() <
                nextSyncAt) {
            return;
        }

        requestAllSync();
    }

    private void updateResumeButton() {
        if (resumeButton == null) {
            return;
        }

        resumeButton.active =
                ChessClientData.getActiveSession()
                        != null;
    }

    private void openActiveSession() {
        ChessClientData.ClientSession session =
                ChessClientData.getActiveSession();

        if (session == null) {
            return;
        }

        Minecraft.getInstance().setScreen(
                new ChessMultiplayerBoardScreen(
                        this,
                        computerScreen,
                        computerPos,
                        session.sessionId()
                )
        );
    }

    @Override
    public void renderBackground(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
    }

    private void renderComputerScreen(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (computerScreen == null) {
            return;
        }

        computerScreen.render(
                guiGraphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        updatePeriodicSync();
        updateResumeButton();

        renderComputerScreen(
                guiGraphics,
                mouseX,
                mouseY,
                partialTick
        );

        guiGraphics.fill(
                leftPos,
                topPos,
                leftPos + PANEL_WIDTH,
                topPos + PANEL_HEIGHT,
                PANEL_BACKGROUND
        );

        drawBorder(
                guiGraphics,
                leftPos,
                topPos,
                PANEL_WIDTH,
                PANEL_HEIGHT,
                ACCENT_COLOR
        );

        guiGraphics.drawCenteredString(
                font,
                title,
                leftPos + PANEL_WIDTH / 2,
                topPos + 17,
                ACCENT_COLOR
        );

        int incomingCount =
                ChessClientData.getIncomingInvites()
                        .size();

        if (incomingCount > 0) {
            guiGraphics.drawCenteredString(
                    font,
                    Component.translatable(
                            "gui.createcybernetics.chess.pending_requests",
                            incomingCount
                    ),
                    leftPos + PANEL_WIDTH / 2,
                    topPos + 115,
                    0xFFFFFF55
            );
        }

        super.render(
                guiGraphics,
                mouseX,
                mouseY,
                partialTick
        );

        renderInviteNotification(
                guiGraphics
        );
    }

    private void renderInviteNotification(
            GuiGraphics guiGraphics
    ) {
        if (!ChessClientData.hasActiveNotification()) {
            return;
        }

        String senderCode =
                ChessClientData.getPendingNotificationCode();

        ChatSpaceClientData.ClientContact contact =
                ChatSpaceClientData.getContact(
                        senderCode
                );

        String senderName =
                contact == null
                        ? senderCode
                        : contact.displayName();

        int popupWidth = 154;
        int popupHeight = 34;

        int popupX =
                width - popupWidth - 8;

        int popupY =
                height - popupHeight - 8;

        guiGraphics.fill(
                popupX,
                popupY,
                popupX + popupWidth,
                popupY + popupHeight,
                0xF0000000
        );

        drawBorder(
                guiGraphics,
                popupX,
                popupY,
                popupWidth,
                popupHeight,
                ACCENT_COLOR
        );

        guiGraphics.drawString(
                font,
                Component.translatable(
                        "gui.createcybernetics.chess.request_from",
                        senderName
                ),
                popupX + 7,
                popupY + 8,
                ACCENT_COLOR,
                false
        );

        guiGraphics.drawString(
                font,
                Component.translatable(
                        "gui.createcybernetics.chess.open_multiplayer"
                ),
                popupX + 7,
                popupY + 20,
                0xFFFFFFFF,
                false
        );
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (ChessClientData.hasActiveNotification()) {
            ChessClientData.clearNotification();
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
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
                computerScreen
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
        private AccentButton(
                int x,
                int y,
                int width,
                int height,
                Component message,
                OnPress onPress
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
                    isHoveredOrFocused()
                            ? BUTTON_HOVER_BACKGROUND
                            : BUTTON_BACKGROUND
            );

            drawBorder(
                    guiGraphics,
                    x,
                    y,
                    width,
                    height,
                    active
                            ? ACCENT_COLOR
                            : DISABLED_COLOR
            );

            guiGraphics.drawCenteredString(
                    Minecraft.getInstance().font,
                    getMessage(),
                    x + width / 2,
                    y + (height - 8) / 2,
                    active
                            ? ACCENT_COLOR
                            : DISABLED_COLOR
            );
        }
    }
}