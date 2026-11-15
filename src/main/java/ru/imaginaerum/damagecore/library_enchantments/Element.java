package ru.imaginaerum.damagecore.library_enchantments;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.IntFunction;

/**
 * Стихии: земля, вода, огонь, воздух, свет, тьма и эфир.
 * <p>
 * Поддерживает Codec (JSON/NBT/datapack) и StreamCodec (сеть) для 1.21.1.
 * Замените MOD_ID на id вашего мода.
 */
public enum Element implements StringRepresentable {

    EARTH("earth", 0x8B5A2B, Category.BASIC),
    WATER("water", 0x3F76E4, Category.BASIC),
    FIRE("fire", 0xFF5A1F, Category.BASIC),
    AIR("air", 0xCFEFFF, Category.BASIC),
    LIGHT("light", 0xFFF3A0, Category.CELESTIAL),
    DARK("dark", 0x2B1A3D, Category.CELESTIAL),
    AETHER("aether", 0xC77DFF, Category.SUPREME);

    public static final String MOD_ID = "damagecore";

    /** Для JSON, NBT, датапаков, data components. */
    public static final Codec<Element> CODEC = StringRepresentable.fromEnum(Element::values);

    /** Для сетевых пакетов и data components. */
    public static final IntFunction<Element> BY_ID =
            ByIdMap.continuous(Element::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, Element> STREAM_CODEC =
            ByteBufCodecs.idMapper(BY_ID, Element::getId);

    private final String name;
    private final int color;
    private final Category category;

    Element(String name, int color, Category category) {
        this.name = name;
        this.color = color;
        this.category = category;
    }

    // ---------- Основные геттеры ----------

    @Override
    public String getSerializedName() {
        return name;
    }

    public int getId() {
        return ordinal();
    }

    /** RGB-цвет стихии (без альфа-канала). */
    public int getColor() {
        return color;
    }

    /** ARGB-цвет с полной непрозрачностью. */
    public int getColorARGB() {
        return 0xFF000000 | color;
    }

    public Category getCategory() {
        return category;
    }

    public ResourceLocation getLocation() {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
    }

    // ---------- Локализация ----------

    public String getTranslationKey() {
        return "element." + MOD_ID + "." + name;
    }

    /** Название стихии, окрашенное в её цвет. */
    public MutableComponent getDisplayName() {
        return Component.translatable(getTranslationKey())
                .withStyle(Style.EMPTY.withColor(color));
    }

    // ---------- Взаимодействия ----------

    /**
     * Противоположная стихия:
     * земля ↔ воздух, вода ↔ огонь, свет ↔ тьма. У эфира противоположности нет.
     */
    public Optional<Element> getOpposite() {
        return Optional.ofNullable(switch (this) {
            case EARTH -> AIR;
            case AIR -> EARTH;
            case WATER -> FIRE;
            case FIRE -> WATER;
            case LIGHT -> DARK;
            case DARK -> LIGHT;
            case AETHER -> null;
        });
    }

    public boolean isOppositeOf(Element other) {
        return getOpposite().map(o -> o == other).orElse(false);
    }

    public Relation getRelationTo(Element other) {
        if (this == other) return Relation.SAME;
        if (isOppositeOf(other)) return Relation.OPPOSED;
        return Relation.NEUTRAL;
    }

    public boolean isBasic() {
        return category == Category.BASIC;
    }

    // ---------- Поиск ----------

    public static Optional<Element> byName(String name) {
        for (Element e : values()) {
            if (e.name.equals(name)) return Optional.of(e);
        }
        return Optional.empty();
    }

    @Nullable
    public static Element byNameOrNull(String name) {
        return byName(name).orElse(null);
    }

    // ---------- Вложенные типы ----------

    public enum Category {
        /** Земля, вода, огонь, воздух. */
        BASIC,
        /** Свет и тьма. */
        CELESTIAL,
        /** Эфир. */
        SUPREME
    }

    public enum Relation {
        SAME, OPPOSED, NEUTRAL
    }
}