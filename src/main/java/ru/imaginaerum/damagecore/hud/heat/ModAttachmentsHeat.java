package ru.imaginaerum.damagecore.hud.heat;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

public class ModAttachmentsHeat {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Damagecore_1_21_1_neo.MODID);

    /**
     * Хранит текущий уровень накопленного жара у игрока.
     * serialize()  - значение сохраняется в NBT сущности при сохранении мира (persist).
     * sync()       - значение синхронизируется с клиентом (нужно для отрисовки HUD-оверлея).
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> HEAT =
            ATTACHMENT_TYPES.register("heat",
                    () -> AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT)
                            .sync(ByteBufCodecs.VAR_INT)
                            .build());
}
