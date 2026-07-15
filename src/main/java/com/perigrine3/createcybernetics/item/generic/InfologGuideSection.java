package com.perigrine3.createcybernetics.item.generic;

import net.neoforged.fml.ModList;

public record InfologGuideSection(String translationKey, String requiredModId) {

    public static InfologGuideSection always(String translationKey) {
        return new InfologGuideSection(translationKey, "");
    }

    public static InfologGuideSection whenModLoaded(String requiredModId, String translationKey) {
        return new InfologGuideSection(translationKey, requiredModId);
    }

    public boolean isEnabled() {
        return requiredModId == null
                || requiredModId.isBlank()
                || ModList.get().isLoaded(requiredModId);
    }
}