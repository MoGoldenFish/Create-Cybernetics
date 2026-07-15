package com.perigrine3.createcybernetics.screen.custom.computer.game;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Queue;

public class MinesweeperScreen extends Screen {
    private static final int BOARD_COLUMNS = 9;
    private static final int BOARD_ROWS = 9;
    private static final int MINE_COUNT = 10;

    private static final int CELL_SIZE = 18;

    private static final int BOARD_WIDTH =
            BOARD_COLUMNS * CELL_SIZE;

    private static final int BOARD_HEIGHT =
            BOARD_ROWS * CELL_SIZE;

    private static final int WINDOW_PADDING = 12;
    private static final int HEADER_HEIGHT = 28;
    private static final int FOOTER_HEIGHT = 30;

    private static final int WINDOW_WIDTH =
            BOARD_WIDTH + WINDOW_PADDING * 2;

    private static final int WINDOW_HEIGHT =
            HEADER_HEIGHT
                    + BOARD_HEIGHT
                    + FOOTER_HEIGHT
                    + WINDOW_PADDING * 2;

    private static final int NEON_GREEN = 0xFF39FF14;
    private static final int NEON_GREEN_DIM = 0xFF147A0A;
    private static final int BACKGROUND = 0xF0000000;
    private static final int CELL_HIDDEN = 0xFF183018;
    private static final int CELL_HOVERED = 0xFF285A28;
    private static final int CELL_REVEALED = 0xFF090F09;
    private static final int CELL_MINE = 0xFF641010;
    private static final int CELL_FLAGGED = 0xFF4D3F08;

    private final Screen parentScreen;

    private final Cell[][] cells =
            new Cell[BOARD_COLUMNS][BOARD_ROWS];

    private final RandomSource random =
            RandomSource.create();

    private int leftPos;
    private int topPos;
    private int boardX;
    private int boardY;

    private boolean minesGenerated;
    private boolean gameOver;
    private boolean won;

    private int revealedSafeCells;

    public MinesweeperScreen(
            Screen parentScreen
    ) {
        super(
                Component.translatable(
                        "gui.createcybernetics.minesweeper.title"
                )
        );

        this.parentScreen = parentScreen;

        resetGame();
    }

    @Override
    protected void init() {
        leftPos =
                (width - WINDOW_WIDTH) / 2;

        topPos =
                (height - WINDOW_HEIGHT) / 2;

        boardX =
                leftPos + WINDOW_PADDING;

        boardY =
                topPos
                        + WINDOW_PADDING
                        + HEADER_HEIGHT;
    }

    private void resetGame() {
        for (int x = 0;
             x < BOARD_COLUMNS;
             x++) {
            for (int y = 0;
                 y < BOARD_ROWS;
                 y++) {
                cells[x][y] =
                        new Cell();
            }
        }

        minesGenerated = false;
        gameOver = false;
        won = false;
        revealedSafeCells = 0;
    }

    private void generateMines(
            int safeX,
            int safeY
    ) {
        int placedMines = 0;

        while (placedMines < MINE_COUNT) {
            int x =
                    random.nextInt(
                            BOARD_COLUMNS
                    );

            int y =
                    random.nextInt(
                            BOARD_ROWS
                    );

            Cell cell =
                    cells[x][y];

            if (cell.mine) {
                continue;
            }

            if (isInsideInitialSafeArea(
                    x,
                    y,
                    safeX,
                    safeY
            )) {
                continue;
            }

            cell.mine = true;
            placedMines++;
        }

        calculateAdjacentMineCounts();

        minesGenerated = true;
    }

    private static boolean isInsideInitialSafeArea(
            int x,
            int y,
            int safeX,
            int safeY
    ) {
        return Math.abs(x - safeX) <= 1
                && Math.abs(y - safeY) <= 1;
    }

    private void calculateAdjacentMineCounts() {
        for (int x = 0;
             x < BOARD_COLUMNS;
             x++) {
            for (int y = 0;
                 y < BOARD_ROWS;
                 y++) {
                if (cells[x][y].mine) {
                    continue;
                }

                cells[x][y].adjacentMines =
                        countAdjacentMines(
                                x,
                                y
                        );
            }
        }
    }

    private int countAdjacentMines(
            int centerX,
            int centerY
    ) {
        int count = 0;

        for (int offsetX = -1;
             offsetX <= 1;
             offsetX++) {
            for (int offsetY = -1;
                 offsetY <= 1;
                 offsetY++) {
                if (offsetX == 0 &&
                        offsetY == 0) {
                    continue;
                }

                int x =
                        centerX + offsetX;

                int y =
                        centerY + offsetY;

                if (!isValidCell(x, y)) {
                    continue;
                }

                if (cells[x][y].mine) {
                    count++;
                }
            }
        }

        return count;
    }

    private void revealCell(
            int x,
            int y
    ) {
        if (!isValidCell(x, y) ||
                gameOver) {
            return;
        }

        Cell cell =
                cells[x][y];

        if (cell.revealed ||
                cell.flagged) {
            return;
        }

        if (!minesGenerated) {
            generateMines(
                    x,
                    y
            );
        }

        if (cell.mine) {
            cell.revealed = true;
            revealAllMines();

            gameOver = true;
            won = false;

            return;
        }

        floodReveal(
                x,
                y
        );

        checkWin();
    }

