package com.perigrine3.createcybernetics.client.biomonitor;

import com.perigrine3.createcybernetics.network.payload.BiomonitorVitalsPayload;
import net.minecraft.client.Minecraft;

public final class BiomonitorClientData {
    private BiomonitorClientData() {
    }

    /*
     * The server refreshes this every 10 client ticks while a target is held.
     * Cache expiry prevents old data appearing over a target after packet loss,
     * target changes, dimension changes, or world unloads.
     */
    private static final long SNAPSHOT_TIMEOUT_TICKS = 30L;

    private static BiomonitorVitalsPayload latestSnapshot;
    private static long receivedGameTime = Long.MIN_VALUE;

    public static void accept(BiomonitorVitalsPayload payload) {
        latestSnapshot = payload;

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level != null) {
            receivedGameTime = minecraft.level.getGameTime();
        }
    }

    public static BiomonitorVitalsPayload getForTarget(int entityId) {
        if (latestSnapshot == null) {
            return null;
        }

        if (latestSnapshot.targetEntityId() != entityId) {
            return null;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return null;
        }

        long currentGameTime = minecraft.level.getGameTime();

        if (currentGameTime - receivedGameTime > SNAPSHOT_TIMEOUT_TICKS) {
            return null;
        }

        return latestSnapshot;
    }

    public static void clear() {
        latestSnapshot = null;
        receivedGameTime = Long.MIN_VALUE;
    }
}