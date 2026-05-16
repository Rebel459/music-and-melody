package net.rebel459.legacies_and_legends.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.rebel459.legacies_and_legends.LaLConstants;
import net.rebel459.unified.platform.UnifiedRegistries;

public class LaLMapDecorationTypes {

	public static UnifiedRegistries.DeferredRegistry<MapDecorationType> DECORATIONS = UnifiedRegistries.DeferredRegistry.create(LaLConstants.MOD_ID, BuiltInRegistries.MAP_DECORATION_TYPE);

	public static final Holder<MapDecorationType> SIMPLE_DUNGEON = register(
			"simple_dungeon",
			true,
			6450790,
			false,
			true
	);
	public static final Holder<MapDecorationType> ARID_DUNGEON = register(
			"arid_dungeon",
			true,
			6450790,
			false,
			true
	);
	public static final Holder<MapDecorationType> FROZEN_DUNGEON = register(
			"frozen_dungeon",
			true,
			6450790,
			false,
			true
	);
	public static final Holder<MapDecorationType> DEEP_DUNGEON = register(
			"deep_dungeon",
			true,
			6450790,
			false,
			true
	);
	public static final Holder<MapDecorationType> VERDANT_DUNGEON = register(
			"verdant_dungeon",
			true,
			6450790,
			false,
			true
	);
	public static final Holder<MapDecorationType> INFERNAL_DUNGEON = register(
			"infernal_dungeon",
			true,
			6450790,
			false,
			true
	);

	public static void init() {}

	private static Holder<MapDecorationType> register(String string, boolean showOnItemFrame, int mapColor, boolean trackCount, boolean explorationMapElement) {
		return DECORATIONS.registerForHolder(string, () -> new MapDecorationType(LaLConstants.id(string), showOnItemFrame, mapColor, explorationMapElement, trackCount));
	}
}