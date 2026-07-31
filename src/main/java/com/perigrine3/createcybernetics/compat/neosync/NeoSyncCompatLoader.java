package com.perigrine3.createcybernetics.compat.neosync;

import com.perigrine3.createcybernetics.compat.ModCompats;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class NeoSyncCompatLoader {

    private static final String NEOSYNC_COMPAT_CLASS = "com.perigrine3.createcybernetics.compat.neosync.NeoSyncCompat";

    private NeoSyncCompatLoader() {}

    public static void initialize() {
        if (!ModCompats.isInstalled("neosync")) {
            return;
        }

        try {
            Class<?> compatClass = Class.forName(NEOSYNC_COMPAT_CLASS);
            Method initializeMethod = compatClass.getMethod("initialize");
            initializeMethod.invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException("Unable to initialize NeoSync compatibility", exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            if (cause instanceof Error error) {
                throw error;
            }

            throw new IllegalStateException("NeoSync compatibility initialization failed", cause);
        }
    }
}