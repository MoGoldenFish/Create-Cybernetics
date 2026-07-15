package com.perigrine3.createcybernetics.common.computer.chess;

public enum ChessPieceType {
    PAWN(100),
    KNIGHT(320),
    BISHOP(330),
    ROOK(500),
    QUEEN(900),
    KING(20_000);

    private final int materialValue;

    ChessPieceType(int materialValue) {
        this.materialValue = materialValue;
    }

    public int materialValue() {
        return materialValue;
    }
}