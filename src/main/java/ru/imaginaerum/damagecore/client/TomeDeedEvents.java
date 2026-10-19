package ru.imaginaerum.damagecore.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import ru.imaginaerum.damagecore.structure_processors.RescuableVillager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Автоматический подсчёт добрых и злых поступков для сферы (шаг TomeLocator.SPHERE_STEP).
 * Каждый поступок засчитывается через {@link #award}, у которого есть кулдаун от фарма.
 */
@EventBusSubscriber(modid = "damagecore")
public class TomeDeedEvents {
    private static final String CURER_TAG = "damagecore_curer";            // ставит TomeEvents
    private static final String GROWER_TAG = "damagecore_ghast_grower";
    private static final String KILLS_COUNT = "damagecore_animal_kills";
    private static final String KILLS_SINCE = "damagecore_animal_kills_since";

    private static final int ANIMAL_KILLS_NEEDED = 5;
    private static final long ANIMAL_KILLS_WINDOW = 12000L; // тиков (10 минут)

    private static final Map<String, Long> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Raid> RAIDS = new HashMap<>();

    // ------------------------------------------------------------------
    // Общий метод засчитывания
    // ------------------------------------------------------------------

    /** Засчитывает поступок, если игрок на шаге сферы, сфера не заполнена и кулдаун прошёл. */
    public static boolean award(Player pl, boolean good, String key, int cooldownTicks) {
        if (!(pl instanceof ServerPlayer p)) return false;
        if (p.getData(ModAttachments.TOME_STEP) != TomeLocator.SPHERE_STEP) return false;

        long now = p.serverLevel().getGameTime();
        String k = p.getUUID() + key;
        Long last = COOLDOWNS.get(k);
        if (last != null && now - last < cooldownTicks) return false;

        if (!TomeDeeds.add(p, good)) return false;
        COOLDOWNS.put(k, now);
        return true;
    }

    private static boolean isHappyGhast(LivingEntity e) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().equals("happy_ghast");
    }

    private static PotionContents contents(ItemStack s) {
        return s.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
    }

    private static boolean hasEffect(PotionContents c, Holder<MobEffect> effect) {
        for (MobEffectInstance i : c.getAllEffects()) {
            if (i.getEffect().equals(effect)) return true;
        }
        return false;
    }

    private static boolean hasHarmful(PotionContents c) {
        for (MobEffectInstance i : c.getAllEffects()) {
            if (i.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Взаимодействие с существами: кормление, голем, гаст
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.getData(ModAttachments.TOME_STEP) != TomeLocator.SPHERE_STEP) return;

        ItemStack stack = event.getItemStack();

        // Починить голема железным слитком
        if (event.getTarget() instanceof IronGolem golem
                && stack.is(Items.IRON_INGOT)
                && golem.getHealth() < golem.getMaxHealth()) {
            award(player, true, "golem", 600);
            return;
        }

        // Покормить животное
        if (event.getTarget() instanceof Animal animal && animal.isFood(stack)) {
            // запоминаем, кто растит детёныша гаста
            if (isHappyGhast(animal) && animal.isBaby()) {
                animal.getPersistentData().putUUID(GROWER_TAG, player.getUUID());
            }
            boolean canFeed = animal.isBaby() || (animal.getAge() == 0 && !animal.isInLove());
            if (canFeed) award(player, true, "feed", 3600);
        }
    }

    // ------------------------------------------------------------------
    // Тик игрока: рейд и взросление гаста
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;
        if (player.getData(ModAttachments.TOME_STEP) != TomeLocator.SPHERE_STEP) {
            RAIDS.remove(player.getUUID());
            return;
        }
        ServerLevel level = player.serverLevel();

        // Спасти деревню от рейда
        Raid near = level.getRaids().getNearbyRaid(player.blockPosition(), 128 * 128);
        if (near != null && near.isActive()) {
            RAIDS.put(player.getUUID(), near);
        } else {
            Raid tracked = RAIDS.get(player.getUUID());
            if (tracked != null) {
                if (tracked.isVictory()) {
                    if (tracked.getCenter().distSqr(player.blockPosition()) < 160 * 160) {
                        award(player, true, "raid", 0);
                    }
                    RAIDS.remove(player.getUUID());
                } else if (tracked.isOver() || tracked.isStopped()) {
                    RAIDS.remove(player.getUUID());
                }
            }
        }

        // Вырастить счастливого гаста
        if (player.tickCount % 40 == 0) {
            AABB box = player.getBoundingBox().inflate(48.0);
            for (Animal a : level.getEntitiesOfClass(Animal.class, box,
                    e -> !e.isBaby() && e.getPersistentData().hasUUID(GROWER_TAG))) {
                if (a.getPersistentData().getUUID(GROWER_TAG).equals(player.getUUID())) {
                    a.getPersistentData().remove(GROWER_TAG);
                    award(player, true, "ghast_grown", 0);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Смерти: убийство монстров / жителей / питомцев / животных
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        if (killer.getData(ModAttachments.TOME_STEP) != TomeLocator.SPHERE_STEP) return;
        ServerLevel level = killer.serverLevel();

        boolean captive = victim.getPersistentData().hasUUID(TomeEvents.RESCUER_TAG)
                || victim.getTags().contains(RescuableVillager.TAG);

        // Злое: пленник / житель
        if (captive || victim instanceof Villager) {
            award(killer, false, "kill_villager", 200);
            return;
        }
        // Злое: прирученный волк
        if (victim instanceof Wolf wolf && wolf.isTame()) {
            award(killer, false, "kill_wolf", 200);
            return;
        }
        // Злое: счастливый гаст
        if (isHappyGhast(victim)) {
            award(killer, false, "kill_ghast", 200);
            return;
        }
        // Доброе: монстр на территории деревни
        if (victim instanceof Enemy) {
            if (level.isVillage(victim.blockPosition())) {
                award(killer, true, "monster_in_village", 1200);
            }
            return;
        }
        // Злое: массовое убийство мирных животных
        if (victim instanceof Cow || victim instanceof Chicken
                || victim instanceof Pig || victim instanceof Sheep) {
            CompoundTag d = killer.getPersistentData();
            long now = level.getGameTime();
            if (d.getInt(KILLS_COUNT) <= 0 || now - d.getLong(KILLS_SINCE) > ANIMAL_KILLS_WINDOW) {
                d.putInt(KILLS_COUNT, 0);
                d.putLong(KILLS_SINCE, now);
            }
            int n = d.getInt(KILLS_COUNT) + 1;
            if (n >= ANIMAL_KILLS_NEEDED) {
                d.putInt(KILLS_COUNT, 0);
                award(killer, false, "animal_massacre", 0);
            } else {
                d.putInt(KILLS_COUNT, n);
            }
        }
    }

    // ------------------------------------------------------------------
    // Приручение волка
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onTame(AnimalTameEvent event) {
        if (event.getAnimal() instanceof Wolf && event.getTamer() instanceof ServerPlayer p) {
            award(p, true, "tame_wolf", 1200);
        }
    }

    // ------------------------------------------------------------------
    // Блоки: посадки, спавнер, крикун
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BlockState state = event.getPlacedBlock();

        if (state.is(BlockTags.SAPLINGS)) {
            award(player, true, "plant_tree", 1200);
        } else if (state.is(BlockTags.CROPS)) {
            award(player, true, "plant_crop", 1200);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        BlockState state = event.getState();

        if (state.is(Blocks.SPAWNER) || state.is(Blocks.TRIAL_SPAWNER)) {
            award(player, true, "break_spawner", 0);
        } else if (state.is(Blocks.SCULK_SHRIEKER)) {
            award(player, true, "break_shrieker", 600);
        }
    }

    // ------------------------------------------------------------------
    // Зелья: варка и бросок
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onBrewed(PlayerBrewedPotionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PotionContents c = contents(event.getStack());
        if (hasEffect(c, MobEffects.HEAL)) {
            award(player, true, "brew_healing", 1200);
        } else if (hasEffect(c, MobEffects.REGENERATION)) {
            award(player, true, "brew_regen", 1200);
        }
    }

    @SubscribeEvent
    public static void onPotionImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof ThrownPotion potion)) return;
        if (potion.level().isClientSide()) return;
        if (!(potion.getOwner() instanceof ServerPlayer thrower)) return;
        if (thrower.getData(ModAttachments.TOME_STEP) != TomeLocator.SPHERE_STEP) return;

        PotionContents c = contents(potion.getItem());
        boolean healing = hasEffect(c, MobEffects.HEAL) || hasEffect(c, MobEffects.REGENERATION);
        boolean harmful = hasHarmful(c);
        if (!healing && !harmful) return;

        AABB box = potion.getBoundingBox().inflate(4.0, 2.0, 4.0);
        boolean hitInnocent = false;
        boolean healedWounded = false;
        for (LivingEntity e : potion.level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (e == thrower) continue;
            if (harmful && (e instanceof AbstractVillager || e instanceof Player)) {
                hitInnocent = true;
            }
            if (healing && e.getHealth() < e.getMaxHealth()
                    && (e instanceof Animal || e instanceof AbstractVillager || e instanceof IronGolem)) {
                healedWounded = true;
            }
        }
        if (hitInnocent) award(thrower, false, "harm_potion", 200);
        if (healedWounded) award(thrower, true, "heal_potion", 600);
    }

    // ------------------------------------------------------------------
    // Жители: лечение и заражение
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onConversion(LivingConversionEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;

        // Исцелить жителя (зомби-житель -> житель)
        if (event.getEntity() instanceof ZombieVillager zombie) {
            Player curer = null;
            if (zombie.getPersistentData().hasUUID(CURER_TAG)) {
                curer = zombie.level().getPlayerByUUID(zombie.getPersistentData().getUUID(CURER_TAG));
            }
            if (curer == null) curer = zombie.level().getNearestPlayer(zombie, 32.0);
            if (curer != null) award(curer, true, "cure_villager", 0);
            return;
        }

        // Заражение жителя в деревне (житель -> зомби-житель)
        if (event.getEntity() instanceof Villager villager
                && event.getOutcome() instanceof ZombieVillager
                && villager.level() instanceof ServerLevel level
                && level.isVillage(villager.blockPosition())) {
            Player blamed = level.getNearestPlayer(villager, 48.0);
            if (blamed != null) award(blamed, false, "infection", 1200);
        }
    }

    // ------------------------------------------------------------------
    // TNT уничтожает кровать жителя
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Explosion ex = event.getExplosion();
        if (!(ex.getDirectSourceEntity() instanceof PrimedTnt)) return;

        PoiManager poi = level.getPoiManager();
        boolean villagerBed = false;
        for (BlockPos pos : event.getAffectedBlocks()) {
            if (level.getBlockState(pos).is(BlockTags.BEDS)
                    && poi.exists(pos, h -> h.is(PoiTypes.HOME))) {
                villagerBed = true;
                break;
            }
        }
        if (!villagerBed) return;

        Player culprit = ex.getIndirectSourceEntity() instanceof ServerPlayer sp ? sp
                : level.getNearestPlayer(ex.center().x, ex.center().y, ex.center().z, 32.0, false);
        if (culprit != null) award(culprit, false, "bed_tnt", 600);
    }
}