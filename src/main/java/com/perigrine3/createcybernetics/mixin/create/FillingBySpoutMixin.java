package com.perigrine3.createcybernetics.mixin.create;

import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.util.DynamicPotionAutoinjectorRules;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllFluids;
import com.simibubi.create.content.fluids.potion.PotionFluid;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FillingBySpout.class, remap = false)
public abstract class FillingBySpoutMixin {

    @Inject(
            method = "canItemBeFilled",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void createcybernetics$canDynamicPotionAutoinjectorBeFilled(
            Level level,
            ItemStack stack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (stack.is(ModItems.EMPTY_AUTOINJECTOR.get())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "getRequiredAmountForItem",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void createcybernetics$getRequiredDynamicPotionAutoinjectorFluidAmount(
            Level level,
            ItemStack stack,
            FluidStack availableFluid,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (!stack.is(ModItems.EMPTY_AUTOINJECTOR.get())) {
            return;
        }

        /*
         * Only take over for valid dynamic potion autoinjector fluids.
         *
         * If the potion is blocked, do NOT return -1.
         * Blocked potions are supposed to fall through to normal Create filling recipes,
         * such as neuropozyne_potion -> neuropozyne_autoinjector.
         */
        if (!canCreateDynamicPotionAutoinjectorFromFluid(availableFluid)) {
            return;
        }

        cir.setReturnValue(DynamicPotionAutoinjectorRules.REQUIRED_POTION_AMOUNT);
    }

    @Inject(
            method = "fillItem",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void createcybernetics$fillDynamicPotionAutoinjector(
            Level level,
            int requiredAmount,
            ItemStack stack,
            FluidStack availableFluid,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (!stack.is(ModItems.EMPTY_AUTOINJECTOR.get())) {
            return;
        }

        /*
         * Only take over if this is a valid non-blocked dynamic potion.
         *
         * If the potion is blocked, return without cancelling.
         * That lets Create's normal create:filling recipe system handle it.
         */
        if (!canCreateDynamicPotionAutoinjectorFromFluid(availableFluid)) {
            return;
        }

        if (requiredAmount != DynamicPotionAutoinjectorRules.REQUIRED_POTION_AMOUNT) {
            return;
        }

        ItemStack result = DynamicPotionAutoinjectorRules.createAutoinjectorFromPotionFluid(availableFluid);

        if (result.isEmpty()) {
            return;
        }

        availableFluid.shrink(DynamicPotionAutoinjectorRules.REQUIRED_POTION_AMOUNT);
        stack.shrink(1);

        cir.setReturnValue(result);
    }

    private static boolean canCreateDynamicPotionAutoinjectorFromFluid(FluidStack fluidStack) {
        if (!isCreateRegularPotionFluid(fluidStack)) {
            return false;
        }

        return DynamicPotionAutoinjectorRules.isValidDynamicPotionFluid(fluidStack);
    }

    private static boolean isCreateRegularPotionFluid(FluidStack fluidStack) {
        if (fluidStack.isEmpty()) {
            return false;
        }

        if (!fluidStack.getFluid().isSame(AllFluids.POTION.get())) {
            return false;
        }

        PotionFluid.BottleType bottleType = fluidStack.getOrDefault(
                AllDataComponents.POTION_FLUID_BOTTLE_TYPE,
                PotionFluid.BottleType.REGULAR
        );

        return bottleType == PotionFluid.BottleType.REGULAR;
    }
}