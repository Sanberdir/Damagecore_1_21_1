package ru.imaginaerum.damagecore.library_enchantments;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class DCDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Element.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<HigherEnchant>>> HIGHER_ENCHANTS =
            COMPONENTS.register("higher_enchants", () -> DataComponentType.<List<HigherEnchant>>builder()
                    .persistent(HigherEnchant.CODEC.listOf())
                    .networkSynchronized(HigherEnchant.STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build());

    private DCDataComponents() {}
}