package net.rebel459.legacies_and_legends;

import net.rebel459.legacies_and_legends.config.LaLConfig;
import net.rebel459.legacies_and_legends.enchantment.LaLEnchantmentEffects;
import net.rebel459.legacies_and_legends.event.PlayerEvents;
import net.rebel459.legacies_and_legends.event.ServerEvents;
import net.rebel459.legacies_and_legends.registry.*;
import net.rebel459.legacies_and_legends.sound.LaLJukeboxSongs;
import net.rebel459.legacies_and_legends.sound.LaLMusic;
import net.rebel459.legacies_and_legends.sound.LaLSounds;
import net.rebel459.legacies_and_legends.worldgen.LaLFeatures;
import net.rebel459.unified.platform.UnifiedHelpers;
import net.rebel459.unified.platform.UnifiedPlatform;
import net.rebel459.unified.util.PackType;

public class LegaciesAndLegends {

	public static boolean isProgressionRebornLoaded = false;
	public static boolean isFarmersDelightLoaded = false;
    public static boolean isBloomLoaded = false;
    public static boolean isWilderWildLoaded = false;
	public static boolean isVariantsAndVenturesLoaded = false;
	public static boolean isTrailierTalesLoaded = false;
    public static boolean isEnchantsAndExpeditionsLoaded() {
        return UnifiedPlatform.isModLoaded("enchants_and_expeditions");
    }
    public static boolean isEndRebornLoaded = false;
	public static boolean isEnderscapeLoaded = false;
    public static boolean isCombatRebornLoaded = false;

	public static void initRegistries() {

        LaLConfig.init();

        loadResources();

		LaLItems.init();
		LaLBlocks.init();
		LaLJukeboxSongs.init();
		LaLSounds.init();
		LaLEntityTypes.init();
		LaLEnchantmentEffects.init();
		LaLMobEffects.init();
		LaLEnchantments.init();
		LaLMapDecorationTypes.init();
		LaLDataComponents.init();
        LaLMenus.init();
        LaLFeatures.init();
	}

    public static void init() {
        LaLCreativeInventorySorting.init();
        LaLLootTables.init();
        LaLMusic.init();
        ServerEvents.init();
        PlayerEvents.init();
    }

    public static void loadResources() {

        isCombatRebornLoaded = UnifiedPlatform.isModLoaded("combat_reborn");

        if (LaLConfig.get().structures.dungeon_overhaul) {
            UnifiedHelpers.PACKS.add(LaLConstants.id("dungeon_overhaul"), PackType.REQUIRED_DATA);
        }
        if (LaLConfig.get().structures.swamp_hut_variants) {
            UnifiedHelpers.PACKS.add(LaLConstants.id("swamp_hut_variants"), PackType.REQUIRED_DATA);
        }
        if (LaLConfig.get().structures.buried_treasure_rework) {
            UnifiedHelpers.PACKS.add(LaLConstants.id("buried_treasure_rework"), PackType.REQUIRED_DATA);
        }
        if (!LaLConfig.get().structures.new_structures) {
            UnifiedHelpers.PACKS.add(LaLConstants.id("no_new_structures"), PackType.REQUIRED_DATA);
        }
        if (!LaLConfig.get().artifacts.travelling_strides) {
            UnifiedHelpers.PACKS.add(LaLConstants.id("no_travelling_strides"), PackType.REQUIRED_DATA);
        }
        if (!LaLConfig.get().magic.sapphire) {
            UnifiedHelpers.PACKS.add(LaLConstants.id("no_sapphire"), PackType.REQUIRED_DATA);
        }
        if (!LaLConfig.get().magic.wands) {
            UnifiedHelpers.PACKS.add(LaLConstants.id("no_wands"), PackType.REQUIRED_DATA);
        }
        if (!LaLConfig.get().magic.jeweling) {
            UnifiedHelpers.PACKS.add(LaLConstants.id("no_jeweling"), PackType.REQUIRED_DATA);
        }
        if (!LaLConfig.get().magic.meteors) {
            UnifiedHelpers.PACKS.add(LaLConstants.id("no_meteors"), PackType.REQUIRED_DATA);
        }
        if (!LaLConfig.get().loot.glow_stick) {
            UnifiedHelpers.PACKS.add(LaLConstants.id("no_glow_sticks"), PackType.REQUIRED_DATA);
        }
        if (LaLConfig.get().misc.no_creeper_discs) {
            UnifiedHelpers.PACKS.add(LaLConstants.id("no_creeper_discs"), PackType.REQUIRED_DATA);
        }
        if (UnifiedPlatform.isModLoaded("end_reborn")) {
            isEndRebornLoaded = true;
        }
        if (UnifiedPlatform.isModLoaded("progression_reborn")) {
            isProgressionRebornLoaded = true;
        }
        if (UnifiedPlatform.isModLoaded("farmersdelight") && LaLConfig.get().integrations.farmers_delight) {
            isFarmersDelightLoaded = true;
            UnifiedHelpers.PACKS.add(LaLConstants.id("farmers_delight_integration"), PackType.REQUIRED_DATA);
        }
        if (LaLConfig.get().misc.wandering_trader_trades) {
            UnifiedHelpers.PACKS.add(LaLConstants.id("wandering_trader_trades"), PackType.REQUIRED_DATA);
        }
        if (UnifiedPlatform.isModLoaded("bloom") && LaLConfig.get().integrations.bloom) {
            isBloomLoaded = true;
            UnifiedHelpers.PACKS.add(LaLConstants.id("bloom_integration"), PackType.REQUIRED_DATA);
        }
        if (UnifiedPlatform.isModLoaded("wilderwild") && LaLConfig.get().integrations.wilder_wild) {
            isWilderWildLoaded = true;
            UnifiedHelpers.PACKS.add(LaLConstants.id("wilder_wild_integration"), PackType.REQUIRED_DATA);
        }
        if (UnifiedPlatform.isModLoaded("trailiertales") && LaLConfig.get().integrations.trailier_tales) {
            isTrailierTalesLoaded = true;
            UnifiedHelpers.PACKS.add(LaLConstants.id("trailier_tales_integration"), PackType.REQUIRED_DATA);
        }
        if (UnifiedPlatform.isModLoaded("variantsandventures") && LaLConfig.get().integrations.variants_and_ventures) {
            isVariantsAndVenturesLoaded = true;
            if (LaLConfig.get().structures.dungeon_overhaul) {
                UnifiedHelpers.PACKS.add(LaLConstants.id("variants_and_ventures_integration"), PackType.REQUIRED_DATA);
            }
        }
        if (UnifiedPlatform.isModLoaded("enderscape") && LaLConfig.get().integrations.enderscape) {
            isEnderscapeLoaded = true;
            UnifiedHelpers.PACKS.add(LaLConstants.id("enderscape_integration"), PackType.REQUIRED_DATA);
            if (LaLConfig.get().magic.jeweling) {
                UnifiedHelpers.PACKS.add(LaLConstants.id("enderscape_jeweling"), PackType.REQUIRED_DATA);
            }
        }
    }
}
