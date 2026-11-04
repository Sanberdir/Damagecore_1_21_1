package ru.imaginaerum.damagecore.Init.fishing.weight;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Вес и размер пойманной рыбы. Вес влияет на бой, размер на внешний вид. */
public final class FishWeight {
    /** Вес, при котором множители равны 1.0 (средняя треска). */
    public static final float REFERENCE_KG = 2.0f;

    // ── легендарные рыбы: та же модель, но тяжелее и крупнее ──
    public static final float LEGENDARY_CHANCE     = 0.02f;  // 2% (для теста можно 1.0f)
    public static final float LEGENDARY_WEIGHT_MUL = 2.5f;
    public static final float LEGENDARY_SCALE_MUL  = 1.8f;

    // ── размеры по видам: вес от min до max, базовый масштаб модели ──
    // треска чуть крупнее ванили, лосось чуть меньше
// было: COD 1.25f, SALMON 0.90f
    private static final Species COD      = new Species(2.0f, 6.0f, 2.0f);
    private static final Species SALMON   = new Species(1.0f, 3.0f, 0.85f);
    private static final Species PUFFER   = new Species(0.2f, 0.5f, 1.00f);
    private static final Species TROPICAL = new Species(0.3f, 1.2f, 0.60f);

    // разброс размера внутри вида: самая лёгкая рыба x0.92, самая тяжёлая x1.08
    private static final float VAR_MIN = 0.92f;
    private static final float VAR_MAX = 1.08f;

    private static final String KEY = "df_weight";

    private record Species(float minKg, float maxKg, float baseScale) {}

    public static final FishWeight DEFAULT = new FishWeight(REFERENCE_KG, 1.0f, false);

    private final float kg;
    private final float scale;
    private final boolean legendary;
    private final float loadFactor;
    private final float reelFactor;

    private FishWeight(float kg, float scale, boolean legendary) {
        this.kg = kg;
        this.scale = Mth.clamp(scale, 0.1f, 8.0f);
        this.legendary = legendary;
        // корень: двойной вес даёт примерно x1.4 к нагрузке, а не x2
        this.loadFactor = Mth.clamp((float) Math.sqrt(kg / REFERENCE_KG), 0.6f, 2.2f);
        // тяжёлая рыба тянется медленнее, лёгкая чуть быстрее
        this.reelFactor = Mth.clamp(1.0f - 0.35f * (loadFactor - 1.0f), 0.5f, 1.15f);
    }
    public static float storedKg(Entity fish) {
        CompoundTag d = fish.getPersistentData();
        return d.contains(KEY, Tag.TAG_COMPOUND) ? d.getCompound(KEY).getFloat("kg") : 0f;
    }
    private static Species speciesOf(ItemStack s) {
        if (s.is(Items.COD))           return COD;
        if (s.is(Items.SALMON))        return SALMON;
        if (s.is(Items.PUFFERFISH))    return PUFFER;
        if (s.is(Items.TROPICAL_FISH)) return TROPICAL;
        return null;
    }

    /** Бросает вес и размер для предмета-добычи. */
    public static FishWeight roll(ItemStack stack, RandomSource r) {
        return roll(stack, r, r.nextFloat() < LEGENDARY_CHANCE);
    }

    /** То же, но легендарность задаётся снаружи (команды, события). */
    public static FishWeight roll(ItemStack stack, RandomSource r, boolean legendary) {
        Species sp = speciesOf(stack);
        if (sp == null) return DEFAULT;

        float t = r.nextFloat();
        float kg = sp.minKg() + t * (sp.maxKg() - sp.minKg());

        // размер от веса: вес ~ размер³; серединa диапазона вида = baseScale
        float mid = (sp.minKg() + sp.maxKg()) * 0.5f;
        float scale = sp.baseScale() * (float) Math.cbrt(kg / mid);

        if (legendary) {
            kg *= LEGENDARY_WEIGHT_MUL;
            scale *= LEGENDARY_SCALE_MUL;
        }
        return new FishWeight(Math.round(kg * 10f) / 10f, scale, legendary);
    }

    /**
     * Вес конкретной рыбы-сущности. Если она уже получала вес, берём сохранённый,
     * иначе бросаем новый, запоминаем в рыбе и применяем размер.
     */
    public static FishWeight forFish(Entity fish, ItemStack stack, RandomSource r) {
        CompoundTag data = fish.getPersistentData();
        if (data.contains(KEY, Tag.TAG_COMPOUND)) {
            CompoundTag t = data.getCompound(KEY);
            FishWeight w = new FishWeight(t.getFloat("kg"), t.getFloat("scale"), t.getBoolean("legendary"));
            w.applyScale(fish);
            return w;
        }
        FishWeight w = roll(stack, r);
        CompoundTag t = new CompoundTag();
        t.putFloat("kg", w.kg);
        t.putFloat("scale", w.scale);
        t.putBoolean("legendary", w.legendary);
        data.put(KEY, t);
        w.applyScale(fish);
        return w;
    }

    /** Ставит размер модели и хитбокса через атрибут scale. */
    public void applyScale(Entity e) {
        if (!(e instanceof LivingEntity le)) return;
        AttributeInstance a = le.getAttribute(Attributes.SCALE);
        if (a == null) return;
        if (Math.abs(a.getBaseValue() - scale) > 1.0E-3) {
            a.setBaseValue(scale);
            le.refreshDimensions();
        }
    }

    public float kg() { return kg; }
    public float scale() { return scale; }
    public boolean isLegendary() { return legendary; }

    /** Множитель скорости роста нагрузки на леску (1.0 = средняя треска). */
    public float loadFactor() { return loadFactor; }

    /** Множитель скорости подмотки (меньше 1.0 = тянется медленнее). */
    public float reelFactor() { return reelFactor; }
}