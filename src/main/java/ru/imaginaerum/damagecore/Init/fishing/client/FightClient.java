package ru.imaginaerum.damagecore.Init.fishing.client;


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
            if (now - e.time() > 5) it.remove();
            else apply(mc.level, e.p());
        }
    }

    private static void apply(net.minecraft.world.level.Level level, FightPosPayload p) {
        net.minecraft.world.entity.Entity hook = level.getEntity(p.hookId());
        if (hook != null) {
            hook.setPos(p.hx(), p.hy(), p.hz());
            hook.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        }
        if (p.visualId() >= 0) {
            net.minecraft.world.entity.Entity v = level.getEntity(p.visualId());
            if (v != null) {
                v.setPos(p.vx(), p.vy(), p.vz());
                v.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                if (!(v instanceof net.minecraft.world.entity.item.ItemEntity)) {
                    v.setYRot(p.yaw());
                    v.yRotO = p.yaw();
                    v.setXRot(0f);
                    if (v instanceof net.minecraft.world.entity.LivingEntity le) {
                        le.yBodyRot = p.yaw();
                        le.yBodyRotO = p.yaw();
                        le.yHeadRot = p.yaw();
                        le.yHeadRotO = p.yaw();
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

    public static void addMouse(double dx, double dy) {
        pendingDx += dx;
        pendingDy += dy;
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
