package ru.imaginaerum.damagecore.structure_processors;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import ru.imaginaerum.damagecore.client.TomeEvents;
import ru.imaginaerum.damagecore.client.TomeLocator;

@EventBusSubscriber(modid = "damagecore", bus = EventBusSubscriber.Bus.MOD)
public class RescueNetwork {

    // сервер -> клиент: открыть экран
    public record OpenScreen(int entityId) implements CustomPacketPayload {
        public static final Type<OpenScreen> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath("damagecore", "open_rescue_screen"));
        public static final StreamCodec<FriendlyByteBuf, OpenScreen> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, OpenScreen::entityId, OpenScreen::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // клиент -> сервер: команда
    public record Command(int entityId, boolean follow) implements CustomPacketPayload {
        public static final Type<Command> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath("damagecore", "rescue_command"));
        public static final StreamCodec<FriendlyByteBuf, Command> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Command::entityId,
                ByteBufCodecs.BOOL, Command::follow,
                Command::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToClient(OpenScreen.TYPE, OpenScreen.CODEC,
                (p, ctx) -> RescueClient.open(p.entityId()));
        r.playToServer(Command.TYPE, Command.CODEC, RescueNetwork::onCommand);
        r.playToServer(ToggleCompass.TYPE, ToggleCompass.CODEC,
                (p, ctx) -> {
                    if (ctx.player() instanceof ServerPlayer sp) TomeLocator.toggle(sp);
                });
    }
    public record ToggleCompass() implements CustomPacketPayload {
        public static final Type<ToggleCompass> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath("damagecore", "toggle_tome_compass"));
        public static final StreamCodec<FriendlyByteBuf, ToggleCompass> CODEC =
                StreamCodec.unit(new ToggleCompass());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    private static void onCommand(Command cmd, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp)) return;
        Entity e = sp.level().getEntity(cmd.entityId());
        if (!(e instanceof Villager v)) return;
        if (!v.getTags().contains(RescuableVillager.TAG)) return; // уже обычный житель
        if (sp.distanceToSqr(v) > 64) return;                     // античит: не дальше 8 блоков

        if (cmd.follow()) {
            v.addTag(RescuableVillager.FOLLOW);
            v.getPersistentData().putUUID(RescuableVillager.OWNER, sp.getUUID());
        } else {
            v.removeTag(RescuableVillager.FOLLOW);
            v.getNavigation().stop();
        }
        TomeEvents.markRescuer(v, sp);
    }
}