package com.perigrine3.createcybernetics.item.generic;

import com.perigrine3.createcybernetics.api.ISpinalInjectableItem;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public abstract class BaseAutoinjectorItem extends Item implements ISpinalInjectableItem {

    protected static final int DEFAULT_CHARGE_TICKS = 16;
    protected static final int DEFAULT_USE_DURATION = 72000;

    public BaseAutoinjectorItem(Properties properties) {
        super(properties);
    }

    protected Optional<String> getDurationTranslationKey() {
        return Optional.empty();
    }

    protected Optional<String> getDescriptionTranslationKey() {
        return Optional.empty();
    }

    protected int getChargeTicks(ItemStack stack) {
        return DEFAULT_CHARGE_TICKS;
    }

    protected double getAdministerRange(Player player) {
        return player.entityInteractionRange();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        appendDefaultAutoinjectorTooltip(stack, context, tooltipComponents, tooltipFlag);
    }

    protected void appendDefaultAutoinjectorTooltip(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        getDurationTranslationKey()
                .filter(key -> !key.isBlank())
                .ifPresent(key -> tooltipComponents.add(Component.translatable(key).withStyle(ChatFormatting.BLUE)));

        getDescriptionTranslationKey()
                .filter(key -> !key.isBlank())
                .ifPresent(key -> tooltipComponents.add(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE)));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return DEFAULT_USE_DURATION;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (!target.isAlive()) {
            return InteractionResult.PASS;
        }

        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int timeLeft) {
        if (!(living instanceof Player player)) {
            return;
        }

        int used = getUseDuration(stack, living) - timeLeft;
        if (used != getChargeTicks(stack)) {
            return;
        }

        InteractionHand usedHand = player.getUsedItemHand();

        LivingEntity target = player.isShiftKeyDown()
                ? findLookedAtLivingTarget(player)
                : player;

        player.stopUsingItem();

        if (level.isClientSide) {
            return;
        }

        if (target == null) {
            return;
        }

        if (!canInjectTarget(player, target, stack)) {
            return;
        }

        applyInjection(player, target, stack);
        playInjectionSound(level, target);
        player.awardStat(Stats.ITEM_USED.get(this));

        consumeAndReturnEmptyInjector(player, stack, usedHand);
    }

    protected boolean canInjectTarget(Player user, LivingEntity target, ItemStack stack) {
        if (!target.isAlive()) {
            return false;
        }

        if (user != target && user.distanceToSqr(target) > getAdministerRange(user) * getAdministerRange(user)) {
            return false;
        }

        return shouldManualInject(user, target, stack);
    }

    protected boolean shouldManualInject(Player user, LivingEntity target, ItemStack stack) {
        return true;
    }

    protected void applyInjection(Player user, LivingEntity target, ItemStack stack) {
        applyEffectInstances(user, target, getSpinalInjectionEffects(stack));
    }

    protected void playInjectionSound(Level level, LivingEntity target) {
        level.playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                ModSounds.AUTOINJECTOR.get(),
                SoundSource.PLAYERS,
                1F,
                1F
        );
    }

    protected void consumeAndReturnEmptyInjector(Player player, ItemStack stack, InteractionHand usedHand) {
        if (player.getAbilities().instabuild) {
            return;
        }

        stack.shrink(1);

        ItemStack empty = new ItemStack(ModItems.EMPTY_AUTOINJECTOR.get());
        if (stack.isEmpty()) {
            player.setItemInHand(usedHand, empty);
        } else if (!player.getInventory().add(empty)) {
            player.drop(empty, false);
        }
    }

    protected void applyEffectInstances(Player user, LivingEntity target, List<MobEffectInstance> effects) {
        for (MobEffectInstance instance : effects) {
            if (instance == null) {
                continue;
            }

            MobEffect effect = instance.getEffect().value();
            if (effect == null) {
                continue;
            }

            if (effect.isInstantenous()) {
                effect.applyInstantenousEffect(
                        user,
                        user,
                        target,
                        instance.getAmplifier(),
                        1.0D
                );
                continue;
            }

            target.addEffect(new MobEffectInstance(
                    instance.getEffect(),
                    instance.getDuration(),
                    instance.getAmplifier(),
                    instance.isAmbient(),
                    instance.isVisible(),
                    instance.showIcon()
            ));
        }
    }

    @Override
    public void applySpinalInjection(ServerPlayer player, ItemStack stack) {
        applyInjection(player, player, stack);
    }

    private LivingEntity findLookedAtLivingTarget(Player player) {
        Level level = player.level();

        double range = getAdministerRange(player);
        Vec3 eyePosition = player.getEyePosition(1.0F);
        Vec3 lookVector = player.getViewVector(1.0F);
        Vec3 endPosition = eyePosition.add(lookVector.scale(range));

        AABB searchBox = player.getBoundingBox()
                .expandTowards(lookVector.scale(range))
                .inflate(1.0D);

        LivingEntity closestTarget = null;
        double closestDistance = range * range;

        for (Entity entity : level.getEntities(player, searchBox, this::isValidLookedAtTarget)) {
            AABB hitbox = entity.getBoundingBox().inflate(entity.getPickRadius());
            Optional<Vec3> hit = hitbox.clip(eyePosition, endPosition);

            double distanceToHit;
            if (hitbox.contains(eyePosition)) {
                distanceToHit = 0.0D;
            } else if (hit.isPresent()) {
                distanceToHit = eyePosition.distanceToSqr(hit.get());
            } else {
                continue;
            }

            if (distanceToHit < closestDistance) {
                closestDistance = distanceToHit;
                closestTarget = (LivingEntity) entity;
            }
        }

        return closestTarget;
    }

    private boolean isValidLookedAtTarget(Entity entity) {
        return entity instanceof LivingEntity living
                && living.isAlive()
                && entity.isPickable()
                && !entity.isSpectator();
    }
}