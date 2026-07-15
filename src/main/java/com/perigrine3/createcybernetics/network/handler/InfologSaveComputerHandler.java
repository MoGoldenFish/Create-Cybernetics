package com.perigrine3.createcybernetics.network.handler;

import com.perigrine3.createcybernetics.block.entity.ComputerTowerBlockEntity;
import com.perigrine3.createcybernetics.item.generic.InfologDataShardItem;
import com.perigrine3.createcybernetics.item.generic.InfologTextData;
import com.perigrine3.createcybernetics.network.payload.InfologSaveComputerPayload;
import com.perigrine3.createcybernetics.screen.custom.computer.ComputerMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class InfologSaveComputerHandler {
    private static final int MAX_TEXT_LENGTH = 32_000;
    private static final int MAX_TITLE_LENGTH = 32;

    private InfologSaveComputerHandler() {
    }

    public static void handle(
            InfologSaveComputerPayload payload,
            ServerPlayer player
    ) {
        if (!(player.containerMenu instanceof ComputerMenu menu)) {
            return;
        }

        if (!menu.getComputerPos().equals(
                payload.computerPos()
        )) {
            return;
        }

        if (player.distanceToSqr(
                payload.computerPos().getX() + 0.5D,
                payload.computerPos().getY() + 0.5D,
                payload.computerPos().getZ() + 0.5D
        ) > 64.0D) {
            return;
        }

        BlockEntity blockEntity =
                player.level().getBlockEntity(
                        payload.computerPos().below()
                );

        if (!(blockEntity instanceof ComputerTowerBlockEntity tower)) {
            return;
        }

        tower.forceGenerateLootFromBlockNbt(player);

        int slot = payload.towerSlot();

        if (slot < 0 ||
                slot >= ComputerTowerBlockEntity.INVENTORY_SIZE) {
            return;
        }

        ItemStack currentStack =
                tower.getItem(slot);

        if (currentStack.isEmpty() ||
                !(currentStack.getItem()
                        instanceof InfologDataShardItem)) {
            return;
        }

        if (InfologTextData.isLocked(currentStack)) {
            return;
        }

        String text =
                sanitizeText(
                        payload.text()
                );

        String title =
                sanitizeTitle(
                        payload.title()
                );

        /*
         * Work on a copy and put it back through setItem(...)
         * so the block entity marks itself dirty and updates state.
         */
        ItemStack updatedStack =
                currentStack.copy();

        InfologTextData.setText(
                updatedStack,
                text
        );

        if (!title.isBlank()) {
            InfologTextData.setTitle(
                    updatedStack,
                    title
            );
        }

        InfologTextData.setLocked(
                updatedStack,
                payload.locked()
        );

        tower.setItem(
                slot,
                updatedStack
        );
    }

    private static String sanitizeText(
            String text
    ) {
        if (text == null) {
            return "";
        }

        if (text.length() > MAX_TEXT_LENGTH) {
            return text.substring(
                    0,
                    MAX_TEXT_LENGTH
            );
        }

        return text;
    }

    private static String sanitizeTitle(
            String title
    ) {
        if (title == null) {
            return "";
        }

        String sanitized =
                title.trim();

        if (sanitized.length() > MAX_TITLE_LENGTH) {
            return sanitized.substring(
                    0,
                    MAX_TITLE_LENGTH
            );
        }

        return sanitized;
    }
}