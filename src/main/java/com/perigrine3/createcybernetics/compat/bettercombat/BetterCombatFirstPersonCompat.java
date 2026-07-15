package com.perigrine3.createcybernetics.compat.bettercombat;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class BetterCombatFirstPersonCompat {

    private static final String BETTER_COMBAT_MODID =
            "bettercombat";

    private static final String BETTER_COMBAT_CLIENT_MOD_CLASS =
            "net.bettercombat.client.BetterCombatClientMod";

    private static final String BETTER_COMBAT_CONFIG_FIELD =
            "config";

    private static final String BETTER_COMBAT_SHOW_ARMS_FIELD =
            "isShowingArmsInFirstPerson";

    private static final String PLAYER_ANIMATOR_FIRST_PERSON_MODE_CLASS =
            "dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode";

    private static final String PLAYER_ANIMATOR_IS_FIRST_PERSON_PASS_METHOD =
            "isFirstPersonPass";

    private static final TagKey<Item> FIRST_PERSON_ARM_HIDING_ITEMS =
            TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(
                            CreateCybernetics.MODID,
                            "first_person_arm_hiding_items"
                    )
            );

    private BetterCombatFirstPersonCompat() {}

    public static boolean isBetterCombatLoaded() {
        return ModList.get().isLoaded(BETTER_COMBAT_MODID);
    }

    public static boolean isShowingArmsInFirstPerson() {
        if (!isBetterCombatLoaded()) {
            return true;
        }

        Boolean liveConfigValue = readBetterCombatLiveClientConfig();

        return liveConfigValue == null || liveConfigValue;
    }

    public static boolean shouldHideFirstPersonArms(AbstractClientPlayer player) {
        if (!isBetterCombatLoaded()) {
            return false;
        }

        if (isShowingArmsInFirstPerson()) {
            return false;
        }

        return isHoldingWeaponOrTool(player);
    }

    public static boolean shouldRenderCreateCyberneticsFirstPersonArms(
            AbstractClientPlayer player
    ) {
        return !shouldHideFirstPersonArms(player);
    }

    public static boolean shouldHideFirstPersonPlayerModelArms(
            AbstractClientPlayer player
    ) {
        if (player == null) {
            return false;
        }

        if (!shouldHideFirstPersonArms(player)) {
            return false;
        }

        if (!isPlayerAnimatorFirstPersonPass()) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) {
            return false;
        }

        if (mc.getCameraEntity() != player) {
            return false;
        }

        return player.getUUID().equals(mc.player.getUUID());
    }

    public static boolean isHoldingWeaponOrTool(AbstractClientPlayer player) {
        if (player == null) {
            return false;
        }

        return isWeaponOrTool(player.getMainHandItem())
                || isWeaponOrTool(player.getOffhandItem());
    }

    public static boolean isWeaponOrTool(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        if (stack.is(FIRST_PERSON_ARM_HIDING_ITEMS)) {
            return true;
        }

        if (stack.has(DataComponents.TOOL)) {
            return true;
        }

        if (stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.PICKAXES)
                || stack.is(ItemTags.SHOVELS)
                || stack.is(ItemTags.HOES)) {
            return true;
        }

        Item item = stack.getItem();

        return item instanceof ProjectileWeaponItem
                || item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof TridentItem
                || item instanceof MaceItem
                || item instanceof ShieldItem;
    }

    private static Boolean readBetterCombatLiveClientConfig() {
        try {
            ClassLoader loader =
                    BetterCombatFirstPersonCompat.class.getClassLoader();

            Class<?> clientModClass = Class.forName(
                    BETTER_COMBAT_CLIENT_MOD_CLASS,
                    false,
                    loader
            );

            Field configField = findField(
                    clientModClass,
                    BETTER_COMBAT_CONFIG_FIELD
            );

            if (configField == null) {
                return null;
            }

            configField.setAccessible(true);

            Object config = configField.get(null);

            if (config == null) {
                return null;
            }

            Field showArmsField = findField(
                    config.getClass(),
                    BETTER_COMBAT_SHOW_ARMS_FIELD
            );

            if (showArmsField == null) {
                return null;
            }

            showArmsField.setAccessible(true);

            Object value = showArmsField.get(config);

            return value instanceof Boolean bool
                    ? bool
                    : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isPlayerAnimatorFirstPersonPass() {
        try {
            ClassLoader loader =
                    BetterCombatFirstPersonCompat.class.getClassLoader();

            Class<?> firstPersonModeClass = Class.forName(
                    PLAYER_ANIMATOR_FIRST_PERSON_MODE_CLASS,
                    false,
                    loader
            );

            Method method = firstPersonModeClass.getMethod(
                    PLAYER_ANIMATOR_IS_FIRST_PERSON_PASS_METHOD
            );

            Object result = method.invoke(null);

            return result instanceof Boolean bool && bool;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> cursor = type;

        while (cursor != null) {
            try {
                return cursor.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                cursor = cursor.getSuperclass();
            }
        }

        return null;
    }
}