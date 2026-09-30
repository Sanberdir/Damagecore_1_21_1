package ru.imaginaerum.damagecore.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.Config;
import ru.imaginaerum.damagecore.hud.elements.ThirstBarElement;
import ru.imaginaerum.damagecore.hud.net.ThirstDamagePacket;

@EventBusSubscriber(value = Dist.CLIENT)
public class ThirstEventHandler {

    // ---------- питьё из воды (ПКМ пустой рукой) ----------
    private static int drinkTicks = 0;
    private static final int DRINK_DURATION = 25;
    private static final float WATER_RESTORE = 4f;

    // ---------- восстановление от предметов ----------
    private static final float WATER_BOTTLE_RESTORE = 6f;
    private static final float OTHER_POTION_RESTORE = 2f;

    // ---------- расход (значения для сложного режима) ----------
    private static float exhaustion = 0f;
    private static int damageTimer = 0;
    private static int peacefulTimer = 0;

    private static final float DRAIN_IDLE   = 0.0015f;
    private static final float DRAIN_WALK   = 0.0030f;
    private static final float DRAIN_SPRINT = 0.0080f;
    private static final int   DAMAGE_INTERVAL = 80;

    // множители расхода по сложности
    private static final float MULT_EASY   = 0.5f;
    private static final float MULT_NORMAL = 0.75f;
    private static final float MULT_HARD   = 1.0f;

    // мирный режим: +1 ед. каждые 10 тиков
    private static final int   PEACEFUL_INTERVAL = 10;
    private static final float PEACEFUL_RESTORE  = 1f;

    // ---------- тик ----------
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        tickThirst(mc);
        tickDrinkFromWater(mc);
    }

    private static void tickThirst(Minecraft mc) {
        if (!Config.enableThirst) return;
        if (mc.isPaused()) return;
        if (mc.player.isCreative() || mc.player.isSpectator()) return;
        if (!mc.player.isAlive()) return;

        Difficulty difficulty = mc.level.getDifficulty();

        // мирный режим: жажда сама восполняется, урона нет
        if (difficulty == Difficulty.PEACEFUL) {
            exhaustion = 0f;
            damageTimer = 0;
            if (ThirstBarElement.thirst < ThirstBarElement.MAX_THIRST) {
                if (++peacefulTimer >= PEACEFUL_INTERVAL) {
                    peacefulTimer = 0;
                    restore(PEACEFUL_RESTORE);
                }
            }
            return;
        }
        peacefulTimer = 0;

        float mult = switch (difficulty) {
            case EASY -> MULT_EASY;
            case NORMAL -> MULT_NORMAL;
            default -> MULT_HARD;
        };

        float drain = DRAIN_IDLE;
        if (mc.player.isSprinting()) {
            drain = DRAIN_SPRINT;
        } else if (mc.player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4) {
            drain = DRAIN_WALK;
        }

        exhaustion += drain * mult;
        if (exhaustion >= 0.1f) {
            exhaustion -= 0.1f;
            ThirstBarElement.thirst = Math.max(0f, ThirstBarElement.thirst - 0.1f);
        }

        // при нуле шлём запрос на урон, а границу (5 сердец / 1 HP / смерть)
        // проверяет сервер в ThirstDamagePacket
        if (ThirstBarElement.thirst <= 0f) {
            if (++damageTimer >= DAMAGE_INTERVAL) {
                damageTimer = 0;
                PacketDistributor.sendToServer(new ThirstDamagePacket());
            }
        } else {
            damageTimer = 0;
        }
    }

    private static void tickDrinkFromWater(Minecraft mc) {
        boolean rmb = mc.options.keyUse.isDown();
        boolean emptyHands = mc.player.getMainHandItem().isEmpty();
        boolean lookingAtWater = isLookingAtWater(mc);

        if (rmb && emptyHands && lookingAtWater) {
            drinkTicks++;
            if (drinkTicks % 4 == 0) {
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
            if (drinkTicks >= DRINK_DURATION) {
                drinkTicks = 0;
                restore(WATER_RESTORE);
                mc.level.playSound(mc.player, mc.player.blockPosition(),
                        SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1f, 1f);
            }
        } else {
            drinkTicks = 0;
        }
    }

    // ---------- бутылки и зелья ----------
    @SubscribeEvent
    public static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || event.getEntity() != mc.player) return;

        ItemStack stack = event.getItem();
        if (!stack.is(Items.POTION)) return;

        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        boolean isWater = contents != null && contents.is(Potions.WATER);
        restore(isWater ? WATER_BOTTLE_RESTORE : OTHER_POTION_RESTORE);
    }

    // ---------- сброс ----------
    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        resetThirst();
    }

    @SubscribeEvent
    public static void onRespawn(ClientPlayerNetworkEvent.Clone event) {
        resetThirst();
    }

    private static void resetThirst() {
        ThirstBarElement.thirst = ThirstBarElement.MAX_THIRST;
        exhaustion = 0f;
        damageTimer = 0;
        peacefulTimer = 0;
        drinkTicks = 0;
    }

    private static void restore(float amount) {
        ThirstBarElement.thirst = Math.min(ThirstBarElement.MAX_THIRST,
                ThirstBarElement.thirst + amount);
    }

    // ---------- вспомогательное ----------
    private static boolean isLookingAtWater(Minecraft mc) {
        if (mc.player == null || mc.level == null) return false;

        double reach = mc.player.blockInteractionRange();
        var start = mc.player.getEyePosition();
        var look = mc.player.getViewVector(1.0f);
        var end = start.add(look.x * reach, look.y * reach, look.z * reach);

        var hit = mc.level.clip(new ClipContext(
                start, end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.ANY,
                mc.player
        ));

        if (hit.getType() != HitResult.Type.BLOCK) return false;

        return mc.level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER);
    }
}