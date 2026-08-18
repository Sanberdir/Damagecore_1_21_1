package ru.imaginaerum.damagecore.mixin.keys_left_hand;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.library_extra_slots.AccessorySlot;
import ru.imaginaerum.damagecore.library_extra_slots.ModAttachments;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {

    @Unique private static final int DAMAGECORE_SLOT_X = 77;
    @Unique private static final int DAMAGECORE_SLOT_Y_BASE = 62;
    @Unique private static final int DAMAGECORE_SLOT_STEP = 18;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void damagecore$addExtraSlots(Inventory inventory, boolean isCrafting, Player owner, CallbackInfo ci) {
        if (!ModAttachments.EXTRA_SLOTS.isBound()) return;

        AbstractContainerMenuInvoker invoker = (AbstractContainerMenuInvoker) (Object) this;

        // ИСПРАВЛЕНО: Получаем хендлер через контролируемый метод,
        // гарантирующий совпадение экземпляров между инвентарем и системой сохранения диска.
        ModAttachments.ExtraSlotsHandler handler = damagecore$getValidHandler(owner);

        for (int i = 0; i < ModAttachments.EXTRA_SLOTS_COUNT; i++) {
            int slotY = DAMAGECORE_SLOT_Y_BASE - DAMAGECORE_SLOT_STEP * (i + 1);
            invoker.damagecore$addSlot(new AccessorySlot(handler, i, DAMAGECORE_SLOT_X, slotY, owner));
        }
    }

    @Unique
    private static ModAttachments.ExtraSlotsHandler damagecore$getValidHandler(Player owner) {
        // Если данные уже жестко сидят в атрибутах — берем их
        if (owner.hasData(ModAttachments.EXTRA_SLOTS)) {
            return owner.getData(ModAttachments.EXTRA_SLOTS);
        }

        // Если это серверный игрок, мы обязаны жестко привязать экземпляр данных,
        // чтобы он синхронизировался с NBT-логикой диска. На сервере это не вызывает краш Netty.
        if (owner instanceof ServerPlayer serverPlayer) {
            ModAttachments.ExtraSlotsHandler handler = new ModAttachments.ExtraSlotsHandler();
            serverPlayer.setData(ModAttachments.EXTRA_SLOTS, handler);
            return handler;
        }

        // На клиенте просто лениво запрашиваем данные без принудительной перезаписи
        return owner.getData(ModAttachments.EXTRA_SLOTS);
    }
}
