package com.perigrine3.createcybernetics.screen.custom.computer.game.chess;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.computer.ChatSpaceClientData;
import com.perigrine3.createcybernetics.client.computer.ChessClientData;
import com.perigrine3.createcybernetics.common.computer.chess.ChessColor;
import com.perigrine3.createcybernetics.common.computer.chess.ChessGame;
import com.perigrine3.createcybernetics.common.computer.chess.ChessGameStatus;
import com.perigrine3.createcybernetics.common.computer.chess.ChessMove;
import com.perigrine3.createcybernetics.common.computer.chess.ChessPiece;
import com.perigrine3.createcybernetics.common.computer.chess.ChessPieceType;
import com.perigrine3.createcybernetics.network.payload.ChessMakeMovePayload;
import com.perigrine3.createcybernetics.network.payload.ChessRequestSyncPayload;
import com.perigrine3.createcybernetics.network.payload.ChessResignPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.UUID;

public final class ChessMultiplayerBoardScreen extends Screen {
    private static final int BOARD_SIZE = 8;
    private static final int CELL_SIZE = 18;
    private static final int PIECE_TEXTURE_SIZE = 16;

    private static final int BOARD_PIXEL_SIZE =
            BOARD_SIZE * CELL_SIZE;

    private static final int PANEL_PADDING = 8;
    private static final int HEADER_HEIGHT = 30;
    private static final int FOOTER_HEIGHT = 26;

    private static final int PANEL_WIDTH =
            BOARD_PIXEL_SIZE
                    + PANEL_PADDING * 2;

    private static final int PANEL_HEIGHT =
            BOARD_PIXEL_SIZE
                    + PANEL_PADDING * 2
                    + HEADER_HEIGHT
                    + FOOTER_HEIGHT;

    private static final int ACCENT_COLOR = 0xFF39FF14;
    private static final int PANEL_BACKGROUND = 0xF0000000;

    private static final int BUTTON_BACKGROUND = 0xFF000000;
    private static final int BUTTON_HOVER_BACKGROUND = 0xFF123B0D;
    private static final int DISABLED_COLOR = 0xFF777777;

    private static final int LIGHT_SQUARE = 0xFFB4C89A;
    private static final int DARK_SQUARE = 0xFF536B43;

    private static final int SELECTED_SQUARE = 0xAAFFFF00;
    private static final int LEGAL_MOVE_SQUARE = 0xAA39FF14;
    private static final int CAPTURE_SQUARE = 0xAAFF5555;
    private static final int WAITING_OVERLAY = 0x77000000;

    private static final long SYNC_INTERVAL_MILLIS = 450L;

    private static final ResourceLocation WHITE_KING_TEXTURE =
            chessPieceTexture(
                    "white_king"
            );

    private static final ResourceLocation WHITE_QUEEN_TEXTURE =
            chessPieceTexture(
                    "white_queen"
            );

    private static final ResourceLocation WHITE_ROOK_TEXTURE =
            chessPieceTexture(
                    "white_rook"
            );

    private static final ResourceLocation WHITE_BISHOP_TEXTURE =
            chessPieceTexture(
                    "white_bishop"
            );

    private static final ResourceLocation WHITE_KNIGHT_TEXTURE =
            chessPieceTexture(
                    "white_knight"
            );

    private static final ResourceLocation WHITE_PAWN_TEXTURE =
            chessPieceTexture(
                    "white_pawn"
            );

    private static final ResourceLocation BLACK_KING_TEXTURE =
            chessPieceTexture(
                    "black_king"
            );

    private static final ResourceLocation BLACK_QUEEN_TEXTURE =
            chessPieceTexture(
                    "black_queen"
            );

    private static final ResourceLocation BLACK_ROOK_TEXTURE =
            chessPieceTexture(
                    "black_rook"
            );

    private static final ResourceLocation BLACK_BISHOP_TEXTURE =
            chessPieceTexture(
                    "black_bishop"
            );

    private static final ResourceLocation BLACK_KNIGHT_TEXTURE =
            chessPieceTexture(
                    "black_knight"
            );

    private static final ResourceLocation BLACK_PAWN_TEXTURE =
            chessPieceTexture(
                    "black_pawn"
            );

    private final Screen multiplayerScreen;
    private final Screen computerScreen;
    private final BlockPos computerPos;
    private final UUID sessionId;

    private int leftPos;
    private int topPos;

    private int boardX;
    private int boardY;

    private int selectedBoardX = -1;
    private int selectedBoardY = -1;

    private List<ChessMove> selectedMoves =
            List.of();

    private long nextSyncAt;

    private boolean resignRequested;

