package com.perigrine3.createcybernetics.client.computer;

import com.perigrine3.createcybernetics.common.computer.chess.ChessColor;
import com.perigrine3.createcybernetics.common.computer.chess.ChessGame;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ChessClientData {
    private static final String TAG_COMPUTER_CODE =
            "ComputerCode";

    private static final String TAG_INVITES =
            "Invites";

    private static final String TAG_SESSIONS =
            "Sessions";

    private static final String TAG_INVITE_ID =
            "InviteId";

    private static final String TAG_SESSION_ID =
            "SessionId";

    private static final String TAG_SENDER_CODE =
            "SenderCode";

    private static final String TAG_RECEIVER_CODE =
            "ReceiverCode";

    private static final String TAG_WHITE_CODE =
            "WhiteCode";

    private static final String TAG_BLACK_CODE =
            "BlackCode";

    private static final String TAG_OPPONENT_CODE =
            "OpponentCode";

    private static final String TAG_PLAYER_COLOR =
            "PlayerColor";

    private static final String TAG_CREATED_AT =
            "CreatedAt";

    private static final String TAG_LAST_ACTIVITY_AT =
            "LastActivityAt";

    private static final String TAG_DIRECTION =
            "Direction";

    private static final String TAG_GAME =
            "Game";

    private static final String DIRECTION_INCOMING =
            "INCOMING";

    private static String computerCode = "";

    private static final List<ClientInvite> invites =
            new ArrayList<>();

    private static final List<ClientSession> sessions =
            new ArrayList<>();

    private static String pendingNotificationCode = "";
    private static long notificationExpiresAt;

    private ChessClientData() {
    }

    public static void acceptSnapshot(
            CompoundTag snapshot
    ) {
        int previousIncomingCount =
                getIncomingInvites().size();

        computerCode =
                snapshot.getString(
                        TAG_COMPUTER_CODE
                );

        invites.clear();
        sessions.clear();

        readInvites(snapshot);
        readSessions(snapshot);

        List<ClientInvite> incoming =
                getIncomingInvites();

        if (incoming.size() >
                previousIncomingCount &&
                !incoming.isEmpty()) {
            pendingNotificationCode =
                    incoming.get(0)
                            .senderCode();

            notificationExpiresAt =
                    System.currentTimeMillis()
                            + 5_000L;
        }
    }

    private static void readInvites(
            CompoundTag snapshot
    ) {
        ListTag inviteTags =
                snapshot.getList(
                        TAG_INVITES,
                        Tag.TAG_COMPOUND
                );

        for (int index = 0;
             index < inviteTags.size();
             index++) {
            CompoundTag inviteTag =
                    inviteTags.getCompound(index);

            UUID inviteId =
                    parseUuid(
                            inviteTag.getString(
                                    TAG_INVITE_ID
                            )
                    );

            if (inviteId == null) {
                continue;
            }

            invites.add(
                    new ClientInvite(
                            inviteId,
                            inviteTag.getString(
                                    TAG_SENDER_CODE
                            ),
                            inviteTag.getString(
                                    TAG_RECEIVER_CODE
                            ),
                            DIRECTION_INCOMING.equals(
                                    inviteTag.getString(
                                            TAG_DIRECTION
                                    )
                            ),
                            inviteTag.getLong(
                                    TAG_CREATED_AT
                            )
                    )
            );
        }
    }

    private static void readSessions(
            CompoundTag snapshot
    ) {
        ListTag sessionTags =
                snapshot.getList(
                        TAG_SESSIONS,
                        Tag.TAG_COMPOUND
                );

        for (int index = 0;
             index < sessionTags.size();
             index++) {
            CompoundTag sessionTag =
                    sessionTags.getCompound(index);

            UUID sessionId =
                    parseUuid(
                            sessionTag.getString(
                                    TAG_SESSION_ID
                            )
                    );

            if (sessionId == null ||
                    !sessionTag.contains(
                            TAG_GAME,
                            Tag.TAG_COMPOUND
                    )) {
                continue;
            }

            ChessColor playerColor =
                    parseColor(
                            sessionTag.getString(
                                    TAG_PLAYER_COLOR
                            )
                    );

            sessions.add(
                    new ClientSession(
                            sessionId,
                            sessionTag.getString(
                                    TAG_WHITE_CODE
                            ),
                            sessionTag.getString(
                                    TAG_BLACK_CODE
                            ),
                            sessionTag.getString(
                                    TAG_OPPONENT_CODE
                            ),
                            playerColor,
                            ChessGame.loadFromTag(
                                    sessionTag.getCompound(
                                            TAG_GAME
                                    )
                            ),
                            sessionTag.getLong(
                                    TAG_CREATED_AT
                            ),
                            sessionTag.getLong(
                                    TAG_LAST_ACTIVITY_AT
                            )
                    )
            );
        }
    }

    public static String getComputerCode() {
        return computerCode;
    }

    public static List<ClientInvite> getInvites() {
        return List.copyOf(invites);
    }

    public static List<ClientInvite> getIncomingInvites() {
        return invites.stream()
                .filter(
                        ClientInvite::incoming
                )
                .toList();
    }

    public static List<ClientInvite> getOutgoingInvites() {
        return invites.stream()
                .filter(
                        invite -> !invite.incoming()
                )
                .toList();
    }

    public static List<ClientSession> getSessions() {
        return List.copyOf(sessions);
    }

    public static ClientSession getActiveSession() {
        for (ClientSession session :
                sessions) {
            if (!session.game()
                    .getStatus()
                    .equals(
                            com.perigrine3.createcybernetics.common.computer.chess.ChessGameStatus.ACTIVE
                    )) {
                continue;
            }

            return session;
        }

        return null;
    }

    public static ClientSession getSession(
            UUID sessionId
    ) {
        for (ClientSession session :
                sessions) {
            if (session.sessionId()
                    .equals(sessionId)) {
                return session;
            }
        }

        return null;
    }

    public static boolean hasActiveNotification() {
        return !pendingNotificationCode.isBlank()
                && System.currentTimeMillis()
                < notificationExpiresAt;
    }

    public static String getPendingNotificationCode() {
        return pendingNotificationCode;
    }

    public static void clearNotification() {
        pendingNotificationCode = "";
        notificationExpiresAt = 0L;
    }

    public static void clear() {
        computerCode = "";
        invites.clear();
        sessions.clear();
        clearNotification();
    }

    private static UUID parseUuid(
            String value
    ) {
        if (value == null ||
                value.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static ChessColor parseColor(
            String value
    ) {
        if (value == null ||
                value.isBlank()) {
            return null;
        }

        try {
            return ChessColor.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public record ClientInvite(
            UUID inviteId,
            String senderCode,
            String receiverCode,
            boolean incoming,
            long createdAt
    ) {
        public String remoteCode() {
            return incoming
                    ? senderCode
                    : receiverCode;
        }
    }

    public record ClientSession(
            UUID sessionId,
            String whiteCode,
            String blackCode,
            String opponentCode,
            ChessColor playerColor,
            ChessGame game,
            long createdAt,
            long lastActivityAt
    ) {
    }
}