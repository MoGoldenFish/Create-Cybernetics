package com.perigrine3.createcybernetics.compat.apothicattributes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ApothicAttributesClientCompat {

    private static final String[] MODIDS = {
            "apothic_attributes",
            "attributeslib"
    };

    private static final String AL_CONFIG_CLASS = "dev.shadowsoffire.apothic_attributes.ALConfig";
    private static final String ATTRIBUTES_GUI_CLASS = "dev.shadowsoffire.apothic_attributes.client.AttributesGui";
    private static final String BUTTON_PLACEMENT_CLASS = "dev.shadowsoffire.apothic_attributes.client.ButtonPlacement";

    private ApothicAttributesClientCompat() {}

    public static boolean isLoaded() {
        for (String modid : MODIDS) {
            if (ModList.get().isLoaded(modid)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isAttributesGuiEnabled() {
        if (!isLoaded()) {
            return false;
        }

        try {
            Class<?> configClass = Class.forName(AL_CONFIG_CLASS);
            Field field = configClass.getField("enableAttributesGui");
            Object value = field.get(null);
            return value instanceof Boolean enabled && enabled;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static AbstractWidget createAttributesButton(AbstractContainerScreen<?> parent) {
        if (parent == null || !isAttributesGuiEnabled()) {
            return null;
        }

        WidgetSprites sprites = getSwordButtonSprites();
        if (sprites == null) {
            return null;
        }

        ImageButton button = new ImageButton(
                0,
                0,
                10,
                10,
                sprites,
                btn -> {
                    openVanillaInventoryForAttributes();
                    btn.setFocused(false);
                },
                Component.translatable("apothic_attributes.gui.show_attributes")
        ) {
            @Override
            public void setFocused(boolean focused) {
            }
        };

        positionAttributesButton(button, parent);
        return button;
    }

    public static void positionAttributesButton(AbstractWidget widget, AbstractContainerScreen<?> parent) {
        if (!(widget instanceof ImageButton button) || parent == null || !isLoaded()) {
            return;
        }

        try {
            Class<?> configClass = Class.forName(AL_CONFIG_CLASS);
            Field offsetField = configClass.getField("attributesGuiButtonOffset");
            Object offset = offsetField.get(null);
            if (offset == null) {
                return;
            }

            Class<?> placementClass = Class.forName(BUTTON_PLACEMENT_CLASS);

            for (Method method : placementClass.getMethods()) {
                if (!method.getName().equals("positionGuiButton")) {
                    continue;
                }

                Class<?>[] params = method.getParameterTypes();
                if (params.length != 4) {
                    continue;
                }

                if (params[0] != ImageButton.class) {
                    continue;
                }

                if (params[2] != int.class || params[3] != int.class) {
                    continue;
                }

                if (!params[1].isInstance(offset)) {
                    continue;
                }

                method.invoke(null, button, offset, parent.getGuiLeft(), parent.getGuiTop());
                return;
            }
        } catch (Throwable ignored) {
        }
    }

    private static WidgetSprites getSwordButtonSprites() {
        try {
            Class<?> attributesGuiClass = Class.forName(ATTRIBUTES_GUI_CLASS);
            Field field = attributesGuiClass.getField("SWORD_BUTTON_SPRITES");
            Object value = field.get(null);
            return value instanceof WidgetSprites sprites ? sprites : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void openVanillaInventoryForAttributes() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }

        Player player = mc.player;
        if (player == null) {
            return;
        }

        setSwappedFromCurios(true);
        mc.setScreen(new InventoryScreen(player));
    }

    private static void setSwappedFromCurios(boolean value) {
        try {
            Class<?> attributesGuiClass = Class.forName(ATTRIBUTES_GUI_CLASS);
            Field field = attributesGuiClass.getDeclaredField("swappedFromCurios");
            field.setAccessible(true);
            field.setBoolean(null, value);
        } catch (Throwable ignored) {
        }
    }
}