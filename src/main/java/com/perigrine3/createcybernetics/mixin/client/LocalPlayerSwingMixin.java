package com.perigrine3.createcybernetics.mixin.client;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.cyberware.arm.MantisBladeItem;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerSwingMixin {

    public boolean swinging;

    public int swingTime;

    @Unique
    private static final Map<UUID, Boolean> createcybernetics$NEXT_MANTIS_SWING_OFFHAND = new HashMap<>();

    @Unique
    private static final Map<UUID, Integer> createcybernetics$LAST_MANTIS_SWING_TICK = new HashMap<>();

    @Unique
    private static final Map<UUID, InteractionHand> createcybernetics$MANTIS_SWING_HAND_THIS_TICK = new HashMap<>();

    @Unique
    private boolean createcybernetics$redirectingMantisSwing;

    @Inject(
            method = "swing(Lnet/minecraft/world/InteractionHand;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void createcybernetics$alternateMantisBladeLocalSwing(
            InteractionHand hand,
            CallbackInfo ci
    ) {
        if (this.createcybernetics$redirectingMantisSwing) {
            return;
        }

        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }

        LocalPlayer player = (LocalPlayer) (Object) this;

        if (!createcybernetics$hasEnabledMantisBladeInBothArms(player)) {
            UUID id = player.getUUID();
            createcybernetics$NEXT_MANTIS_SWING_OFFHAND.remove(id);
            createcybernetics$LAST_MANTIS_SWING_TICK.remove(id);
            createcybernetics$MANTIS_SWING_HAND_THIS_TICK.remove(id);
            return;
        }

        InteractionHand chosenHand = createcybernetics$getSwingHandForThisAttack(player);

        this.swinging = false;
        this.swingTime = -1;

        ci.cancel();

        this.createcybernetics$redirectingMantisSwing = true;

        try {
            player.swing(chosenHand);
        } finally {
            this.createcybernetics$redirectingMantisSwing = false;
        }
    }

    @Unique
    private static InteractionHand createcybernetics$getSwingHandForThisAttack(LocalPlayer player) {
        UUID id = player.getUUID();
        int tick = player.tickCount;

        int lastTick = createcybernetics$LAST_MANTIS_SWING_TICK.getOrDefault(id, -999999);

        if (lastTick == tick) {
            InteractionHand cached = createcybernetics$MANTIS_SWING_HAND_THIS_TICK.get(id);
            return cached == null ? InteractionHand.MAIN_HAND : cached;
        }

        boolean useOffhand = createcybernetics$NEXT_MANTIS_SWING_OFFHAND.getOrDefault(id, false);
        InteractionHand chosenHand = useOffhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;

        createcybernetics$NEXT_MANTIS_SWING_OFFHAND.put(id, !useOffhand);
        createcybernetics$LAST_MANTIS_SWING_TICK.put(id, tick);
        createcybernetics$MANTIS_SWING_HAND_THIS_TICK.put(id, chosenHand);

        return chosenHand;
    }

    @Unique
    private static boolean createcybernetics$hasEnabledMantisBladeInBothArms(LocalPlayer player) {
        if (player == null) {
            return false;
        }

        if (!player.hasData(ModAttachments.CYBERWARE)) {
            return false;
        }

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);

        if (data == null) {
            return false;
        }

        return createcybernetics$hasEnabledMantisBladeInSlot(data, CyberwareSlot.LARM)
                && createcybernetics$hasEnabledMantisBladeInSlot(data, CyberwareSlot.RARM);
    }

    @Unique
    private static boolean createcybernetics$hasEnabledMantisBladeInSlot(PlayerCyberwareData data, CyberwareSlot slot) {
        if (data == null || slot == null) {
            return false;
        }

        InstalledCyberware[] arr = data.getAll().get(slot);

        if (arr == null) {
            return false;
        }

        for (int i = 0; i < arr.length; i++) {
            InstalledCyberware installed = arr[i];

            if (installed == null) {
                continue;
            }

            ItemStack stack = installed.getItem();

            if (stack == null || stack.isEmpty()) {
                continue;
            }

            if (!(stack.getItem() instanceof MantisBladeItem)) {
                continue;
            }

            int index = installed.getIndex();

            if (index < 0) {
                index = i;
            }

            if (!data.isEnabled(slot, index)) {
                continue;
            }

            return true;
        }

        return false;
    }
}