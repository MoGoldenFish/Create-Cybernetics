package com.perigrine3.createcybernetics.common.computer.chess;

import com.perigrine3.createcybernetics.common.computer.ChatSpaceSavedData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ChessSavedData extends SavedData {
    private static final String DATA_NAME =
            "createcybernetics_chess";

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

    private static final String TAG_CREATED_AT =
            "CreatedAt";

    private static final String TAG_LAST_ACTIVITY_AT =
            "LastActivityAt";

    private static final String TAG_GAME =
            "Game";

    private static final String TAG_DIRECTION =
            "Direction";

    private static final String TAG_PLAYER_COLOR =
            "PlayerColor";

    private static final String TAG_OPPONENT_CODE =
            "OpponentCode";

    private static final String DIRECTION_INCOMING =
            "INCOMING";

    private static final String DIRECTION_OUTGOING =
            "OUTGOING";

    public static final long INVITE_EXPIRATION_MILLIS =
            5L * 60L * 1000L;

    private final Map<UUID, ChessInvite> invites =
            new HashMap<>();

    private final Map<UUID, ChessSession> sessions =
            new HashMap<>();

    public static ChessSavedData get(
            MinecraftServer server
    ) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        new SavedData.Factory<>(
                                ChessSavedData::new,
                                ChessSavedData::load
                        ),
                        DATA_NAME
                );
    }

    private static ChessSavedData load(
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {
        ChessSavedData data =
                new ChessSavedData();

        data.loadInvites(tag);
        data.loadSessions(tag);

        return data;
    }

    private void loadInvites(
            CompoundTag rootTag
    ) {
        ListTag inviteTags =
                rootTag.getList(
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

            String senderCode =
                    ChatSpaceSavedData.normalizeCode(
                            inviteTag.getString(
                                    TAG_SENDER_CODE
                            )
                    );

            String receiverCode =
                    ChatSpaceSavedData.normalizeCode(
                            inviteTag.getString(
                                    TAG_RECEIVER_CODE
                            )
                    );

            if (inviteId == null ||
                    senderCode.isBlank() ||
                    receiverCode.isBlank() ||
                    senderCode.equals(receiverCode)) {
                continue;
            }

            invites.put(
                    inviteId,
                    new ChessInvite(
                            inviteId,
                            senderCode,
                            receiverCode,
                            inviteTag.getLong(
                                    TAG_CREATED_AT
                            )
                    )
            );
        }
    }

    private void loadSessions(
            CompoundTag rootTag
    ) {
        ListTag sessionTags =
                rootTag.getList(
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

            String whiteCode =
                    ChatSpaceSavedData.normalizeCode(
                            sessionTag.getString(
                                    TAG_WHITE_CODE
                            )
                    );

            String blackCode =
                    ChatSpaceSavedData.normalizeCode(
                            sessionTag.getString(
                                    TAG_BLACK_CODE
                            )
                    );

            if (sessionId == null ||
                    whiteCode.isBlank() ||
                    blackCode.isBlank() ||
                    whiteCode.equals(blackCode) ||
                    !sessionTag.contains(
                            TAG_GAME,
                            Tag.TAG_COMPOUND
                    )) {
                continue;
            }

            ChessGame game =
                    ChessGame.loadFromTag(
                            sessionTag.getCompound(
                                    TAG_GAME
                            )
                    );

            ChessSession session =
                    new ChessSession(
                            sessionId,
                            whiteCode,
                            blackCode,
                            game,
                            sessionTag.getLong(
                                    TAG_CREATED_AT
                            ),
                            sessionTag.getLong(
                                    TAG_LAST_ACTIVITY_AT
                            )
                    );

            sessions.put(
                    sessionId,
                    session
            );
        }
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {
        ListTag inviteTags =
                new ListTag();

        for (ChessInvite invite :
                invites.values()) {
            CompoundTag inviteTag =
                    new CompoundTag();

            inviteTag.putString(
                    TAG_INVITE_ID,
                    invite.inviteId()
                            .toString()
            );

            inviteTag.putString(
                    TAG_SENDER_CODE,
                    invite.senderCode()
            );

            inviteTag.putString(
                    TAG_RECEIVER_CODE,
                    invite.receiverCode()
            );

            inviteTag.putLong(
                    TAG_CREATED_AT,
                    invite.createdAt()
            );

            inviteTags.add(
                    inviteTag
            );
        }

        tag.put(
                TAG_INVITES,
                inviteTags
        );

        ListTag sessionTags =
                new ListTag();

        for (ChessSession session :
                sessions.values()) {
            CompoundTag sessionTag =
                    new CompoundTag();

            sessionTag.putString(
                    TAG_SESSION_ID,
                    session.getSessionId()
                            .toString()
            );

            sessionTag.putString(
                    TAG_WHITE_CODE,
                    session.getWhiteComputerCode()
            );

            sessionTag.putString(
                    TAG_BLACK_CODE,
                    session.getBlackComputerCode()
            );

            sessionTag.putLong(
                    TAG_CREATED_AT,
                    session.getCreatedAt()
            );

            sessionTag.putLong(
                    TAG_LAST_ACTIVITY_AT,
                    session.getLastActivityAt()
            );

            sessionTag.put(
                    TAG_GAME,
                    session.getGame()
                            .saveToTag()
            );

            sessionTags.add(
                    sessionTag
            );
        }

        tag.put(
                TAG_SESSIONS,
                sessionTags
        );

        return tag;
    }

    public CreateInviteResult createInvite(
            MinecraftServer server,
            String senderCode,
            String receiverCode
    ) {
        pruneExpiredInvites();

        String normalizedSender =
                ChatSpaceSavedData.normalizeCode(
                        senderCode
                );

        String normalizedReceiver =
                ChatSpaceSavedData.normalizeCode(
                        receiverCode
                );

        if (normalizedSender.isBlank() ||
                normalizedReceiver.isBlank()) {
            return CreateInviteResult.INVALID_CODE;
        }

        if (normalizedSender.equals(
                normalizedReceiver
        )) {
            return CreateInviteResult.SAME_COMPUTER;
        }

        ChatSpaceSavedData chatData =
                ChatSpaceSavedData.get(server);

        if (chatData.findComputer(
                normalizedSender
        ).isEmpty()) {
            return CreateInviteResult.SENDER_NOT_FOUND;
        }

        if (chatData.findComputer(
                normalizedReceiver
        ).isEmpty()) {
            return CreateInviteResult.RECEIVER_NOT_FOUND;
        }

        if (findActiveSessionForComputer(
                normalizedSender
        ).isPresent()) {
            return CreateInviteResult.SENDER_ALREADY_PLAYING;
        }

        if (findActiveSessionForComputer(
                normalizedReceiver
        ).isPresent()) {
            return CreateInviteResult.RECEIVER_ALREADY_PLAYING;
        }

        if (hasPendingInviteBetween(
                normalizedSender,
                normalizedReceiver
        )) {
            return CreateInviteResult.INVITE_ALREADY_EXISTS;
        }

        /*
         * Manual chess connections become ChatSpace contacts.
         */
        chatData.addContact(
                normalizedSender,
                normalizedReceiver,
                normalizedReceiver
        );

        chatData.addContact(
                normalizedReceiver,
                normalizedSender,
                normalizedSender
        );

        ChessInvite invite =
                new ChessInvite(
                        UUID.randomUUID(),
                        normalizedSender,
                        normalizedReceiver,
                        System.currentTimeMillis()
                );

        invites.put(
                invite.inviteId(),
                invite
        );

        setDirty();

        return CreateInviteResult.SUCCESS;
    }

    public RespondInviteResult respondToInvite(
            MinecraftServer server,
            UUID inviteId,
            String respondingComputerCode,
            boolean accepted
    ) {
        pruneExpiredInvites();

        ChessInvite invite =
                invites.get(inviteId);

        if (invite == null) {
            return RespondInviteResult.INVITE_NOT_FOUND;
        }

        String normalizedResponder =
                ChatSpaceSavedData.normalizeCode(
                        respondingComputerCode
                );

        if (!invite.receiverCode().equals(
                normalizedResponder
        )) {
            return RespondInviteResult.NOT_INVITE_RECEIVER;
        }

        invites.remove(inviteId);

        if (!accepted) {
            setDirty();

            return RespondInviteResult.DECLINED;
        }

        if (findActiveSessionForComputer(
                invite.senderCode()
        ).isPresent() ||
                findActiveSessionForComputer(
                        invite.receiverCode()
                ).isPresent()) {
            setDirty();

            return RespondInviteResult.COMPUTER_ALREADY_PLAYING;
        }

        ChatSpaceSavedData chatData =
                ChatSpaceSavedData.get(server);

        if (chatData.findComputer(
                invite.senderCode()
        ).isEmpty() ||
                chatData.findComputer(
                        invite.receiverCode()
                ).isEmpty()) {
            setDirty();

            return RespondInviteResult.COMPUTER_NOT_FOUND;
        }

        boolean senderIsWhite =
                server.overworld()
                        .getRandom()
                        .nextBoolean();

        String whiteCode =
                senderIsWhite
                        ? invite.senderCode()
                        : invite.receiverCode();

        String blackCode =
                senderIsWhite
                        ? invite.receiverCode()
                        : invite.senderCode();

        long now =
                System.currentTimeMillis();

        ChessSession session =
                new ChessSession(
                        UUID.randomUUID(),
                        whiteCode,
                        blackCode,
                        new ChessGame(),
                        now,
                        now
                );

        sessions.put(
                session.getSessionId(),
                session
        );

        removeInvitesInvolving(
                invite.senderCode()
        );

        removeInvitesInvolving(
                invite.receiverCode()
        );

        setDirty();

        return RespondInviteResult.ACCEPTED;
    }

    public MoveResult makeMove(
            UUID sessionId,
            String computerCode,
            ChessMove move
    ) {
        ChessSession session =
                sessions.get(sessionId);

        if (session == null) {
            return MoveResult.SESSION_NOT_FOUND;
        }

        String normalizedCode =
                ChatSpaceSavedData.normalizeCode(
                        computerCode
                );

        ChessColor playerColor =
                session.getColorFor(
                        normalizedCode
                );

        if (playerColor == null) {
            return MoveResult.NOT_A_PLAYER;
        }

        ChessGame game =
                session.getGame();

        if (game.getStatus() !=
                ChessGameStatus.ACTIVE) {
            return MoveResult.GAME_FINISHED;
        }

        if (game.getTurn() != playerColor) {
            return MoveResult.NOT_YOUR_TURN;
        }

        if (!game.makeMove(move)) {
            return MoveResult.ILLEGAL_MOVE;
        }

        session.touch();
        setDirty();

        return MoveResult.SUCCESS;
    }

    public boolean resign(
            UUID sessionId,
            String computerCode
    ) {
        ChessSession session =
                sessions.get(sessionId);

        String normalizedCode =
                ChatSpaceSavedData.normalizeCode(
                        computerCode
                );

        if (session == null ||
                !session.involves(
                        normalizedCode
                )) {
            return false;
        }

        sessions.remove(sessionId);
        setDirty();

        return true;
    }

    public Optional<ChessInvite> getInvite(
            UUID inviteId
    ) {
        pruneExpiredInvites();

        return Optional.ofNullable(
                invites.get(inviteId)
        );
    }

    public Optional<ChessSession> getSession(
            UUID sessionId
    ) {
        return Optional.ofNullable(
                sessions.get(sessionId)
        );
    }

    public List<ChessInvite> getIncomingInvites(
            String computerCode
    ) {
        pruneExpiredInvites();

        String normalizedCode =
                ChatSpaceSavedData.normalizeCode(
                        computerCode
                );

        List<ChessInvite> result =
                new ArrayList<>();

        for (ChessInvite invite :
                invites.values()) {
            if (invite.receiverCode().equals(
                    normalizedCode
            )) {
                result.add(invite);
            }
        }

        result.sort(
                Comparator.comparingLong(
                        ChessInvite::createdAt
                ).reversed()
        );

        return List.copyOf(result);
    }

    public List<ChessInvite> getOutgoingInvites(
            String computerCode
    ) {
        pruneExpiredInvites();

        String normalizedCode =
                ChatSpaceSavedData.normalizeCode(
                        computerCode
                );

        List<ChessInvite> result =
                new ArrayList<>();

        for (ChessInvite invite :
                invites.values()) {
            if (invite.senderCode().equals(
                    normalizedCode
            )) {
                result.add(invite);
            }
        }

        result.sort(
                Comparator.comparingLong(
                        ChessInvite::createdAt
                ).reversed()
        );

        return List.copyOf(result);
    }

    public Optional<ChessSession> findActiveSessionForComputer(
            String computerCode
    ) {
        String normalizedCode =
                ChatSpaceSavedData.normalizeCode(
                        computerCode
                );

        for (ChessSession session :
                sessions.values()) {
            if (session.involves(normalizedCode) &&
                    !session.isFinished()) {
                return Optional.of(session);
            }
        }

        return Optional.empty();
    }

    public List<ChessSession> getSessionsForComputer(
            String computerCode
    ) {
        String normalizedCode =
                ChatSpaceSavedData.normalizeCode(
                        computerCode
                );

        List<ChessSession> result =
                new ArrayList<>();

        for (ChessSession session :
                sessions.values()) {
            if (session.involves(
                    normalizedCode
            )) {
                result.add(session);
            }
        }

        result.sort(
                Comparator.comparingLong(
                        ChessSession::getLastActivityAt
                ).reversed()
        );

        return List.copyOf(result);
    }

    public CompoundTag createClientSnapshot(
            String computerCode
    ) {
        pruneExpiredInvites();

        String normalizedCode =
                ChatSpaceSavedData.normalizeCode(
                        computerCode
                );

        CompoundTag snapshot =
                new CompoundTag();

        snapshot.putString(
                TAG_COMPUTER_CODE,
                normalizedCode
        );

        ListTag inviteTags =
                new ListTag();

        for (ChessInvite invite :
                getIncomingInvites(normalizedCode)) {
            inviteTags.add(
                    createInviteSnapshotTag(
                            invite,
                            DIRECTION_INCOMING
                    )
            );
        }

        for (ChessInvite invite :
                getOutgoingInvites(normalizedCode)) {
            inviteTags.add(
                    createInviteSnapshotTag(
                            invite,
                            DIRECTION_OUTGOING
                    )
            );
        }

        snapshot.put(
                TAG_INVITES,
                inviteTags
        );

        ListTag sessionTags =
                new ListTag();

        for (ChessSession session :
                getSessionsForComputer(normalizedCode)) {
            CompoundTag sessionTag =
                    new CompoundTag();

            ChessColor playerColor =
                    session.getColorFor(
                            normalizedCode
                    );

            sessionTag.putString(
                    TAG_SESSION_ID,
                    session.getSessionId()
                            .toString()
            );

            sessionTag.putString(
                    TAG_WHITE_CODE,
                    session.getWhiteComputerCode()
            );

            sessionTag.putString(
                    TAG_BLACK_CODE,
                    session.getBlackComputerCode()
            );

            sessionTag.putString(
                    TAG_OPPONENT_CODE,
                    session.getOpponentCode(
                            normalizedCode
                    )
            );

            sessionTag.putString(
                    TAG_PLAYER_COLOR,
                    playerColor == null
                            ? ""
                            : playerColor.name()
            );

            sessionTag.putLong(
                    TAG_CREATED_AT,
                    session.getCreatedAt()
            );

            sessionTag.putLong(
                    TAG_LAST_ACTIVITY_AT,
                    session.getLastActivityAt()
            );

            sessionTag.put(
                    TAG_GAME,
                    session.getGame()
                            .saveToTag()
            );

            sessionTags.add(
                    sessionTag
            );
        }

        snapshot.put(
                TAG_SESSIONS,
                sessionTags
        );

        return snapshot;
    }

    private static CompoundTag createInviteSnapshotTag(
            ChessInvite invite,
            String direction
    ) {
        CompoundTag inviteTag =
                new CompoundTag();

        inviteTag.putString(
                TAG_INVITE_ID,
                invite.inviteId()
                        .toString()
        );

        inviteTag.putString(
                TAG_SENDER_CODE,
                invite.senderCode()
        );

        inviteTag.putString(
                TAG_RECEIVER_CODE,
                invite.receiverCode()
        );

        inviteTag.putString(
                TAG_DIRECTION,
                direction
        );

        inviteTag.putLong(
                TAG_CREATED_AT,
                invite.createdAt()
        );

        return inviteTag;
    }

    private boolean hasPendingInviteBetween(
            String firstCode,
            String secondCode
    ) {
        for (ChessInvite invite :
                invites.values()) {
            boolean direct =
                    invite.senderCode().equals(
                            firstCode
                    ) &&
                            invite.receiverCode().equals(
                                    secondCode
                            );

            boolean reverse =
                    invite.senderCode().equals(
                            secondCode
                    ) &&
                            invite.receiverCode().equals(
                                    firstCode
                            );

            if (direct || reverse) {
                return true;
            }
        }

        return false;
    }

    private void removeInvitesInvolving(
            String computerCode
    ) {
        invites.entrySet()
                .removeIf(
                        entry ->
                                entry.getValue()
                                        .involves(
                                                computerCode
                                        )
                );
    }

    private void pruneExpiredInvites() {
        long cutoff =
                System.currentTimeMillis()
                        - INVITE_EXPIRATION_MILLIS;

        boolean removed =
                invites.entrySet()
                        .removeIf(
                                entry ->
                                        entry.getValue()
                                                .createdAt()
                                                < cutoff
                        );

        if (removed) {
            setDirty();
        }
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

    public enum CreateInviteResult {
        SUCCESS,
        INVALID_CODE,
        SAME_COMPUTER,
        SENDER_NOT_FOUND,
        RECEIVER_NOT_FOUND,
        SENDER_ALREADY_PLAYING,
        RECEIVER_ALREADY_PLAYING,
        INVITE_ALREADY_EXISTS
    }

    public enum RespondInviteResult {
        ACCEPTED,
        DECLINED,
        INVITE_NOT_FOUND,
        NOT_INVITE_RECEIVER,
        COMPUTER_NOT_FOUND,
        COMPUTER_ALREADY_PLAYING
    }

    public enum MoveResult {
        SUCCESS,
        SESSION_NOT_FOUND,
        NOT_A_PLAYER,
        NOT_YOUR_TURN,
        GAME_FINISHED,
        ILLEGAL_MOVE
    }
}