package com.perigrine3.createcybernetics.common.computer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ChatSpaceSavedData extends SavedData {
    private static final String DATA_NAME =
            "createcybernetics_chatspace";

    private static final String TAG_COMPUTERS =
            "Computers";

    private static final String TAG_CODE =
            "Code";

    private static final String TAG_DIMENSION =
            "Dimension";

    private static final String TAG_X =
            "X";

    private static final String TAG_Y =
            "Y";

    private static final String TAG_Z =
            "Z";

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

    private static final String CODE_CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final int CODE_LENGTH = 5;
    private static final int MAX_GENERATION_ATTEMPTS = 10_000;

    public static final int MAX_CONTACT_NAME_LENGTH = 32;
    public static final int MAX_MESSAGE_LENGTH = 512;
    public static final int MAX_MESSAGES_PER_CONVERSATION = 200;

    private final Map<String, ComputerRecord> computers =
            new HashMap<>();

    public static ChatSpaceSavedData get(
            MinecraftServer server
    ) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        new SavedData.Factory<>(
                                ChatSpaceSavedData::new,
                                ChatSpaceSavedData::load
                        ),
                        DATA_NAME
                );
    }

    private static ChatSpaceSavedData load(
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {
        ChatSpaceSavedData data =
                new ChatSpaceSavedData();

        ListTag computerTags =
                tag.getList(
                        TAG_COMPUTERS,
                        Tag.TAG_COMPOUND
                );

        for (int computerIndex = 0;
             computerIndex < computerTags.size();
             computerIndex++) {
            CompoundTag computerTag =
                    computerTags.getCompound(computerIndex);

            String code = normalizeCode(
                    computerTag.getString(TAG_CODE)
            );

            ResourceLocation dimensionLocation =
                    ResourceLocation.tryParse(
                            computerTag.getString(TAG_DIMENSION)
                    );

            if (code.isBlank() ||
                    dimensionLocation == null) {
                continue;
            }

            ResourceKey<Level> dimension =
                    ResourceKey.create(
                            Registries.DIMENSION,
                            dimensionLocation
                    );

            BlockPos position =
                    new BlockPos(
                            computerTag.getInt(TAG_X),
                            computerTag.getInt(TAG_Y),
                            computerTag.getInt(TAG_Z)
                    );

            ComputerRecord record =
                    new ComputerRecord(
                            code,
                            dimension,
                            position
                    );

            loadContacts(
                    computerTag,
                    record
            );

            data.computers.put(
                    code,
                    record
            );
        }

        return data;
    }

    private static void loadContacts(
            CompoundTag computerTag,
            ComputerRecord record
    ) {
        ListTag contactTags =
                computerTag.getList(
                        TAG_CONTACTS,
                        Tag.TAG_COMPOUND
                );

        for (int contactIndex = 0;
             contactIndex < contactTags.size();
             contactIndex++) {
            CompoundTag contactTag =
                    contactTags.getCompound(contactIndex);

            String remoteCode = normalizeCode(
                    contactTag.getString(TAG_REMOTE_CODE)
            );

            if (remoteCode.isBlank()) {
                continue;
            }

            ContactRecord contact =
                    new ContactRecord(
                            remoteCode,
                            sanitizeName(
                                    contactTag.getString(
                                            TAG_DISPLAY_NAME
                                    )
                            )
                    );

            contact.unreadCount = Math.max(
                    0,
                    contactTag.getInt(TAG_UNREAD)
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

                String senderCode = normalizeCode(
                        messageTag.getString(
                                TAG_SENDER_CODE
                        )
                );

                String text = sanitizeMessage(
                        messageTag.getString(TAG_TEXT)
                );

                if (senderCode.isBlank() ||
                        text.isBlank()) {
                    continue;
                }

                contact.messages.add(
                        new MessageRecord(
                                senderCode,
                                text,
                                messageTag.getLong(
                                        TAG_TIMESTAMP
                                )
                        )
                );
            }

            trimMessages(contact.messages);

            record.contacts.put(
                    remoteCode,
                    contact
            );
        }
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {
        ListTag computerTags =
                new ListTag();

        for (ComputerRecord computer :
                computers.values()) {
            CompoundTag computerTag =
                    new CompoundTag();

            computerTag.putString(
                    TAG_CODE,
                    computer.code
            );

            computerTag.putString(
                    TAG_DIMENSION,
                    computer.dimension
                            .location()
                            .toString()
            );

            computerTag.putInt(
                    TAG_X,
                    computer.position.getX()
            );

            computerTag.putInt(
                    TAG_Y,
                    computer.position.getY()
            );

            computerTag.putInt(
                    TAG_Z,
                    computer.position.getZ()
            );

            ListTag contactTags =
                    new ListTag();

            for (ContactRecord contact :
                    computer.contacts.values()) {
                CompoundTag contactTag =
                        new CompoundTag();

                contactTag.putString(
                        TAG_REMOTE_CODE,
                        contact.remoteCode
                );

                contactTag.putString(
                        TAG_DISPLAY_NAME,
                        contact.displayName
                );

                contactTag.putInt(
                        TAG_UNREAD,
                        contact.unreadCount
                );

                ListTag messageTags =
                        new ListTag();

                for (MessageRecord message :
                        contact.messages) {
                    CompoundTag messageTag =
                            new CompoundTag();

                    messageTag.putString(
                            TAG_SENDER_CODE,
                            message.senderCode
                    );

                    messageTag.putString(
                            TAG_TEXT,
                            message.text
                    );

                    messageTag.putLong(
                            TAG_TIMESTAMP,
                            message.timestamp
                    );

                    messageTags.add(messageTag);
                }

                contactTag.put(
                        TAG_MESSAGES,
                        messageTags
                );

                contactTags.add(contactTag);
            }

            computerTag.put(
                    TAG_CONTACTS,
                    contactTags
            );

            computerTags.add(computerTag);
        }

        tag.put(
                TAG_COMPUTERS,
                computerTags
        );

        return tag;
    }

    public String registerComputer(
            ServerLevel level,
            BlockPos position,
            String requestedCode
    ) {
        String normalizedRequestedCode =
                normalizeCode(requestedCode);

        if (!normalizedRequestedCode.isBlank()) {
            ComputerRecord existing =
                    computers.get(normalizedRequestedCode);

            if (existing == null ||
                    existing.matches(
                            level.dimension(),
                            position
                    )) {
                if (existing == null) {
                    computers.put(
                            normalizedRequestedCode,
                            new ComputerRecord(
                                    normalizedRequestedCode,
                                    level.dimension(),
                                    position.immutable()
                            )
                    );
                } else {
                    existing.dimension =
                            level.dimension();

                    existing.position =
                            position.immutable();
                }

                setDirty();

                return normalizedRequestedCode;
            }
        }

        String generatedCode =
                generateUniqueCode(
                        level.getRandom()
                );

        computers.put(
                generatedCode,
                new ComputerRecord(
                        generatedCode,
                        level.dimension(),
                        position.immutable()
                )
        );

        setDirty();

        return generatedCode;
    }

    public void unregisterComputer(
            ResourceKey<Level> dimension,
            BlockPos position,
            String code
    ) {
        String normalizedCode =
                normalizeCode(code);

        ComputerRecord existing =
                computers.get(normalizedCode);

        if (existing == null ||
                !existing.matches(
                        dimension,
                        position
                )) {
            return;
        }

        computers.remove(normalizedCode);
        setDirty();
    }

    public boolean addContact(
            String sourceCode,
            String remoteCode,
            String displayName
    ) {
        ComputerRecord source =
                computers.get(
                        normalizeCode(sourceCode)
                );

        String normalizedRemoteCode =
                normalizeCode(remoteCode);

        if (source == null ||
                normalizedRemoteCode.isBlank() ||
                normalizedRemoteCode.equals(source.code) ||
                !computers.containsKey(normalizedRemoteCode)) {
            return false;
        }

        String sanitizedName =
                sanitizeName(displayName);

        if (sanitizedName.isBlank()) {
            return false;
        }

        ContactRecord existing =
                source.contacts.get(
                        normalizedRemoteCode
                );

        if (existing != null) {
            existing.displayName =
                    sanitizedName;
        } else {
            source.contacts.put(
                    normalizedRemoteCode,
                    new ContactRecord(
                            normalizedRemoteCode,
                            sanitizedName
                    )
            );
        }

        setDirty();

        return true;
    }

    public boolean sendMessage(
            String senderCode,
            String receiverCode,
            String messageText,
            long timestamp
    ) {
        ComputerRecord sender =
                computers.get(
                        normalizeCode(senderCode)
                );

        ComputerRecord receiver =
                computers.get(
                        normalizeCode(receiverCode)
                );

        String sanitizedMessage =
                sanitizeMessage(messageText);

        if (sender == null ||
                receiver == null ||
                sender == receiver ||
                sanitizedMessage.isBlank()) {
            return false;
        }

        ContactRecord senderContact =
                sender.contacts.computeIfAbsent(
                        receiver.code,
                        code -> new ContactRecord(
                                code,
                                code
                        )
                );

        ContactRecord receiverContact =
                receiver.contacts.computeIfAbsent(
                        sender.code,
                        code -> new ContactRecord(
                                code,
                                code
                        )
                );

        MessageRecord message =
                new MessageRecord(
                        sender.code,
                        sanitizedMessage,
                        timestamp
                );

        senderContact.messages.add(message);
        receiverContact.messages.add(message);

        receiverContact.unreadCount++;

        trimMessages(senderContact.messages);
        trimMessages(receiverContact.messages);

        setDirty();

        return true;
    }

    public void markRead(
            String computerCode,
            String remoteCode
    ) {
        ComputerRecord computer =
                computers.get(
                        normalizeCode(computerCode)
                );

        if (computer == null) {
            return;
        }

        ContactRecord contact =
                computer.contacts.get(
                        normalizeCode(remoteCode)
                );

        if (contact == null ||
                contact.unreadCount == 0) {
            return;
        }

        contact.unreadCount = 0;
        setDirty();
    }

    public Optional<ComputerRecord> findComputer(
            String code
    ) {
        return Optional.ofNullable(
                computers.get(
                        normalizeCode(code)
                )
        );
    }

    public CompoundTag createClientSnapshot(
            String computerCode
    ) {
        CompoundTag snapshot =
                new CompoundTag();

        ComputerRecord computer =
                computers.get(
                        normalizeCode(computerCode)
                );

        if (computer == null) {
            return snapshot;
        }

        snapshot.putString(
                TAG_CODE,
                computer.code
        );

        ListTag contactTags =
                new ListTag();

        List<ContactRecord> sortedContacts =
                new ArrayList<>(
                        computer.contacts.values()
                );

        sortedContacts.sort(
                Comparator.comparing(
                        contact -> contact.displayName
                                .toLowerCase()
                )
        );

        for (ContactRecord contact :
                sortedContacts) {
            CompoundTag contactTag =
                    new CompoundTag();

            contactTag.putString(
                    TAG_REMOTE_CODE,
                    contact.remoteCode
            );

            contactTag.putString(
                    TAG_DISPLAY_NAME,
                    contact.displayName
            );

            contactTag.putInt(
                    TAG_UNREAD,
                    contact.unreadCount
            );

            ListTag messageTags =
                    new ListTag();

            for (MessageRecord message :
                    contact.messages) {
                CompoundTag messageTag =
                        new CompoundTag();

                messageTag.putString(
                        TAG_SENDER_CODE,
                        message.senderCode
                );

                messageTag.putString(
                        TAG_TEXT,
                        message.text
                );

                messageTag.putLong(
                        TAG_TIMESTAMP,
                        message.timestamp
                );

                messageTags.add(messageTag);
            }

            contactTag.put(
                    TAG_MESSAGES,
                    messageTags
            );

            contactTags.add(contactTag);
        }

        snapshot.put(
                TAG_CONTACTS,
                contactTags
        );

        return snapshot;
    }

    private String generateUniqueCode(
            RandomSource random
    ) {
        for (int attempt = 0;
             attempt < MAX_GENERATION_ATTEMPTS;
             attempt++) {
            String generatedCode =
                    generateCode(random);

            if (!computers.containsKey(
                    generatedCode
            )) {
                return generatedCode;
            }
        }

        throw new IllegalStateException(
                "Unable to generate a unique ChatSpace computer code"
        );
    }

    private static String generateCode(
            RandomSource random
    ) {
        StringBuilder builder =
                new StringBuilder(CODE_LENGTH);

        for (int index = 0;
             index < CODE_LENGTH;
             index++) {
            builder.append(
                    CODE_CHARACTERS.charAt(
                            random.nextInt(
                                    CODE_CHARACTERS.length()
                            )
                    )
            );
        }

        return builder.toString();
    }

    public static String normalizeCode(
            String code
    ) {
        if (code == null) {
            return "";
        }

        String normalized =
                code.trim().toUpperCase();

        if (normalized.length() != CODE_LENGTH) {
            return "";
        }

        for (int index = 0;
             index < normalized.length();
             index++) {
            if (CODE_CHARACTERS.indexOf(
                    normalized.charAt(index)
            ) < 0) {
                return "";
            }
        }

        return normalized;
    }

    private static String sanitizeName(
            String name
    ) {
        if (name == null) {
            return "";
        }

        String sanitized =
                name.trim();

        if (sanitized.length() >
                MAX_CONTACT_NAME_LENGTH) {
            sanitized = sanitized.substring(
                    0,
                    MAX_CONTACT_NAME_LENGTH
            );
        }

        return sanitized;
    }

    private static String sanitizeMessage(
            String text
    ) {
        if (text == null) {
            return "";
        }

        String sanitized =
                text.trim();

        if (sanitized.length() >
                MAX_MESSAGE_LENGTH) {
            sanitized = sanitized.substring(
                    0,
                    MAX_MESSAGE_LENGTH
            );
        }

        return sanitized;
    }

    private static void trimMessages(
            List<MessageRecord> messages
    ) {
        while (messages.size() >
                MAX_MESSAGES_PER_CONVERSATION) {
            messages.remove(0);
        }
    }

    public static final class ComputerRecord {
        private final String code;

        private ResourceKey<Level> dimension;
        private BlockPos position;

        private final Map<String, ContactRecord> contacts =
                new HashMap<>();

        private ComputerRecord(
                String code,
                ResourceKey<Level> dimension,
                BlockPos position
        ) {
            this.code = code;
            this.dimension = dimension;
            this.position = position;
        }

        public String code() {
            return code;
        }

        public ResourceKey<Level> dimension() {
            return dimension;
        }

        public BlockPos position() {
            return position;
        }

        public boolean matches(
                ResourceKey<Level> otherDimension,
                BlockPos otherPosition
        ) {
            return dimension.equals(otherDimension)
                    && position.equals(otherPosition);
        }
    }

    private static final class ContactRecord {
        private final String remoteCode;

        private String displayName;
        private int unreadCount;

        private final List<MessageRecord> messages =
                new ArrayList<>();

        private ContactRecord(
                String remoteCode,
                String displayName
        ) {
            this.remoteCode = remoteCode;
            this.displayName = displayName;
        }
    }

    private record MessageRecord(
            String senderCode,
            String text,
            long timestamp
    ) {
    }
}