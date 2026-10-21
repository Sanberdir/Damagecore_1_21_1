package ru.imaginaerum.damagecore.Init.fishing.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

/** Server -> client: current state of the fight (for the HUD and for freezing the camera). */
public record FightStatePayload(boolean active, float tension, float progress, float lateral, byte dash, byte warn)
        implements CustomPacketPayload {

    public static final Type<FightStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "fight_state"));

    public static final StreamCodec<ByteBuf, FightStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, FightStatePayload::active,
            ByteBufCodecs.FLOAT, FightStatePayload::tension,
            ByteBufCodecs.FLOAT, FightStatePayload::progress,
            ByteBufCodecs.FLOAT, FightStatePayload::lateral,
            ByteBufCodecs.BYTE, FightStatePayload::dash,
            ByteBufCodecs.BYTE, FightStatePayload::warn,
            FightStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
