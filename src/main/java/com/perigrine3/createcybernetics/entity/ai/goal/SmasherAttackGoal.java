package com.perigrine3.createcybernetics.entity.ai.goal;

import com.perigrine3.createcybernetics.entity.custom.SmasherEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class SmasherAttackGoal extends MeleeAttackGoal {
    private final SmasherEntity smasher;

    public SmasherAttackGoal(SmasherEntity smasher, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(smasher, speedModifier, followingTargetEvenIfNotSeen);
        this.smasher = smasher;
    }

    @Override
    public void tick() {
        if (this.smasher.isPerformingAttack()) {
            LivingEntity target = this.smasher.getTarget();

            this.mob.getNavigation().stop();

            if (target != null) {
                this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }

            return;
        }

        super.tick();
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target) {
        double reach = this.getSmasherAttackReachSqr(target);

        if (this.smasher.isPerformingAttack()) {
            return;
        }

        if (this.isTimeToAttack() && this.smasher.distanceToSqr(target) <= reach) {
            this.resetAttackCooldown();
            this.smasher.startAttack(target);
        }
    }

    private double getSmasherAttackReachSqr(LivingEntity target) {
        float reach = this.mob.getBbWidth() * 2.0F;

        return reach * reach + target.getBbWidth();
    }
}