    public ChessMultiplayerBoardScreen(
            Screen multiplayerScreen,
            Screen computerScreen,
            BlockPos computerPos,
            UUID sessionId
    ) {
        super(
                Component.translatable(
                        "gui.createcybernetics.chess.title"
                )
        );

        this.multiplayerScreen = multiplayerScreen;
        this.computerScreen = computerScreen;
        this.computerPos = computerPos.immutable();
        this.sessionId = sessionId;
    }

    @Override
    protected void init() {
        leftPos =
                (width - PANEL_WIDTH) / 2;

        topPos =
                (height - PANEL_HEIGHT) / 2;

        boardX =
                leftPos + PANEL_PADDING;

        boardY =
                topPos
                        + PANEL_PADDING
                        + HEADER_HEIGHT;

        selectedBoardX = -1;
        selectedBoardY = -1;
        selectedMoves = List.of();

        nextSyncAt = 0L;
        resignRequested = false;

        addRenderableWidget(
                new AccentButton(
                        leftPos + PANEL_PADDING,
                        topPos + PANEL_HEIGHT - 21,
                        60,
                        16,
                        Component.translatable(
                                "gui.createcybernetics.computer.back"
                        ),
                        button -> onClose()
                )
        );

        addRenderableWidget(
                new AccentButton(
                        leftPos + PANEL_WIDTH - PANEL_PADDING - 60,
                        topPos + PANEL_HEIGHT - 21,
                        60,
                        16,
                        Component.translatable(
                                "gui.createcybernetics.chess.resign"
                        ),
                        button -> requestResign()
                )
        );

        requestSync();
    }

