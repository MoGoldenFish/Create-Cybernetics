package com.perigrine3.createcybernetics.client.computer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public final class ChatSpaceClientData {
    private static final String TAG_CODE =
            "Code";

    private static final String TAG_CONTACTS =
            "Contacts";

    private static final String TAG_REMOTE_CODE =
            "RemoteCode";

    private static final String TAG_DISPLAY_NAME =
            "DisplayName";

    private static final String TAG_MESSAGES =
            "Messages";

    private static final String TAG_SENDER_CODE =
            "SenderCode";

    private static final String TAG_TEXT =
            "Text";

    private static final String TAG_TIMESTAMP =
            "Timestamp";

    private static final String TAG_UNREAD =
            "Unread";

    private static String computerCode = "";

    private static final List<ClientContact> contacts =
            new ArrayList<>();

    private static String pendingNotification = "";
    private static long notificationExpiresAt;

    private ChatSpaceClientData() {
    }

    public static void acceptSnapshot(
            CompoundTag snapshot
    ) {
        int previousUnread =
                getTotalUnreadCount();

        computerCode =
                snapshot.getString(TAG_CODE);

        contacts.clear();

        ListTag contactTags =
                snapshot.getList(
                        TAG_CONTACTS,
                        Tag.TAG_COMPOUND
                );

        for (int contactIndex = 0;
             contactIndex < contactTags.size();
             contactIndex++) {
            CompoundTag contactTag =
                    contactTags.getCompound(contactIndex);

            ClientContact contact =
                    new ClientContact(
                            contactTag.getString(
                                    TAG_REMOTE_CODE
                            ),
                            contactTag.getString(
                                    TAG_DISPLAY_NAME
                            ),
                            Math.max(
                                    0,
                                    contactTag.getInt(
                                            TAG_UNREAD
                                    )
                            )
                    );

            ListTag messageTags =
                    contactTag.getList(
                            TAG_MESSAGES,
                            Tag.TAG_COMPOUND
                    );

            for (int messageIndex = 0;
                 messageIndex < messageTags.size();
                 messageIndex++) {
                CompoundTag messageTag =
                        messageTags.getCompound(messageIndex);

                contact.messages.add(
                        new ClientMessage(
                                messageTag.getString(
                                        TAG_SENDER_CODE
                                ),
                                messageTag.getString(
                                        TAG_TEXT
                                ),
                                messageTag.getLong(
                                        TAG_TIMESTAMP
                                )
                        )
                );
            }

            contacts.add(contact);
        }

        int currentUnread =
                getTotalUnreadCount();

        if (currentUnread > previousUnread) {
            ClientContact newestUnread =
                    findNewestUnreadContact();

            if (newestUnread != null) {
                pendingNotification =
                        newestUnread.displayName;

                notificationExpiresAt =
                        System.currentTimeMillis()
                                + 5_000L;
            }
        }
    }

    private static ClientContact findNewestUnreadContact() {
        ClientContact result = null;
        long newestTimestamp = Long.MIN_VALUE;

        for (ClientContact contact : contacts) {
            if (contact.unreadCount <= 0 ||
                    contact.messages.isEmpty()) {
                continue;
            }

            ClientMessage finalMessage =
                    contact.messages.get(
                            contact.messages.size() - 1
                    );

            if (finalMessage.timestamp >
                    newestTimestamp) {
                newestTimestamp =
                        finalMessage.timestamp;

                result = contact;
            }
        }

        return result;
    }

    public static String getComputerCode() {
        return computerCode;
    }

    public static List<ClientContact> getContacts() {
        return List.copyOf(contacts);
    }

    public static ClientContact getContact(
            String remoteCode
    ) {
        for (ClientContact contact : contacts) {
            if (contact.remoteCode.equals(
                    remoteCode
            )) {
                return contact;
            }
        }

        return null;
    }

    public static int getTotalUnreadCount() {
        int unread = 0;

        for (ClientContact contact : contacts) {
            unread += contact.unreadCount;
        }

        return unread;
    }

    public static boolean hasActiveNotification() {
        return !pendingNotification.isBlank()
                && System.currentTimeMillis()
                < notificationExpiresAt;
    }

    public static String getPendingNotification() {
        return pendingNotification;
    }

    public static void clearNotification() {
        pendingNotification = "";
        notificationExpiresAt = 0L;
    }

    public static final class ClientContact {
        private final String remoteCode;
        private final String displayName;
        private final int unreadCount;

        private final List<ClientMessage> messages =
                new ArrayList<>();

        private ClientContact(
                String remoteCode,
                String displayName,
                int unreadCount
        ) {
            this.remoteCode = remoteCode;
            this.displayName = displayName;
            this.unreadCount = unreadCount;
        }

        public String remoteCode() {
            return remoteCode;
        }

        public String displayName() {
            return displayName;
        }

        public int unreadCount() {
            return unreadCount;
        }

        public List<ClientMessage> messages() {
            return List.copyOf(messages);
        }
    }

    public record ClientMessage(
            String senderCode,
            String text,
            long timestamp
    ) {
    }
}