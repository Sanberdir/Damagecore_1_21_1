package ru.imaginaerum.damagecore.library_enchantments;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.IntFunction;
import java.util.function.Supplier;

public enum HigherEnchant implements StringRepresentable {

    FIRE_DASH("fire_dash", Element.FIRE, 5, () -> Items.FIRE_CHARGE);

    public static final Codec<HigherEnchant> CODEC = StringRepresentable.fromEnum(HigherEnchant::values);
    public static final IntFunction<HigherEnchant> BY_ID =
            ByIdMap.continuous(HigherEnchant::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, HigherEnchant> STREAM_CODEC =
            ByteBufCodecs.idMapper(BY_ID, HigherEnchant::ordinal);

    private final String name;
    private final Element element;
    private final int levelCost;

    private final Supplier<Item> icon;

    HigherEnchant(String name, Element element, int levelCost, Supplier<Item> icon) {
        this.name = name;
        this.element = element;
        this.levelCost = levelCost;
        this.icon = icon;
    }
    public Item getIconItem() { return icon.get(); }
    @Override public String getSerializedName() { return name; }
    public Element getElement() { return element; }
    public int getLevelCost() { return levelCost; }

    public String getTranslationKey() { return "higher_enchant." + Element.MOD_ID + "." + name; }

    public MutableComponent getDisplayName() {
        return Component.translatable(getTranslationKey())
                .withStyle(Style.EMPTY.withColor(element.getColor()));
    }

    public MutableComponent getDescription() {
        return Component.translatable(getTranslationKey() + ".desc");
    }
}