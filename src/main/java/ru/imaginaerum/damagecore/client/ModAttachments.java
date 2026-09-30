package ru.imaginaerum.damagecore.client;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "damagecore");

    /** Текущий шаг книги: 1 - галактический текст, 2 - видение, 3 - заглушка. */
    public static final Supplier<AttachmentType<Integer>> TOME_STEP = ATTACHMENTS.register("tome_step",
            () -> AttachmentType.builder(() -> 1)
                    .serialize(Codec.INT)
                    .sync(ByteBufCodecs.VAR_INT)
                    .copyOnDeath()
                    .build());

    /** Игрок уже открывал книгу на шаге 1 (сообщение в чат показано). */
    public static final Supplier<AttachmentType<Boolean>> TOME_OPENED = ATTACHMENTS.register("tome_opened",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL)
                    .sync(ByteBufCodecs.BOOL)
                    .copyOnDeath()
                    .build());

    /** Игрок уже получил сообщение-видение на шаге 2. */
    public static final Supplier<AttachmentType<Boolean>> TOME_VISION_READ = ATTACHMENTS.register("tome_vision_read",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL)
                    .sync(ByteBufCodecs.BOOL)
                    .copyOnDeath()
                    .build());
    public static final Supplier<AttachmentType<Integer>> TOME_ELEMENTS = ATTACHMENTS.register("tome_elements",
            () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT)
                    .sync(ByteBufCodecs.VAR_INT)
                    .copyOnDeath()
                    .build());
}