package ru.imaginaerum.damagecore.Init.blocks.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.imaginaerum.damagecore.Init.blocks.entity.StructureBlockEntity;

import java.util.List;

public class BlockConfigNetwork {

    private static final StreamCodec<FriendlyByteBuf, List<String>> STRINGS =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(256))
                    .mapStream(buf -> buf);

    // сервер -> клиент: открыть экран с текущим выбором
    public record OpenConfig(BlockPos pos, List<String> entityIds, List<String> blockIds)
            implements CustomPacketPayload {
        public static final Type<OpenConfig> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath("damagecore", "open_block_config"));
        public static final StreamCodec<FriendlyByteBuf, OpenConfig> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, OpenConfig::pos,
                STRINGS, OpenConfig::entityIds,
                STRINGS, OpenConfig::blockIds,
                OpenConfig::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // клиент -> сервер: сохранить выбор
    public record SaveConfig(BlockPos pos, List<String> entityIds, List<String> blockIds)
            implements CustomPacketPayload {
        public static final Type<SaveConfig> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath("damagecore", "save_block_config"));
        public static final StreamCodec<FriendlyByteBuf, SaveConfig> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, SaveConfig::pos,
                STRINGS, SaveConfig::entityIds,
                STRINGS, SaveConfig::blockIds,
                SaveConfig::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void onSave(SaveConfig msg, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp)) return;
        if (sp.distanceToSqr(msg.pos().getCenter()) > 64) return;          // античит
        if (!(sp.level().getBlockEntity(msg.pos()) instanceof StructureBlockEntity be)) return;

        be.setSelection(clean(msg.entityIds(), true), clean(msg.blockIds(), false));
    }

    private static List<String> clean(List<String> ids, boolean entity) {
        return ids.stream().distinct().filter(id -> valid(id, entity)).toList();
    }

    private static boolean valid(String id, boolean entity) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return false;
        return entity ? BuiltInRegistries.ENTITY_TYPE.containsKey(rl)
                : BuiltInRegistries.BLOCK.containsKey(rl);
    }
}