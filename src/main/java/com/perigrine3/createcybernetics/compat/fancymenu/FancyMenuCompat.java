package com.perigrine3.createcybernetics.compat.fancymenu;

import net.minecraft.client.player.AbstractClientPlayer;
import net.neoforged.fml.ModList;

public final class FancyMenuCompat {

    private static final boolean FANCY_MENU_LOADED =
            ModList.get().isLoaded("fancymenu");

    private static final boolean FANCY_ENTITY_RENDERER_LOADED =
            ModList.get().isLoaded("fancyentityrenderer");

    private FancyMenuCompat() {}

    public static boolean shouldIgnorePlayer(AbstractClientPlayer player) {
        if (!FANCY_MENU_LOADED && !FANCY_ENTITY_RENDERER_LOADED) return false;

        String className = player.getClass().getName();

        return className.startsWith("de.keksuccino.fancymenu.")
                || className.startsWith("de.keksuccino.fancyentityrenderer.")
                || className.contains("FancyPlayer")
                || className.contains("FakePlayer")
                || className.contains("PreviewPlayer")
                || className.contains("WidgetPlayer");
    }
}