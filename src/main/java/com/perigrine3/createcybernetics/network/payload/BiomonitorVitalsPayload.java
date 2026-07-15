package com.perigrine3.createcybernetics.network.payload;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.client.biomonitor.BiomonitorClientData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record BiomonitorVitalsPayload(
        int targetEntityId,
        float health,
        float maxHealth,
        int armor,
        boolean hasHungerData,
        int foodLevel,
        float saturationLevel,
        List<EffectData> effects
) implements CustomPacketPayload {

    public static final Type<BiomonitorVitalsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CreateCybernetics.MODID,
                    "biomonitor_vitals"
            ));

    public static final StreamCodec<ByteBuf, BiomonitorVitalsPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public BiomonitorVitalsPayload decode(ByteBuf buffer) {
                    int targetEntityId = buffer.readInt();
                    float health = buffer.readFloat();
                    float maxHealth = buffer.readFloat();
                    int armor = buffer.readInt();

                    boolean hasHungerData = buffer.readBoolean();
                    int foodLevel = buffer.readInt();
                    float saturationLevel = buffer.readFloat();

                    int effectCount = Math.max(0, Math.min(buffer.readInt(), 64));
                    List<EffectData> effects = new ArrayList<>(effectCount);

                    for (int index = 0; index < effectCount; index++) {
                        String displayName = ByteBufCodecs.STRING_UTF8.decode(buffer);
                        int amplifier = buffer.readInt();
                        int duration = buffer.readInt();
                        boolean infiniteDuration = buffer.readBoolean();

                        effects.add(new EffectData(
                                displayName,
                                amplifier,
                                duration,
                                infiniteDuration
                        ));
                    }

                    return new BiomonitorVitalsPayload(
                            targetEntityId,
                            health,
                            maxHealth,
                            armor,
                            hasHungerData,
                            foodLevel,
                            saturationLevel,
                            List.copyOf(effects)
                    );
                }

                @Override
                public void encode(ByteBuf buffer, BiomonitorVitalsPayload payload) {
                    buffer.writeInt(payload.targetEntityId());
                    buffer.writeFloat(payload.health());
                    buffer.writeFloat(payload.maxHealth());
                    buffer.writeInt(payload.armor());

                    buffer.writeBoolean(payload.hasHungerData());
                    buffer.writeInt(payload.foodLevel());
                    buffer.writeFloat(payload.saturationLevel());

                    List<EffectData> effects = payload.effects();

                    buffer.writeInt(effects.size());

                    for (EffectData effect : effects) {
                        ByteBufCodecs.STRING_UTF8.encode(
                                buffer,
                                effect.displayName()
                        );

                        buffer.writeInt(effect.amplifier());
                        buffer.writeInt(effect.duration());
                        buffer.writeBoolean(effect.infiniteDuration());
                    }
                }
            };

    public BiomonitorVitalsPayload {
        effects = List.copyOf(effects);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BiomonitorVitalsPayload payload) {
        BiomonitorClientData.accept(payload);
    }

    public record EffectData(
            String displayName,
            int amplifier,
            int duration,
            boolean infiniteDuration
    ) {
    }
}