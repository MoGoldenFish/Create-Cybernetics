package com.perigrine3.createcybernetics.common.computer.chess;

public record ChessMove(
        int fromX,
        int fromY,
        int toX,
        int toY,
        ChessPieceType promotion
) {
    public ChessMove(
            int fromX,
            int fromY,
            int toX,
            int toY
    ) {
        this(
                fromX,
                fromY,
                toX,
                toY,
                null
        );
    }

    public boolean isPromotion() {
        return promotion != null;
    }

    public boolean isCastle() {
        return Math.abs(toX - fromX) == 2;
    }

    public boolean isKingSideCastle() {
        return isCastle() && toX > fromX;
    }

    public boolean isQueenSideCastle() {
        return isCastle() && toX < fromX;
    }
}