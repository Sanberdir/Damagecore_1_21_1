package ru.imaginaerum.damagecore.libraty_effects;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

import java.util.function.Supplier;

public class FoodProtectionAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Damagecore_1_21_1_neo.MODID);

    // Один объект FoodProtectionManager будет создан ОДИН РАЗ на игрока
    // и закеширован NeoForge'ом — при повторных вызовах getData() будет
    // возвращаться ТОТ ЖЕ инстанс, а не новый.
    public static final Supplier<AttachmentType<FoodProtectionManager>> FOOD_PROTECTION_MANAGER =
            ATTACHMENT_TYPES.register("food_protection_manager",
                    () -> AttachmentType.builder(() -> new FoodProtectionManager(null)).build());
}