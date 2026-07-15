package com.perigrine3.createcybernetics.common.command;

public final class CommandTeleportContext {
    private CommandTeleportContext() {}

    private static final ThreadLocal<Integer> COMMAND_DEPTH =
            ThreadLocal.withInitial(() -> 0);

    public static void beginCommand() {
        COMMAND_DEPTH.set(COMMAND_DEPTH.get() + 1);
    }

    public static void endCommand() {
        int nextDepth = COMMAND_DEPTH.get() - 1;

        if (nextDepth <= 0) {
            COMMAND_DEPTH.remove();
            return;
        }

        COMMAND_DEPTH.set(nextDepth);
    }

    public static boolean isExecutingCommand() {
        return COMMAND_DEPTH.get() > 0;
    }
}