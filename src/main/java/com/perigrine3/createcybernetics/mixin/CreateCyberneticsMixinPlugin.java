package com.perigrine3.createcybernetics.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class CreateCyberneticsMixinPlugin implements IMixinConfigPlugin {
    private static final String CREATE_FILLING_BY_SPOUT_CLASS =
            "com.simibubi.create.content.fluids.spout.FillingBySpout";

    private static final String CREATE_FILLING_BY_SPOUT_MIXIN =
            "com.perigrine3.createcybernetics.mixin.compat.create.FillingBySpoutMixin";

    private Boolean createFillingBySpoutPresent;

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (CREATE_FILLING_BY_SPOUT_MIXIN.equals(mixinClassName)) {
            return isCreateFillingBySpoutPresent();
        }

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    private boolean isCreateFillingBySpoutPresent() {
        if (createFillingBySpoutPresent != null) {
            return createFillingBySpoutPresent;
        }

        createFillingBySpoutPresent = isClassPresent(CREATE_FILLING_BY_SPOUT_CLASS);
        return createFillingBySpoutPresent;
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, CreateCyberneticsMixinPlugin.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}