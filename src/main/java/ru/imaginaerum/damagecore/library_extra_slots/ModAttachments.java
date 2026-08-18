package ru.imaginaerum.damagecore.library_extra_slots;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {

    private ModAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "damagecore");

    public static final int EXTRA_SLOTS_COUNT = 3;

    // ИСПРАВЛЕНО: Автоматический .sync(...) убран из билдера.
    // Это предотвращает отправку пакетов до инициализации player.connection и полностью убирает ошибку "Неверные данные игрока".
    // Синхронизация теперь безопасно происходит через AccessorySlot.setChanged() и пакет обмена, когда игрок уже в мире.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ExtraSlotsHandler>> EXTRA_SLOTS =
            ATTACHMENT_TYPES.register("extra_shield_slots", () ->
                    AttachmentType.builder(ExtraSlotsHandler::new)
                            .serialize(ExtraSlotsHandlerSerializer.INSTANCE)
                            .copyOnDeath()
                            .build()
            );

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    public static class ExtraSlotsHandler extends ItemStackHandler {
        public ExtraSlotsHandler() {
            super(EXTRA_SLOTS_COUNT);
        }
    }

    private static final class ExtraSlotsHandlerSerializer
            implements IAttachmentSerializer<CompoundTag, ExtraSlotsHandler> {

        static final ExtraSlotsHandlerSerializer INSTANCE = new ExtraSlotsHandlerSerializer();

        @Override
        public ExtraSlotsHandler read(net.neoforged.neoforge.attachment.IAttachmentHolder holder,
                                      CompoundTag tag,
                                      HolderLookup.Provider provider) {
            ExtraSlotsHandler handler = new ExtraSlotsHandler();
            if (tag != null && !tag.isEmpty()) {
                handler.deserializeNBT(provider, tag);
            }
            return handler;
        }

        @Override
        public CompoundTag write(ExtraSlotsHandler attachment, HolderLookup.Provider provider) {
            return attachment.serializeNBT(provider);
        }
    }
}