    private void requestSync() {
        PacketDistributor.sendToServer(
                new ChessRequestSyncPayload(
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

        requestSync();
    }

    private ChessClientData.ClientSession getSession() {
        return ChessClientData.getSession(
                sessionId
        );
    }

    private ChessGame getGame() {
        ChessClientData.ClientSession session =
                getSession();

        return session == null
                ? null
                : session.game();
    }

    private ChessColor getPlayerColor() {
        ChessClientData.ClientSession session =
                getSession();

        return session == null
                ? null
                : session.playerColor();
    }

    private boolean isPlayersTurn() {
        ChessGame game =
                getGame();

        ChessColor playerColor =
                getPlayerColor();

        return game != null
                && playerColor != null
                && game.getStatus() ==
                ChessGameStatus.ACTIVE
                && game.getTurn() ==
                playerColor;
    }

    private void requestResign() {
        if (resignRequested) {
            return;
        }

        resignRequested = true;

        PacketDistributor.sendToServer(
                new ChessResignPayload(
                        computerPos,
                        sessionId
                )
        );

        requestSync();
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

        ChessClientData.ClientSession session =
                getSession();

        ChessGame game =
                session == null
                        ? null
                        : session.game();

        if (session == null ||
                game == null) {
            guiGraphics.drawCenteredString(
                    font,
                    Component.translatable(
                            resignRequested
                                    ? "gui.createcybernetics.chess.match_ended"
                                    : "gui.createcybernetics.chess.synchronizing"
                    ),
                    leftPos + PANEL_WIDTH / 2,
                    topPos + PANEL_HEIGHT / 2,
                    resignRequested
                            ? 0xFFFF5555
                            : ACCENT_COLOR
            );

            super.render(
                    guiGraphics,
                    mouseX,
                    mouseY,
                    partialTick
            );

            return;
        }

        renderHeader(
                guiGraphics,
                session,
                game
        );

        renderBoard(
                guiGraphics,
                session,
                game
        );

        super.render(
                guiGraphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    private void renderHeader(
            GuiGraphics guiGraphics,
            ChessClientData.ClientSession session,
            ChessGame game
    ) {
        String opponentCode =
                session.opponentCode();

        ChatSpaceClientData.ClientContact contact =
                ChatSpaceClientData.getContact(
                        opponentCode
                );

        String opponentName =
                contact == null
                        ? opponentCode
                        : contact.displayName();

        String status;

        if (game.getStatus() ==
                ChessGameStatus.WHITE_WON) {
            status =
                    Component.translatable(
                            "gui.createcybernetics.chess.white_won"
                    ).getString();
        } else if (game.getStatus() ==
                ChessGameStatus.BLACK_WON) {
            status =
                    Component.translatable(
                            "gui.createcybernetics.chess.black_won"
                    ).getString();
        } else if (game.getStatus() ==
                ChessGameStatus.STALEMATE) {
            status =
                    Component.translatable(
                            "gui.createcybernetics.chess.stalemate"
                    ).getString();
        } else if (game.isKingInCheck(
                game.getTurn()
        )) {
            status =
                    Component.translatable(
                            "gui.createcybernetics.chess.turn_check",
                            getColorName(
                                    game.getTurn()
                            )
                    ).getString();
        } else {
            status =
                    Component.translatable(
                            "gui.createcybernetics.chess.turn",
                            getColorName(
                                    game.getTurn()
                            )
                    ).getString();
        }

        guiGraphics.drawCenteredString(
                font,
                font.plainSubstrByWidth(
                        opponentName,
                        PANEL_WIDTH - 16
                ),
                leftPos + PANEL_WIDTH / 2,
                topPos + 8,
                ACCENT_COLOR
        );

        guiGraphics.drawCenteredString(
                font,
                status,
                leftPos + PANEL_WIDTH / 2,
                topPos + 19,
                game.getStatus() ==
                        ChessGameStatus.ACTIVE
                        ? 0xFFFFFFFF
                        : 0xFFFFFF55
        );
    }

    private void renderBoard(
            GuiGraphics guiGraphics,
            ChessClientData.ClientSession session,
            ChessGame game
    ) {
        ChessColor playerColor =
                session.playerColor();

        for (int screenCellX = 0;
             screenCellX < BOARD_SIZE;
             screenCellX++) {
            for (int screenCellY = 0;
                 screenCellY < BOARD_SIZE;
                 screenCellY++) {
                int boardCellX =
                        toBoardX(
                                screenCellX,
                                playerColor
                        );

                int boardCellY =
                        toBoardY(
                                screenCellY,
                                playerColor
                        );

                int squareX =
                        boardX
                                + screenCellX
                                * CELL_SIZE;

                int squareY =
                        boardY
                                + screenCellY
                                * CELL_SIZE;

                int squareColor =
                        (boardCellX + boardCellY)
                                % 2 == 0
                                ? LIGHT_SQUARE
                                : DARK_SQUARE;

                guiGraphics.fill(
                        squareX,
                        squareY,
                        squareX + CELL_SIZE,
                        squareY + CELL_SIZE,
                        squareColor
                );

                if (boardCellX == selectedBoardX &&
                        boardCellY == selectedBoardY) {
                    guiGraphics.fill(
                            squareX,
                            squareY,
                            squareX + CELL_SIZE,
                            squareY + CELL_SIZE,
                            SELECTED_SQUARE
                    );
                }

                ChessMove destinationMove =
                        getSelectedMoveTo(
                                boardCellX,
                                boardCellY
                        );

                if (destinationMove != null) {
                    ChessPiece target =
                            game.getPiece(
                                    boardCellX,
                                    boardCellY
                            );

                    guiGraphics.fill(
                            squareX,
                            squareY,
                            squareX + CELL_SIZE,
                            squareY + CELL_SIZE,
                            target == null
                                    ? LEGAL_MOVE_SQUARE
                                    : CAPTURE_SQUARE
                    );
                }

                ChessPiece piece =
                        game.getPiece(
                                boardCellX,
                                boardCellY
                        );

                if (piece != null) {
                    renderPiece(
                            guiGraphics,
                            piece,
                            squareX,
                            squareY
                    );
                }
            }
        }

        if (!isPlayersTurn() &&
                game.getStatus() ==
                        ChessGameStatus.ACTIVE) {
            guiGraphics.fill(
                    boardX,
                    boardY,
                    boardX + BOARD_PIXEL_SIZE,
                    boardY + BOARD_PIXEL_SIZE,
                    WAITING_OVERLAY
            );
        }

        drawBorder(
                guiGraphics,
                boardX,
                boardY,
                BOARD_PIXEL_SIZE,
                BOARD_PIXEL_SIZE,
                ACCENT_COLOR
        );
    }

    private void renderPiece(
            GuiGraphics guiGraphics,
            ChessPiece piece,
            int squareX,
            int squareY
    ) {
        ResourceLocation texture =
                getPieceTexture(
                        piece
                );

        int textureX =
                squareX
                        + (CELL_SIZE
                        - PIECE_TEXTURE_SIZE)
                        / 2;

        int textureY =
                squareY
                        + (CELL_SIZE
                        - PIECE_TEXTURE_SIZE)
                        / 2;

        guiGraphics.blit(
                texture,
                textureX,
                textureY,
                0,
                0,
                PIECE_TEXTURE_SIZE,
                PIECE_TEXTURE_SIZE,
                PIECE_TEXTURE_SIZE,
                PIECE_TEXTURE_SIZE
        );
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (!isInsideBoard(
                mouseX,
                mouseY
        )) {
            return super.mouseClicked(
                    mouseX,
                    mouseY,
                    button
            );
        }

        if (button !=
                GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return true;
        }

        ChessClientData.ClientSession session =
                getSession();

        ChessGame game =
                getGame();

        if (session == null ||
                game == null ||
                !isPlayersTurn()) {
            return true;
        }

        int screenCellX =
                (int) (
                        (mouseX - boardX)
                                / CELL_SIZE
                );

        int screenCellY =
                (int) (
                        (mouseY - boardY)
                                / CELL_SIZE
                );

        int clickedBoardX =
                toBoardX(
                        screenCellX,
                        session.playerColor()
                );

        int clickedBoardY =
                toBoardY(
                        screenCellY,
                        session.playerColor()
                );

        ChessMove selectedMove =
                getSelectedMoveTo(
                        clickedBoardX,
                        clickedBoardY
                );

        if (selectedMove != null) {
            sendMove(
                    selectedMove
            );

            clearSelection();
            return true;
        }

        ChessPiece clickedPiece =
                game.getPiece(
                        clickedBoardX,
                        clickedBoardY
                );

        if (clickedPiece != null &&
                clickedPiece.color() ==
                        session.playerColor() &&
                clickedPiece.color() ==
                        game.getTurn()) {
            selectedBoardX =
                    clickedBoardX;

            selectedBoardY =
                    clickedBoardY;

            selectedMoves =
                    game.getLegalMovesFrom(
                            clickedBoardX,
                            clickedBoardY
                    );
        } else {
            clearSelection();
        }

        return true;
    }

    private void sendMove(
            ChessMove move
    ) {
        PacketDistributor.sendToServer(
                new ChessMakeMovePayload(
                        computerPos,
                        sessionId,
                        move.fromX(),
                        move.fromY(),
                        move.toX(),
                        move.toY(),
                        move.promotion()
                )
        );

        requestSync();
    }

    private ChessMove getSelectedMoveTo(
            int x,
            int y
    ) {
        ChessMove queenPromotion =
                null;

        for (ChessMove move :
                selectedMoves) {
            if (move.toX() != x ||
                    move.toY() != y) {
                continue;
            }

            if (!move.isPromotion()) {
                return move;
            }

            if (move.promotion() ==
                    ChessPieceType.QUEEN) {
                queenPromotion =
                        move;
            }
        }

        return queenPromotion;
    }

    private void clearSelection() {
        selectedBoardX = -1;
        selectedBoardY = -1;

        selectedMoves =
                List.of();
    }

    private boolean isInsideBoard(
            double mouseX,
            double mouseY
    ) {
        return mouseX >= boardX
                && mouseX < boardX
                + BOARD_PIXEL_SIZE
                && mouseY >= boardY
                && mouseY < boardY
                + BOARD_PIXEL_SIZE;
    }

    private static int toBoardX(
            int screenX,
            ChessColor playerColor
    ) {
        return playerColor ==
                ChessColor.BLACK
                ? 7 - screenX
                : screenX;
    }

    private static int toBoardY(
            int screenY,
            ChessColor playerColor
    ) {
        return playerColor ==
                ChessColor.BLACK
                ? 7 - screenY
                : screenY;
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        if (keyCode ==
                GLFW.GLFW_KEY_ESCAPE) {
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
                multiplayerScreen
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static Component getColorName(
            ChessColor color
    ) {
        return Component.translatable(
                color == ChessColor.WHITE
                        ? "gui.createcybernetics.chess.white"
                        : "gui.createcybernetics.chess.black"
        );
    }

    private static ResourceLocation getPieceTexture(
            ChessPiece piece
    ) {
        if (piece.color() ==
                ChessColor.WHITE) {
            return switch (piece.type()) {
                case KING ->
                        WHITE_KING_TEXTURE;

                case QUEEN ->
                        WHITE_QUEEN_TEXTURE;

                case ROOK ->
                        WHITE_ROOK_TEXTURE;

                case BISHOP ->
                        WHITE_BISHOP_TEXTURE;

                case KNIGHT ->
                        WHITE_KNIGHT_TEXTURE;

                case PAWN ->
                        WHITE_PAWN_TEXTURE;
            };
        }

        return switch (piece.type()) {
            case KING ->
                    BLACK_KING_TEXTURE;

            case QUEEN ->
                    BLACK_QUEEN_TEXTURE;

            case ROOK ->
                    BLACK_ROOK_TEXTURE;

            case BISHOP ->
                    BLACK_BISHOP_TEXTURE;

            case KNIGHT ->
                    BLACK_KNIGHT_TEXTURE;

            case PAWN ->
                    BLACK_PAWN_TEXTURE;
        };
    }

    private static ResourceLocation chessPieceTexture(
            String name
    ) {
        return ResourceLocation.fromNamespaceAndPath(
                CreateCybernetics.MODID,
                "textures/gui/computer/chess/"
                        + name
                        + ".png"
        );
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