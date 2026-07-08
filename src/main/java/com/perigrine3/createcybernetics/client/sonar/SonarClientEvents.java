package com.perigrine3.createcybernetics.client.sonar;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.ModItems;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.List;

@EventBusSubscriber(modid = CreateCybernetics.MODID, value = Dist.CLIENT)
public final class SonarClientEvents {
    private static final ResourceLocation SONAR_POST =
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "shaders/post/sonar.json");

    private static final float RADIUS_BLOCKS = 10.0F;
    private static final float LIFE_SECONDS = 10.0F;
    private static final float WAVE_SPEED = 2.0F;
    private static final float WAVE_THICK = 2.5F;
    private static final float WAVE_FEATHER = 1.5F;
    private static final float WAVE_MAX_R = 60.0F;
    private static final float SKY_EPS = 0.0005F;

    private static Field postChainField;

    private SonarClientEvents() {
    }

    @SubscribeEvent
    public static void onPlaySoundEvent(PlaySoundEvent event) {
        if (event.getSound() == null) {
            return;
        }

        pushSound(
                event.getSound().getX(),
                event.getSound().getY(),
                event.getSound().getZ(),
                event.getSound().isRelative()
        );
    }

    @SubscribeEvent
    public static void onPlaySoundSourceEvent(PlaySoundSourceEvent event) {
        if (event.getSound() == null) {
            return;
        }

        pushSound(
                event.getSound().getX(),
                event.getSound().getY(),
                event.getSound().getZ(),
                event.getSound().isRelative()
        );
    }

    @SubscribeEvent
    public static void onPlayStreamingSourceEvent(PlayStreamingSourceEvent event) {
        if (event.getSound() == null) {
            return;
        }

        pushSound(
                event.getSound().getX(),
                event.getSound().getY(),
                event.getSound().getZ(),
                event.getSound().isRelative()
        );
    }

    private static void pushSound(double x, double y, double z, boolean isRelative) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            return;
        }

        if (!isSonarActive(mc)) {
            return;
        }

        Vec3 pos = isRelative ? mc.player.position() : new Vec3(x, y, z);

        if (!isValidPosition(pos)) {
            return;
        }

        if (pos.distanceToSqr(mc.player.position()) > RADIUS_BLOCKS * RADIUS_BLOCKS) {
            return;
        }

        SonarPingManager.push(pos);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            mc.gameRenderer.shutdownEffect();
            SonarPingManager.clear();
            return;
        }

        if (!isSonarActive(mc)) {
            mc.gameRenderer.shutdownEffect();
            SonarPingManager.clear();
            return;
        }

        if (getPostChain(mc.gameRenderer) == null) {
            mc.gameRenderer.loadEffect(SONAR_POST);
        }

        PostChain chain = getPostChain(mc.gameRenderer);
        if (chain == null) {
            return;
        }

        SonarPingManager.prune(LIFE_SECONDS);

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        float aspect = height == 0 ? 1.0F : (float) width / (float) height;

        double fovDegrees = mc.options.fov().get();
        float tanHalfFov = (float) Math.tan(Math.toRadians(fovDegrees * 0.5D));

        chain.setUniform("Aspect", aspect);
        chain.setUniform("TanHalfFov", tanHalfFov);

        chain.setUniform("EdgeThreshold", 0.10F);
        chain.setUniform("EdgeBoost", 1.25F);

        chain.setUniform("SoundLifeSeconds", LIFE_SECONDS);

        chain.setUniform("WaveSpeed", WAVE_SPEED);
        chain.setUniform("WaveThickness", WAVE_THICK);
        chain.setUniform("WaveFeather", WAVE_FEATHER);
        chain.setUniform("WaveMaxRadius", WAVE_MAX_R);
        chain.setUniform("SkyDepthEps", SKY_EPS);

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        Quaternionf inverseCameraRotation = new Quaternionf(camera.rotation()).conjugate();

        List<SonarPingManager.Ping> pings = SonarPingManager.snapshotNewestFirst();
        int count = Math.min(SonarPingManager.MAX_PINGS, pings.size());

        chain.setUniform("SoundCount", (float) count);

        long now = Util.getNanos();

        for (int i = 0; i < SonarPingManager.MAX_PINGS; i++) {
            float vx = 0.0F;
            float vy = 0.0F;
            float vz = 0.0F;
            float ageSeconds = 999.0F;

            if (i < count) {
                SonarPingManager.Ping ping = pings.get(i);

                if (ping != null && isValidPosition(ping.worldPos)) {
                    Vec3 delta = ping.worldPos.subtract(cameraPos);
                    Vector3f viewPos = new Vector3f((float) delta.x, (float) delta.y, (float) delta.z);
                    inverseCameraRotation.transform(viewPos);

                    if (viewPos.isFinite() && viewPos.length() <= 256.0F) {
                        vx = viewPos.x;
                        vy = viewPos.y;
                        vz = viewPos.z;
                        ageSeconds = (now - ping.timeNanos) / 1_000_000_000.0F;
                    }
                }
            }

            chain.setUniform("SoundPosX" + i, vx);
            chain.setUniform("SoundPosY" + i, vy);
            chain.setUniform("SoundPosZ" + i, vz);
            chain.setUniform("SoundAge" + i, ageSeconds);
        }
    }

    private static boolean isSonarActive(Minecraft mc) {
        if (mc.player == null) {
            return false;
        }

        PlayerCyberwareData data = mc.player.getData(ModAttachments.CYBERWARE);
        return data.hasSpecificItem(ModItems.WETWARE_WARDENANTLERS.get(), CyberwareSlot.EYES);
    }

    private static boolean isValidPosition(Vec3 pos) {
        return pos != null
                && Double.isFinite(pos.x)
                && Double.isFinite(pos.y)
                && Double.isFinite(pos.z);
    }

    private static PostChain getPostChain(GameRenderer renderer) {
        try {
            if (postChainField == null) {
                postChainField = findPostChainField(renderer);
            }

            if (postChainField == null) {
                return null;
            }

            Object value = postChainField.get(renderer);
            return value instanceof PostChain postChain ? postChain : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Field findPostChainField(GameRenderer renderer) {
        Class<?> type = renderer.getClass();

        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (PostChain.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    return field;
                }
            }

            type = type.getSuperclass();
        }

        return null;
    }
}