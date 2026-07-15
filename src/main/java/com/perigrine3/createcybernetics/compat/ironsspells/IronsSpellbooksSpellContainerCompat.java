package com.perigrine3.createcybernetics.compat.ironsspells;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class IronsSpellbooksSpellContainerCompat {
    private IronsSpellbooksSpellContainerCompat() {}

    private static final String COMPONENT_REGISTRY_CLASS = "io.redspace.ironsspellbooks.registries.ComponentRegistry";
    private static final String SPELL_CONTAINER_CLASS = "io.redspace.ironsspellbooks.capabilities.magic.SpellContainer";

    private static boolean spellContainerLookedUp;
    private static boolean castingImplementLookedUp;

    private static DataComponentType<?> spellContainerComponent;
    private static DataComponentType<?> castingImplementComponent;

    private static Constructor<?> spellContainerConstructor;

    public static Item.Properties withSpellContainer(
            Item.Properties properties,
            int maxSpells,
            boolean addsToSpellWheel,
            boolean mustBeEquipped
    ) {
        if (properties == null) {
            properties = new Item.Properties();
        }

        if (!IronsSpellbooksCompat.isLoaded()) {
            return properties;
        }

        if (!resolveSpellContainer()) {
            return properties;
        }

        try {
            Object container = spellContainerConstructor.newInstance(maxSpells, addsToSpellWheel, mustBeEquipped);

            @SuppressWarnings({"rawtypes", "unchecked"})
            DataComponentType rawType = spellContainerComponent;

            properties.component(rawType, container);
        } catch (Throwable ignored) {
        }

        return properties;
    }

    public static Item.Properties withRightClickCasting(Item.Properties properties) {
        if (properties == null) {
            properties = new Item.Properties();
        }

        if (!IronsSpellbooksCompat.isLoaded()) {
            return properties;
        }

        if (!resolveCastingImplement()) {
            return properties;
        }

        try {
            @SuppressWarnings({"rawtypes", "unchecked"})
            DataComponentType rawType = castingImplementComponent;

            properties.component(rawType, Unit.INSTANCE);
        } catch (Throwable ignored) {
        }

        return properties;
    }

    public static Item.Properties withSpellContainerAndRightClickCasting(
            Item.Properties properties,
            int maxSpells,
            boolean addsToSpellWheel,
            boolean mustBeEquipped
    ) {
        properties = withSpellContainer(properties, maxSpells, addsToSpellWheel, mustBeEquipped);
        properties = withRightClickCasting(properties);
        return properties;
    }

    private static boolean resolveSpellContainer() {
        if (spellContainerLookedUp) {
            return spellContainerComponent != null && spellContainerConstructor != null;
        }

        spellContainerLookedUp = true;

        try {
            Class<?> componentRegistryClass = Class.forName(COMPONENT_REGISTRY_CLASS);
            Field spellContainerField = componentRegistryClass.getField("SPELL_CONTAINER");

            Object fieldValue = spellContainerField.get(null);
            Object componentValue = unwrapSupplierLike(fieldValue);

            if (!(componentValue instanceof DataComponentType<?> componentType)) {
                spellContainerComponent = null;
                spellContainerConstructor = null;
                return false;
            }

            Class<?> spellContainerClass = Class.forName(SPELL_CONTAINER_CLASS);
            spellContainerConstructor = spellContainerClass.getConstructor(
                    int.class,
                    boolean.class,
                    boolean.class
            );

            spellContainerComponent = componentType;
            return true;
        } catch (Throwable ignored) {
            spellContainerComponent = null;
            spellContainerConstructor = null;
            return false;
        }
    }

    private static boolean resolveCastingImplement() {
        if (castingImplementLookedUp) {
            return castingImplementComponent != null;
        }

        castingImplementLookedUp = true;

        try {
            Class<?> componentRegistryClass = Class.forName(COMPONENT_REGISTRY_CLASS);
            Field castingImplementField = componentRegistryClass.getField("CASTING_IMPLEMENT");

            Object fieldValue = castingImplementField.get(null);
            Object componentValue = unwrapSupplierLike(fieldValue);

            if (!(componentValue instanceof DataComponentType<?> componentType)) {
                castingImplementComponent = null;
                return false;
            }

            castingImplementComponent = componentType;
            return true;
        } catch (Throwable ignored) {
            castingImplementComponent = null;
            return false;
        }
    }

    private static Object unwrapSupplierLike(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof DataComponentType<?>) {
            return value;
        }

        try {
            Method getMethod = value.getClass().getMethod("get");
            return getMethod.invoke(value);
        } catch (Throwable ignored) {
            return value;
        }
    }
}