package ru.imaginaerum.damagecore.animation_attack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.animation_attack.combat.CombatContext;
import ru.imaginaerum.damagecore.animation_attack.combat.WeaponCombatController;
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.hud.elements.StaminaManager;
import ru.imaginaerum.damagecore.key_bind.BackstepHandler;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.PacketTypedAttack;
import ru.imaginaerum.damagecore.library_weapon_types.WeaponTypeManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT)
public class WeaponAttackAnimationHandler {

    private static final Map<UUID, WeaponCombatController> CONTROLLERS = new ConcurrentHashMap<>();

    /** true, если у предмета в основной руке есть кастомные анимации атаки. */
    private static boolean holdsAnimatedWeapon(AbstractClientPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());

        if (WeaponTypeManager.INSTANCE.getType(id) == null) return false;

        WeaponAnimationManager am = WeaponAnimationManager.INSTANCE;
        return !am.getRegularSwings(id).isEmpty()
                || !am.getChargeKeysOrder(id).isEmpty();
    }

    public static boolean triggerSwing(AbstractClientPlayer player) {
        return controller(player).onPrimaryDown(null);
    }
    @SubscribeEvent
    public static void onAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.player instanceof AbstractClientPlayer player)) return;
        if (!holdsAnimatedWeapon(player)) return;
        if (StaminaManager.isExhausted() && !player.isCreative() && !player.isSpectator()) {
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }
        // Твёрдый блок и боя нет: отдаём клик ваниле (копание)
        if (!isBusy(player)
                && mc.hitResult instanceof BlockHitResult bhr
                && bhr.getType() == HitResult.Type.BLOCK) {
            BlockState state = player.level().getBlockState(bhr.getBlockPos());
            if (!state.isAir()
                    && state.getDestroySpeed(player.level(), bhr.getBlockPos()) != 0.0F) return;
        }
        if (!isBusy(player) && isAirborne(player)) {
            if (mc.hitResult instanceof EntityHitResult ehr
                    && ehr.getEntity() instanceof LivingEntity victim && victim != player) {

                ResourceLocation id = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem());
                var swings = WeaponAnimationManager.INSTANCE.getRegularSwings(id);
                var entry = swings.isEmpty() ? null : swings.get(0);

                java.util.List<DamageType> types =
                        entry != null && entry.damageTypes() != null && !entry.damageTypes().isEmpty()
                                ? entry.damageTypes()
                                : java.util.List.of(DamageType.PIERCING);
                double mult = entry != null ? entry.damageMultiplier() : 1.0;

                ModNetwork.sendToServer(new PacketTypedAttack(victim.getId(), types, mult));
                StaminaManager.drainForAttack(player);
            }
            event.setCanceled(true);   // ванильный пакет не отправляем, иначе будет двойной урон
            event.setSwingHand(true);  // ванильный взмах руки остаётся
            return;
        }
        // Гасим ДО отправки пакета на сервер
        event.setCanceled(true);
        event.setSwingHand(false);

        LivingEntity target = mc.hitResult instanceof EntityHitResult ehr
                && ehr.getEntity() instanceof LivingEntity l ? l : null;
        controller(player).onPrimaryDown(target);
    }
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;
        if (!player.level().isClientSide()) return;
        if (!holdsAnimatedWeapon(player)) return; // рука/инструменты: ванильное поведение
        if (!isBusy(player) && !player.onGround() && !player.isInWater()
                && !player.onClimbable() && !player.getAbilities().flying) {
            event.setCanceled(true);

            return;
        }
        if (!isBusy(player) && isAirborne(player)) {
            return; // ваниль: копание/удар по блоку в прыжке
        }
        // Твёрдый блок (hardness != 0) и мы не в бою — отдаём клик ваниле: копание/ломание.
        // Если комбо/замах уже идёт (isBusy) — по-прежнему гасим, чтобы не копать посреди анимации.
        if (!isBusy(player)) {
            BlockState state = player.level().getBlockState(event.getPos());
            float hardness = state.getDestroySpeed(player.level(), event.getPos());
            boolean softOrAir = state.isAir() || hardness == 0.0F;
            if (!softOrAir) return; // не отменяем событие, анимацию не запускаем
        }


        event.setCanceled(true);
        triggerSwing(player);
    }
    private static boolean isAirborne(AbstractClientPlayer player) {
        return !player.onGround() && !player.isInWater()
                && !player.onClimbable() && !player.getAbilities().flying;
    }
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;
        if (!holdsAnimatedWeapon(player)) return;

        // В прыжке ванильный удар проходит как есть, но стамину тратим
        if (!isBusy(player) && isAirborne(player)) {
            StaminaManager.drainForAttack(player);
            return;
        }

        LivingEntity target = event.getTarget() instanceof LivingEntity l ? l : null;
        if (controller(player).onPrimaryDown(target)) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;
        if (!holdsAnimatedWeapon(player)) return;
        if (!isBusy(player) && isAirborne(player)) return; // ванильный взмах
        controller(player).onPrimaryDown(null);
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        // без изменений
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;

        WeaponCombatController c = CONTROLLERS.get(player.getUUID());
        boolean combatBusy = c != null && c.isBusy();

        if (!combatBusy && !BackstepHandler.isInputLocked()) return;

        Input input = event.getInput();
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.forwardImpulse = 0f;
        input.leftImpulse = 0f;
        input.jumping = false;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        WeaponCombatController c = CONTROLLERS.get(player.getUUID());
        boolean weapon = holdsAnimatedWeapon(player);

        // Ничего не делаем для руки/инструментов, если контроллер не занят
        if (c == null) {
            if (!weapon) return;
            c = controller(player);
        }

        if (!weapon && !c.isBusy()) {
            // Сменили оружие на руку/инструмент во время зажатия ЛКМ — сбрасываем,
            // чтобы не подавлять ванильный взмах
            if (c.suppressVanillaSwing()) c.reset();
            return;
        }

        long now = System.currentTimeMillis();

        if (!Minecraft.getInstance().options.keyAttack.isDown()) {
            c.onPrimaryUp();
        }
        c.tick(now);

        // Гасим ванильный взмах руки только при кастомной анимации
        if (c.suppressVanillaSwing()) {
            player.swinging = false;
            player.swingTime = 0;
            player.attackAnim = 0f;
            player.oAttackAnim = 0f;
        }
    }

    public static boolean isBusy(AbstractClientPlayer player) {
        WeaponCombatController c = CONTROLLERS.get(player.getUUID());
        return c != null && (c.isBusy() || c.suppressVanillaSwing());
    }

    private static WeaponCombatController controller(AbstractClientPlayer player) {
        WeaponCombatController existing = CONTROLLERS.get(player.getUUID());
        if (existing != null && existing.context().player == player) {
            return existing;
        }
        WeaponCombatController fresh = new WeaponCombatController(player);
        CONTROLLERS.put(player.getUUID(), fresh);
        return fresh;
    }
}