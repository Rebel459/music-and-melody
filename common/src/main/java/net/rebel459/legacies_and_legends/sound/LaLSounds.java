package net.rebel459.legacies_and_legends.sound;

import net.rebel459.legacies_and_legends.LaLConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.rebel459.unified.platform.UnifiedRegistries;
import net.rebel459.unified.util.registry.Supplied;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class LaLSounds {
	
	public static UnifiedRegistries.SoundEvents SOUNDS = UnifiedRegistries.SoundEvents.create(LaLConstants.MOD_ID);
	
	public static final Holder<SoundEvent> MUSIC_DISC_SVALL = SOUNDS.registerForHolder("music_disc.svall");
	public static final Holder<SoundEvent> MUSIC_DISC_TASWELL = SOUNDS.registerForHolder("music_disc.taswell");
	public static final Holder<SoundEvent> MUSIC_DISC_SHULKER = SOUNDS.registerForHolder("music_disc.shulker");
	public static final Holder<SoundEvent> MUSIC_DISC_TUNDRA = SOUNDS.registerForHolder("music_disc.tundra");
	public static final Holder<SoundEvent> MUSIC_DISC_FAR_LANDS = SOUNDS.registerForHolder("music_disc.far_lands");
	public static final Holder<SoundEvent> MUSIC_DISC_INFINITE_SPOOKY_AMETHYST = SOUNDS.registerForHolder("music_disc.infinite_spooky_amethyst");
	public static final Holder<SoundEvent> MUSIC_DISC_113 = SOUNDS.registerForHolder("music_disc.113");
	public static final Holder<SoundEvent> MUSIC_DISC_GRAVEL = SOUNDS.registerForHolder("music_disc.gravel");

	public static final Holder<SoundEvent> TABLET_USE = SOUNDS.registerForHolder("tablet.use");
	public static final Holder<SoundEvent> TABLET_BREAK = SOUNDS.registerForHolder("tablet.break");
	public static final Supplied<SoundEvent> TABLET_TELEPORT = SOUNDS.register("tablet.teleport");

	public static final Supplied<SoundEvent> BOOMERANG_THROW = SOUNDS.register("boomerang.throw");
	public static final Supplied<SoundEvent> BOOMERANG_HIT = SOUNDS.register("boomerang.hit");
	public static final Supplied<SoundEvent> BOOMERANG_RETURN = SOUNDS.register("boomerang.return");
	public static final Supplied<SoundEvent> BOOMERANG_WHOOSH = SOUNDS.register("boomerang.whoosh");

	public static final Supplied<SoundEvent> WAND_SUMMON = SOUNDS.register("wand.summon");
	public static final Supplied<SoundEvent> WAND_RECALL = SOUNDS.register("wand.recall");

	public static final Supplied<SoundEvent> TOTEM_EQUIP = SOUNDS.register("accessory.totem_equip");
	public static final Supplied<SoundEvent> AMULET_EQUIP = SOUNDS.register("accessory.amulet_equip");
	public static final Supplied<SoundEvent> RING_EQUIP = SOUNDS.register("accessory.ring_equip");
	public static final Supplied<SoundEvent> NECKLACE_EQUIP = SOUNDS.register("accessory.necklace_equip");

	public static final Supplied<SoundEvent> ACCESSORY_BREAK = SOUNDS.register("accessory.break");

	public static final Supplied<SoundEvent> SAPPHIRE_BLOCK_BREAK = SOUNDS.register("block.sapphire_block.break");
	public static final Supplied<SoundEvent> SAPPHIRE_BLOCK_STEP = SOUNDS.register("block.sapphire_block.step");
	public static final Supplied<SoundEvent> SAPPHIRE_BLOCK_PLACE = SOUNDS.register("block.sapphire_block.place");
	public static final Supplied<SoundEvent> SAPPHIRE_BLOCK_HIT = SOUNDS.register("block.sapphire_block.hit");
	public static final Supplied<SoundEvent> SAPPHIRE_BLOCK_FALL = SOUNDS.register("block.sapphire_block.fall");

	public static final Supplied<SoundEvent> WAND_PLATFORM_BREAK = SOUNDS.register("block.wand_platform.break");
	public static final Supplied<SoundEvent> WAND_PLATFORM_STEP = SOUNDS.register("block.wand_platform.step");
	public static final Supplied<SoundEvent> WAND_PLATFORM_PLACE = SOUNDS.register("block.wand_platform.place");
	public static final Supplied<SoundEvent> WAND_PLATFORM_HIT = SOUNDS.register("block.wand_platform.hit");
	public static final Supplied<SoundEvent> WAND_PLATFORM_FALL = SOUNDS.register("block.wand_platform.fall");

	public static final Supplied<SoundEvent> METEORITE_BREAK = SOUNDS.register("block.meteorite.break");
	public static final Supplied<SoundEvent> METEORITE_STEP = SOUNDS.register("block.meteorite.step");
	public static final Supplied<SoundEvent> METEORITE_PLACE = SOUNDS.register("block.meteorite.place");
	public static final Supplied<SoundEvent> METEORITE_HIT = SOUNDS.register("block.meteorite.hit");
	public static final Supplied<SoundEvent> METEORITE_FALL = SOUNDS.register("block.meteorite.fall");

	public static final Supplied<SoundEvent> JEWEL = SOUNDS.register("block.jeweling_table.jewel");

	public static final Supplied<SoundEvent> SHATTER = SOUNDS.register("enchantment.shatter");

	public static final Supplied<SoundEvent> COMMON_MUSIC = SOUNDS.register("music.common");

	public static final Holder<SoundEvent> SNOWY_MUSIC = SOUNDS.registerForHolder("music.overworld.snowy");
	public static final Holder<SoundEvent> SAVANNA_MUSIC = SOUNDS.registerForHolder("music.overworld.savanna");
	public static final Holder<SoundEvent> DARK_FOREST_MUSIC = SOUNDS.registerForHolder("music.overworld.dark_forest");
	public static final Holder<SoundEvent> MAIN_END_ISLAND_MUSIC = SOUNDS.registerForHolder("music.the_end.main_island");

	public static final Holder<SoundEvent> STRONGHOLD_MUSIC = SOUNDS.registerForHolder("music.structure.stronghold");
	public static final Holder<SoundEvent> ANCIENT_CITY_MUSIC = SOUNDS.registerForHolder("music.structure.ancient_city");

	public static void init() {}
}
