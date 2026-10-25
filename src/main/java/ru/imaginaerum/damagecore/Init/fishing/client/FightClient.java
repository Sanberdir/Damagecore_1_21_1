package ru.imaginaerum.damagecore.Init.fishing.client;


import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import ru.imaginaerum.damagecore.Init.fishing.net.FightPosPayload;
import ru.imaginaerum.damagecore.Init.fishing.net.FightStatePayload;

/** Client-side mirror of the fight state. */
public final class FightClient {
    public static volatile boolean active;
    public static float tension;
    public static float progress;
    public static float lateral;
    public static int dash;
    public static int warn;
    public static final float BOBBER_RAISE = 0.18f;
    private static double pendingDx;
    private static double pendingDy;

    private FightClient() {}
    private static final java.util.Map<Integer, Pos> POS = new java.util.HashMap<>();

    private record Pos(FightPosPayload p, long time) {}

    public static void applyPos(FightPosPayload p) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null) return;
        POS.put(p.hookId(), new Pos(p, mc.level.getGameTime()));
        apply(mc.level, p);
    }

    /** Повторяет последние позиции каждый клиентский тик. */
    public static void tickPositions() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null) {
            POS.clear();
            return;
        }
        long now = mc.level.getGameTime();
        java.util.Iterator<Pos> it = POS.values().iterator();
        while (it.hasNext()) {
            Pos e = it.next();
            if (now - e.time() > (active ? 40 : 5)) it.remove();
            else apply(mc.level, e.p());
        }
    }
    /** Высота поверхности воды над точкой, NaN если воды нет. */
    private static double waterSurfaceY(Level level, double x, double y, double z) {
        BlockPos.MutableBlockPos p = BlockPos.containing(x, y, z).mutable();
        if (!level.getFluidState(p).is(FluidTags.WATER)) {
            p.move(0, -1, 0);
            if (!level.getFluidState(p).is(FluidTags.WATER)) return Double.NaN;
        }
        for (int i = 0; i < 64 && level.getFluidState(p.above()).is(FluidTags.WATER); i++) {
            p.move(0, 1, 0);
        }
        return p.getY() + level.getFluidState(p).getHeight(level, p);
    }
    /** Максимальная высота верха хитбокса рыбы (чуть ниже поверхности), NaN если это не рыба боя или воды нет. */
    public static double maxTopY(net.minecraft.world.entity.Entity e) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null) return Double.NaN;
        for (Pos pos : POS.values()) {
            FightPosPayload p = pos.p();
            if (p.visualId() != e.getId()) continue;
            double surf = waterSurfaceY(mc.level, p.hx(), p.hy(), p.hz());
            return Double.isNaN(surf) ? Double.NaN : surf - 0.12;
        }
        return Double.NaN;
    }
    /** Верхний предел для любой рыбы рядом с поплавком игрока (до подсечки), NaN если не подходит. */
    public static double maxTopYNearHook(net.minecraft.world.entity.Entity e) {
        if (!(e instanceof AbstractFish)) return Double.NaN;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return Double.NaN;
        FishingHook h = mc.player.fishing;
        if (h == null || e.distanceToSqr(h) > 9.0) return Double.NaN;   // в пределах ~3 блоков
        double surf = waterSurfaceY(mc.level, h.getX(), h.getY(), h.getZ());
        return Double.isNaN(surf) ? Double.NaN : surf - 0.12;
    }
    private static void apply(net.minecraft.world.level.Level level, FightPosPayload p) {
        Entity hook = level.getEntity(p.hookId());
        if (hook != null) {
            hook.setPos(p.hx(), p.hy(), p.hz());
            hook.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        }
        if (p.visualId() >= 0) {
            net.minecraft.world.entity.Entity v = level.getEntity(p.visualId());
            if (v != null) {
                double x = p.hx(), y = p.hy(), z = p.hz();

                if (!(v instanceof net.minecraft.world.entity.item.ItemEntity)) {
                    float yaw = p.yaw();
                    double rad = Math.toRadians(yaw);
                    // рот примерно на высоте глаз и на половину ширины впереди центра
                    double fwd = v.getBbWidth() * 0.5;
                    x += Math.sin(rad) * fwd;       // голова смотрит вперёд, тело позади крючка
                    z -= Math.cos(rad) * fwd;
                    y -= v.getEyeHeight();
                    // рыба целиком под водой: верх хитбокса ниже поверхности, поплавок над ней
                    double surf = waterSurfaceY(level, p.hx(), p.hy(), p.hz());
                    if (!Double.isNaN(surf)) {
                        double maxY = surf - v.getBbHeight() - 0.12;
                        double minY = p.hy() - v.getBbHeight() - 0.3;   // рот остаётся рядом с крючком
                        y = Math.max(minY, Math.min(y, maxY));
                    }
                } else {
                    y -= v.getBbHeight() * 0.5;
                }

                v.setPos(x, y, z);
                v.xo = x; v.yo = y; v.zo = z;
                v.xOld = x; v.yOld = y; v.zOld = z;
                v.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);

                if (!(v instanceof net.minecraft.world.entity.item.ItemEntity)) {
                    v.setYRot(p.yaw());
                    v.yRotO = p.yaw();
                    v.setXRot(0f);
                    v.xRotO = 0f;
                    if (v instanceof net.minecraft.world.entity.LivingEntity le) {
                        le.yBodyRot = p.yaw();  le.yBodyRotO = p.yaw();
                        le.yHeadRot = p.yaw();  le.yHeadRotO = p.yaw();
                    }
                }
            }
        }
    }
    public static void applyState(FightStatePayload p) {
        if (p.active() && !active) {
            pendingDx = 0;
            pendingDy = 0;
        }
        active = p.active();
        tension = p.tension();
        progress = p.progress();
        lateral = p.lateral();
        dash = p.dash();
        warn = p.warn();
        if (!active) {
            reset();
        }
    }

    public static double[] consumeMouse() {
        double[] r = {pendingDx, pendingDy};
        pendingDx = 0;
        pendingDy = 0;
        return r;
    }

    public static void reset() {
        active = false;
        tension = 0;
        progress = 0;
        lateral = 0;
        dash = 0;
        warn = 0;
        pendingDx = 0;
        pendingDy = 0;
    }
}
