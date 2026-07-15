package com.perigrine3.createcybernetics.compat.ironsspells;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.lang.reflect.Method;

public final class IronsSpellbooksClientManaCompat {
    private IronsSpellbooksClientManaCompat() {}

    private static final String CLIENT_MAGIC_DATA_CLASS = "io.redspace.ironsspellbooks.player.ClientMagicData";

    private static boolean lookedUp;
    private static Method setManaMethod;

    public static void setClientMana(int mana) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        if (!resolve()) return;

        try {
            setManaMethod.invoke(null, Math.max(0, mana));
        } catch (Throwable ignored) {
        }
    }

    private static boolean resolve() {
        if (lookedUp) {
            return setManaMethod != null;
        }

        lookedUp = true;

        try {
            Class<?> cls = Class.forName(CLIENT_MAGIC_DATA_CLASS);
            setManaMethod = cls.getMethod("setMana", int.class);
            return true;
        } catch (Throwable ignored) {
            setManaMethod = null;
            return false;
        }
    }
}