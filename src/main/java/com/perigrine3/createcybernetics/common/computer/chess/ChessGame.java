package com.perigrine3.createcybernetics.common.computer.chess;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public final class ChessGame {
    private static final String TAG_BOARD =
            "Board";

    private static final String TAG_TURN =
            "Turn";

    private static final String TAG_STATUS =
            "Status";

    private static final String TAG_WHITE_KING_SIDE =
            "WhiteKingSideCastle";

    private static final String TAG_WHITE_QUEEN_SIDE =
            "WhiteQueenSideCastle";

    private static final String TAG_BLACK_KING_SIDE =
            "BlackKingSideCastle";

    private static final String TAG_BLACK_QUEEN_SIDE =
            "BlackQueenSideCastle";

    private static final String TAG_EN_PASSANT_X =
            "EnPassantX";

    private static final String TAG_EN_PASSANT_Y =
            "EnPassantY";

    private static final String TAG_HALFMOVE_CLOCK =
            "HalfmoveClock";

    private static final String TAG_FULLMOVE_NUMBER =
            "FullmoveNumber";

    private final ChessPiece[][] board =
            new ChessPiece[8][8];

    private ChessColor turn =
            ChessColor.WHITE;

    private ChessGameStatus status =
            ChessGameStatus.ACTIVE;

    private boolean whiteKingSideCastle = true;
    private boolean whiteQueenSideCastle = true;
    private boolean blackKingSideCastle = true;
    private boolean blackQueenSideCastle = true;

    private int enPassantX = -1;
    private int enPassantY = -1;

    private int halfmoveClock;
    private int fullmoveNumber = 1;

    public ChessGame() {
        setupInitialPosition();
    }

    private ChessGame(
            boolean initialize
    ) {
        if (initialize) {
            setupInitialPosition();
        }
    }

    private void setupInitialPosition() {
        clearBoard();

        placeBackRank(
                ChessColor.BLACK,
                0
        );

        placePawns(
                ChessColor.BLACK,
                1
        );

        placePawns(
                ChessColor.WHITE,
                6
        );

        placeBackRank(
                ChessColor.WHITE,
                7
        );
    }

    private void clearBoard() {
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                board[x][y] = null;
            }
        }
    }

    private void placePawns(
            ChessColor color,
            int y
    ) {
        for (int x = 0; x < 8; x++) {
            board[x][y] =
                    new ChessPiece(
                            color,
                            ChessPieceType.PAWN
                    );
        }
    }

    private void placeBackRank(
            ChessColor color,
            int y
    ) {
        board[0][y] =
                new ChessPiece(
                        color,
                        ChessPieceType.ROOK
                );

        board[1][y] =
                new ChessPiece(
                        color,
                        ChessPieceType.KNIGHT
                );

        board[2][y] =
                new ChessPiece(
                        color,
                        ChessPieceType.BISHOP
                );

        board[3][y] =
                new ChessPiece(
                        color,
                        ChessPieceType.QUEEN
                );

        board[4][y] =
                new ChessPiece(
                        color,
                        ChessPieceType.KING
                );

        board[5][y] =
                new ChessPiece(
                        color,
                        ChessPieceType.BISHOP
                );

        board[6][y] =
                new ChessPiece(
                        color,
                        ChessPieceType.KNIGHT
                );

        board[7][y] =
                new ChessPiece(
                        color,
                        ChessPieceType.ROOK
                );
    }

    public ChessPiece getPiece(
            int x,
            int y
    ) {
        if (!isInside(x, y)) {
            return null;
        }

        return board[x][y];
    }

    public ChessColor getTurn() {
        return turn;
    }

    public ChessGameStatus getStatus() {
        return status;
    }

    public int getHalfmoveClock() {
        return halfmoveClock;
    }

    public int getFullmoveNumber() {
        return fullmoveNumber;
    }

    public boolean canWhiteCastleKingSide() {
        return whiteKingSideCastle;
    }

    public boolean canWhiteCastleQueenSide() {
        return whiteQueenSideCastle;
    }

    public boolean canBlackCastleKingSide() {
        return blackKingSideCastle;
    }

    public boolean canBlackCastleQueenSide() {
        return blackQueenSideCastle;
    }

    public int getEnPassantX() {
        return enPassantX;
    }

    public int getEnPassantY() {
        return enPassantY;
    }

    public List<ChessMove> getLegalMoves() {
        return getLegalMoves(turn);
    }

    public List<ChessMove> getLegalMoves(
            ChessColor color
    ) {
        List<ChessMove> legalMoves =
                new ArrayList<>();

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                ChessPiece piece =
                        board[x][y];

                if (piece == null ||
                        piece.color() != color) {
                    continue;
                }

                for (ChessMove move :
                        getPseudoLegalMoves(x, y)) {
                    ChessGame copy =
                            copy();

                    copy.applyMoveUnchecked(
                            move
                    );

                    if (!copy.isKingInCheck(color)) {
                        legalMoves.add(move);
                    }
                }
            }
        }

        return legalMoves;
    }

    public List<ChessMove> getLegalMovesFrom(
            int x,
            int y
    ) {
        ChessPiece piece =
                getPiece(x, y);

        if (piece == null ||
                piece.color() != turn ||
                status != ChessGameStatus.ACTIVE) {
            return List.of();
        }

        List<ChessMove> result =
                new ArrayList<>();

        for (ChessMove move :
                getLegalMoves(turn)) {
            if (move.fromX() == x &&
                    move.fromY() == y) {
                result.add(move);
            }
        }

        return result;
    }

    public boolean makeMove(
            ChessMove requestedMove
    ) {
        if (requestedMove == null ||
                status != ChessGameStatus.ACTIVE) {
            return false;
        }

        ChessMove legalMove =
                findMatchingLegalMove(
                        requestedMove
                );

        if (legalMove == null) {
            return false;
        }

        ChessColor movingColor =
                turn;

        applyMoveUnchecked(
                legalMove
        );

        turn =
                turn.opposite();

        if (movingColor == ChessColor.BLACK) {
            fullmoveNumber++;
        }

        updateStatus();

        return true;
    }

    private ChessMove findMatchingLegalMove(
            ChessMove requestedMove
    ) {
        for (ChessMove legalMove :
                getLegalMoves(turn)) {
            if (legalMove.fromX() != requestedMove.fromX() ||
                    legalMove.fromY() != requestedMove.fromY() ||
                    legalMove.toX() != requestedMove.toX() ||
                    legalMove.toY() != requestedMove.toY()) {
                continue;
            }

            if (legalMove.isPromotion()) {
                ChessPieceType requestedPromotion =
                        requestedMove.promotion() == null
                                ? ChessPieceType.QUEEN
                                : requestedMove.promotion();

                if (legalMove.promotion() != requestedPromotion) {
                    continue;
                }
            }

            return legalMove;
        }

        return null;
    }

    private void updateStatus() {
        List<ChessMove> nextMoves =
                getLegalMoves(turn);

        if (!nextMoves.isEmpty()) {
            status = ChessGameStatus.ACTIVE;
            return;
        }

        if (isKingInCheck(turn)) {
            status =
                    turn == ChessColor.WHITE
                            ? ChessGameStatus.BLACK_WON
                            : ChessGameStatus.WHITE_WON;
        } else {
            status =
                    ChessGameStatus.STALEMATE;
        }
    }

    private List<ChessMove> getPseudoLegalMoves(
            int x,
            int y
    ) {
        ChessPiece piece =
                board[x][y];

        if (piece == null) {
            return List.of();
        }

        List<ChessMove> moves =
                new ArrayList<>();

        switch (piece.type()) {
            case PAWN ->
                    addPawnMoves(
                            moves,
                            x,
                            y,
                            piece.color()
                    );

            case KNIGHT ->
                    addKnightMoves(
                            moves,
                            x,
                            y,
                            piece.color()
                    );

            case BISHOP ->
                    addSlidingMoves(
                            moves,
                            x,
                            y,
                            piece.color(),
                            new int[][]{
                                    {1, 1},
                                    {1, -1},
                                    {-1, 1},
                                    {-1, -1}
                            }
                    );

            case ROOK ->
                    addSlidingMoves(
                            moves,
                            x,
                            y,
                            piece.color(),
                            new int[][]{
                                    {1, 0},
                                    {-1, 0},
                                    {0, 1},
                                    {0, -1}
                            }
                    );

            case QUEEN ->
                    addSlidingMoves(
                            moves,
                            x,
                            y,
                            piece.color(),
                            new int[][]{
                                    {1, 0},
                                    {-1, 0},
                                    {0, 1},
                                    {0, -1},
                                    {1, 1},
                                    {1, -1},
                                    {-1, 1},
                                    {-1, -1}
                            }
                    );

            case KING ->
                    addKingMoves(
                            moves,
                            x,
                            y,
                            piece.color()
                    );
        }

        return moves;
    }

    private void addPawnMoves(
            List<ChessMove> moves,
            int x,
            int y,
            ChessColor color
    ) {
        int direction =
                color.pawnDirection();

        int oneForwardY =
                y + direction;

        if (isInside(x, oneForwardY) &&
                board[x][oneForwardY] == null) {
            addPawnMoveOrPromotions(
                    moves,
                    x,
                    y,
                    x,
                    oneForwardY,
                    color
            );

            int twoForwardY =
                    y + direction * 2;

            if (y == color.pawnStartRank() &&
                    board[x][twoForwardY] == null) {
                moves.add(
                        new ChessMove(
                                x,
                                y,
                                x,
                                twoForwardY
                        )
                );
            }
        }

        for (int offsetX : new int[]{-1, 1}) {
            int targetX =
                    x + offsetX;

            int targetY =
                    y + direction;

            if (!isInside(targetX, targetY)) {
                continue;
            }

            ChessPiece target =
                    board[targetX][targetY];

            boolean normalCapture =
                    target != null &&
                            target.color() != color;

            boolean enPassantCapture =
                    targetX == enPassantX &&
                            targetY == enPassantY;

            if (normalCapture || enPassantCapture) {
                addPawnMoveOrPromotions(
                        moves,
                        x,
                        y,
                        targetX,
                        targetY,
                        color
                );
            }
        }
    }

    private void addPawnMoveOrPromotions(
            List<ChessMove> moves,
            int fromX,
            int fromY,
            int toX,
            int toY,
            ChessColor color
    ) {
        if (toY != color.promotionRank()) {
            moves.add(
                    new ChessMove(
                            fromX,
                            fromY,
                            toX,
                            toY
                    )
            );

            return;
        }

        moves.add(
                new ChessMove(
                        fromX,
                        fromY,
                        toX,
                        toY,
                        ChessPieceType.QUEEN
                )
        );

        moves.add(
                new ChessMove(
                        fromX,
                        fromY,
                        toX,
                        toY,
                        ChessPieceType.ROOK
                )
        );

        moves.add(
                new ChessMove(
                        fromX,
                        fromY,
                        toX,
                        toY,
                        ChessPieceType.BISHOP
                )
        );

        moves.add(
                new ChessMove(
                        fromX,
                        fromY,
                        toX,
                        toY,
                        ChessPieceType.KNIGHT
                )
        );
    }

    private void addKnightMoves(
            List<ChessMove> moves,
            int x,
            int y,
            ChessColor color
    ) {
        int[][] offsets = {
                {1, 2},
                {2, 1},
                {2, -1},
                {1, -2},
                {-1, -2},
                {-2, -1},
                {-2, 1},
                {-1, 2}
        };

        for (int[] offset : offsets) {
            addMoveIfAvailable(
                    moves,
                    x,
                    y,
                    x + offset[0],
                    y + offset[1],
                    color
            );
        }
    }

    private void addSlidingMoves(
            List<ChessMove> moves,
            int x,
            int y,
            ChessColor color,
            int[][] directions
    ) {
        for (int[] direction : directions) {
            int targetX =
                    x + direction[0];

            int targetY =
                    y + direction[1];

            while (isInside(targetX, targetY)) {
                ChessPiece target =
                        board[targetX][targetY];

                if (target == null) {
                    moves.add(
                            new ChessMove(
                                    x,
                                    y,
                                    targetX,
                                    targetY
                            )
                    );
                } else {
                    if (target.color() != color) {
                        moves.add(
                                new ChessMove(
                                        x,
                                        y,
                                        targetX,
                                        targetY
                                )
                        );
                    }

                    break;
                }

                targetX += direction[0];
                targetY += direction[1];
            }
        }
    }

    private void addKingMoves(
            List<ChessMove> moves,
            int x,
            int y,
            ChessColor color
    ) {
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

                addMoveIfAvailable(
                        moves,
                        x,
                        y,
                        x + offsetX,
                        y + offsetY,
                        color
                );
            }
        }

        addCastlingMoves(
                moves,
                color
        );
    }

    private void addCastlingMoves(
            List<ChessMove> moves,
            ChessColor color
    ) {
        int y =
                color.backRank();

        if (board[4][y] == null ||
                board[4][y].type() != ChessPieceType.KING ||
                board[4][y].color() != color ||
                isSquareAttacked(
                        4,
                        y,
                        color.opposite()
                )) {
            return;
        }

        if (canCastleKingSide(color) &&
                board[5][y] == null &&
                board[6][y] == null &&
                isCorrectRook(
                        7,
                        y,
                        color
                ) &&
                !isSquareAttacked(
                        5,
                        y,
                        color.opposite()
                ) &&
                !isSquareAttacked(
                        6,
                        y,
                        color.opposite()
                )) {
            moves.add(
                    new ChessMove(
                            4,
                            y,
                            6,
                            y
                    )
            );
        }

        if (canCastleQueenSide(color) &&
                board[1][y] == null &&
                board[2][y] == null &&
                board[3][y] == null &&
                isCorrectRook(
                        0,
                        y,
                        color
                ) &&
                !isSquareAttacked(
                        3,
                        y,
                        color.opposite()
                ) &&
                !isSquareAttacked(
                        2,
                        y,
                        color.opposite()
                )) {
            moves.add(
                    new ChessMove(
                            4,
                            y,
                            2,
                            y
                    )
            );
        }
    }

    private boolean isCorrectRook(
            int x,
            int y,
            ChessColor color
    ) {
        ChessPiece piece =
                board[x][y];

        return piece != null &&
                piece.color() == color &&
                piece.type() == ChessPieceType.ROOK;
    }

    private boolean canCastleKingSide(
            ChessColor color
    ) {
        return color == ChessColor.WHITE
                ? whiteKingSideCastle
                : blackKingSideCastle;
    }

    private boolean canCastleQueenSide(
            ChessColor color
    ) {
        return color == ChessColor.WHITE
                ? whiteQueenSideCastle
                : blackQueenSideCastle;
    }

    private void addMoveIfAvailable(
            List<ChessMove> moves,
            int fromX,
            int fromY,
            int toX,
            int toY,
            ChessColor color
    ) {
        if (!isInside(toX, toY)) {
            return;
        }

        ChessPiece target =
                board[toX][toY];

        if (target == null ||
                target.color() != color) {
            moves.add(
                    new ChessMove(
                            fromX,
                            fromY,
                            toX,
                            toY
                    )
            );
        }
    }

    private void applyMoveUnchecked(
            ChessMove move
    ) {
        ChessPiece movingPiece =
                board[move.fromX()][move.fromY()];

        if (movingPiece == null) {
            return;
        }

        ChessPiece capturedPiece =
                board[move.toX()][move.toY()];

        updateCastlingRightsForMove(
                movingPiece,
                move
        );

        updateCastlingRightsForCapture(
                capturedPiece,
                move.toX(),
                move.toY()
        );

        boolean pawnMove =
                movingPiece.type() == ChessPieceType.PAWN;

        boolean enPassantCapture =
                pawnMove &&
                        move.toX() == enPassantX &&
                        move.toY() == enPassantY &&
                        capturedPiece == null &&
                        move.fromX() != move.toX();

        if (enPassantCapture) {
            int capturedPawnY =
                    move.toY() -
                            movingPiece.color()
                                    .pawnDirection();

            board[move.toX()][capturedPawnY] =
                    null;
        }

        board[move.fromX()][move.fromY()] =
                null;

        ChessPiece placedPiece =
                movingPiece;

        if (pawnMove &&
                move.toY() ==
                        movingPiece.color()
                                .promotionRank()) {
            ChessPieceType promotion =
                    move.promotion() == null
                            ? ChessPieceType.QUEEN
                            : move.promotion();

            placedPiece =
                    new ChessPiece(
                            movingPiece.color(),
                            promotion
                    );
        }

        board[move.toX()][move.toY()] =
                placedPiece;

        if (movingPiece.type() == ChessPieceType.KING &&
                move.isCastle()) {
            moveCastleRook(
                    move,
                    movingPiece.color()
            );
        }

        enPassantX = -1;
        enPassantY = -1;

        if (pawnMove &&
                Math.abs(move.toY() - move.fromY()) == 2) {
            enPassantX =
                    move.fromX();

            enPassantY =
                    (move.fromY() + move.toY()) / 2;
        }

        if (pawnMove || capturedPiece != null ||
                enPassantCapture) {
            halfmoveClock = 0;
        } else {
            halfmoveClock++;
        }
    }

    private void moveCastleRook(
            ChessMove move,
            ChessColor color
    ) {
        int y =
                color.backRank();

        if (move.isKingSideCastle()) {
            board[5][y] =
                    board[7][y];

            board[7][y] =
                    null;
        } else {
            board[3][y] =
                    board[0][y];

            board[0][y] =
                    null;
        }
    }

    private void updateCastlingRightsForMove(
            ChessPiece movingPiece,
            ChessMove move
    ) {
        if (movingPiece.type() == ChessPieceType.KING) {
            if (movingPiece.color() ==
                    ChessColor.WHITE) {
                whiteKingSideCastle = false;
                whiteQueenSideCastle = false;
            } else {
                blackKingSideCastle = false;
                blackQueenSideCastle = false;
            }

            return;
        }

        if (movingPiece.type() !=
                ChessPieceType.ROOK) {
            return;
        }

        disableRookCastlingRight(
                movingPiece.color(),
                move.fromX(),
                move.fromY()
        );
    }

    private void updateCastlingRightsForCapture(
            ChessPiece capturedPiece,
            int capturedX,
            int capturedY
    ) {
        if (capturedPiece == null ||
                capturedPiece.type() !=
                        ChessPieceType.ROOK) {
            return;
        }

        disableRookCastlingRight(
                capturedPiece.color(),
                capturedX,
                capturedY
        );
    }

    private void disableRookCastlingRight(
            ChessColor color,
            int x,
            int y
    ) {
        if (y != color.backRank()) {
            return;
        }

        if (color == ChessColor.WHITE) {
            if (x == 0) {
                whiteQueenSideCastle = false;
            } else if (x == 7) {
                whiteKingSideCastle = false;
            }
        } else {
            if (x == 0) {
                blackQueenSideCastle = false;
            } else if (x == 7) {
                blackKingSideCastle = false;
            }
        }
    }

    public boolean isKingInCheck(
            ChessColor color
    ) {
        int kingX = -1;
        int kingY = -1;

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                ChessPiece piece =
                        board[x][y];

                if (piece != null &&
                        piece.color() == color &&
                        piece.type() ==
                                ChessPieceType.KING) {
                    kingX = x;
                    kingY = y;
                    break;
                }
            }
        }

        if (kingX < 0) {
            return true;
        }

        return isSquareAttacked(
                kingX,
                kingY,
                color.opposite()
        );
    }

    public boolean isSquareAttacked(
            int targetX,
            int targetY,
            ChessColor attacker
    ) {
        int pawnSourceY =
                targetY -
                        attacker.pawnDirection();

        for (int pawnSourceX :
                new int[]{
                        targetX - 1,
                        targetX + 1
                }) {
            ChessPiece pawn =
                    getPiece(
                            pawnSourceX,
                            pawnSourceY
                    );

            if (pawn != null &&
                    pawn.color() == attacker &&
                    pawn.type() ==
                            ChessPieceType.PAWN) {
                return true;
            }
        }

        int[][] knightOffsets = {
                {1, 2},
                {2, 1},
                {2, -1},
                {1, -2},
                {-1, -2},
                {-2, -1},
                {-2, 1},
                {-1, 2}
        };

        for (int[] offset : knightOffsets) {
            ChessPiece knight =
                    getPiece(
                            targetX + offset[0],
                            targetY + offset[1]
                    );

            if (knight != null &&
                    knight.color() == attacker &&
                    knight.type() ==
                            ChessPieceType.KNIGHT) {
                return true;
            }
        }

        if (isAttackedBySlider(
                targetX,
                targetY,
                attacker,
                new int[][]{
                        {1, 0},
                        {-1, 0},
                        {0, 1},
                        {0, -1}
                },
                ChessPieceType.ROOK,
                ChessPieceType.QUEEN
        )) {
            return true;
        }

        if (isAttackedBySlider(
                targetX,
                targetY,
                attacker,
                new int[][]{
                        {1, 1},
                        {1, -1},
                        {-1, 1},
                        {-1, -1}
                },
                ChessPieceType.BISHOP,
                ChessPieceType.QUEEN
        )) {
            return true;
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

                ChessPiece king =
                        getPiece(
                                targetX + offsetX,
                                targetY + offsetY
                        );

                if (king != null &&
                        king.color() == attacker &&
                        king.type() ==
                                ChessPieceType.KING) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isAttackedBySlider(
            int targetX,
            int targetY,
            ChessColor attacker,
            int[][] directions,
            ChessPieceType firstType,
            ChessPieceType secondType
    ) {
        for (int[] direction : directions) {
            int x =
                    targetX + direction[0];

            int y =
                    targetY + direction[1];

            while (isInside(x, y)) {
                ChessPiece piece =
                        board[x][y];

                if (piece == null) {
                    x += direction[0];
                    y += direction[1];
                    continue;
                }

                if (piece.color() == attacker &&
                        (piece.type() == firstType ||
                                piece.type() == secondType)) {
                    return true;
                }

                break;
            }
        }

        return false;
    }

    public CompoundTag saveToTag() {
        CompoundTag tag =
                new CompoundTag();

        int[] encodedBoard =
                new int[64];

        for (int y = 0;
             y < 8;
             y++) {
            for (int x = 0;
                 x < 8;
                 x++) {
                encodedBoard[
                        x + y * 8
                        ] = encodePiece(
                        board[x][y]
                );
            }
        }

        tag.putIntArray(
                TAG_BOARD,
                encodedBoard
        );

        tag.putString(
                TAG_TURN,
                turn.name()
        );

        tag.putString(
                TAG_STATUS,
                status.name()
        );

        tag.putBoolean(
                TAG_WHITE_KING_SIDE,
                whiteKingSideCastle
        );

        tag.putBoolean(
                TAG_WHITE_QUEEN_SIDE,
                whiteQueenSideCastle
        );

        tag.putBoolean(
                TAG_BLACK_KING_SIDE,
                blackKingSideCastle
        );

        tag.putBoolean(
                TAG_BLACK_QUEEN_SIDE,
                blackQueenSideCastle
        );

        tag.putInt(
                TAG_EN_PASSANT_X,
                enPassantX
        );

        tag.putInt(
                TAG_EN_PASSANT_Y,
                enPassantY
        );

        tag.putInt(
                TAG_HALFMOVE_CLOCK,
                halfmoveClock
        );

        tag.putInt(
                TAG_FULLMOVE_NUMBER,
                fullmoveNumber
        );

        return tag;
    }

    public static ChessGame loadFromTag(
            CompoundTag tag
    ) {
        ChessGame game =
                new ChessGame(false);

        int[] encodedBoard =
                tag.getIntArray(
                        TAG_BOARD
                );

        if (encodedBoard.length == 64) {
            for (int y = 0;
                 y < 8;
                 y++) {
                for (int x = 0;
                     x < 8;
                     x++) {
                    game.board[x][y] =
                            decodePiece(
                                    encodedBoard[
                                            x + y * 8
                                            ]
                            );
                }
            }
        } else {
            game.setupInitialPosition();
        }

        game.turn =
                parseEnum(
                        ChessColor.class,
                        tag.getString(TAG_TURN),
                        ChessColor.WHITE
                );

        game.status =
                parseEnum(
                        ChessGameStatus.class,
                        tag.getString(TAG_STATUS),
                        ChessGameStatus.ACTIVE
                );

        game.whiteKingSideCastle =
                tag.getBoolean(
                        TAG_WHITE_KING_SIDE
                );

        game.whiteQueenSideCastle =
                tag.getBoolean(
                        TAG_WHITE_QUEEN_SIDE
                );

        game.blackKingSideCastle =
                tag.getBoolean(
                        TAG_BLACK_KING_SIDE
                );

        game.blackQueenSideCastle =
                tag.getBoolean(
                        TAG_BLACK_QUEEN_SIDE
                );

        game.enPassantX =
                tag.contains(TAG_EN_PASSANT_X)
                        ? tag.getInt(TAG_EN_PASSANT_X)
                        : -1;

        game.enPassantY =
                tag.contains(TAG_EN_PASSANT_Y)
                        ? tag.getInt(TAG_EN_PASSANT_Y)
                        : -1;

        game.halfmoveClock =
                Math.max(
                        0,
                        tag.getInt(
                                TAG_HALFMOVE_CLOCK
                        )
                );

        game.fullmoveNumber =
                Math.max(
                        1,
                        tag.getInt(
                                TAG_FULLMOVE_NUMBER
                        )
                );

        return game;
    }

    private static int encodePiece(
            ChessPiece piece
    ) {
        if (piece == null) {
            return 0;
        }

        int typeValue =
                switch (piece.type()) {
                    case PAWN -> 1;
                    case KNIGHT -> 2;
                    case BISHOP -> 3;
                    case ROOK -> 4;
                    case QUEEN -> 5;
                    case KING -> 6;
                };

        if (piece.color() ==
                ChessColor.BLACK) {
            typeValue += 6;
        }

        return typeValue;
    }

    private static ChessPiece decodePiece(
            int value
    ) {
        if (value <= 0 ||
                value > 12) {
            return null;
        }

        ChessColor color =
                value > 6
                        ? ChessColor.BLACK
                        : ChessColor.WHITE;

        int typeValue =
                value > 6
                        ? value - 6
                        : value;

        ChessPieceType type =
                switch (typeValue) {
                    case 1 ->
                            ChessPieceType.PAWN;

                    case 2 ->
                            ChessPieceType.KNIGHT;

                    case 3 ->
                            ChessPieceType.BISHOP;

                    case 4 ->
                            ChessPieceType.ROOK;

                    case 5 ->
                            ChessPieceType.QUEEN;

                    case 6 ->
                            ChessPieceType.KING;

                    default ->
                            null;
                };

        if (type == null) {
            return null;
        }

        return new ChessPiece(
                color,
                type
        );
    }

    private static <T extends Enum<T>> T parseEnum(
            Class<T> enumClass,
            String value,
            T fallback
    ) {
        if (value == null ||
                value.isBlank()) {
            return fallback;
        }

        try {
            return Enum.valueOf(
                    enumClass,
                    value
            );
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    public ChessGame copy() {
        ChessGame copy =
                new ChessGame(false);

        for (int x = 0; x < 8; x++) {
            System.arraycopy(
                    board[x],
                    0,
                    copy.board[x],
                    0,
                    8
            );
        }

        copy.turn =
                turn;

        copy.status =
                status;

        copy.whiteKingSideCastle =
                whiteKingSideCastle;

        copy.whiteQueenSideCastle =
                whiteQueenSideCastle;

        copy.blackKingSideCastle =
                blackKingSideCastle;

        copy.blackQueenSideCastle =
                blackQueenSideCastle;

        copy.enPassantX =
                enPassantX;

        copy.enPassantY =
                enPassantY;

        copy.halfmoveClock =
                halfmoveClock;

        copy.fullmoveNumber =
                fullmoveNumber;

        return copy;
    }

    private static boolean isInside(
            int x,
            int y
    ) {
        return x >= 0 &&
                x < 8 &&
                y >= 0 &&
                y < 8;
    }
}