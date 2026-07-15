package com.perigrine3.createcybernetics.screen.custom.computer.game.chess;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.common.computer.chess.ChessAi;
import com.perigrine3.createcybernetics.common.computer.chess.ChessColor;
import com.perigrine3.createcybernetics.common.computer.chess.ChessGame;
import com.perigrine3.createcybernetics.common.computer.chess.ChessGameStatus;
import com.perigrine3.createcybernetics.common.computer.chess.ChessMove;
import com.perigrine3.createcybernetics.common.computer.chess.ChessPiece;
import com.perigrine3.createcybernetics.common.computer.chess.ChessPieceType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ChessBoardScreen extends Screen {
    private static final int BOARD_SIZE = 8;

    private static final int CELL_SIZE = 18;

    private static final int PIECE_TEXTURE_SIZE = 16;

    private static final ResourceLocation WHITE_KING_TEXTURE =
            chessPieceTexture("white_king");

    private static final ResourceLocation WHITE_QUEEN_TEXTURE =
            chessPieceTexture("white_queen");

    private static final ResourceLocation WHITE_ROOK_TEXTURE =
            chessPieceTexture("white_rook");

    private static final ResourceLocation WHITE_BISHOP_TEXTURE =
            chessPieceTexture("white_bishop");

    private static final ResourceLocation WHITE_KNIGHT_TEXTURE =
            chessPieceTexture("white_knight");

    private static final ResourceLocation WHITE_PAWN_TEXTURE =
            chessPieceTexture("white_pawn");

    private static final ResourceLocation BLACK_KING_TEXTURE =
            chessPieceTexture("black_king");

    private static final ResourceLocation BLACK_QUEEN_TEXTURE =
            chessPieceTexture("black_queen");

    private static final ResourceLocation BLACK_ROOK_TEXTURE =
            chessPieceTexture("black_rook");

    private static final ResourceLocation BLACK_BISHOP_TEXTURE =
            chessPieceTexture("black_bishop");

    private static final ResourceLocation BLACK_KNIGHT_TEXTURE =
            chessPieceTexture("black_knight");

    private static final ResourceLocation BLACK_PAWN_TEXTURE =
            chessPieceTexture("black_pawn");

    private static final int BOARD_PIXEL_SIZE =
            BOARD_SIZE * CELL_SIZE;

    private static final int PANEL_PADDING = 8;
    private static final int HEADER_HEIGHT = 23;
    private static final int FOOTER_HEIGHT = 25;

    private static final int PANEL_WIDTH =
            BOARD_PIXEL_SIZE
                    + PANEL_PADDING * 2;

    private static final int PANEL_HEIGHT =
            BOARD_PIXEL_SIZE
                    + PANEL_PADDING * 2
                    + HEADER_HEIGHT
                    + FOOTER_HEIGHT;

    private static final int FOOTER_BUTTON_WIDTH = 62;
    private static final int FOOTER_BUTTON_HEIGHT = 16;

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

    private static final int AI_DEPTH = 4;

    private final Screen launcherScreen;

    private final Screen computerScreen;

    private final boolean singlePlayer;

    private final ChessGame game =
            new ChessGame();

    private int leftPos;
    private int topPos;

    private int boardX;
    private int boardY;

    private int selectedX = -1;
    private int selectedY = -1;

    private List<ChessMove> selectedMoves =
            List.of();

    private boolean aiThinking;
    private boolean screenClosed;

    public ChessBoardScreen(
            Screen launcherScreen,
            Screen computerScreen,
            boolean singlePlayer
    ) {
        super(
                Component.translatable(
                        "gui.createcybernetics.chess.title"
                )
        );

        this.launcherScreen = launcherScreen;
        this.computerScreen = computerScreen;
        this.singlePlayer = singlePlayer;
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

        screenClosed = false;

        addRenderableWidget(
                new AccentButton(
                        leftPos + PANEL_PADDING,
                        topPos
                                + PANEL_HEIGHT
                                - FOOTER_HEIGHT
                                + 5,
                        FOOTER_BUTTON_WIDTH,
                        FOOTER_BUTTON_HEIGHT,
                        Component.translatable(
                                "gui.createcybernetics.computer.back"
                        ),
                        button -> onClose()
                )
        );

        addRenderableWidget(
                new AccentButton(
                        leftPos
                                + PANEL_WIDTH
                                - PANEL_PADDING
                                - FOOTER_BUTTON_WIDTH,
                        topPos
                                + PANEL_HEIGHT
                                - FOOTER_HEIGHT
                                + 5,
                        FOOTER_BUTTON_WIDTH,
                        FOOTER_BUTTON_HEIGHT,
                        Component.translatable(
                                "gui.createcybernetics.chess.new_game"
                        ),
                        button -> restartGame()
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
        /*
         * Intentionally empty to prevent the vanilla menu blur.
         */
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

    private void restartGame() {
        screenClosed = true;

        Minecraft.getInstance().setScreen(
                new ChessBoardScreen(
                        launcherScreen,
                        computerScreen,
                        singlePlayer
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
        /*
         * Render only the computer GUI underneath this screen.
         * Do not render launcherScreen here.
         */
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
                getStatusText(),
                leftPos + PANEL_WIDTH / 2,
                topPos + 11,
                getStatusColor()
        );

        renderBoard(
                guiGraphics
        );

        super.render(
                guiGraphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    private Component getStatusText() {
        if (aiThinking) {
            return Component.translatable(
                    "gui.createcybernetics.chess.ai_thinking"
            );
        }

        return switch (game.getStatus()) {
            case ACTIVE -> {
                if (game.isKingInCheck(
                        game.getTurn()
                )) {
                    yield Component.translatable(
                            "gui.createcybernetics.chess.turn_check",
                            getColorName(
                                    game.getTurn()
                            )
                    );
                }

                yield Component.translatable(
                        "gui.createcybernetics.chess.turn",
                        getColorName(
                                game.getTurn()
                        )
                );
            }

            case WHITE_WON ->
                    Component.translatable(
                            "gui.createcybernetics.chess.white_won"
                    );

            case BLACK_WON ->
                    Component.translatable(
                            "gui.createcybernetics.chess.black_won"
                    );

            case STALEMATE ->
                    Component.translatable(
                            "gui.createcybernetics.chess.stalemate"
                    );
        };
    }

    private int getStatusColor() {
        return game.getStatus()
                == ChessGameStatus.ACTIVE
                ? ACCENT_COLOR
                : 0xFFFFFFFF;
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

    private void renderBoard(
            GuiGraphics guiGraphics
    ) {
        for (int x = 0;
             x < BOARD_SIZE;
             x++) {
            for (int y = 0;
                 y < BOARD_SIZE;
                 y++) {
                int screenX =
                        boardX + x * CELL_SIZE;

                int screenY =
                        boardY + y * CELL_SIZE;

                int squareColor =
                        (x + y) % 2 == 0
                                ? LIGHT_SQUARE
                                : DARK_SQUARE;

                guiGraphics.fill(
                        screenX,
                        screenY,
                        screenX + CELL_SIZE,
                        screenY + CELL_SIZE,
                        squareColor
                );

                if (x == selectedX &&
                        y == selectedY) {
                    guiGraphics.fill(
                            screenX,
                            screenY,
                            screenX + CELL_SIZE,
                            screenY + CELL_SIZE,
                            SELECTED_SQUARE
                    );
                }

                ChessMove destinationMove =
                        getSelectedMoveTo(
                                x,
                                y
                        );

                if (destinationMove != null) {
                    ChessPiece target =
                            game.getPiece(
                                    x,
                                    y
                            );

                    guiGraphics.fill(
                            screenX,
                            screenY,
                            screenX + CELL_SIZE,
                            screenY + CELL_SIZE,
                            target == null
                                    ? LEGAL_MOVE_SQUARE
                                    : CAPTURE_SQUARE
                    );
                }

                ChessPiece piece =
                        game.getPiece(
                                x,
                                y
                        );

                if (piece != null) {
                    renderPiece(
                            guiGraphics,
                            piece,
                            screenX,
                            screenY
                    );
                }
            }
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
                getPieceTexture(piece);

        int textureX =
                squareX
                        + (CELL_SIZE - PIECE_TEXTURE_SIZE) / 2;

        int textureY =
                squareY
                        + (CELL_SIZE - PIECE_TEXTURE_SIZE) / 2;

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

    private static ResourceLocation getPieceTexture(
            ChessPiece piece
    ) {
        if (piece.color() == ChessColor.WHITE) {
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

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        /*
         * Allow the custom Back/New Game buttons to process the click first.
         */
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
            return super.mouseClicked(
                    mouseX,
                    mouseY,
                    button
            );
        }

        if (aiThinking ||
                game.getStatus()
                        != ChessGameStatus.ACTIVE) {
            return true;
        }

        if (singlePlayer &&
                game.getTurn()
                        == ChessColor.BLACK) {
            return true;
        }

        int boardCellX =
                (int) (
                        (mouseX - boardX)
                                / CELL_SIZE
                );

        int boardCellY =
                (int) (
                        (mouseY - boardY)
                                / CELL_SIZE
                );

        ChessMove selectedMove =
                getSelectedMoveTo(
                        boardCellX,
                        boardCellY
                );

        if (selectedMove != null) {
            makePlayerMove(
                    selectedMove
            );

            return true;
        }

        ChessPiece clickedPiece =
                game.getPiece(
                        boardCellX,
                        boardCellY
                );

        if (clickedPiece != null &&
                clickedPiece.color()
                        == game.getTurn()) {
            selectedX =
                    boardCellX;

            selectedY =
                    boardCellY;

            selectedMoves =
                    game.getLegalMovesFrom(
                            selectedX,
                            selectedY
                    );
        } else {
            clearSelection();
        }

        return true;
    }

    private void makePlayerMove(
            ChessMove move
    ) {
        if (!game.makeMove(move)) {
            return;
        }

        clearSelection();

        if (singlePlayer &&
                game.getStatus()
                        == ChessGameStatus.ACTIVE &&
                game.getTurn()
                        == ChessColor.BLACK) {
            beginAiMove();
        }
    }

    private void beginAiMove() {
        aiThinking = true;

        ChessGame calculationPosition =
                game.copy();

        CompletableFuture.supplyAsync(
                () -> ChessAi.findBestMove(
                        calculationPosition,
                        ChessColor.BLACK,
                        AI_DEPTH
                )
        ).thenAccept(
                move ->
                        Minecraft.getInstance().execute(
                                () -> {
                                    if (screenClosed) {
                                        return;
                                    }

                                    aiThinking = false;

                                    if (move != null &&
                                            game.getTurn()
                                                    == ChessColor.BLACK &&
                                            game.getStatus()
                                                    == ChessGameStatus.ACTIVE) {
                                        game.makeMove(
                                                move
                                        );
                                    }
                                }
                        )
        );
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

            if (move.promotion()
                    == ChessPieceType.QUEEN) {
                queenPromotion =
                        move;
            }
        }

        return queenPromotion;
    }

    private void clearSelection() {
        selectedX = -1;
        selectedY = -1;

        selectedMoves =
                List.of();
    }

    private boolean isInsideBoard(
            double mouseX,
            double mouseY
    ) {
        return mouseX >= boardX
                && mouseX < boardX + BOARD_PIXEL_SIZE
                && mouseY >= boardY
                && mouseY < boardY + BOARD_PIXEL_SIZE;
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
        screenClosed = true;

        Minecraft.getInstance().setScreen(
                launcherScreen
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
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