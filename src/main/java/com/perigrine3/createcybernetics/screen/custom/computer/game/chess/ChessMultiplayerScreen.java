package com.perigrine3.createcybernetics.screen.custom.computer.game.chess;

import com.perigrine3.createcybernetics.client.computer.ChatSpaceClientData;
import com.perigrine3.createcybernetics.client.computer.ChessClientData;
import com.perigrine3.createcybernetics.network.payload.ChatSpaceRequestSyncPayload;
import com.perigrine3.createcybernetics.network.payload.ChessCreateInvitePayload;
import com.perigrine3.createcybernetics.network.payload.ChessRequestSyncPayload;
import com.perigrine3.createcybernetics.network.payload.ChessRespondInvitePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

public final class ChessMultiplayerScreen extends Screen {
    private static final int PANEL_WIDTH = 270;
    private static final int PANEL_HEIGHT = 220;

    private static final int ACCENT_COLOR = 0xFF39FF14;
    private static final int PANEL_BACKGROUND = 0xF0000000;

    private static final int BUTTON_BACKGROUND = 0xFF000000;
    private static final int BUTTON_HOVER_BACKGROUND = 0xFF123B0D;
    private static final int SELECTED_BACKGROUND = 0xFF1C4F16;
    private static final int DISABLED_COLOR = 0xFF777777;

    private static final int LIST_X_OFFSET = 10;
    private static final int LIST_Y_OFFSET = 31;
    private static final int LIST_WIDTH = 122;
    private static final int LIST_HEIGHT = 116;

    private static final int ENTRY_HEIGHT = 18;
    private static final int MAX_VISIBLE_CONTACTS = 6;

    private static final long SYNC_INTERVAL_MILLIS = 650L;

    private final Screen launcherScreen;
    private final Screen computerScreen;
    private final BlockPos computerPos;

    private int leftPos;
    private int topPos;

    private int contactScrollOffset;

    private String selectedRemoteCode = "";

    private EditBox codeBox;

    private AccentButton inviteButton;
    private AccentButton acceptButton;
    private AccentButton declineButton;
    private AccentButton resumeButton;

    private long nextSyncAt;

    private String localStatus = "";
    private long localStatusExpiresAt;

    public ChessMultiplayerScreen(
            Screen launcherScreen,
            Screen computerScreen,
            BlockPos computerPos
    ) {
        super(
                Component.translatable(
                        "gui.createcybernetics.chess.multiplayer"
                )
        );

        this.launcherScreen = launcherScreen;
        this.computerScreen = computerScreen;
        this.computerPos = computerPos.immutable();
    }

    @Override
    protected void init() {
        leftPos =
                (width - PANEL_WIDTH) / 2;

        topPos =
                (height - PANEL_HEIGHT) / 2;

        contactScrollOffset = 0;
        nextSyncAt = 0L;

        int rightX =
                leftPos + 143;

        codeBox = new EditBox(
                font,
                rightX,
                topPos + 48,
                116,
                18,
                Component.translatable(
                        "gui.createcybernetics.chess.computer_code"
                )
        );

        codeBox.setMaxLength(5);

        codeBox.setFilter(
                value ->
                        value.chars()
                                .allMatch(
                                        character ->
                                                Character.isLetterOrDigit(
                                                        character
                                                )
                                )
        );

        codeBox.setResponder(
                value -> {
                    selectedRemoteCode =
                            normalizeCode(
                                    value
                            );

                    updateButtons();
                }
        );

        addRenderableWidget(
                codeBox
        );

        inviteButton = addRenderableWidget(
                new AccentButton(
                        rightX,
                        topPos + 72,
                        116,
                        18,
                        Component.translatable(
                                "gui.createcybernetics.chess.send_invite"
                        ),
                        button -> sendInvite()
                )
        );

        acceptButton = addRenderableWidget(
                new AccentButton(
                        rightX,
                        topPos + 113,
                        55,
                        18,
                        Component.translatable(
                                "gui.createcybernetics.chess.accept"
                        ),
                        button -> respondToNewestInvite(
                                true
                        )
                )
        );

        declineButton = addRenderableWidget(
                new AccentButton(
                        rightX + 61,
                        topPos + 113,
                        55,
                        18,
                        Component.translatable(
                                "gui.createcybernetics.chess.decline"
                        ),
                        button -> respondToNewestInvite(
                                false
                        )
                )
        );

        resumeButton = addRenderableWidget(
                new AccentButton(
                        rightX,
                        topPos + 153,
                        116,
                        18,
                        Component.translatable(
                                "gui.createcybernetics.chess.resume"
                        ),
                        button -> openActiveSession()
                )
        );

        addRenderableWidget(
                new AccentButton(
                        leftPos + 10,
                        topPos + PANEL_HEIGHT - 27,
                        74,
                        18,
                        Component.translatable(
                                "gui.createcybernetics.computer.back"
                        ),
                        button -> onClose()
                )
        );

        addRenderableWidget(
                new AccentButton(
                        leftPos + PANEL_WIDTH - 84,
                        topPos + PANEL_HEIGHT - 27,
                        74,
                        18,
                        Component.translatable(
                                "gui.createcybernetics.chess.refresh"
                        ),
                        button -> requestAllSync()
                )
        );

        requestAllSync();
        updateButtons();
    }

