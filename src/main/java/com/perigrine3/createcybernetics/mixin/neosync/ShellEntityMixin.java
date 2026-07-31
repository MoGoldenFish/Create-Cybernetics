package com.perigrine3.createcybernetics.mixin.neosync;

import com.breakinblocks.neosync.api.shell.ShellState;
import com.breakinblocks.neosync.common.block.entity.ShellEntity;
import com.perigrine3.createcybernetics.api.ICyberwareVisualDataHolder;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.compat.neosync.NeoSyncCyberwareComponent;
import net.minecraft.core.HolderLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ShellEntity.class, remap = false)
public abstract class ShellEntityMixin implements ICyberwareVisualDataHolder {

    @Shadow
    public abstract ShellState getState();

    @Override
    public PlayerCyberwareData createcybernetics$getCyberwareVisualData(HolderLookup.Provider provider) {
        ShellState state = getState();
        if (state == null) {
            return null;
        }

        NeoSyncCyberwareComponent component = state.getComponent().as(NeoSyncCyberwareComponent.class);
        if (component == null) {
            return null;
        }

        return component.createVisualData(provider);
    }
}