package ru.imaginaerum.damagecore;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent; // Импортируем тик сервера

import ru.imaginaerum.damagecore.Init.entity.DCEntities.custom.DCEntities;
import ru.imaginaerum.damagecore.Init.entity.DCEntities.custom.item.ChainLightingArrowRenderer;
import ru.imaginaerum.damagecore.Init.items.DCItems;
import ru.imaginaerum.damagecore.Init.items.chain_lighting_arrow.ArrowLightningStorm;
import ru.imaginaerum.damagecore.Init.tab.DCTabs;
import ru.imaginaerum.damagecore.armor.DamageArmorModifier;
import ru.imaginaerum.damagecore.library_damage.WeaponDamageManager;
import ru.imaginaerum.damagecore.library_damage.arrow_data.ArrowDamageManager;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;
import ru.imaginaerum.damagecore.sounds.DCSoundEvents;

@Mod(Damagecore_1_21_1_neo.MODID)
public class Damagecore_1_21_1_neo {
    public static final String MODID = "damagecore";

    public Damagecore_1_21_1_neo(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        // Регистрируем этот класс на глобальной шине событий NeoForge (для серверного тика)
        NeoForge.EVENT_BUS.register(this);

        DCItems.ITEMS.register(modEventBus);
        DCEntities.ENTITIES.register(modEventBus);
        DCTabs.CREATIVE_MODE_TAB.register(modEventBus);
        DCSoundEvents.SOUND_EVENTS.register(modEventBus);
        modEventBus.register(DCTabs.class);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        IEventBus neoForgeEventBus = NeoForge.EVENT_BUS;
        neoForgeEventBus.addListener(this::onAddReloadListeners);
        PlayerStatsCapability.register(modEventBus);
    }
    public static final WeaponDamageManager WEAPON_DAMAGE_MANAGER = new WeaponDamageManager();
    public static final ArrowDamageManager ARROW_DAMAGE_MANAGER = new ArrowDamageManager();
    public static final DamageArmorModifier ARMOR_MODIFIER = new DamageArmorModifier(); // только здесь, final

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(ARROW_DAMAGE_MANAGER);
        event.addListener(ARMOR_MODIFIER);
        event.addListener(WEAPON_DAMAGE_MANAGER);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    /**
     * ОЧЕНЬ ВАЖНО: Добавляем вызов тиков шторма для стрелы.
     * Без этого метода молния никогда не сделает 2-й, 3-й и 4-й прыжки!
     */
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        ArrowLightningStorm.tick();
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Регистрация 3D отображения летящей стрелы в мире
            EntityRenderers.register(DCEntities.CHAIN_LIGHTNING_ARROW.get(), ChainLightingArrowRenderer::new);
        }
    }
}
