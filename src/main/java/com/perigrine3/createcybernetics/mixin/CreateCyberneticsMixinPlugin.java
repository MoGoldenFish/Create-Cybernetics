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

    private static final String FANCY_MENU_PLAYER_ENTITY_MODEL_CLASS =
            "de.keksuccino.fancymenu.customization.element.elements.playerentity.v1.model.PlayerEntityModel";
    private static final String FANCY_MENU_PLAYER_ENTITY_MODEL_MIXIN =
            "com.perigrine3.createcybernetics.mixin.compat.fancymenu.FancyMenuPlayerEntityModelMixin";

    private static final String NEOSYNC_SHELL_STATE_CLASS =
            "com.breakinblocks.neosync.api.shell.ShellState";
    private static final String NEOSYNC_SHELL_STATE_CONTAINER_CLASS =
            "com.breakinblocks.neosync.api.shell.ShellStateContainer";
    private static final String NEOSYNC_SHELL_ENTITY_CLASS =
            "com.breakinblocks.neosync.common.block.entity.ShellEntity";
    private static final String NEOSYNC_SHELL_STATE_MIXIN =
            "com.perigrine3.createcybernetics.mixin.compat.neosync.ShellStateMixin";
    private static final String NEOSYNC_SHELL_ENTITY_MIXIN =
            "com.perigrine3.createcybernetics.mixin.compat.neosync.ShellEntityMixin";
    private static final String NEOSYNC_SHELL_ENTITY_RENDERER_CLASS =
            "com.breakinblocks.neosync.client.render.entity.ShellEntityRenderer";
    private static final String NEOSYNC_SHELL_ENTITY_RENDERER_MIXIN =
            "com.perigrine3.createcybernetics.mixin.compat.neosync.ShellEntityRendererMixin";
    private static final String NEOSYNC_SHELL_CONTAINER_COLLISION_MIXIN =
            "com.perigrine3.createcybernetics.mixin.neosync.NeoSyncShellContainerCollisionMixin";

    private Boolean createFillingBySpoutPresent;
    private Boolean fancyMenuPlayerEntityModelPresent;
    private Boolean neoSyncShellStatePresent;
    private Boolean neoSyncShellStateContainerPresent;
    private Boolean neoSyncShellEntityPresent;
    private Boolean neoSyncShellEntityRendererPresent;

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

        if (FANCY_MENU_PLAYER_ENTITY_MODEL_MIXIN.equals(mixinClassName)) {
            return isFancyMenuPlayerEntityModelPresent();
        }

        if (NEOSYNC_SHELL_STATE_MIXIN.equals(mixinClassName)) {
            return isNeoSyncShellStatePresent();
        }

        if (NEOSYNC_SHELL_ENTITY_MIXIN.equals(mixinClassName)) {
            return isNeoSyncShellEntityPresent();
        }

        if (NEOSYNC_SHELL_ENTITY_RENDERER_MIXIN.equals(mixinClassName)) {
            return isNeoSyncShellEntityRendererPresent();
        }

        if (NEOSYNC_SHELL_CONTAINER_COLLISION_MIXIN.equals(mixinClassName)) {
            return isNeoSyncShellStateContainerPresent();
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

    private boolean isFancyMenuPlayerEntityModelPresent() {
        if (fancyMenuPlayerEntityModelPresent != null) {
            return fancyMenuPlayerEntityModelPresent;
        }

        fancyMenuPlayerEntityModelPresent = isClassPresent(FANCY_MENU_PLAYER_ENTITY_MODEL_CLASS);
        return fancyMenuPlayerEntityModelPresent;
    }

    private boolean isNeoSyncShellStatePresent() {
        if (neoSyncShellStatePresent != null) {
            return neoSyncShellStatePresent;
        }

        neoSyncShellStatePresent = isClassPresent(NEOSYNC_SHELL_STATE_CLASS);
        return neoSyncShellStatePresent;
    }

    private boolean isNeoSyncShellStateContainerPresent() {
        if (neoSyncShellStateContainerPresent != null) {
            return neoSyncShellStateContainerPresent;
        }

        neoSyncShellStateContainerPresent = isClassPresent(NEOSYNC_SHELL_STATE_CONTAINER_CLASS);
        return neoSyncShellStateContainerPresent;
    }

    private boolean isNeoSyncShellEntityPresent() {
        if (neoSyncShellEntityPresent != null) {
            return neoSyncShellEntityPresent;
        }

        neoSyncShellEntityPresent = isClassPresent(NEOSYNC_SHELL_ENTITY_CLASS);
        return neoSyncShellEntityPresent;
    }

    private boolean isNeoSyncShellEntityRendererPresent() {
        if (neoSyncShellEntityRendererPresent != null) {
            return neoSyncShellEntityRendererPresent;
        }

        neoSyncShellEntityRendererPresent = isClassPresent(NEOSYNC_SHELL_ENTITY_RENDERER_CLASS);
        return neoSyncShellEntityRendererPresent;
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