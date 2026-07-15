package com.perigrine3.createcybernetics.network.handler;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.item.generic.InfologTextData;
import com.perigrine3.createcybernetics.network.payload.InfologSaveChipwarePayload;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class InfologSaveChipwareHandler {
    private static final int MAX_TEXT_LENGTH = 32000;
    private static final int MAX_TITLE_LENGTH = 32;

    private InfologSaveChipwareHandler() {}

    public static void handle(InfologSaveChipwarePayload payload, ServerPlayer sp) {
        if (!sp.hasData(ModAttachments.CYBERWARE)) return;

        PlayerCyberwareData data = sp.getData(ModAttachments.CYBERWARE);
        if (data == null) return;

        if (!data.hasSpecificItem(
                ModItems.BRAINUPGRADES_CHIPWARESLOTS.get(),
                CyberwareSlot.BRAIN
        )) {
            return;
        }

        int slot = payload.chipwareSlot();
        if (slot < 0 || slot >= PlayerCyberwareData.CHIPWARE_SLOT_COUNT) return;

        ItemStack current = data.getChipwareStack(slot);
        if (current.isEmpty()) return;
        if (!current.is(ModItems.DATA_SHARD_INFOLOG.get())) return;

        if (InfologTextData.isLocked(current)) return;

        String text = payload.text();
        if (text == null) text = "";

        if (text.length() > MAX_TEXT_LENGTH) {
            text = text.substring(0, MAX_TEXT_LENGTH);
        }

        String title = payload.title();
        if (title == null) title = "";

        title = title.trim();

        if (title.length() > MAX_TITLE_LENGTH) {
            title = title.substring(0, MAX_TITLE_LENGTH);
        }

        if (!payload.locked()) {
            ItemStack updated = current.copy();
            updated.setCount(1);

            InfologTextData.setText(updated, text);
            data.setChipwareStack(slot, updated);
            data.setDirty();
            sp.syncData(ModAttachments.CYBERWARE);
            return;
        }

        if (title.isEmpty()) return;

        ItemStack updated = current.copy();
        updated.setCount(1);

        InfologTextData.setText(updated, text);
        InfologTextData.setTitle(updated, title);
        InfologTextData.setLocked(updated, true);

        updated.set(DataComponents.CUSTOM_NAME, Component.literal(title));

        data.setChipwareStack(slot, updated);
        data.setDirty();
        sp.syncData(ModAttachments.CYBERWARE);
    }
}