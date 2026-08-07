package ru.imaginaerum.damagecore.api.skill_tree.implementation_skills.alchemy;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeNode;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeServerHandler;

@EventBusSubscriber
public class AdeptInsightEvent {

    // Вариант 0: усиление лечебного зелья
    @SubscribeEvent
    public static void onPlayerHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        SkillTreeNode node = SkillTreeServerHandler.getNodeForPlayer(player, "adept_insight");
        if (node == null || node.selectedOption != 0) return;

        ItemStack stack = player.getUseItem();
        if (!stack.isEmpty() && stack.is(Items.POTION)) {
            // В 1.21.1 получаем данные зелья через компонент POTION_CONTENTS
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null && contents.potion().isPresent()) {
                // Сравниваем тип зелья напрямую через Holder ванильного реестра
                if (contents.potion().get().is(Potions.HEALING)) {
                    event.setAmount(event.getAmount() * 2f);
                }
            }
        }
    }

    // Вариант 1: усиление урона от взрывного/вредоносного зелья
    @SubscribeEvent
    public static void onPlayerHurt(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        SkillTreeNode node = SkillTreeServerHandler.getNodeForPlayer(player, "adept_insight");
        if (node == null || node.selectedOption != 1) return;

        ItemStack stack = player.getUseItem();
        if (!stack.isEmpty() && stack.is(Items.POTION)) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null && contents.potion().isPresent()) {
                if (contents.potion().get().is(Potions.HARMING)) {
                    // В 1.21.1 изменение урона в Pre ивенте происходит через setNewDamage
                    event.setNewDamage(event.getNewDamage() * 2f);
                }
            }
        }
    }

    // Вариант 2: удвоение длительности полезных эффектов
    // Вариант 2: удвоение длительности полезных эффектов
    @SubscribeEvent
    public static void onPotionFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        SkillTreeNode node = SkillTreeServerHandler.getNodeForPlayer(player, "adept_insight");
        if (node == null || node.selectedOption != 2) return;

        ItemStack stack = event.getItem();

        if (!stack.isEmpty() && stack.has(DataComponents.POTION_CONTENTS)) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents == null) return;

            // В 1.21.1 вместо getEffects() используется метод getAllEffects()
            for (MobEffectInstance effect : contents.getAllEffects()) {
                if (effect.getDuration() <= 0) continue;

                // Извлекаем чистый MobEffect из Holder для проверки категории
                if (effect.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL) continue;

                // В конструктор передается Holder<MobEffect>
                MobEffectInstance extended = new MobEffectInstance(
                        effect.getEffect(),
                        effect.getDuration() * 2,
                        effect.getAmplifier(),
                        effect.isAmbient(),
                        effect.isVisible(),
                        effect.showIcon()
                );

                player.addEffect(extended);
            }
        }
    }

}
