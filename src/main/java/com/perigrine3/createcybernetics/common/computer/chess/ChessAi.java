package com.perigrine3.createcybernetics.common.computer.chess;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ChessAi {
    private static final int CHECKMATE_SCORE =
            1_000_000;

    private ChessAi() {
    }

    public static ChessMove findBestMove(
            ChessGame game,
            ChessColor aiColor,
            int depth
    ) {
        List<ChessMove> legalMoves =
                new ArrayList<>(
                        game.getLegalMoves(aiColor)
                );

        if (legalMoves.isEmpty()) {
            return null;
        }

        orderMoves(
                game,
                legalMoves
        );

        ChessMove bestMove = null;

        int bestScore =
                Integer.MIN_VALUE;

        int alpha =
                Integer.MIN_VALUE + 1;

        int beta =
                Integer.MAX_VALUE;

        for (ChessMove move : legalMoves) {
            ChessGame copy =
                    game.copy();

            copy.makeMove(move);

            int score =
                    minimax(
                            copy,
                            depth - 1,
                            alpha,
                            beta,
                            aiColor,
                            1
                    );

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }

            alpha =
                    Math.max(
                            alpha,
                            bestScore
                    );
        }

        return bestMove;
    }

    private static int minimax(
            ChessGame game,
            int depth,
            int alpha,
            int beta,
            ChessColor aiColor,
            int ply
    ) {
        if (game.getStatus() !=
                ChessGameStatus.ACTIVE) {
            return evaluateTerminal(
                    game,
                    aiColor,
                    ply
            );
        }

        if (depth <= 0) {
            return evaluate(
                    game,
                    aiColor
            );
        }

        List<ChessMove> moves =
                new ArrayList<>(
                        game.getLegalMoves()
                );

        orderMoves(
                game,
                moves
        );

        boolean maximizing =
                game.getTurn() == aiColor;

        if (maximizing) {
            int score =
                    Integer.MIN_VALUE;

            for (ChessMove move : moves) {
                ChessGame copy =
                        game.copy();

                copy.makeMove(move);

                score =
                        Math.max(
                                score,
                                minimax(
                                        copy,
                                        depth - 1,
                                        alpha,
                                        beta,
                                        aiColor,
                                        ply + 1
                                )
                        );

                alpha =
                        Math.max(
                                alpha,
                                score
                        );

                if (beta <= alpha) {
                    break;
                }
            }

            return score;
        }

        int score =
                Integer.MAX_VALUE;

        for (ChessMove move : moves) {
            ChessGame copy =
                    game.copy();

            copy.makeMove(move);

            score =
                    Math.min(
                            score,
                            minimax(
                                    copy,
                                    depth - 1,
                                    alpha,
                                    beta,
                                    aiColor,
                                    ply + 1
                            )
                    );

            beta =
                    Math.min(
                            beta,
                            score
                    );

            if (beta <= alpha) {
                break;
            }
        }

        return score;
    }

    private static int evaluateTerminal(
            ChessGame game,
            ChessColor aiColor,
            int ply
    ) {
        if (game.getStatus() ==
                ChessGameStatus.STALEMATE) {
            return 0;
        }

        boolean aiWon =
                aiColor == ChessColor.WHITE
                        ? game.getStatus() ==
                        ChessGameStatus.WHITE_WON
                        : game.getStatus() ==
                        ChessGameStatus.BLACK_WON;

        return aiWon
                ? CHECKMATE_SCORE - ply
                : -CHECKMATE_SCORE + ply;
    }

    private static int evaluate(
            ChessGame game,
            ChessColor aiColor
    ) {
        int score = 0;

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                ChessPiece piece =
                        game.getPiece(x, y);

                if (piece == null) {
                    continue;
                }

                int value =
                        piece.type()
                                .materialValue();

                value += positionalBonus(
                        piece,
                        x,
                        y
                );

                if (piece.color() == aiColor) {
                    score += value;
                } else {
                    score -= value;
                }
            }
        }

        int aiMobility =
                game.getLegalMoves(aiColor)
                        .size();

        int opponentMobility =
                game.getLegalMoves(
                        aiColor.opposite()
                ).size();

        score +=
                (aiMobility - opponentMobility) * 3;

        if (game.isKingInCheck(
                aiColor.opposite()
        )) {
            score += 35;
        }

        if (game.isKingInCheck(aiColor)) {
            score -= 35;
        }

        return score;
    }

    private static int positionalBonus(
            ChessPiece piece,
            int x,
            int y
    ) {
        int centerDistance =
                Math.abs(x - 3) +
                        Math.abs(y - 3);

        return switch (piece.type()) {
            case PAWN -> {
                int advancement =
                        piece.color() ==
                                ChessColor.WHITE
                                ? 6 - y
                                : y - 1;

                yield advancement * 8;
            }

            case KNIGHT ->
                    30 - centerDistance * 6;

            case BISHOP ->
                    20 - centerDistance * 3;

            case ROOK ->
                    5;

            case QUEEN ->
                    10 - centerDistance * 2;

            case KING ->
                    0;
        };
    }

    private static void orderMoves(
            ChessGame game,
            List<ChessMove> moves
    ) {
        moves.sort(
                (firstMove, secondMove) ->
                        Integer.compare(
                                moveOrderingScore(
                                        game,
                                        secondMove
                                ),
                                moveOrderingScore(
                                        game,
                                        firstMove
                                )
                        )
        );
    }

    private static int moveOrderingScore(
            ChessGame game,
            ChessMove move
    ) {
        ChessPiece movingPiece =
                game.getPiece(
                        move.fromX(),
                        move.fromY()
                );

        ChessPiece capturedPiece =
                game.getPiece(
                        move.toX(),
                        move.toY()
                );

        int score = 0;

        if (capturedPiece != null &&
                movingPiece != null) {
            score +=
                    capturedPiece.type()
                            .materialValue() * 10;

            score -=
                    movingPiece.type()
                            .materialValue();
        }

        if (move.isPromotion()) {
            score +=
                    move.promotion()
                            .materialValue();
        }

        if (move.isCastle()) {
            score += 75;
        }

        return score;
    }
}