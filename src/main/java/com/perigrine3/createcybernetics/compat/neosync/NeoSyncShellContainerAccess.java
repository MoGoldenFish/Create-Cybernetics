package com.perigrine3.createcybernetics.compat.neosync;

import com.perigrine3.createcybernetics.compat.ModCompats;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class NeoSyncShellContainerAccess {

    private static final String SHELL_STATE_CONTAINER_CLASS = "com.breakinblocks.neosync.api.shell.ShellStateContainer";

    private static boolean initialized;
    private static boolean available;
    private static Method findMethod;
    private static Method getShellStateMethod;

    private NeoSyncShellContainerAccess() {}

    public static boolean isEmpty(Level level, BlockPos pos) {
        initialize();

        if (!available) {
            return false;
        }

        Object container = find(level, pos);
        if (container == null) {
            return false;
        }

        try {
            return getShellStateMethod.invoke(container) == null;
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return false;
        }
    }

    public static BlockPos findNearbyEmpty(Level level, BlockPos pos) {
        if (isEmpty(level, pos)) {
            return pos;
        }

        BlockPos below = pos.below();
        if (isEmpty(level, below)) {
            return below;
        }

        BlockPos above = pos.above();
        if (isEmpty(level, above)) {
            return above;
        }

        return null;
    }

    private static Object find(Level level, BlockPos pos) {
        try {
            return findMethod.invoke(null, level, pos);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return null;
        }
    }

    private static synchronized void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;

        try {
            Class<?> containerClass = Class.forName(SHELL_STATE_CONTAINER_CLASS, false, NeoSyncShellContainerAccess.class.getClassLoader());
            findMethod = containerClass.getMethod("find", Level.class, BlockPos.class);
            getShellStateMethod = containerClass.getMethod("getShellState");
            available = true;
        } catch (ClassNotFoundException | NoSuchMethodException | LinkageError exception) {
            available = false;
            findMethod = null;
            getShellStateMethod = null;
        }
    }
}