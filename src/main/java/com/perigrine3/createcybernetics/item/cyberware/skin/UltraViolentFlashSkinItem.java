package com.perigrine3.createcybernetics.item.cyberware.skin;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.compat.vampirism.UltraViolentFlashLightManager;
import com.perigrine3.createcybernetics.compat.vampirism.VampirismCompat;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class UltraViolentFlashSkinItem extends Item implements ICyberwareItem {
    private static final int CHARGE_TIME_TICKS = 60;
    private static final int COOLDOWN_TICKS = 1200;
    private static final int LIGHT_DURATION_TICKS = 40;

    private static final int ACTIVATION_ENERGY_COST = 10000;

    private static final int EFFECT_RADIUS = 3;
    private static final int FIRE_DURATION_SECONDS = 10;
    private static final int BLINDNESS_DURATION_TICKS = 200;

    private static final String CHARGE_TICKS = "cc_ultraviolet_flash_charge_ticks";
    private static final String COOLDOWN_END_TICK = "cc_ultraviolet_flash_cooldown_end";

    private static final TagKey<EntityType<?>> UV_VULNERABLE_TAG = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "uv_vulnerable")
    );

    private final int humanityCost;

    public UltraViolentFlashSkinItem(Properties props, int humanityCost) {
        super(props);
        this.humanityCost = humanityCost;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.createcybernetics.humanity", humanityCost).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.createcybernetics.skinupgrades_ultraviolent.energy").withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("tooltip.createcybernetics.skinupgrades_ultraviolent.activation").withStyle(ChatFormatting.YELLOW));
        }
    }

    @Override
    public Map<Item, Integer> getAdditionalAnvilRepairMaterials(ItemStack cyberwareStack) {
        return Map.of(
                Items.TINTED_GLASS, 750,
                Items.PEARLESCENT_FROGLIGHT, 500
        );
    }

    @Override
    public int getHumanityCost() {
        return humanityCost;
    }

    @Override
    public boolean isToggleableByWheel(ItemStack installedStack, CyberwareSlot slot) {
        return true;
    }

    @Override
    public Set<TagKey<Item>> requiresCyberwareTags(ItemStack installedStack, CyberwareSlot slot) {
        return Set.of(ModTags.Items.SKIN_ITEMS);
    }

    @Override
    public Set<CyberwareSlot> getSupportedSlots() {
        return Set.of(CyberwareSlot.SKIN);
    }

    @Override
    public boolean replacesOrgan() {
        return false;
    }

    @Override
    public Set<CyberwareSlot> getReplacedOrgans() {
        return Set.of();
    }

    private int findEnabledIndex(PlayerCyberwareData data, CyberwareSlot slot) {
        InstalledCyberware[] arr = data.getAll().get(slot);
        if (arr == null) return -1;

        for (int i = 0; i < arr.length; i++) {
            InstalledCyberware inst = arr[i];
            if (inst == null) continue;

            ItemStack st = inst.getItem();
            if (st == null || st.isEmpty()) continue;
            if (st.getItem() != this) continue;

            return data.isEnabled(slot, i) ? i : -1;
        }

        return -1;
    }

    private boolean isEnabled(PlayerCyberwareData data, CyberwareSlot slot) {
        return findEnabledIndex(data, slot) >= 0;
    }

    private boolean isOnCooldown(Player player) {
        long cooldownEnd = player.getPersistentData().getLong(COOLDOWN_END_TICK);
        return player.level().getGameTime() < cooldownEnd;
    }

    @Override
    public int getEnergyUsedPerTick(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot) {
        return 0;
    }

    @Override
    public int getEnergyActivationCost(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot) {
        return 0;
    }

    @Override
    public boolean shouldConsumeActivationEnergyThisTick(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot) {
        return false;
    }

    @Override
    public void onTick(LivingEntity entity, ItemStack installedStack, CyberwareSlot slot, int index) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (slot != CyberwareSlot.SKIN) return;
        if (!player.hasData(ModAttachments.CYBERWARE)) return;

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);

        if (!isEnabled(data, slot)) {
            resetCharge(player);
            return;
        }

        if (isOnCooldown(player)) {
            resetCharge(player);
            return;
        }

        if (!player.isCrouching()) {
            resetCharge(player);
            return;
        }

        int chargeTicks = player.getPersistentData().getInt(CHARGE_TICKS) + 1;
        player.getPersistentData().putInt(CHARGE_TICKS, chargeTicks);

        sendChargeMessage(player, chargeTicks);
        spawnChargeParticles(player, chargeTicks);

        if (chargeTicks < CHARGE_TIME_TICKS) return;

        resetCharge(player);

        if (!data.tryConsumeEnergy(ACTIVATION_ENERGY_COST)) {
            player.displayClientMessage(Component.translatable("message.createcybernetics.ultraviolet_flash.insufficient_energy").withStyle(ChatFormatting.RED), true);
            return;
        }

        activateFlash(player);

        player.getPersistentData().putLong(COOLDOWN_END_TICK, player.level().getGameTime() + COOLDOWN_TICKS);
    }

    private void activateFlash(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        UltraViolentFlashLightManager.createFlash(level, player.blockPosition(), LIGHT_DURATION_TICKS);

        AABB affectedArea = new AABB(player.getX() - EFFECT_RADIUS, player.getY() - EFFECT_RADIUS, player.getZ() - EFFECT_RADIUS,
                player.getX() + EFFECT_RADIUS + 1.0D, player.getY() + EFFECT_RADIUS + 1.0D, player.getZ() + EFFECT_RADIUS + 1.0D);

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, affectedArea, LivingEntity::isAlive);

        for (LivingEntity target : targets) {
            if (target instanceof Player targetPlayer && targetPlayer != player) {
                targetPlayer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION_TICKS, 0, false, true, true));
            }

            if (isUvVulnerable(target)) {
                target.igniteForSeconds(FIRE_DURATION_SECONDS);
            }
        }

        level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.0D, player.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0D, player.getZ(), 250, 3.5D, 3.5D, 3.5D, 0.3D);
        level.sendParticles(ParticleTypes.FIREWORK, player.getX(), player.getY() + 1.0D, player.getZ(), 150, 3.5D, 3.5D, 3.5D, 0.2D);

        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 4.0F, 1.75F);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0F, 1.5F);

        player.displayClientMessage(Component.translatable("message.createcybernetics.ultraviolet_flash.activated").withStyle(ChatFormatting.YELLOW), true);
    }

    private boolean isUvVulnerable(LivingEntity entity) {
        return entity.getType().is(EntityTypeTags.UNDEAD)
                || entity.getType().is(UV_VULNERABLE_TAG)
                || VampirismCompat.isVampire(entity);
    }

    private void sendChargeMessage(ServerPlayer player, int chargeTicks) {
        if (chargeTicks % 5 != 0 && chargeTicks != 1) return;

        int percentage = Math.min(100, Math.round(chargeTicks / (float) CHARGE_TIME_TICKS * 100.0F));

        player.displayClientMessage(Component.translatable("message.createcybernetics.ultraviolet_flash.charging", percentage).withStyle(ChatFormatting.YELLOW), true);
    }

    private void spawnChargeParticles(ServerPlayer player, int chargeTicks) {
        if (chargeTicks % 3 != 0) return;

        ServerLevel level = player.serverLevel();

        double progress = chargeTicks / (double) CHARGE_TIME_TICKS;
        int particleCount = 2 + (int) (progress * 8.0D);

        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0D, player.getZ(), particleCount, 0.4D + progress, 0.8D + progress, 0.4D + progress, 0.02D);
    }

    private void resetCharge(Player player) {
        player.getPersistentData().remove(CHARGE_TICKS);
    }

    @Override
    public int maxStacksPerSlotType(ItemStack stack, CyberwareSlot slotType) {
        return 1;
    }

    @Override
    public void onInstalled(LivingEntity entity) {
    }

    @Override
    public void onRemoved(LivingEntity entity) {
        if (!(entity instanceof Player player)) return;

        resetCharge(player);
        player.getPersistentData().remove(COOLDOWN_END_TICK);
    }

    @Override
    public void onTick(LivingEntity entity) {
    }
}