package ru.imaginaerum.damagecore.api.skill_tree.implementation_skills.blocking;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeServerHandler;

@EventBusSubscriber
public class CorneredEvent {

    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        // 1. Проверяем, что заблокировал именно игрок на сервере
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Проверка ноды талантов
        if (!SkillTreeServerHandler.isNodeLearned(player, "cornered")) {
            return;
        }

        ItemStack shield = player.getUseItem();
        if (shield.isEmpty() || !(shield.getItem() instanceof ShieldItem)) {
            return;
        }

        // Запоминаем состояние щита ДО того, как игра нанесёт ему урон от блока
        int damageBeforeBlock = shield.getDamageValue();
        int maxDamage = shield.getMaxDamage();

        // Запускаем проверку на следующем тике — после того, как игра обработает прочность щита
        if (player.getServer() != null) {
            player.getServer().tell(new net.minecraft.server.TickTask(
                    player.getServer().getTickCount() + 1,
                    () -> {
                        // Проверяем, жив ли еще игрок
                        if (!player.isAlive()) return;

                        ItemStack currentItem = player.getUseItem();

                        boolean shieldBroke =
                                // Щит пропал из руки (сломался и исчез)
                                currentItem.isEmpty()
                                        // Или durability достиг максимума
                                        || (currentItem.getItem() instanceof ShieldItem
                                        && currentItem.getDamageValue() >= maxDamage)
                                        // Или до блока оставалось мало прочности и теперь в руке не щит
                                        || (damageBeforeBlock < maxDamage
                                        && !(currentItem.getItem() instanceof ShieldItem));

                        if (shieldBroke) {
                            // В 1.21.1 эффекты лежат в Holder, .value() гарантирует получение MobEffect
                            player.addEffect(new MobEffectInstance(
                                    (Holder<MobEffect>) MobEffects.DAMAGE_BOOST.value(),
                                    20 * 20, // 20 секунд (в тиках)
                                    1        // Уровень эффекта II (0 = I)
                            ));
                        }
                    }
            ));
        }
    }
}
