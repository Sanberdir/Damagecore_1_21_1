package ru.imaginaerum.damagecore.library_enchantments;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.RawAnimation;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.bones.AdvancedPlayerAnimBone;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.enums.PlayState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.joml.Vector3f;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;
import ru.imaginaerum.damagecore.library_damage.DamageContext;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.ModDamageTypes;
import ru.imaginaerum.damagecore.library_damage.TypedDamageSource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HigherEnchantFireDash {
    public static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "fire_dash");
    public static final ResourceLocation PREPARE_ANIMATION = ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "fire_charge_1");
    public static final ResourceLocation DASH_ANIMATION = ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "fire_charge_2");

    public static final int DASH_DISTANCE_BLOCKS = 6;
    public static final int DASH_TICKS = 10;
    public static final float SPIN_START_TICK = 10.0F;
    public static final float CONE_TICK = 15.0F;
    public static final float CONE_ANGLE_DEGREES = 90.0F;
    public static final double CONE_RANGE = 4.0D;
    public static final double SPIN_RADIUS = 3.0D;
    public static final double DASH_HIT_RADIUS = 1.1D;
    public static final double SERVER_HIT_RANGE = 8.0D;
    public static final int COOLDOWN_TICKS = 40;

    private static final int SERVER_COOLDOWN_TICKS = 73;
    private static final Map<UUID, Long> SERVER_COOLDOWN = new ConcurrentHashMap<>();

    public enum HitKind {
        DASH(List.of(DamageType.PIERCING, DamageType.FIRE), Map.of(DamageType.PIERCING, 2.0F, DamageType.FIRE, 10.0F)),
        SPIN(List.of(DamageType.FIRE), Map.of(DamageType.FIRE, 6.0F)),
        CONE(List.of(DamageType.SLASHING, DamageType.FIRE), Map.of(DamageType.SLASHING, 4.0F, DamageType.FIRE, 6.0F));

        private final List<DamageType> types;
        private final Map<DamageType, Float> amounts;

        HitKind(List<DamageType> types, Map<DamageType, Float> amounts) {
            this.types = types;
            this.amounts = amounts;
        }

        public List<DamageType> types() {
            return types;
        }

        public Map<DamageType, Float> amounts() {
            return amounts;
        }

        public float total() {
            float total = 0.0F;
            for (float amount : amounts.values()) {
                total += amount;
            }
            return total;
        }
    }

    public record FireDashStart() implements CustomPacketPayload {
        public static final FireDashStart INSTANCE = new FireDashStart();

        public static final Type<FireDashStart> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "fire_dash_start"));

        public static final StreamCodec<FriendlyByteBuf, FireDashStart> STREAM_CODEC = StreamCodec.of((buf, packet) -> {}, buf -> INSTANCE);

        @Override public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record FireDashSync(int entityId) implements CustomPacketPayload {
        public static final Type<FireDashSync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "fire_dash_sync"));

        public static final StreamCodec<FriendlyByteBuf, FireDashSync> STREAM_CODEC =
                StreamCodec.of(
                        (buf, packet) -> buf.writeVarInt(packet.entityId()),
                        buf -> new FireDashSync(buf.readVarInt())
                );

        @Override public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record FireDashHit(int targetId, int kind) implements CustomPacketPayload {
        public static final Type<FireDashHit> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "fire_dash_hit"));

        public static final StreamCodec<FriendlyByteBuf, FireDashHit> STREAM_CODEC =
                StreamCodec.of(
                        (buf, packet) -> {
                            buf.writeVarInt(packet.targetId());
                            buf.writeByte(packet.kind());
                        },
                        buf -> new FireDashHit(buf.readVarInt(), buf.readUnsignedByte())
                );

        @Override public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static final class FireDashDamageSource extends TypedDamageSource {
        FireDashDamageSource(Holder<net.minecraft.world.damagesource.DamageType> vanillaType, List<DamageType> damageCoreTypes, Entity attacker) {
            super(vanillaType, damageCoreTypes, attacker);
        }

        @Override public Entity getDirectEntity() {
            return null;
        }

        @Override public Vec3 getSourcePosition() {
            Entity attacker = getEntity();
            return attacker != null ? attacker.position() : null;
        }
    }

    @EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID) public static final class Network {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
            PayloadRegistrar registrar = event.registrar("1");
            registrar.playToServer(FireDashStart.TYPE, FireDashStart.STREAM_CODEC, Network::handleStart);
            registrar.playToServer(FireDashHit.TYPE, FireDashHit.STREAM_CODEC, Network::handleHit);
            registrar.playToClient(FireDashSync.TYPE, FireDashSync.STREAM_CODEC, Network::handleSync);
        }

        private static void handleStart(FireDashStart payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) return;
                if (!player.isAlive() || player.isSpectator()) return;
                ItemStack stack = player.getMainHandItem();
                if (stack.isEmpty() || !HigherEnchants.has(stack, HigherEnchant.FIRE_DASH)) return;

                long now = player.serverLevel().getGameTime();
                Long until = SERVER_COOLDOWN.get(player.getUUID());
                if (until != null && now < until) return;
                SERVER_COOLDOWN.put(player.getUUID(), now + SERVER_COOLDOWN_TICKS);

                FireDashSync sync = new FireDashSync(player.getId());
                for (ServerPlayer other : player.serverLevel().players()) {
                    if (other != player) {
                        PacketDistributor.sendToPlayer(other, sync);
                    }
                }
            });
        }

        private static void handleHit(FireDashHit payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) return;
                if (!player.isAlive() || player.isSpectator()) return;
                ItemStack stack = player.getMainHandItem();
                if (stack.isEmpty() || !HigherEnchants.has(stack, HigherEnchant.FIRE_DASH)) return;

                HitKind[] kinds = HitKind.values();
                if (payload.kind() < 0 || payload.kind() >= kinds.length) return;
                HitKind kind = kinds[payload.kind()];

                Entity entity = player.level().getEntity(payload.targetId());
                if (!(entity instanceof LivingEntity target)) return;
                if (target == player || !target.isAlive()) return;
                if (player.distanceToSqr(target) > SERVER_HIT_RANGE * SERVER_HIT_RANGE) return;

                var registry = player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
                var holder = registry.getHolderOrThrow(ModDamageTypes.TYPED_ATTACK);

                for (var entry : kind.amounts().entrySet()) {
                    DamageContext.add(target, entry.getKey(), entry.getValue());
                }

                target.hurt(new FireDashDamageSource(holder, kind.types(), player), kind.total());
                DamageContext.clear(target);
            });
        }

        private static void handleSync(FireDashSync payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (FMLEnvironment.dist != Dist.CLIENT) return;
                Client.handleSync(payload);
            });
        }
    }

    @EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID, value = Dist.CLIENT)
    public static final class Client {

        private static final Map<Integer, DashState> ACTIVE = new HashMap<>();
        private static final String[] ROTATABLE_BONES = {
                "body", "right_arm", "left_arm", "right_leg", "left_leg", "head", "torso",
                "right_item", "left_item"
        };

        @SubscribeEvent public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                    LAYER_ID,
                    2000,
                    player -> {
                        PlayerAnimationController controller = new PlayerAnimationController(
                                player,
                                (animationController, state, animationSetter) -> PlayState.STOP
                        );
                        controller.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
                        controller.setFirstPersonConfiguration(new FirstPersonConfiguration()
                                .setShowRightItem(true)
                                .setShowLeftItem(true));
                        return controller;
                    }
            ));
        }

        @SubscribeEvent public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
            if (event.getHand() != InteractionHand.MAIN_HAND) return;
            if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;

            Minecraft minecraft = Minecraft.getInstance();
            if (player != minecraft.player) return;
            if (event.isCanceled()) return;
            if (player.isSpectator()) return;

            ItemStack stack = player.getMainHandItem();
            if (stack.isEmpty() || !HigherEnchants.has(stack, HigherEnchant.FIRE_DASH)) return;
            if (player.getCooldowns().isOnCooldown(stack.getItem())) return;
            if (ACTIVE.containsKey(player.getId())) return;

            PlayerAnimationController controller = controller(player);
            if (controller == null) return;

            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            controller.triggerAnimation(PREPARE_ANIMATION);
            ACTIVE.put(player.getId(), new DashState(player, true, stack.getItem()));
            PacketDistributor.sendToServer(FireDashStart.INSTANCE);
        }

        @SubscribeEvent public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null) {
                ACTIVE.clear();
                return;
            }
            if (minecraft.isPaused()) return;

            Iterator<Map.Entry<Integer, DashState>> iterator = ACTIVE.entrySet().iterator();
            while (iterator.hasNext()) {
                DashState state = iterator.next().getValue();
                AbstractClientPlayer player = state.player;

                if (player.isRemoved() || !player.isAlive() || player.level() != minecraft.level) {
                    PlayerAnimationController stopped = controller(player);
                    if (stopped != null) {
                        stopped.stop();
                    }
                    iterator.remove();
                    continue;
                }

                PlayerAnimationController controller = controller(player);
                if (controller == null) {
                    iterator.remove();
                    continue;
                }

                state.age++;

                if (state.phase == 1) {
                    if (controller.getAnimationTicks() >= length(PREPARE_ANIMATION) - 0.5F) {
                        state.phase = 2;
                        controller.triggerAnimation(DASH_ANIMATION);

                        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
                        double speed = DASH_DISTANCE_BLOCKS / (double) DASH_TICKS;
                        state.dashX = -Mth.sin(yaw) * speed;
                        state.dashZ = Mth.cos(yaw) * speed;
                        state.dashTicks = 0;
                    }
                    continue;
                }

                float animationTick = controller.getAnimationTicks();

                if (state.dashTicks < DASH_TICKS && animationTick < SPIN_START_TICK) {
                    Vec3 motion = player.getDeltaMovement();
                    player.setDeltaMovement(state.dashX, motion.y, state.dashZ);
                    player.hasImpulse = true;
                    state.dashTicks++;
                    if (state.dealer) {
                        dashHit(player, state);
                    }
                }

                if (animationTick >= SPIN_START_TICK && animationTick < CONE_TICK) {
                    if (state.dealer) {
                        spinHit(player, state);
                    }
                    swordTrail(player, controller, state);
                }

                if (animationTick >= CONE_TICK && !state.coneDone) {
                    state.coneDone = true;
                    if (state.dealer) {
                        coneHit(player);
                    }
                }

                if (animationTick >= length(DASH_ANIMATION) - 0.5F || state.age > 60) {
                    fadeOut(controller);
                    if (state.dealer) {
                        player.getCooldowns().addCooldown(state.startItem, COOLDOWN_TICKS);
                    }
                    iterator.remove();
                }
            }

            aura(minecraft);
        }

        public static void handleSync(FireDashSync payload) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null) return;

            Entity entity = minecraft.level.getEntity(payload.entityId());
            if (!(entity instanceof AbstractClientPlayer player)) return;
            if (player == minecraft.player) return;
            if (ACTIVE.containsKey(player.getId())) return;

            PlayerAnimationController controller = controller(player);
            if (controller == null) return;

            controller.triggerAnimation(PREPARE_ANIMATION);
            ACTIVE.put(player.getId(), new DashState(player, false, ItemStack.EMPTY.getItem()));
        }

        private static PlayerAnimationController controller(AbstractClientPlayer player) {
            var layer = PlayerAnimationAccess.getPlayerAnimationLayer(player, LAYER_ID);
            return layer instanceof PlayerAnimationController animationController ? animationController : null;
        }

        private static float length(ResourceLocation animationId) {
            Animation animation = PlayerAnimResources.getAnimation(animationId);
            return animation != null ? animation.length() : 20.0F;
        }

        private static void dashHit(AbstractClientPlayer player, DashState state) {
            AABB area = player.getBoundingBox().inflate(DASH_HIT_RADIUS, 0.5D, DASH_HIT_RADIUS);
            for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area)) {
                if (target == player || !target.isAlive()) continue;
                if (state.dashHits.add(target.getId())) {
                    PacketDistributor.sendToServer(new FireDashHit(target.getId(), HitKind.DASH.ordinal()));
                }
            }
        }

        private static void spinHit(AbstractClientPlayer player, DashState state) {
            AABB area = player.getBoundingBox().inflate(SPIN_RADIUS, 1.0D, SPIN_RADIUS);
            for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area)) {
                if (target == player || !target.isAlive()) continue;
                double dx = target.getX() - player.getX();
                double dz = target.getZ() - player.getZ();
                if (dx * dx + dz * dz > SPIN_RADIUS * SPIN_RADIUS) continue;
                if (state.spinHits.add(target.getId())) {
                    PacketDistributor.sendToServer(new FireDashHit(target.getId(), HitKind.SPIN.ordinal()));
                }
            }
        }

        private static void coneHit(AbstractClientPlayer player) {
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            double threshold = Math.cos(Math.toRadians(CONE_ANGLE_DEGREES * 0.5D));

            AABB area = player.getBoundingBox().inflate(CONE_RANGE, 1.0D, CONE_RANGE);
            for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area)) {
                if (target == player || !target.isAlive()) continue;

                Vec3 offset = target.getBoundingBox().getCenter().subtract(eye);
                double distance = offset.length();
                if (distance < 0.01D || distance > CONE_RANGE) continue;
                if (look.dot(offset.scale(1.0D / distance)) < threshold) continue;

                PacketDistributor.sendToServer(new FireDashHit(target.getId(), HitKind.CONE.ordinal()));
            }
        }

        private static void swordTrail(AbstractClientPlayer player, PlayerAnimationController controller, DashState state) {
            Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            PoseStack pose = controller.getBoneWorldPositionPoseStack("right_item", 1.0F, camera);
            if (pose == null) {
                pose = controller.getBoneWorldPositionPoseStack("right_arm", 1.0F, camera);
            }
            if (pose == null) return;

            Vector3f translation = pose.last().pose().getTranslation(new Vector3f());
            Vec3 position = new Vec3(translation.x, translation.y, translation.z).add(camera);

            if (state.lastTrail != null) {
                for (int i = 1; i <= 2; i++) {
                    Vec3 point = state.lastTrail.lerp(position, i / 3.0D);
                    spawnFlame(player, point, 0.025D);
                }
            }

            spawnFlame(player, position, 0.035D);
            state.lastTrail = position;
        }

        private static void spawnFlame(AbstractClientPlayer player, Vec3 position, double spread) {
            var random = player.getRandom();
            player.level().addParticle(
                    ParticleTypes.FLAME,
                    position.x + (random.nextDouble() - 0.5D) * 0.08D,
                    position.y + (random.nextDouble() - 0.5D) * 0.08D,
                    position.z + (random.nextDouble() - 0.5D) * 0.08D,
                    (random.nextDouble() - 0.5D) * spread,
                    (random.nextDouble() - 0.5D) * spread,
                    (random.nextDouble() - 0.5D) * spread
            );
        }

        private static void aura(Minecraft minecraft) {
            if (minecraft.level.getGameTime() % 2L != 0L) return;

            for (AbstractClientPlayer player : minecraft.level.players()) {
                if (!player.isAlive() || player.isSpectator()) continue;
                ItemStack stack = player.getMainHandItem();
                if (stack.isEmpty() || !HigherEnchants.has(stack, HigherEnchant.FIRE_DASH)) continue;
                spawnAura(player);
            }
        }

        private static void spawnAura(AbstractClientPlayer player) {
            var random = player.getRandom();
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = 0.55D + random.nextDouble() * 0.4D;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + 0.15D + random.nextDouble() * 1.7D;

            player.level().addParticle(ParticleTypes.FLAME, x, y, z, 0.0D, 0.01D, 0.0D);
            if (random.nextInt(3) == 0) {
                player.level().addParticle(ParticleTypes.SMOKE, x, y + 0.15D, z, 0.0D, 0.02D, 0.0D);
            }
        }

        private static void fadeOut(PlayerAnimationController controller) {
            for (String boneName : ROTATABLE_BONES) {
                AdvancedPlayerAnimBone bone = controller.getBone(boneName);
                if (bone == null) continue;
                bone.setRotX(wrapRadians(bone.getRotX()));
                bone.setRotY(wrapRadians(bone.getRotY()));
                bone.setRotZ(wrapRadians(bone.getRotZ()));
            }

            RawAnimation waitAnimation = RawAnimation.begin().thenWait(28);
            controller.replaceAnimationWithFade(
                    AbstractFadeModifier.standardFadeIn(8, EasingType.EASE_IN_OUT_SINE),
                    waitAnimation
            );
        }

        private static float wrapRadians(float radians) {
            float twoPi = (float) (Math.PI * 2.0D);
            float value = radians % twoPi;
            if (value >= Math.PI) {
                value -= twoPi;
            } else if (value < -Math.PI) {
                value += twoPi;
            }
            return value;
        }

        private static final class DashState {
            final AbstractClientPlayer player;
            final boolean dealer;
            final Item startItem;
            final Set<Integer> dashHits = new HashSet<>();
            final Set<Integer> spinHits = new HashSet<>();
            int phase = 1;
            int age;
            int dashTicks;
            double dashX;
            double dashZ;
            boolean coneDone;
            Vec3 lastTrail;

            DashState(AbstractClientPlayer player, boolean dealer, Item startItem) {
                this.player = player;
                this.dealer = dealer;
                this.startItem = startItem;
            }
        }
    }
}
