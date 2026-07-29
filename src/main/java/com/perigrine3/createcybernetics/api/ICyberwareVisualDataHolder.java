package com.perigrine3.createcybernetics.api;

import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import net.minecraft.core.HolderLookup;

public interface ICyberwareVisualDataHolder {

    PlayerCyberwareData createcybernetics$getCyberwareVisualData(HolderLookup.Provider provider);
}