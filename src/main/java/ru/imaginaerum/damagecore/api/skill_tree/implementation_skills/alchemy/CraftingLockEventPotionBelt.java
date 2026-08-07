package ru.imaginaerum.damagecore.api.skill_tree.implementation_skills.alchemy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import ru.imaginaerum.damagecore.api.skill_tree.SkillTreeServerHandler;

@EventBusSubscriber
public class CraftingLockEventPotionBelt {
    // В 1.21.1 для создания ResourceLocation используется метод fromNamespaceAndPath
    private static final ResourceLocation POTION_BAG =
            ResourceLocation.fromNamespaceAndPath("damagecore", "potion_bag");

    private static final ResourceLocation POTION_BELT =
            ResourceLocation.fromNamespaceAndPath("damagecore", "potion_belt");

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        // В 1.21+ вместо event.player используется event.getEntity()
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (!(player.containerMenu instanceof CraftingMenu menu)) return;

        boolean learned = SkillTreeServerHandler.isNodeLearned(player, "alchemy_belt");
        if (learned) return;

        Slot resultSlot = menu.slots.get(0);
        ItemStack result = resultSlot.getItem();
        if (result.isEmpty()) return;

        // В 1.21.1 ID предмета безопасно извлекается через встроенный реестр BuiltInRegistries.ITEM
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(result.getItem());

        if (POTION_BAG.equals(id) || POTION_BELT.equals(id)) {
            resultSlot.set(ItemStack.EMPTY);
            menu.broadcastChanges();
        }
    }
}
