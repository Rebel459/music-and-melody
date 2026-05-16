package net.rebel459.legacies_and_legends.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.core.HolderLookup;
import net.rebel459.legacies_and_legends.registry.LaLBlocks;
import net.rebel459.legacies_and_legends.registry.LaLItems;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class LaLBlockLootProvider extends FabricBlockLootSubProvider {

	public LaLBlockLootProvider(@NotNull FabricPackOutput dataOutput, CompletableFuture<HolderLookup.Provider> registries) {
		super(dataOutput, registries);
	}

	@Override
	public void generate() {
		this.dropSelf(LaLBlocks.SAPPHIRE_BLOCK.get());
		this.dropSelf(LaLBlocks.SAPPHIRE_LANTERN.get());
		this.dropOther(LaLBlocks.GLOW_STICK.get(), LaLItems.GLOW_STICK);
		this.dropSelf(LaLBlocks.METEORITE.get());
		this.dropSelf(LaLBlocks.METEORITE_BRICKS.get());
		this.dropSelf(LaLBlocks.METEORITE_BRICK_WALL.get());
		this.dropSelf(LaLBlocks.METEORITE_BRICK_STAIRS.get());
		this.dropSelf(LaLBlocks.METEORITE_BRICK_SLAB.get());
		this.dropSelf(LaLBlocks.CHISELED_METEORITE_BRICKS.get());
	}
}