    private void requestAllSync() {
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

    private void updateButtons() {
        if (inviteButton != null) {
            inviteButton.active =
                    selectedRemoteCode.length() == 5
                            && !selectedRemoteCode.equals(
                            ChatSpaceClientData.getComputerCode()
                    );
        }

        boolean hasIncomingInvite =
                !ChessClientData.getIncomingInvites()
                        .isEmpty();

        if (acceptButton != null) {
            acceptButton.active =
                    hasIncomingInvite;
        }

        if (declineButton != null) {
            declineButton.active =
                    hasIncomingInvite;
        }

        if (resumeButton != null) {
            resumeButton.active =
                    ChessClientData.getActiveSession()
                            != null;
        }
    }

    private void selectContact(
            ChatSpaceClientData.ClientContact contact
    ) {
        selectedRemoteCode =
                normalizeCode(
                        contact.remoteCode()
                );

        codeBox.setValue(
                selectedRemoteCode
        );

        updateButtons();
    }

    private void sendInvite() {
        String receiverCode =
                normalizeCode(
                        selectedRemoteCode
                );

        if (receiverCode.length() != 5) {
            showLocalStatus(
                    Component.translatable(
                            "gui.createcybernetics.chess.invalid_code"
                    ).getString()
            );

            return;
        }

        PacketDistributor.sendToServer(
                new ChessCreateInvitePayload(
                        computerPos,
                        receiverCode
                )
        );

        showLocalStatus(
                Component.translatable(
                        "gui.createcybernetics.chess.invite_sent",
                        receiverCode
                ).getString()
        );

        requestAllSync();
    }

    private void respondToNewestInvite(
            boolean accepted
    ) {
        List<ChessClientData.ClientInvite> incoming =
                ChessClientData.getIncomingInvites();

        if (incoming.isEmpty()) {
            return;
        }

        ChessClientData.ClientInvite invite =
                incoming.get(0);

        PacketDistributor.sendToServer(
                new ChessRespondInvitePayload(
                        computerPos,
                        invite.inviteId(),
                        accepted
                )
        );

        showLocalStatus(
                Component.translatable(
                        accepted
                                ? "gui.createcybernetics.chess.request_accepted"
                                : "gui.createcybernetics.chess.request_declined"
                ).getString()
        );

        requestAllSync();
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

    private void showLocalStatus(
            String status
    ) {
        localStatus =
                status == null
                        ? ""
                        : status;

        localStatusExpiresAt =
                System.currentTimeMillis()
                        + 4_000L;
    }

    private boolean hasLocalStatus() {
        return !localStatus.isBlank()
                && System.currentTimeMillis()
                < localStatusExpiresAt;
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
        updateButtons();

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
                topPos + 13,
                ACCENT_COLOR
        );

        renderContacts(
                guiGraphics,
                mouseX,
                mouseY
        );

        renderRightPanel(
                guiGraphics
        );

        super.render(
                guiGraphics,
                mouseX,
                mouseY,
                partialTick
        );

        drawEditBoxBorder(
                guiGraphics
        );

        if (hasLocalStatus()) {
            guiGraphics.drawCenteredString(
                    font,
                    localStatus,
                    leftPos + PANEL_WIDTH / 2,
                    topPos + PANEL_HEIGHT - 39,
                    0xFFFFFF55
            );
        }
    }

    private void renderContacts(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY
    ) {
        int listX =
                leftPos + LIST_X_OFFSET;

        int listY =
                topPos + LIST_Y_OFFSET;

        guiGraphics.drawString(
                font,
                Component.translatable(
                        "gui.createcybernetics.chess.connections"
                ),
                listX,
                listY - 12,
                ACCENT_COLOR,
                false
        );

        guiGraphics.fill(
                listX,
                listY,
                listX + LIST_WIDTH,
                listY + LIST_HEIGHT,
                0xCC000000
        );

        drawBorder(
                guiGraphics,
                listX,
                listY,
                LIST_WIDTH,
                LIST_HEIGHT,
                ACCENT_COLOR
        );

        List<ChatSpaceClientData.ClientContact> contacts =
                ChatSpaceClientData.getContacts();

        int maximumOffset =
                Math.max(
                        0,
                        contacts.size()
                                - MAX_VISIBLE_CONTACTS
                );

        contactScrollOffset =
                Math.max(
                        0,
                        Math.min(
                                contactScrollOffset,
                                maximumOffset
                        )
                );

        int endIndex =
                Math.min(
                        contacts.size(),
                        contactScrollOffset
                                + MAX_VISIBLE_CONTACTS
                );

        int renderedRow = 0;

        for (int index = contactScrollOffset;
             index < endIndex;
             index++) {
            ChatSpaceClientData.ClientContact contact =
                    contacts.get(index);

            int entryY =
                    listY + 4
                            + renderedRow * ENTRY_HEIGHT;

            boolean selected =
                    normalizeCode(
                            contact.remoteCode()
                    ).equals(
                            selectedRemoteCode
                    );

            boolean hovered =
                    isInside(
                            mouseX,
                            mouseY,
                            listX + 3,
                            entryY,
                            LIST_WIDTH - 6,
                            ENTRY_HEIGHT - 2
                    );

            if (selected || hovered) {
                guiGraphics.fill(
                        listX + 3,
                        entryY,
                        listX + LIST_WIDTH - 3,
                        entryY + ENTRY_HEIGHT - 2,
                        selected
                                ? SELECTED_BACKGROUND
                                : 0xFF102A0D
                );
            }

            String displayName =
                    contact.displayName();

            displayName =
                    font.plainSubstrByWidth(
                            displayName,
                            LIST_WIDTH - 12
                    );

            guiGraphics.drawString(
                    font,
                    displayName,
                    listX + 6,
                    entryY + 2,
                    selected
                            ? ACCENT_COLOR
                            : 0xFFFFFFFF,
                    false
            );

            guiGraphics.drawString(
                    font,
                    contact.remoteCode(),
                    listX + 6,
                    entryY + 10,
                    0xFF7FAF78,
                    false
            );

            renderedRow++;
        }

        if (contacts.isEmpty()) {
            guiGraphics.drawCenteredString(
                    font,
                    Component.translatable(
                            "gui.createcybernetics.chess.no_connections"
                    ),
                    listX + LIST_WIDTH / 2,
                    listY + LIST_HEIGHT / 2 - 4,
                    DISABLED_COLOR
            );
        }
    }

    private void renderRightPanel(
            GuiGraphics guiGraphics
    ) {
        int rightX =
                leftPos + 143;

        guiGraphics.drawString(
                font,
                Component.translatable(
                        "gui.createcybernetics.chess.computer_code"
                ),
                rightX,
                topPos + 35,
                ACCENT_COLOR,
                false
        );

        List<ChessClientData.ClientInvite> incoming =
                ChessClientData.getIncomingInvites();

        guiGraphics.drawString(
                font,
                Component.translatable(
                        "gui.createcybernetics.chess.incoming_request"
                ),
                rightX,
                topPos + 99,
                ACCENT_COLOR,
                false
        );

        if (!incoming.isEmpty()) {
            ChessClientData.ClientInvite invite =
                    incoming.get(0);

            String senderCode =
                    invite.senderCode();

            ChatSpaceClientData.ClientContact contact =
                    ChatSpaceClientData.getContact(
                            senderCode
                    );

            String senderName =
                    contact == null
                            ? senderCode
                            : contact.displayName();

            guiGraphics.drawString(
                    font,
                    font.plainSubstrByWidth(
                            senderName,
                            116
                    ),
                    rightX,
                    topPos + 136,
                    0xFFFFFFFF,
                    false
            );
        } else {
            guiGraphics.drawString(
                    font,
                    Component.translatable(
                            "gui.createcybernetics.chess.none"
                    ),
                    rightX,
                    topPos + 136,
                    DISABLED_COLOR,
                    false
            );
        }

        int outgoingCount =
                ChessClientData.getOutgoingInvites()
                        .size();

        guiGraphics.drawString(
                font,
                Component.translatable(
                        "gui.createcybernetics.chess.outgoing_count",
                        outgoingCount
                ),
                rightX,
                topPos + 178,
                outgoingCount > 0
                        ? 0xFFFFFF55
                        : DISABLED_COLOR,
                false
        );
    }

    private void drawEditBoxBorder(
            GuiGraphics guiGraphics
    ) {
        if (codeBox == null) {
            return;
        }

        drawBorder(
                guiGraphics,
                codeBox.getX() - 1,
                codeBox.getY() - 1,
                codeBox.getWidth() + 2,
                codeBox.getHeight() + 2,
                codeBox.isFocused()
                        ? ACCENT_COLOR
                        : 0xFF367A30
        );
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button ==
                GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int listX =
                    leftPos + LIST_X_OFFSET;

            int listY =
                    topPos + LIST_Y_OFFSET;

            if (isInside(
                    mouseX,
                    mouseY,
                    listX,
                    listY,
                    LIST_WIDTH,
                    LIST_HEIGHT
            )) {
                List<ChatSpaceClientData.ClientContact> contacts =
                        ChatSpaceClientData.getContacts();

                int row =
                        ((int) mouseY
                                - listY
                                - 4)
                                / ENTRY_HEIGHT;

                int contactIndex =
                        contactScrollOffset
                                + row;

                if (row >= 0 &&
                        row < MAX_VISIBLE_CONTACTS &&
                        contactIndex >= 0 &&
                        contactIndex < contacts.size()) {
                    selectContact(
                            contacts.get(
                                    contactIndex
                            )
                    );

                    return true;
                }
            }
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY
    ) {
        int listX =
                leftPos + LIST_X_OFFSET;

        int listY =
                topPos + LIST_Y_OFFSET;

        if (!isInside(
                mouseX,
                mouseY,
                listX,
                listY,
                LIST_WIDTH,
                LIST_HEIGHT
        )) {
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

        contactScrollOffset =
                Math.max(
                        0,
                        contactScrollOffset
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

        if (codeBox != null &&
                codeBox.isFocused() &&
                inventoryKey) {
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER ||
                keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            sendInvite();
            return true;
        }

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
                launcherScreen
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String normalizeCode(
            String code
    ) {
        if (code == null) {
            return "";
        }

        return code.trim()
                .toUpperCase(
                        Locale.ROOT
                );
    }

    private static boolean isInside(
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height
    ) {
        return mouseX >= x
                && mouseX < x + width
                && mouseY >= y
                && mouseY < y + height;
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