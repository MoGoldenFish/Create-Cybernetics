package com.perigrine3.createcybernetics.compat.vampirism;

import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

public final class VampirismCompat {
    private static final String VAMPIRISM_MOD_ID =
            "vampirism";

    private static final String VAMPIRISM_HELPER_CLASS =
            "de.teamlapen.vampirism.util.Helper";

    private static final boolean VAMPIRISM_LOADED =
            ModList.get().isLoaded(VAMPIRISM_MOD_ID);

    private static Method isVampireMethod;
    private static boolean methodResolved;

    private VampirismCompat() {}

    public static boolean isVampire(Entity entity) {
        if (!VAMPIRISM_LOADED) return false;
        if (entity == null) return false;

        Method method = getIsVampireMethod();
        if (method == null) return false;

        try {
            Object result = method.invoke(null, entity);
            return result instanceof Boolean value && value;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static Method getIsVampireMethod() {
        if (methodResolved) {
            return isVampireMethod;
        }

        methodResolved = true;

        try {
            Class<?> helperClass = Class.forName(
                    VAMPIRISM_HELPER_CLASS,
                    false,
                    VampirismCompat.class.getClassLoader()
            );

            isVampireMethod = helperClass.getMethod(
                    "isVampire",
                    Entity.class
            );
        } catch (ReflectiveOperationException | LinkageError ignored) {
            isVampireMethod = null;
        }

        return isVampireMethod;
    }
}