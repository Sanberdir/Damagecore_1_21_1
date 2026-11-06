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
    private static float pBend, pSide, pFwd, pBaseSide;

    public static float bendAt(float pt)     { return net.minecraft.util.Mth.lerp(pt, pBend, rodBend); }
    public static float sideAt(float pt)     { return net.minecraft.util.Mth.lerp(pt, pSide, rodSide); }
    public static float fwdAt(float pt)      { return net.minecraft.util.Mth.lerp(pt, pFwd, baseFwd); }
    public static float baseSideAt(float pt) { return net.minecraft.util.Mth.lerp(pt, pBaseSide, baseSide); }
    public static volatile boolean active;
    public static float tension;
    public static float progress;
    public static float lateral;
    public static int dash;
    public static int warn;
    public static final float BOBBER_RAISE = 0.18f;
    private static double pendingDx;
    private static double pendingDy;
    public static float rodBend;   // 0..~0.4 в единицах модели
    public static float tipDx, tipDy, tipDz;
    public static float rodSide;                 // боковой изгиб, -1..1 в единицах модели
    public static float inSide;                  // -1 = A, +1 = D
    public static boolean inTighten, inSlack;    // S / W
    public static float baseFwd;   // сдвиг основания вперёд/назад, модельные единицы
    public static float baseSide;  // сдвиг основания влево/вправо
    public static float rodBendVel, rodSideVel;   // скорости пружин кончика
    public static float baseFwdVel, baseSideVel;  // скорости рукояти
    private FightClient() {}
    private static final java.util.Map<Integer, Pos> POS = new java.util.HashMap<>();

    private record Pos(FightPosPayload p, long time) {}

    public static void applyPos(FightPosPayload p) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null) return;
        POS.put(p.hookId(), new Pos(p, mc.level.getGameTime()));
        apply(mc.level, p);
    }
    public static float tensionShown;

    public static float rodPitch, rodRoll;

    public static void smoothTick() {
        pBend = rodBend; pSide = rodSide; pFwd = baseFwd; pBaseSide = baseSide;
        tensionShown += (tension - tensionShown) * 0.35f;

        float tb = 0f, ts = 0f, tbf = 0f, tbs = 0f;
        float t = net.minecraft.client.Minecraft.getInstance().level != null
                ? net.minecraft.client.Minecraft.getInstance().level.getGameTime() : 0;

        if (active) {
            // 1) Рука игрока: A/D ведут рукоять вбок, S/W двигают её от себя / к себе
            tbs = inSide * 0.20f;
            if (inTighten)    tbf =  0.12f;
            else if (inSlack) tbf = -0.10f;

            // 2) Сила, с которой рыба тянет кончик. В рывке сильнее, когда устала, слабее
            float k = net.minecraft.util.Mth.clamp((tensionShown - 0.25f) / 0.70f, 0f, 1f);
            float phaseK = dash == 2 ? 1.0f : (dash == 3 ? 0.45f : 0.70f);
            float pull = k * phaseK;

            // вперёд-вниз, к воде
            tb = 0.34f * pull;
            if (inTighten) tb += 0.08f * (0.4f + pull);   // придержал леску: удилище нагружается
            if (inSlack)   tb -= 0.06f;

            // вбок: кончик тянется в сторону рыбы (lateral > 0 = вправо)
            float fishSide = net.minecraft.util.Mth.clamp(lateral * 2.5f, -1f, 1f) * 0.30f * pull;
            ts = fishSide;
            // тянешь против рыбы - нагрузка растёт
            if (inSide != 0 && Math.signum(fishSide) == -inSide) ts += fishSide * 0.35f;
            // инерция: рукоять ушла вбок, кончик отстаёт и прогибается в обратную сторону
            ts -= baseSideVel * 3.0f;

            // дрожь, пока рыба рвётся
            if (dash == 2) {
                tb += net.minecraft.util.Mth.sin(t * 1.7f) * 0.015f * pull;
                ts += net.minecraft.util.Mth.sin(t * 2.3f + 1f) * 0.020f * pull;
            }
            tb = net.minecraft.util.Mth.clamp(tb, 0f, 0.42f);
            ts = net.minecraft.util.Mth.clamp(ts, -0.42f, 0.42f);
        }

        // рукоять: быстрая, хорошо задемпфированная пружина
        float oldF = baseFwd, oldS = baseSide;
        baseFwdVel  += (tbf - baseFwd)  * 0.22f; baseFwdVel  *= 0.70f; baseFwd  += baseFwdVel;
        baseSideVel += (tbs - baseSide) * 0.22f; baseSideVel *= 0.70f; baseSide += baseSideVel;
        baseFwdVel = baseFwd - oldF;
        baseSideVel = baseSide - oldS;

        // кончик: упругая пружина с лёгким перелётом (отдача после рывка и обрыва)
        rodBendVel  += (tb - rodBend) * 0.20f; rodBendVel  *= 0.74f; rodBend += rodBendVel;
        rodSideVel  += (ts - rodSide) * 0.18f; rodSideVel  *= 0.72f; rodSide += rodSideVel;
        rodBend = net.minecraft.util.Mth.clamp(rodBend, -0.04f, 0.45f);
        rodSide = net.minecraft.util.Mth.clamp(rodSide, -0.45f, 0.45f);

        rodPitch += (0f - rodPitch) * 0.2f;
        rodRoll  += (0f - rodRoll)  * 0.2f;
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
        tensionShown = 0;
        inSide = 0; inTighten = false; inSlack = false;
        lateral = 0;
        dash = 0;
        warn = 0;
        pendingDx = 0;
        pendingDy = 0;
        baseFwd = 0f;
        baseSide = 0f;
        rodBendVel = 0f; rodSideVel = 0f;
        baseFwdVel = 0f; baseSideVel = 0f;
    }
}
