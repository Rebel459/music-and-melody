package net.rebel459.legacies_and_legends.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.CollapsibleObject;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.rebel459.legacies_and_legends.LaLConstants;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;


@Config(name = LaLConstants.MOD_ID)
public class LaLConfig implements ConfigData {

	@Contract(pure = true)
	public static @NotNull Path configPath(boolean json5) {
		return Path.of("./config/" + LaLConstants.MOD_ID + "." + (json5 ? "json5" : "json"));
	}

	public static LaLConfig get() {
		return AutoConfig.getConfigHolder(LaLConfig.class).getConfig();
	}

	public static void init() {
		AutoConfig.register(LaLConfig.class, JanksonConfigSerializer::new);
	}

	@CollapsibleObject
	public final StructureConfig structures = new StructureConfig();

	@CollapsibleObject
	public LootConfig loot = new LootConfig();

	@CollapsibleObject
	public final MagicConfig magic = new MagicConfig();

	@CollapsibleObject
	public ArtifactConfig artifacts = new ArtifactConfig();

	@CollapsibleObject
	public AccessoryConfig accessories = new AccessoryConfig();

	@CollapsibleObject
	public EnchantmentConfig enchantments = new EnchantmentConfig();

	@CollapsibleObject
	public MusicConfig music = new MusicConfig();

	@CollapsibleObject
	public MiscConfig misc = new MiscConfig();

	@CollapsibleObject
	public IntegrationConfig integrations = new IntegrationConfig();

	public static class StructureConfig {
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean dungeon_overhaul = true;
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean swamp_hut_variants = true;
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean buried_treasure_rework = true;
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean new_structures = true;
	}

	public static class LootConfig {
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean enchanted_beetroot = true;
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean metal_chunk = true;
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean trident_shard = true;
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean wooden_buckets = true;
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean glow_stick = true;
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean new_music_discs = true;

		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean boomerang = true;
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean hook = true;
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean knife = true;

		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean lore_books = true;
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean improved_loot = true;
	}

	public static class MagicConfig {
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean sapphire = true;
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean wands = true;
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean jeweling = true;
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean meteors = true;
	}

	public static class ArtifactConfig {
		@ConfigEntry.Category("config")
		public boolean reinforced_chestplate = true;
		@ConfigEntry.Category("config")
		public boolean travelling_strides = true;
		@ConfigEntry.Category("config")
		public boolean wanderer_boots = true;

		@ConfigEntry.Category("config")
		public boolean verdant_sword = true;
		@ConfigEntry.Category("config")
		public boolean cleaving_battleaxe = true;
		@ConfigEntry.Category("config")
		public boolean molten_pickaxe = true;
		@ConfigEntry.Category("config")
		public boolean prospector_shovel = true;
        @ConfigEntry.Category("config")
        public boolean withered_hoe = true;
        @ConfigEntry.Category("config")
        public boolean frosted_spear = true;

		@ConfigEntry.Category("config")
		public boolean tablet_of_recall = true;
		@ConfigEntry.Category("config")
		public boolean tablet_of_haste = true;
		@ConfigEntry.Category("config")
		public boolean tablet_of_instability = true;
		@ConfigEntry.Category("config")
		public boolean tablet_of_warping = true;
		@ConfigEntry.Category("config")
		public boolean tablet_of_channeling = true;
		@ConfigEntry.Category("config")
		public boolean tablet_of_deafening = true;
		@ConfigEntry.Category("config")
		public boolean tablet_of_revealing = true;
	}

	public static class AccessoryConfig {

		@CollapsibleObject
		public Slot slot = new Slot();
		public static class Slot {
			@ConfigEntry.Category("config")
			@ConfigEntry.Gui.Tooltip
			public boolean enabled = true;
			@ConfigEntry.Category("config")
			public int offset_x = 0;
			@ConfigEntry.Category("config")
			public int offset_y = 0;
			@ConfigEntry.Category("config")
			@ConfigEntry.Gui.Tooltip
			public boolean use_equip = true;
		}

