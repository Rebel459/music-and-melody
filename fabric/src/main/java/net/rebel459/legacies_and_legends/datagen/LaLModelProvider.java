package net.rebel459.legacies_and_legends.datagen;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.world.level.block.Blocks;
import net.rebel459.legacies_and_legends.registry.*;
import net.minecraft.client.data.models.model.*;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.rebel459.unified.util.registry.SuppliedItem;
import org.jetbrains.annotations.NotNull;

public final class LaLModelProvider extends FabricModelProvider {
	public LaLModelProvider(FabricPackOutput output) {
		super(output);
	}

	public static final BlockFamily FAMILY_METEORITE_BRICKS = BlockFamilies.familyBuilder(LaLBlocks.METEORITE_BRICKS.get())
			.stairs(LaLBlocks.METEORITE_BRICK_STAIRS.get())
			.slab(LaLBlocks.METEORITE_BRICK_SLAB.get())
			.wall(LaLBlocks.METEORITE_BRICK_WALL.get())
			.getFamily();

	@Override
	public void generateBlockStateModels(@NotNull BlockModelGenerators generator) {
		generator.createLantern(LaLBlocks.SAPPHIRE_LANTERN.get());
		generator.createTrivialCube(LaLBlocks.SAPPHIRE_BLOCK.get());
		generator.createTrivialCube(LaLBlocks.SAPPHIRE_ORE.get());
		generator.createTrivialCube(LaLBlocks.DEEPSLATE_SAPPHIRE_ORE.get());
		generator.createTrivialCube(LaLBlocks.METEORITE.get());
		generator.createTrivialCube(LaLBlocks.CONCENTRATED_METEORITE.get());
		generator.family(LaLBlocks.METEORITE_BRICKS.get()).generateFor(FAMILY_METEORITE_BRICKS);
		generator.createTrivialCube(LaLBlocks.CHISELED_METEORITE_BRICKS.get());
		createJewelingTable(generator);
	}

	public void createJewelingTable(@NotNull BlockModelGenerators generator) {
		TextureMapping mapping = new TextureMapping()
				.put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(LaLBlocks.JEWELING_TABLE.get(), "_back")) // particle
				.put(TextureSlot.DOWN, TextureMapping.getBlockTexture(LaLBlocks.JEWELING_TABLE.get(), "_bottom")) // bottom
				.put(TextureSlot.UP, TextureMapping.getBlockTexture(LaLBlocks.JEWELING_TABLE.get(), "_top")) // top
				.put(TextureSlot.NORTH, TextureMapping.getBlockTexture(LaLBlocks.JEWELING_TABLE.get(), "_front")) // front
				.put(TextureSlot.SOUTH, TextureMapping.getBlockTexture(LaLBlocks.JEWELING_TABLE.get(), "_back")) // back
				.put(TextureSlot.EAST, TextureMapping.getBlockTexture(LaLBlocks.JEWELING_TABLE.get(), "_left")) // left
				.put(TextureSlot.WEST, TextureMapping.getBlockTexture(LaLBlocks.JEWELING_TABLE.get(), "_right")); // right
		generator.blockStateOutput
				.accept(BlockModelGenerators.createSimpleBlock(LaLBlocks.JEWELING_TABLE.get(), BlockModelGenerators.plainVariant(ModelTemplates.CUBE.create(LaLBlocks.JEWELING_TABLE.get(), mapping, generator.modelOutput))));
	}

	@Override
	public void generateItemModels(@NotNull ItemModelGenerators generator) {
        generator.generateFlatItem(LaLItems.REINFORCED_CHESTPLATE.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(LaLItems.TRAVELLING_STRIDES.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.WANDERER_BOOTS.get(), ModelTemplates.FLAT_ITEM);

		generator.generateFlatItem(LaLItems.BOOMERANG.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
		generator.generateFlatItem(LaLItems.KNIFE.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
		generator.generateFlatItem(LaLItems.HOOK.get(), ModelTemplates.FLAT_HANDHELD_ITEM);

		generator.generateFlatItem(LaLItems.VERDANT_SWORD.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
		generator.generateFlatItem(LaLItems.CLEAVING_BATTLEAXE.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
		generator.generateFlatItem(LaLItems.MOLTEN_PICKAXE.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
		generator.generateFlatItem(LaLItems.PROSPECTOR_SHOVEL.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        generator.generateFlatItem(LaLItems.WITHERED_HOE.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        generator.generateSpear(LaLItems.FROSTED_SPEAR.get());

		generator.generateFlatItem(LaLItems.TOTEM_OF_TELEPORTATION.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.TOTEM_OF_RESURRECTION.get(), ModelTemplates.FLAT_ITEM);

		generator.generateFlatItem(LaLItems.RING_OF_EVASION.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.RING_OF_CONSTRUCTION.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.RING_OF_STRIKING.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.RING_OF_EXCAVATION.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.RING_OF_RESTORATION.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.RING_OF_HUNTING.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.RING_OF_ARCHERY.get(), ModelTemplates.FLAT_ITEM);

		generator.generateFlatItem(LaLItems.NECKLACE_OF_PROTECTION.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.NECKLACE_OF_BARTERING.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.NECKLACE_OF_ISOLATION.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.NECKLACE_OF_REGENERATION.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.NECKLACE_OF_LEAPING.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.NECKLACE_OF_PURITY.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.NECKLACE_OF_RESILIENCE.get(), ModelTemplates.FLAT_ITEM);

		generator.generateFlatItem(LaLItems.AMULET_OF_ABSORPTION.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.AMULET_OF_OBSIDIAN.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.AMULET_OF_DEFLECTION.get(), ModelTemplates.FLAT_ITEM);

		generator.generateFlatItem(LaLItems.SAPPHIRE.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.METAL_CHUNK.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.TABLET.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.TRIDENT_SHARD.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.DISC_FRAGMENT_FAR_LANDS.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.METEORITE_BRICK.get(), ModelTemplates.FLAT_ITEM);

		generator.generateFlatItem(LaLItems.WOODEN_BUCKET.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.CHARCOAL_BUCKET.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.COAL_BUCKET.get(), ModelTemplates.FLAT_ITEM);

		for (SuppliedItem item : LaLItems.GEMS) {
			generator.generateFlatItem(item.get(), ModelTemplates.FLAT_ITEM);
		}

		generator.generateFlatItem(LaLItems.MUSIC_DISC_SVALL.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.MUSIC_DISC_CASTLES.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.MUSIC_DISC_TASWELL.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.MUSIC_DISC_TUNDRA.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.MUSIC_DISC_FAR_LANDS.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.MUSIC_DISC_SHULKER.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.MUSIC_DISC_INFINITE_SPOOKY_AMETHYST.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.MUSIC_DISC_113.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.MUSIC_DISC_GRAVEL.get(), ModelTemplates.FLAT_ITEM);

		generator.generateFlatItem(LaLItems.DUSK_POTTERY_SHERD.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.HARVEST_POTTERY_SHERD.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.VERDANT_POTTERY_SHERD.get(), ModelTemplates.FLAT_ITEM);
		generator.generateFlatItem(LaLItems.FORAGER_POTTERY_SHERD.get(), ModelTemplates.FLAT_ITEM);

	}
}