    private void floodReveal(
            int startX,
            int startY
    ) {
        Queue<CellPosition> pending =
                new ArrayDeque<>();

        pending.add(
                new CellPosition(
                        startX,
                        startY
                )
        );

        while (!pending.isEmpty()) {
            CellPosition position =
                    pending.remove();

            int x =
                    position.x();

            int y =
                    position.y();

            if (!isValidCell(x, y)) {
                continue;
            }

            Cell cell =
                    cells[x][y];

            if (cell.revealed ||
                    cell.flagged ||
                    cell.mine) {
                continue;
            }

            cell.revealed = true;
            revealedSafeCells++;

            if (cell.adjacentMines != 0) {
                continue;
            }

            for (int offsetX = -1;
                 offsetX <= 1;
                 offsetX++) {
                for (int offsetY = -1;
                     offsetY <= 1;
                     offsetY++) {
                    if (offsetX == 0 &&
                            offsetY == 0) {
                        continue;
                    }

                    pending.add(
                            new CellPosition(
                                    x + offsetX,
                                    y + offsetY
                            )
                    );
                }
            }
        }
    }

    private void toggleFlag(
            int x,
            int y
    ) {
        if (!isValidCell(x, y) ||
                gameOver) {
            return;
        }

        Cell cell =
                cells[x][y];

        if (cell.revealed) {
            return;
        }

        cell.flagged =
                !cell.flagged;
    }

    private void checkWin() {
        int safeCellCount =
                BOARD_COLUMNS
                        * BOARD_ROWS
                        - MINE_COUNT;

        if (revealedSafeCells <
                safeCellCount) {
            return;
        }

        won = true;
        gameOver = true;

        for (int x = 0;
             x < BOARD_COLUMNS;
             x++) {
            for (int y = 0;
                 y < BOARD_ROWS;
                 y++) {
                if (cells[x][y].mine) {
                    cells[x][y].flagged = true;
                }
            }
        }
    }

    private void revealAllMines() {
        for (int x = 0;
             x < BOARD_COLUMNS;
             x++) {
            for (int y = 0;
                 y < BOARD_ROWS;
                 y++) {
                if (cells[x][y].mine) {
                    cells[x][y].revealed = true;
                }
            }
        }
    }

    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(
                guiGraphics,
                mouseX,
                mouseY,
                partialTick
        );

        guiGraphics.fill(
                leftPos,
                topPos,
                leftPos + WINDOW_WIDTH,
                topPos + WINDOW_HEIGHT,
                BACKGROUND
        );

        drawBorder(
                guiGraphics,
                leftPos,
                topPos,
                WINDOW_WIDTH,
                WINDOW_HEIGHT,
                NEON_GREEN
        );

        guiGraphics.drawCenteredString(
                font,
                Component.translatable(
                        "gui.createcybernetics.minesweeper.title"
                ),
                leftPos + WINDOW_WIDTH / 2,
                topPos + 11,
                NEON_GREEN
        );

        renderBoard(
                guiGraphics,
                mouseX,
                mouseY
        );

        renderStatus(
                guiGraphics
        );

        renderButton(
                guiGraphics,
                leftPos + 12,
                topPos + WINDOW_HEIGHT - 23,
                58,
                16,
                Component.translatable(
                        "gui.createcybernetics.minesweeper.back"
                ),
                isInside(
                        mouseX,
                        mouseY,
                        leftPos + 12,
                        topPos + WINDOW_HEIGHT - 23,
                        58,
                        16
                )
        );

