package com.perigrine3.createcybernetics.compat.neosync;

import net.neoforged.neoforge.common.NeoForge;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

public final class NeoSyncCompat {

    private static final String SHELL_STATE_COMPONENT_FACTORY_REGISTRY_CLASS = "com.breakinblocks.neosync.api.shell.ShellStateComponentFactoryRegistry";
    private static final String NEOSYNC_CYBERWARE_COMPONENT_CLASS = "com.perigrine3.createcybernetics.compat.neosync.NeoSyncCyberwareComponent";

    private static boolean initialized = false;

    private NeoSyncCompat() {}

    public static void initialize() {
        if (initialized) {
            return;
        }

        try {
            Class<?> registryClass = Class.forName(SHELL_STATE_COMPONENT_FACTORY_REGISTRY_CLASS);
            Class<?> componentClass = Class.forName(NEOSYNC_CYBERWARE_COMPONENT_CLASS);

            Method getInstanceMethod = registryClass.getMethod("getInstance");
            Object registry = getInstanceMethod.invoke(null);

            Constructor<?> constructor = componentClass.getConstructor(net.minecraft.server.level.ServerPlayer.class);

            Function<Object, Object> componentFactory = player -> {
                try {
                    return constructor.newInstance(player);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException exception) {
                    throw new IllegalStateException("Failed to create NeoSync cyberware component", exception);
                }
            };

            Method registerMethod = registryClass.getMethod("register", Function.class);
            registerMethod.invoke(registry, componentFactory);

            NeoForge.EVENT_BUS.register(NeoSyncShellPhasingHandler.class);
            initialized = true;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Failed to initialize NeoSync compatibility", exception);
        }
    }
}