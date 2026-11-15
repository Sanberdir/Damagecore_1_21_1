package ru.imaginaerum.damagecore.events;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

@EventBusSubscriber(modid = Damagecore_1_21_1_neo.MODID)
public class ChannelingEvents {

    private static boolean isMeleeWeapon(ItemStack s) {
        return s.is(ItemTags.SWORDS) || s.is(ItemTags.AXES);
    }

    // 1. Эффект при ударе
    @SubscribeEvent
    public static void onHit(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        if (event.getSource().getDirectEntity() != attacker) return; // только ближний бой
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide) return;

        ItemStack stack = attacker.getMainHandItem();
        if (!isMeleeWeapon(stack)) return;
        Holder<Enchantment> channeling = level.registryAccess()
                .holderOrThrow(Enchantments.CHANNELING);
        if (EnchantmentHelper.getItemEnchantmentLevel(channeling, stack) <= 0) return;

        // урон молнией
        victim.invulnerableTime = 0;
        victim.hurt(level.damageSources().lightningBolt(), 3.0F);

        // молния в грозу под открытым небом
        if (level.isThundering()
                && level.canSeeSky(victim.blockPosition())
                && level.random.nextFloat() < 0.3F) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(Vec3.atBottomCenterOf(victim.blockPosition()));
                if (attacker instanceof ServerPlayer sp) bolt.setCause(sp);
                level.addFreshEntity(bolt);
            }
        }
    }

    // 2. Наковальня: книга Channeling -> меч/топор
    @SubscribeEvent
    public static void onAnvil(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (!isMeleeWeapon(left) || !right.is(Items.ENCHANTED_BOOK)) return;

        Holder<Enchantment> channeling = event.getPlayer().level().registryAccess()
                .holderOrThrow(Enchantments.CHANNELING);
        ItemEnchantments stored = right.getOrDefault(
                DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (stored.getLevel(channeling) <= 0) return;
        if (EnchantmentHelper.getTagEnchantmentLevel(channeling, left) > 0) return;

        ItemStack out = left.copy();
        EnchantmentHelper.updateEnchantments(out, mutable -> {
            for (var e : stored.entrySet()) {
                Holder<Enchantment> h = e.getKey();
                // переносим Channeling и всё, что и так подходит предмету
                if (h.equals(channeling) || h.value().isSupportedItem(out)) {
                    mutable.upgrade(h, e.getIntValue());
                }
            }
        });
        event.setOutput(out);
        event.setCost(8);
        event.setMaterialCost(1);
    }
}