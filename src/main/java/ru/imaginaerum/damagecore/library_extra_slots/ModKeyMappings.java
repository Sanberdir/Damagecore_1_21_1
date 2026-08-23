package ru.imaginaerum.damagecore.library_extra_slots;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

public final class ModKeyMappings {

    private ModKeyMappings() {}

    public static final String CATEGORY = "key.categories.damagecore";

    // Клавиша по умолчанию — F. Если конфликтует, смените на свободную,
    // либо оставьте InputConstants.UNKNOWN.getValue() (тогда клавиша будет "не привязана").
    public static final KeyMapping USE_ACCESSORY_SLOT = new KeyMapping(
            "key.damagecore.combat_mode",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F,
            CATEGORY
    );
}