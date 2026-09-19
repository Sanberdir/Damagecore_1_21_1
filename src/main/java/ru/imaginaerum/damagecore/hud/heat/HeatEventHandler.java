package ru.imaginaerum.damagecore.hud.heat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.library_damage.DamageContext;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.TypedDamageSource;
import ru.imaginaerum.damagecore.library_stats.IPlayerStats;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Логика накопления "жара" при стоянии на магматическом блоке,
 * нахождении рядом с зажжённой печью / плавильней / коптильней,
 * рядом с ОТКРЫТОЙ лавой или ОТКРЫТЫМ огнём (обычным или огнём души).
 * Радиусы обнаружения лавы и огня зависят от измерения - в Незере они больше.
 * Работает по аналогии с ванильным механизмом обморожения (freezing) в рыхлом снегу.
 * Урон от перегрева классифицируется как ru.imaginaerum.damagecore.library_damage.DamageType.TEMPERATURE.
 *
 * Интервал между тиками урона растягивается в зависимости от IPlayerStats.getImmunityPercent(),
 * по тому же принципу дробного аккумулятора, что и FreezeThrottleMixin для обморожения.
 */
@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID)
public class HeatEventHandler {

    // Максимальный уровень жара (в тиках), после которого начинает наноситься урон
    public static final int MAX_HEAT = 300;

    // Скорость накопления жара за тик, пока игрок стоит на магме
    private static final int HEAT_PER_TICK_MAGMA = 1;
    // Скорость накопления жара за тик от близости к зажжённой печи
    private static final int HEAT_PER_TICK_FURNACE = 1;
    // Скорость накопления жара за тик от близости к открытой лаве
    private static final int HEAT_PER_TICK_LAVA = 1;
    // Скорость накопления жара за тик от обычного открытого огня
    private static final int HEAT_PER_TICK_FIRE = 1;
    // Огонь души греет вдвое сильнее обычного огня
    private static final int HEAT_PER_TICK_SOUL_FIRE = HEAT_PER_TICK_FIRE * 2;
    // Скорость остывания за тик, когда игрок вне источников жара
    private static final int HEAT_COOLDOWN_PER_TICK = 2;

    // Базовый интервал нанесения урона (в тиках) при полном накоплении жара - как у обморожения (40 тиков = 2 сек)
    private static final int DAMAGE_INTERVAL_TICKS = 40;
    // Величина урона за одно срабатывание
    private static final float DAMAGE_AMOUNT = 1.0F;

    // Радиус (в блоках) вокруг игрока, в котором ищем зажжённые печи
    private static final int FURNACE_SEARCH_RADIUS = 1;

    // Радиус обнаружения лавы в обычных измерениях (Оверворлд, Энд и т.д.)
    private static final int LAVA_SEARCH_RADIUS_DEFAULT = 3;
    // Радиус обнаружения лавы в Незере - там лава считается более фоновым источником жара
    private static final int LAVA_SEARCH_RADIUS_NETHER = 10;

    // Радиус обнаружения огня (обычного и огня души) в обычных измерениях
    private static final int FIRE_SEARCH_RADIUS_DEFAULT = 2;
    // Радиус обнаружения огня в Незере - там огонь тоже греет на большем расстоянии
    private static final int FIRE_SEARCH_RADIUS_NETHER = 8;

    /**
     * Дробный аккумулятор прогресса урона от перегрева, персонально на игрока.
     * Работает по тому же принципу, что и damagecore$freezeAccumulator в FreezeThrottleMixin:
     * каждый тик "в максимальном жаре" добавляет 1.0, а сработать урон может только
     * когда накоплено significa >= DAMAGE_INTERVAL_TICKS * (1 + immunityPercent / 100.0).
     */
    private static final Map<UUID, Double> HEAT_DAMAGE_ACCUMULATOR = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();

        // Логику считаем только на сервере - клиенту значение придёт через синхронизацию attachment'а
        if (level.isClientSide()) {
            return;
        }

        // Игроков в творческом/спектейторе или с неуязвимостью не трогаем
        if (player.isSpectator() || player.getAbilities().invulnerable) {
            return;
        }

        int currentHeat = player.getData(ModAttachmentsHeat.HEAT.get());

