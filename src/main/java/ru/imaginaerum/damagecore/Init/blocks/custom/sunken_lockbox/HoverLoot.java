package ru.imaginaerum.damagecore.Init.blocks.custom.sunken_lockbox;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID)
public final class HoverLoot {
    public static final String KEY = "df_hover";

    /** Длительность всплытия, тиков (20 = 1 секунда). Меньше значит резче. */
    static final int RISE_TICKS = 14;
    /** Насколько выше точки зависания предмет в итоге оказывается. */
    static final double EXTRA_RISE = 0.25;
    /** Сколько тиков нельзя подобрать: чуть дольше, чем всплытие. */
    static final int PICKUP_DELAY = RISE_TICKS + 4;

    private HoverLoot() {}

    public static void mark(ItemEntity ie, Vec3 start, Vec3 hover) {
        CompoundTag h = new CompoundTag();
        h.putDouble("sx", start.x); h.putDouble("sy", start.y); h.putDouble("sz", start.z);
        h.putDouble("hx", hover.x); h.putDouble("hy", hover.y); h.putDouble("hz", hover.z);
        h.putInt("age", 0);
        ie.getPersistentData().put(KEY, h);
        ie.setNoGravity(true);
        ie.setUnlimitedLifetime();
        ie.setDeltaMovement(Vec3.ZERO);
        ie.setPickUpDelay(PICKUP_DELAY);
    }

    /** 0 -> 1 за RISE_TICKS: быстрый вылет и резкое торможение (ease-out cubic). */
    private static double ease(int age) {
        double p = Math.min(1.0, (age + 1) / (double) RISE_TICKS);
        double q = 1.0 - p;
        return 1.0 - q * q * q;
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post e) {
        if (!(e.getEntity() instanceof ItemEntity ie) || !(ie.level() instanceof ServerLevel)) return;
        CompoundTag d = ie.getPersistentData();
        if (!d.contains(KEY, Tag.TAG_COMPOUND)) return;
        CompoundTag h = d.getCompound(KEY);

        int age = h.getInt("age");
        double sx = h.getDouble("sx"), sy = h.getDouble("sy"), sz = h.getDouble("sz");
        double hx = h.getDouble("hx"), hy = h.getDouble("hy"), hz = h.getDouble("hz");

        double f = ease(age);
        ie.setPos(Mth.lerp(f, sx, hx), Mth.lerp(f, sy, hy + EXTRA_RISE), Mth.lerp(f, sz, hz));
        ie.setDeltaMovement(Vec3.ZERO);
        if (age <= RISE_TICKS + 1) ie.hasImpulse = true;   // шлём позицию каждый тик, пока движется

        if (age < 1_000_000) h.putInt("age", age + 1);
    }
}