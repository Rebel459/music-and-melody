package net.rebel459.legacies_and_legends.registry;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.rebel459.legacies_and_legends.LaLConstants;
import net.rebel459.legacies_and_legends.block.GlowStickBlock;
import net.rebel459.legacies_and_legends.block.JewelingTableBlock;
import net.rebel459.legacies_and_legends.block.WandPlatformBlock;
import net.rebel459.legacies_and_legends.sound.LaLBlockSounds;
import net.rebel459.unified.platform.UnifiedHelpers;
import net.rebel459.unified.platform.UnifiedRegistries;
import net.rebel459.unified.util.registry.SuppliedBlock;

import java.util.List;

public class LaLBlocks {
    
    public static UnifiedRegistries.Blocks BLOCKS = UnifiedRegistries.Blocks.create(LaLConstants.MOD_ID);

    public static final SuppliedBlock JEWELING_TABLE = BLOCKS.register("jeweling_table",
            JewelingTableBlock::new,
            () -> Properties.ofFullCopy(Blocks.SMITHING_TABLE)
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
    );

    public static final SuppliedBlock SAPPHIRE_LANTERN = BLOCKS.register("sapphire_lantern",
            LanternBlock::new,
            () -> Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .forceSolidOn()
                    .strength(3.5F)
                    .lightLevel(_ -> 14)
                    .sound(SoundType.LANTERN)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)
    );
    public static final SuppliedBlock SAPPHIRE_BLOCK = BLOCKS.register("sapphire_block",
            Block::new,
            () -> Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .forceSolidOn()
                    .strength(5F, 6F)
                    .sound(LaLBlockSounds.SAPPHIRE_BLOCK)
                    .requiresCorrectToolForDrops()
    );
    public static final SuppliedBlock SAPPHIRE_ORE = BLOCKS.register("sapphire_ore",
            (properties) -> new DropExperienceBlock(UniformInt.of(2, 5), properties),
            () -> Properties.of()
                    .mapColor(MapColor.STONE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
    );
    public static final SuppliedBlock DEEPSLATE_SAPPHIRE_ORE = BLOCKS.register("deepslate_sapphire_ore",
            (properties) -> new DropExperienceBlock(UniformInt.of(2, 5), properties),
            () -> Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(4.5F, 3.0F)
                    .sound(SoundType.DEEPSLATE)
    );

    public static final SuppliedBlock WAND_PLATFORM = BLOCKS.register("wand_platform",
            WandPlatformBlock::new,
            () -> Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .noOcclusion()
                    .isViewBlocking(Blocks::never)
                    .noLootTable()
                    .isValidSpawn((_, _, _, _) -> false)
                    .strength(3F, 6F)
                    .sound(LaLBlockSounds.WAND_PLATFORM)
                    .pushReaction(PushReaction.DESTROY)
    );

    public static final SuppliedBlock GLOW_STICK = BLOCKS.registerWithoutItem("glow_stick",
            GlowStickBlock::new,
            () -> Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .lightLevel(_ -> 15)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .instabreak()
                    .pushReaction(PushReaction.DESTROY)
    );

    public static final SuppliedBlock METEORITE = BLOCKS.register("meteorite",
            Block::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                    .strength(4.5F, 20F)
                    .mapColor(MapColor.TERRACOTTA_ORANGE)
                    .sound(LaLBlockSounds.METEORITE)
    );
    public static final SuppliedBlock CONCENTRATED_METEORITE = BLOCKS.register("concentrated_meteorite",
            (properties) -> new DropExperienceBlock(UniformInt.of(2, 5), properties),
            () -> BlockBehaviour.Properties.ofFullCopy(METEORITE.get())
                    .strength(6F, 20F)
    );
    public static final SuppliedBlock METEORITE_BRICKS = BLOCKS.register("meteorite_bricks",
            Block::new,
            () -> BlockBehaviour.Properties.ofFullCopy(METEORITE.get())
    );
    public static final SuppliedBlock METEORITE_BRICK_SLAB = BLOCKS.register("meteorite_brick_wall",
            SlabBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(METEORITE_BRICKS.get())
    );
    public static final SuppliedBlock METEORITE_BRICK_WALL = BLOCKS.register("meteorite_brick_slab",
            WallBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(METEORITE_BRICKS.get())
    );
    public static final SuppliedBlock METEORITE_BRICK_STAIRS = BLOCKS.register("meteorite_brick_stairs",
            properties -> new StairBlock(METEORITE_BRICKS.get().defaultBlockState(), properties),
            () -> BlockBehaviour.Properties.ofFullCopy(METEORITE_BRICKS.get())
    );
    public static final SuppliedBlock CHISELED_METEORITE_BRICKS = BLOCKS.register("chiseled_meteorite_bricks",
            Block::new,
            () -> BlockBehaviour.Properties.ofFullCopy(METEORITE_BRICKS.get())
    );

    public static void init() {
        for (SuppliedBlock block : List.of(METEORITE, CONCENTRATED_METEORITE, METEORITE_BRICKS, METEORITE_BRICK_SLAB, METEORITE_BRICK_WALL, METEORITE_BRICK_STAIRS)) {
            UnifiedHelpers.DATA_COMPONENTS.addWithProvider(block, DataComponents.DAMAGE_RESISTANT, (provider) -> new DamageResistant(provider.getOrThrow(DamageTypeTags.IS_FIRE)));
            UnifiedHelpers.DATA_COMPONENTS.addWithProvider(block, DataComponents.DAMAGE_RESISTANT, (provider) -> new DamageResistant(provider.getOrThrow(DamageTypeTags.IS_EXPLOSION)));
            UnifiedHelpers.DATA_COMPONENTS.add(block, DataComponents.RARITY, Rarity.UNCOMMON);
        }
    }
}