        boolean standingOnMagma = isStandingOnMagma(player, level);
        boolean nearOpenLava = isNearOpenLava(player, level);
        boolean nearOpenSoulFire = isNearOpenFire(player, level, true);
        boolean nearOpenFire = !nearOpenSoulFire && isNearOpenFire(player, level, false);
        boolean nearLitFurnace = isNearLitFurnace(player, level);

        if (standingOnMagma) {
            // Магма греет сильнее и быстрее - высший приоритет
            currentHeat = Math.min(MAX_HEAT, currentHeat + HEAT_PER_TICK_MAGMA);
        } else if (nearOpenLava) {
            // Открытая лава рядом - копим жар (радиус зависит от измерения)
            currentHeat = Math.min(MAX_HEAT, currentHeat + HEAT_PER_TICK_LAVA);
        } else if (nearOpenSoulFire) {
            // Открытый огонь души рядом - греет вдвое сильнее обычного огня
            currentHeat = Math.min(MAX_HEAT, currentHeat + HEAT_PER_TICK_SOUL_FIRE);
        } else if (nearOpenFire) {
            // Открытый обычный огонь рядом
            currentHeat = Math.min(MAX_HEAT, currentHeat + HEAT_PER_TICK_FIRE);
        } else if (nearLitFurnace) {
            // Зажжённая печь/плавильня/коптильня рядом - тоже копим жар
            currentHeat = Math.min(MAX_HEAT, currentHeat + HEAT_PER_TICK_FURNACE);
        } else {
            // Вне источников тепла жар постепенно спадает
            currentHeat = Math.max(0, currentHeat - HEAT_COOLDOWN_PER_TICK);
        }

        player.setData(ModAttachmentsHeat.HEAT.get(), currentHeat);

