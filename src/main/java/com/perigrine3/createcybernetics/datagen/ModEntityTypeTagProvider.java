package com.perigrine3.createcybernetics.datagen;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.entity.ModEntities;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.tags.EntityTypeTags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModEntityTypeTagProvider extends EntityTypeTagsProvider {
    public ModEntityTypeTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, CreateCybernetics.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(EntityTypeTags.UNDEAD)
                .add(ModEntities.CYBERZOMBIE.get())
                .add(ModEntities.CYBERSKELETON.get());

        tag(EntityTypeTags.SKELETONS)
                .add(ModEntities.CYBERSKELETON.get());

        tag(EntityTypeTags.ZOMBIES)
                .add(ModEntities.CYBERZOMBIE.get());

        tag(EntityTypeTags.ILLAGER)
                .add(ModEntities.RIPPER.get())
                .add(ModEntities.SMASHER.get());

        tag(EntityTypeTags.DEFLECTS_PROJECTILES)
                .add(ModEntities.SMASHER.get());

        tag(EntityTypeTags.FALL_DAMAGE_IMMUNE)
                .add(ModEntities.SMASHER.get());

        tag(EntityTypeTags.RAIDERS)
                .add(ModEntities.SMASHER.get());
    }
}