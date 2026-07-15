package com.perigrine3.createcybernetics.common.computer.chess;

import java.util.UUID;

public final class ChessSession {
    private final UUID sessionId;

    private final String whiteComputerCode;
    private final String blackComputerCode;

    private final long createdAt;

    private ChessGame game;
    private long lastActivityAt;

    public ChessSession(
            UUID sessionId,
            String whiteComputerCode,
            String blackComputerCode,
            ChessGame game,
            long createdAt,
            long lastActivityAt
    ) {
        this.sessionId = sessionId;
        this.whiteComputerCode = whiteComputerCode;
        this.blackComputerCode = blackComputerCode;
        this.game = game;
        this.createdAt = createdAt;
        this.lastActivityAt = lastActivityAt;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getWhiteComputerCode() {
        return whiteComputerCode;
    }

    public String getBlackComputerCode() {
        return blackComputerCode;
    }

    public ChessGame getGame() {
        return game;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastActivityAt() {
        return lastActivityAt;
    }

    public void setGame(
            ChessGame game
    ) {
        if (game == null) {
            return;
        }

        this.game = game;
        touch();
    }

    public void touch() {
        lastActivityAt =
                System.currentTimeMillis();
    }

    public boolean involves(
            String computerCode
    ) {
        return whiteComputerCode.equals(computerCode)
                || blackComputerCode.equals(computerCode);
    }

    public ChessColor getColorFor(
            String computerCode
    ) {
        if (whiteComputerCode.equals(computerCode)) {
            return ChessColor.WHITE;
        }

        if (blackComputerCode.equals(computerCode)) {
            return ChessColor.BLACK;
        }

        return null;
    }

    public String getOpponentCode(
            String computerCode
    ) {
        if (whiteComputerCode.equals(computerCode)) {
            return blackComputerCode;
        }

        if (blackComputerCode.equals(computerCode)) {
            return whiteComputerCode;
        }

        return "";
    }

    public boolean isFinished() {
        return game.getStatus()
                != ChessGameStatus.ACTIVE;
    }
}