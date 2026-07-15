package com.perigrine3.createcybernetics.compat.ironsspells;

import com.perigrine3.createcybernetics.network.payload.IronsManaClientSyncPayload;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.reflect.Method;
import java.util.List;

public final class IronsSpellbooksManaCompat {
    private IronsSpellbooksManaCompat() {}

    public static final String MODID = "irons_spellbooks";
    private static final boolean LOADED = ModList.get().isLoaded(MODID);

    private static final String MAGIC_DATA_CLASS = "io.redspace.ironsspellbooks.api.magic.MagicData";

    private static boolean lookedUp;

    private static Class<?> magicDataClass;
    private static Method getPlayerMagicDataMethod;
    private static Method setServerPlayerMethod;
    private static Method getManaMethod;
    private static Method setManaMethod;

    public static boolean isLoaded() {
        return LOADED;
    }

    private static boolean resolve() {
        if (!LOADED) return false;

        if (lookedUp) {
            return magicDataClass != null
                    && getPlayerMagicDataMethod != null
                    && getManaMethod != null
                    && setManaMethod != null;
        }

        lookedUp = true;

        try {
            magicDataClass = Class.forName(MAGIC_DATA_CLASS);
            getPlayerMagicDataMethod = magicDataClass.getMethod("getPlayerMagicData", LivingEntity.class);
            getManaMethod = magicDataClass.getMethod("getMana");
            setManaMethod = magicDataClass.getMethod("setMana", float.class);

            try {
                setServerPlayerMethod = magicDataClass.getMethod("setServerPlayer", ServerPlayer.class);
            } catch (NoSuchMethodException ignored) {
                setServerPlayerMethod = null;
            }

            return true;
        } catch (Throwable ignored) {
            magicDataClass = null;
            getPlayerMagicDataMethod = null;
            setServerPlayerMethod = null;
            getManaMethod = null;
            setManaMethod = null;
            return false;
        }
    }

    public static float getMana(LivingEntity target) {
        if (target == null) return 0.0F;
        if (!resolve()) return 0.0F;

        try {
            Object data = getMagicData(target);
            if (data == null) return 0.0F;

            return ((Number) getManaMethod.invoke(data)).floatValue();
        } catch (Throwable ignored) {
            return 0.0F;
        }
    }

    public static float getMaxMana(LivingEntity target) {
        if (target == null) return 0.0F;
        if (!IronsSpellbooksCompat.isLoaded()) return 0.0F;

        try {
            var holder = IronsSpellbooksCompat.getAttributeHolder(IronsSpellbooksCompat.ATTR_MAX_MANA);
            if (holder == null) return 0.0F;

            return (float) target.getAttributeValue(holder);
        } catch (Throwable ignored) {
            return 0.0F;
        }
    }

    public static int addMana(LivingEntity target, int amount) {
        if (target == null || amount <= 0) return 0;
        if (!resolve()) return 0;

        try {
            Object data = getMagicData(target);
            if (data == null) return 0;

            float oldMana = ((Number) getManaMethod.invoke(data)).floatValue();
            float maxMana = getMaxMana(target);

            if (maxMana <= 0.0F) return 0;
            if (oldMana >= maxMana) return 0;

            float newMana = Math.min(maxMana, oldMana + amount);
            if (newMana <= oldMana) return 0;

            setManaMethod.invoke(data, newMana);

            int added = Math.max(0, (int) Math.floor(newMana - oldMana));
            if (added > 0) {
                forceSyncManaAndMax(target);
            }

            return added;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public static boolean setMana(LivingEntity target, float mana) {
        if (target == null) return false;
        if (!resolve()) return false;

        try {
            Object data = getMagicData(target);
            if (data == null) return false;

            float maxMana = getMaxMana(target);
            float clamped = maxMana > 0.0F ? Math.min(maxMana, Math.max(0.0F, mana)) : Math.max(0.0F, mana);

            float oldMana = ((Number) getManaMethod.invoke(data)).floatValue();
            if (Float.compare(oldMana, clamped) == 0) return false;

            setManaMethod.invoke(data, clamped);
            forceSyncManaAndMax(target);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean drainMana(LivingEntity target, float amount) {
        if (target == null || amount <= 0.0F) return false;
        if (!resolve()) return false;

        try {
            Object data = getMagicData(target);
            if (data == null) return false;

            float oldMana = ((Number) getManaMethod.invoke(data)).floatValue();
            if (oldMana <= 0.0F) return false;

            float newMana = Math.max(0.0F, oldMana - amount);
            if (Float.compare(oldMana, newMana) == 0) return false;

            setManaMethod.invoke(data, newMana);

            forceSyncManaAndMax(target);

            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void forceSyncManaAndMax(LivingEntity target) {
        if (target == null) return;

        if (target instanceof ServerPlayer player) {
            forceSyncMaxMana(player);
        }

        forceSyncClientMana(target);
    }

    private static void forceSyncClientMana(LivingEntity target) {
        if (!(target instanceof ServerPlayer player)) return;
        if (!resolve()) return;

        try {
            Object data = getMagicData(target);
            if (data == null) return;

            int mana = Math.max(0, (int) ((Number) getManaMethod.invoke(data)).floatValue());
            PacketDistributor.sendToPlayer(player, new IronsManaClientSyncPayload(mana));
        } catch (Throwable ignored) {
        }
    }

    public static void forceSyncMaxMana(ServerPlayer player) {
        if (player == null) return;
        if (!IronsSpellbooksCompat.isLoaded()) return;

        var holder = IronsSpellbooksCompat.getAttributeHolder(IronsSpellbooksCompat.ATTR_MAX_MANA);
        if (holder == null) return;

        AttributeInstance instance = player.getAttribute(holder);
        if (instance == null) return;

        player.connection.send(new ClientboundUpdateAttributesPacket(player.getId(), List.of(instance)));
    }

    private static Object getMagicData(LivingEntity target) throws ReflectiveOperationException {
        Object data = getPlayerMagicDataMethod.invoke(null, target);
        if (data == null) return null;

        if (target instanceof ServerPlayer player && setServerPlayerMethod != null) {
            setServerPlayerMethod.invoke(data, player);
        }

        return data;
    }
}