        renderButton(
                guiGraphics,
                leftPos + WINDOW_WIDTH - 70,
                topPos + WINDOW_HEIGHT - 23,
                58,
                16,
                Component.translatable(
                        "gui.createcybernetics.minesweeper.reset"
                ),
                isInside(
                        mouseX,
                        mouseY,
                        leftPos + WINDOW_WIDTH - 70,
                        topPos + WINDOW_HEIGHT - 23,
                        58,
                        16
                )
        );
    }

    private void renderBoard(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY
    ) {
        for (int x = 0;
             x < BOARD_COLUMNS;
             x++) {
            for (int y = 0;
                 y < BOARD_ROWS;
                 y++) {
                int cellX =
                        boardX + x * CELL_SIZE;

                int cellY =
                        boardY + y * CELL_SIZE;

                Cell cell =
                        cells[x][y];

                boolean hovered =
                        isInside(
                                mouseX,
                                mouseY,
                                cellX,
                                cellY,
                                CELL_SIZE,
                                CELL_SIZE
                        );

                int cellColor;

                if (cell.revealed) {
                    cellColor =
                            cell.mine
                                    ? CELL_MINE
                                    : CELL_REVEALED;
                } else if (cell.flagged) {
                    cellColor =
                            CELL_FLAGGED;
                } else if (hovered &&
                        !gameOver) {
                    cellColor =
                            CELL_HOVERED;
                } else {
                    cellColor =
                            CELL_HIDDEN;
                }

                guiGraphics.fill(
                        cellX,
                        cellY,
                        cellX + CELL_SIZE,
                        cellY + CELL_SIZE,
                        cellColor
                );

                drawBorder(
                        guiGraphics,
                        cellX,
                        cellY,
                        CELL_SIZE,
                        CELL_SIZE,
                        NEON_GREEN_DIM
                );

                renderCellContents(
                        guiGraphics,
                        cell,
                        cellX,
                        cellY
                );
            }
        }
    }

    private void renderCellContents(
            GuiGraphics guiGraphics,
            Cell cell,
            int cellX,
            int cellY
    ) {
        if (cell.flagged &&
                !cell.revealed) {
            guiGraphics.drawCenteredString(
                    font,
                    "F",
                    cellX + CELL_SIZE / 2,
                    cellY + 5,
                    0xFFFFFF55
            );

            return;
        }

        if (!cell.revealed) {
            return;
        }

        if (cell.mine) {
            guiGraphics.drawCenteredString(
                    font,
                    "*",
                    cellX + CELL_SIZE / 2,
                    cellY + 5,
                    0xFFFF5555
            );

            return;
        }

        if (cell.adjacentMines <= 0) {
            return;
        }

        guiGraphics.drawCenteredString(
                font,
                Integer.toString(
                        cell.adjacentMines
                ),
                cellX + CELL_SIZE / 2,
                cellY + 5,
                getNumberColor(
                        cell.adjacentMines
                )
        );
    }

    private void renderStatus(
            GuiGraphics guiGraphics
    ) {
        Component status;

        int color;

        if (won) {
            status =
                    Component.translatable(
                            "gui.createcybernetics.minesweeper.won"
                    );

            color = NEON_GREEN;
        } else if (gameOver) {
            status =
                    Component.translatable(
                            "gui.createcybernetics.minesweeper.lost"
                    );

            color = 0xFFFF5555;
        } else {
            status =
                    Component.translatable(
                            "gui.createcybernetics.minesweeper.mines",
                            MINE_COUNT
                    );

            color = NEON_GREEN;
        }

        guiGraphics.drawCenteredString(
                font,
                status,
                leftPos + WINDOW_WIDTH / 2,
                topPos + WINDOW_PADDING + 17,
                color
        );
    }

    private void renderButton(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            Component label,
            boolean hovered
    ) {
        guiGraphics.fill(
                x,
                y,
                x + width,
                y + height,
                hovered
                        ? 0xFF163F16
                        : 0xFF000000
        );

        drawBorder(
                guiGraphics,
                x,
                y,
                width,
                height,
                NEON_GREEN
        );

        guiGraphics.drawCenteredString(
                font,
                label,
                x + width / 2,
                y + 4,
                NEON_GREEN
        );
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (isInside(
                mouseX,
                mouseY,
                leftPos + 12,
                topPos + WINDOW_HEIGHT - 23,
                58,
                16
        )) {
            onClose();
            return true;
        }

        if (isInside(
                mouseX,
                mouseY,
                leftPos + WINDOW_WIDTH - 70,
                topPos + WINDOW_HEIGHT - 23,
                58,
                16
        )) {
            resetGame();
            return true;
        }

        int cellX =
                (int) ((mouseX - boardX) /
                        CELL_SIZE);

        int cellY =
                (int) ((mouseY - boardY) /
                        CELL_SIZE);

        if (!isValidCell(
                cellX,
                cellY
        )) {
            return super.mouseClicked(
                    mouseX,
                    mouseY,
                    button
            );
        }

        if (!isInside(
                mouseX,
                mouseY,
                boardX,
                boardY,
                BOARD_WIDTH,
                BOARD_HEIGHT
        )) {
            return super.mouseClicked(
                    mouseX,
                    mouseY,
                    button
            );
        }

        if (button ==
                GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            revealCell(
                    cellX,
                    cellY
            );

            return true;
        }

        if (button ==
                GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            toggleFlag(
                    cellX,
                    cellY
            );

            return true;
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
        if (keyCode ==
                GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        if (keyCode ==
                GLFW.GLFW_KEY_R) {
            resetGame();
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

    private static boolean isValidCell(
            int x,
            int y
    ) {
        return x >= 0
                && x < BOARD_COLUMNS
                && y >= 0
                && y < BOARD_ROWS;
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

    private static int getNumberColor(
            int number
    ) {
        return switch (number) {
            case 1 -> 0xFF55AAFF;
            case 2 -> 0xFF55FF55;
            case 3 -> 0xFFFF5555;
            case 4 -> 0xFFAA55FF;
            case 5 -> 0xFFFFAA00;
            case 6 -> 0xFF55FFFF;
            case 7 -> 0xFFFFFFFF;
            default -> 0xFFAAAAAA;
        };
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

    private static final class Cell {
        private boolean mine;
        private boolean revealed;
        private boolean flagged;

        private int adjacentMines;
    }

    private record CellPosition(
            int x,
            int y
    ) {
    }
}