		@ConfigEntry.Category("config")
		public boolean amulet_of_absorption = true;
		@ConfigEntry.Category("config")
		public boolean amulet_of_obsidian = true;
		@ConfigEntry.Category("config")
		public boolean amulet_of_deflection = true;
		@ConfigEntry.Category("config")
		public boolean ring_of_evasion = true;
		@ConfigEntry.Category("config")
		public boolean ring_of_striking = true;
		@ConfigEntry.Category("config")
		public boolean ring_of_hunting = true;
		@ConfigEntry.Category("config")
		public boolean ring_of_construction = true;
		@ConfigEntry.Category("config")
		public boolean ring_of_archery = true;
		@ConfigEntry.Category("config")
		public boolean ring_of_excavation = true;
		@ConfigEntry.Category("config")
		public boolean ring_of_restoration = true;
		@ConfigEntry.Category("config")
		public boolean necklace_of_purity = true;
		@ConfigEntry.Category("config")
		public boolean necklace_of_leaping = true;
		@ConfigEntry.Category("config")
		public boolean necklace_of_protection = true;
		@ConfigEntry.Category("config")
		public boolean necklace_of_regeneration = true;
		@ConfigEntry.Category("config")
		public boolean necklace_of_resilience = true;
		@ConfigEntry.Category("config")
		public boolean necklace_of_bartering = true;
		@ConfigEntry.Category("config")
		public boolean necklace_of_isolation = true;
		@ConfigEntry.Category("config")
		public boolean totem_of_resurrection = true;
		@ConfigEntry.Category("config")
		public boolean totem_of_teleportation = true;
	}

	public static class EnchantmentConfig {
		@ConfigEntry.Category("config")
		public boolean tangled = true;
		@ConfigEntry.Category("config")
		public boolean freeze = true;
		@ConfigEntry.Category("config")
		public boolean featherweight = true;
		@ConfigEntry.Category("config")
		public boolean shadowstep = true;
		@ConfigEntry.Category("config")
		public boolean rejuvenate = true;
		@ConfigEntry.Category("config")
		public boolean decay = true;
	}

	public static class MusicConfig {
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean snowy_music = true;
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean savanna_music = true;
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean dark_forest_music = true;

        @ConfigEntry.Category("config")
        @ConfigEntry.Gui.Tooltip
        public int structure_music_min = 300;
        @ConfigEntry.Category("config")
        @ConfigEntry.Gui.Tooltip
        public int structure_music_max = 600;
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean stronghold_music = true;
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean ancient_city_music = true;

		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean main_end_island_music = true;
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean end_portal_music = true;

		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean music_and_melody = true;

		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean creative_fix = true;

		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean common_music = false;
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.BoundedDiscrete(min = 0, max = 100)
		public int common_music_chance = 50;
	}

	public static class MiscConfig {
        @ConfigEntry.Category("config")
        @ConfigEntry.Gui.Tooltip
        public boolean stackable_saddles = true;
        @ConfigEntry.Category("config")
        @ConfigEntry.Gui.Tooltip
        public boolean improved_turtle_shell = true;
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean echo_shard_trim = true;
        @ConfigEntry.Category("config")
        @ConfigEntry.Gui.Tooltip
        public boolean wandering_trader_trades = true;
		@ConfigEntry.Category("config")
		@ConfigEntry.Gui.Tooltip
		public boolean no_creeper_discs = false;
	}

	public static class IntegrationConfig {
        @ConfigEntry.Category("config")
        public boolean bloom = true;
        @ConfigEntry.Category("config")
        public boolean wilder_wild = true;
		@ConfigEntry.Category("config")
		public boolean trailier_tales = true;
		@ConfigEntry.Category("config")
		public boolean farmers_delight = true;
		@ConfigEntry.Category("config")
		public boolean enderscape = true;
		@ConfigEntry.Category("config")
		public boolean variants_and_ventures = true;
	}
}