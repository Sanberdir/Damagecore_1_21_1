package ru.imaginaerum.damagecore.libraty_effects;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import ru.imaginaerum.damagecore.Damagecore_1_21_1_neo;

import java.util.function.Supplier;

public class FoodProtectionAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Damagecore_1_21_1_neo.MODID);

    public static final Supplier<AttachmentType<FoodProtectionManager>> FOOD_PROTECTION_MANAGER =
            ATTACHMENT_TYPES.register("food_protection_manager",
                    () -> AttachmentType.builder(() -> new FoodProtectionManager(null))
                            .serialize(new IAttachmentSerializer<CompoundTag, FoodProtectionManager>() {
                                @Override
                                public FoodProtectionManager read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                                    FoodProtectionManager manager = new FoodProtectionManager(null);
                                    manager.load(tag, provider);
                                    return manager;
                                }

                                @Override
                                public CompoundTag write(FoodProtectionManager manager, HolderLookup.Provider provider) {
                                    return manager.save(provider);
                                }
                            })
                            .build());
}