        if (currentHeat >= MAX_HEAT) {
            // Жар максимален - копим дробный аккумулятор урона с учётом иммунитета игрока
            tickHeatDamageAccumulator(player, level);
        } else {
            // Жар спал ниже максимума - сбрасываем накопленный прогресс урона,
            // чтобы при следующем перегреве отсчёт начинался заново (как с оттаиванием)
            HEAT_DAMAGE_ACCUMULATOR.remove(player.getUUID());
        }
    }

    /**
     * Аналог логики из FreezeThrottleMixin: интервал между уроном растягивается
     * пропорционально immunityPercent игрока. При immunityPercent = 0 поведение
     * идентично простому "каждые DAMAGE_INTERVAL_TICKS тиков".
     */
    private static void tickHeatDamageAccumulator(Player player, Level level) {
        int immunityPercent = PlayerStatsCapability.get(player)
                .map(IPlayerStats::getImmunityPercent)
                .orElse(0);

        // Требуемый интервал в "тиках-эквивалентах" с учётом иммунитета
        double requiredInterval = DAMAGE_INTERVAL_TICKS * (1.0 + immunityPercent / 100.0);

        UUID id = player.getUUID();
        double acc = HEAT_DAMAGE_ACCUMULATOR.merge(id, 1.0, Double::sum);

        if (acc >= requiredInterval) {
            HEAT_DAMAGE_ACCUMULATOR.put(id, acc - requiredInterval);
            applyOverheatDamage(player, level);
        }
        // иначе урон в этот тик "пропускается" - копим дальше
    }

    /**
     * Наносит урон от перегрева, помечая его как DamageType.TEMPERATURE
     * из вашей внутренней системы урона (library_damage).
     * Ванильный тип HOT_FLOOR используется как основа, чтобы сохранить
     * корректный текст в чате при смерти и совместимость с DamageSource-логикой
     * (иммунитет к урону от жары через isFireImmune и т.д.).
     */
    private static void applyOverheatDamage(Player player, Level level) {
        Holder<net.minecraft.world.damagesource.DamageType> hotFloorHolder =
                level.registryAccess().holderOrThrow(DamageTypes.HOT_FLOOR);

        // attacker = null, так как это чисто средовой (environmental) урон, без атакующей сущности
        TypedDamageSource overheatSource = new TypedDamageSource(
                hotFloorHolder,
                List.of(DamageType.TEMPERATURE),
                null
        );

        boolean damaged = player.hurt(overheatSource, DAMAGE_AMOUNT);

        // Если урон прошёл (не заблокирован иммунитетом/щитом) - фиксируем его в контексте
        if (damaged) {
            DamageContext.add(player, ru.imaginaerum.damagecore.library_damage.DamageType.TEMPERATURE, DAMAGE_AMOUNT);
        }
    }

    /**
     * Очищает аккумулятор при смерти игрока, чтобы не копить "мёртвые" записи в карте
     * (аналог необходимости чистки WeakHashMap в DamageContext, но здесь ключ - UUID, не сама сущность,
     * поэтому GC не подчистит запись автоматически).
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            HEAT_DAMAGE_ACCUMULATOR.remove(player.getUUID());
        }
    }

    /**
     * Проверяет, стоит ли игрок непосредственно на блоке магмы.
     */
    private static boolean isStandingOnMagma(Player player, Level level) {
        BlockPos posBelow = player.getOnPos();
        BlockState stateBelow = level.getBlockState(posBelow);
        return stateBelow.is(Blocks.MAGMA_BLOCK);
    }

    /**
     * Проверяет наличие зажжённой печи (обычной, плавильни или коптильни)
     * в радиусе FURNACE_SEARCH_RADIUS вокруг позиции игрока.
     * Свойство LIT (горит ли печь) общее для всех трёх блоков,
     * так как все они наследуются от AbstractFurnaceBlock.
     */
    private static boolean isNearLitFurnace(Player player, Level level) {
        BlockPos center = player.blockPosition();

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-FURNACE_SEARCH_RADIUS, -FURNACE_SEARCH_RADIUS, -FURNACE_SEARCH_RADIUS),
                center.offset(FURNACE_SEARCH_RADIUS, FURNACE_SEARCH_RADIUS, FURNACE_SEARCH_RADIUS))) {

            BlockState state = level.getBlockState(pos);

            boolean isFurnaceType = state.is(Blocks.FURNACE)
                    || state.is(Blocks.BLAST_FURNACE)
                    || state.is(Blocks.SMOKER);

            if (isFurnaceType && state.hasProperty(AbstractFurnaceBlock.LIT) && state.getValue(AbstractFurnaceBlock.LIT)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Проверяет наличие ОТКРЫТОЙ лавы (источник или поток) в радиусе вокруг игрока.
     * "Открытая" означает, что лава не полностью запечатана твёрдыми блоками -
     * у неё есть хотя бы одна соседняя грань с воздухом, то есть жар от неё
     * реально может дойти до игрока, а не заперт внутри горной породы.
     * Радиус поиска зависит от измерения: в Незере он больше.
     */
    private static boolean isNearOpenLava(Player player, Level level) {
        int radius = level.dimension() == Level.NETHER
                ? LAVA_SEARCH_RADIUS_NETHER
                : LAVA_SEARCH_RADIUS_DEFAULT;

        BlockPos center = player.blockPosition();

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {

            BlockState state = level.getBlockState(pos);

            if (state.getFluidState().is(FluidTags.LAVA) && isBlockOpen(level, pos)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Проверяет наличие ОТКРЫТОГО огня (обычного или огня души) в радиусе вокруг игрока.
     * Радиус поиска зависит от измерения: в Незере огонь греет на большем расстоянии.
     *
     * @param soulFire true - ищем блок огня души (Blocks.SOUL_FIRE),
     *                 false - ищем блок обычного огня (Blocks.FIRE)
     */
    private static boolean isNearOpenFire(Player player, Level level, boolean soulFire) {
        int radius = level.dimension() == Level.NETHER
                ? FIRE_SEARCH_RADIUS_NETHER
                : FIRE_SEARCH_RADIUS_DEFAULT;

        BlockPos center = player.blockPosition();

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {

            BlockState state = level.getBlockState(pos);
            boolean isTargetFire = soulFire ? state.is(Blocks.SOUL_FIRE) : state.is(Blocks.FIRE);

            if (isTargetFire && isBlockOpen(level, pos)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Проверяет, что блок по позиции pos является "открытым" -
     * то есть хотя бы одна из 6 соседних клеток пуста (воздух).
     * Используется как для лавы, так и для огня: если источник тепла
     * полностью замурован в твёрдых блоках, он не считается доступным жаром.
     */
    private static boolean isBlockOpen(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);

            if (neighborState.isAir()) {
                return true;
            }
        }

        return false;
    }
}