package net.rebel459.legacies_and_legends.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.rebel459.item_tooltips.config.ITConfig;
import net.rebel459.item_tooltips.util.ScreenHelper;
import net.rebel459.legacies_and_legends.registry.LaLItems;
import net.rebel459.unified.util.registry.SuppliedItem;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public enum Gem implements StringRepresentable {
    EMPTY(0, "empty", "ffffff"),
    SAPPHIRE(1, "sapphire", "5a76c8"),
    SLIME(2, "slime", "8fc85a"),
    METEORITE(3, "meteorite", "c8815a"),
    ICE(4, "ice", "5ab2c8"),
    BREEZE(5, "breeze", "5c5ac8"),
    OBSIDIAN(6, "obsidian", "725ac8"),
    PRISMARINE(7, "prismarine", "5ac8ab"),
    TIMELOST(8, "timelost", "5ac87f"),
    NEBULITE(9, "nebulite", "b65ac8"),
    RUBY(10, "ruby", "c85a6a");

    private static int decimal(String hex) {
        String normalized = hex.startsWith("#") ? hex.substring(1) : hex;
        return Integer.parseInt(normalized, 16);
    }
    private static int argb(String hex) {
        return 0xFF000000 | decimal(hex);
    }

    public static final Codec<Gem> CODEC = StringRepresentable.fromValues(Gem::values);
    public static final IntFunction<Gem> BY_ID = ByIdMap.continuous((r) -> r.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, Gem> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, (r) -> r.id);
    private final int id;
    private final String name;
    private final String color;

    Gem(int id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public int color() {
        return decimal(color);
    }

    public int tint() {
        return argb(color);
    }

    public Item item() {
        return switch (this) {
            case EMPTY -> ItemStack.EMPTY.getItem();
            case SAPPHIRE -> LaLItems.SAPPHIRE_GEM.get();
            case SLIME -> LaLItems.SLIME_GEM.get();
            case METEORITE -> LaLItems.METEORITE_GEM.get();
            case ICE -> LaLItems.ICE_GEM.get();
            case BREEZE -> LaLItems.BREEZE_GEM.get();
            case OBSIDIAN -> LaLItems.OBSIDIAN_GEM.get();
            case PRISMARINE -> LaLItems.PRISMARINE_GEM.get();
            case TIMELOST -> LaLItems.TIMELOST_GEM.get();
            case NEBULITE -> LaLItems.NEBULITE_GEM.get();
            case RUBY -> LaLItems.RUBY_GEM.get();
        };
    }

    public String getSerializedName() {
        return this.name;
    }

    public record Slots(@NotNull Gem primary, @NotNull Gem secondary) {
        public static Slots DEFAULT = new Slots(Gem.SAPPHIRE, Gem.EMPTY);
        public static Slots EMPTY = new Slots(Gem.EMPTY, Gem.EMPTY);

        public static final Codec<Slots> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Gem.CODEC.fieldOf("primary").forGetter(Slots::primary),
                Gem.CODEC.fieldOf("secondary").forGetter(Slots::secondary)
        ).apply(instance, Slots::new));

        public static final StreamCodec<ByteBuf, Slots> STREAM_CODEC = StreamCodec.composite(
                Gem.STREAM_CODEC, Slots::primary,
                Gem.STREAM_CODEC, Slots::secondary,
                Slots::new
        );
    }
}
