package ru.imaginaerum.damagecore.library_stats.attributes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

public class AttributeApplier {

    // ✅ ИСПРАВЛЕНО ДЛЯ 1.21.1: Вместо UUID теперь используется ResourceLocation
    private static final ResourceLocation LIVE_FORGE_RL =
            ResourceLocation.fromNamespaceAndPath(Damagecore_1_21_1_neo.MODID, "live_forge_bonus");

    public static void applyLiveForge(Player player, int level) {
        var attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;

        // ✅ ИСПРАВЛЕНО ДЛЯ 1.21.1: Метод принимает ResourceLocation вместо UUID
        attr.removeModifier(LIVE_FORGE_RL);

        if (level <= 0) return;

        // +1 HP за каждый уровень (1 единица = половина сердца)
        // ✅ ИСПРАВЛЕНО ДЛЯ 1.21.1: Новый конструктор и измененное имя операции ADD_VALUE
        attr.addPermanentModifier(new AttributeModifier(
                LIVE_FORGE_RL,
                (double) level / 2,
                AttributeModifier.Operation.ADD_VALUE
        ));
    }
}
