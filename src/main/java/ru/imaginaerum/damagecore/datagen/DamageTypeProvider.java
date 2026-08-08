package ru.imaginaerum.damagecore.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;


import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DamageTypeProvider extends DatapackBuiltinEntriesProvider {
    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.DAMAGE_TYPE, DamageTypesGenerator::bootstrap);

    public DamageTypeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper helper) {
        super(output, registries, BUILDER, Set.of(Damagecore_1_21_1_neo.MODID));
    }
}