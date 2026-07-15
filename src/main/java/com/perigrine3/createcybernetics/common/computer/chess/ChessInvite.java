package com.perigrine3.createcybernetics.common.computer.chess;

import java.util.UUID;

public record ChessInvite(
        UUID inviteId,
        String senderCode,
        String receiverCode,
        long createdAt
) {
    public boolean involves(
            String computerCode
    ) {
        return senderCode.equals(computerCode)
                || receiverCode.equals(computerCode);
    }

    public boolean isFor(
            String computerCode
    ) {
        return receiverCode.equals(computerCode);
    }
}