package com.perigrine3.createcybernetics.item.generic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class GameShardItem extends DataShardItem {
    private final String gameId;
    private final String executableTranslationKey;

    public GameShardItem(
            Properties properties,
            String gameId,
            String executableTranslationKey
    ) {
        super(properties);

        this.gameId = gameId;
        this.executableTranslationKey = executableTranslationKey;
    }

    public String getGameId() {
        return gameId;
    }

    public Component getExecutableName(
            ItemStack stack
    ) {
        if (executableTranslationKey == null ||
                executableTranslationKey.isBlank()) {
            return stack.getHoverName();
        }

        return Component.translatable(
                executableTranslationKey
        );
    }
}