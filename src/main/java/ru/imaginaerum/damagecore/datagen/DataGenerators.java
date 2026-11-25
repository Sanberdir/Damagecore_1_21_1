package ru.imaginaerum.damagecore.datagen;

import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, bus = EventBusSubscriber.Bus.MOD)
public class DataGenerators {
    @SubscribeEvent
    public static void gatherData(final GatherDataEvent event) {
        var generator = event.getGenerator();
        var packOutput = generator.getPackOutput();
        var lookupProvider = event.getLookupProvider();
        var existingFileHelper = event.getExistingFileHelper();
        var blockTags = generator.addProvider(event.includeServer(),
                new ModBlockTagProvider(packOutput, lookupProvider, existingFileHelper));
        generator.addProvider(event.includeClient(),
                new ModBlockStateProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(),
                new ModItemModelProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeServer(),
                new DamageTypeProvider(packOutput, lookupProvider, existingFileHelper));
        generator.addProvider(event.includeServer(), new LootTableProvider(
                packOutput, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(
                        ModBlockLootTableProvider::new, LootContextParamSets.BLOCK)),
                lookupProvider));
        generator.addProvider(event.includeServer(),
                new ModItemTagProvider(packOutput, lookupProvider,
                        blockTags.contentsGetter(), existingFileHelper));
        generator.addProvider(event.includeServer(),
                new ModRecipeProvider(packOutput, lookupProvider));
    }
}