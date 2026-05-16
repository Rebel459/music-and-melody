package net.rebel459.legacies_and_legends.registry;

import net.minecraft.advancements.criterion.LocationPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.functions.*;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.rebel459.legacies_and_legends.LaLConstants;
import net.rebel459.legacies_and_legends.config.LaLConfig;
import net.rebel459.unified.platform.UnifiedEvents;
import net.rebel459.unified.platform.UnifiedPlatform;
import net.rebel459.unified.util.LootEntry;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LaLLootTables {
	public static final ResourceKey<LootTable> BIRCH_RUINS = register("chests/forest_ruins/birch");
	public static final ResourceKey<LootTable> CHERRY_RUINS = register("chests/forest_ruins/cherry");
    public static final ResourceKey<LootTable> GOLDEN_BIRCH_RUINS = register("chests/forest_ruins/golden_birch");
    public static final ResourceKey<LootTable> MAPLE_RUINS = register("chests/forest_ruins/maple");

	public static final ResourceKey<LootTable> DEEP_RUINS = register("chests/deep_ruins/deep");
	public static final ResourceKey<LootTable> SCULK_RUINS = register("chests/deep_ruins/sculk");

	public static final ResourceKey<LootTable> PALE_CABIN = register("chests/pale_cabin/chest");
	public static final ResourceKey<LootTable> PALE_CABIN_SECRET = register("chests/pale_cabin/secret");

	public static final ResourceKey<LootTable> RUINED_AETHER_PORTAL = register("chests/ruined_aether_portal");

	public static final ResourceKey<LootTable> RUINED_LIBRARY = register("chests/ruined_library");

	public static final ResourceKey<LootTable> END_RUINS = register("chests/end_ruins");

	public static final ResourceKey<LootTable> SWAMP_HUT = register("chests/swamp_hut");

	public static final ResourceKey<LootTable> RUINS = register("chests/ruins");
	public static final ResourceKey<LootTable> RUINS_ARCHAEOLOGY = register("archaeology/ruins");

	public static final ResourceKey<LootTable> OBELISK_ARCHAEOLOGY = register("archaeology/obelisk");

	public static final ResourceKey<LootTable> UNDERGROUND_CABIN = register("chests/cabin/underground");
	public static final ResourceKey<LootTable> DEEP_CABIN = register("chests/cabin/deep");

	public static final ResourceKey<LootTable> SPIRE = register("chests/spire");
	public static final ResourceKey<LootTable> SPIRE_BASE = register("chests/spire_base");

	public static final ResourceKey<LootTable> DUNGEON_CHEST = register("chests/dungeon/chest");
	public static final ResourceKey<LootTable> DUNGEON_BARREL = register("chests/dungeon/barrel");
	public static final ResourceKey<LootTable> DUNGEON_LIBRARY = register("chests/dungeon/library");
	public static final ResourceKey<LootTable> DUNGEON_CHEST_SIMPLE = register("chests/dungeon/simple/chest");
	public static final ResourceKey<LootTable> DUNGEON_BARREL_SIMPLE = register("chests/dungeon/simple/barrel");
	public static final ResourceKey<LootTable> DUNGEON_LIBRARY_SIMPLE = register("chests/dungeon/simple/library");
	public static final ResourceKey<LootTable> DUNGEON_CHEST_DEEP = register("chests/dungeon/deep/chest");
	public static final ResourceKey<LootTable> DUNGEON_BARREL_DEEP = register("chests/dungeon/deep/barrel");
	public static final ResourceKey<LootTable> DUNGEON_LIBRARY_DEEP = register("chests/dungeon/deep/library");
	public static final ResourceKey<LootTable> DUNGEON_CHEST_ARID = register("chests/dungeon/arid/chest");
	public static final ResourceKey<LootTable> DUNGEON_BARREL_ARID = register("chests/dungeon/arid/barrel");
	public static final ResourceKey<LootTable> DUNGEON_LIBRARY_ARID = register("chests/dungeon/arid/library");
	public static final ResourceKey<LootTable> DUNGEON_CHEST_FROZEN = register("chests/dungeon/frozen/chest");
	public static final ResourceKey<LootTable> DUNGEON_BARREL_FROZEN = register("chests/dungeon/frozen/barrel");
	public static final ResourceKey<LootTable> DUNGEON_LIBRARY_FROZEN = register("chests/dungeon/frozen/library");
	public static final ResourceKey<LootTable> DUNGEON_CHEST_VERDANT = register("chests/dungeon/verdant/chest");
	public static final ResourceKey<LootTable> DUNGEON_BARREL_VERDANT = register("chests/dungeon/verdant/barrel");
	public static final ResourceKey<LootTable> DUNGEON_LIBRARY_VERDANT = register("chests/dungeon/verdant/library");
	public static final ResourceKey<LootTable> DUNGEON_CHEST_INFERNAL = register("chests/dungeon/infernal/chest");
	public static final ResourceKey<LootTable> DUNGEON_BARREL_INFERNAL = register("chests/dungeon/infernal/barrel");
	public static final ResourceKey<LootTable> DUNGEON_LIBRARY_INFERNAL = register("chests/dungeon/infernal/library");

	public static final ResourceKey<LootTable> OVERWORLD_GENERAL_ACCESSORIES = register("accessories/overworld/general");
	public static final ResourceKey<LootTable> OVERWORLD_ARCHAEOLOGY_ACCESSORIES = register("accessories/overworld/archaeology");
	public static final ResourceKey<LootTable> OVERWORLD_SWAMP_HUT_ACCESSORIES = register("accessories/overworld/swamp_hut");

	public static final ResourceKey<LootTable> UNDERGROUND_GENERAL_ACCESSORIES = register("accessories/underground/general");
	public static final ResourceKey<LootTable> UNDERGROUND_MINESHAFT_ACCESSORIES = register("accessories/underground/mineshaft");
	public static final ResourceKey<LootTable> UNDERGROUND_DEEP_ACCESSORIES = register("accessories/underground/deep");

	public static final ResourceKey<LootTable> NETHER_GENERAL_ACCESSORIES = register("accessories/nether/general");
	public static final ResourceKey<LootTable> NETHER_FORTRESS_ACCESSORIES = register("accessories/nether/fortress");
	public static final ResourceKey<LootTable> NETHER_PIGLIN_ACCESSORIES = register("accessories/nether/piglin");

	public static final ResourceKey<LootTable> END_GENERAL_ACCESSORIES = register("accessories/end/general");
	public static final ResourceKey<LootTable> END_CITY_ACCESSORIES = register("accessories/end/end_city");
	public static final ResourceKey<LootTable> END_RUINS_ACCESSORIES = register("accessories/end/ruins");

	public static final ResourceKey<LootTable> END_REMAINS = register("end_reborn", "chests/end_remains");

	public static final ResourceKey<LootTable> END_CITY_CHEST = registerEnderscape("end_city/chest");
	public static final ResourceKey<LootTable> END_CITY_VAULT = registerEnderscape("end_city/vault");
    public static final ResourceKey<LootTable> END_CITY_ELYTRA_VAULT = registerEnderscape("end_city/elytra_vault");

    public static final ResourceKey<LootTable> ENDERSCAPE_STRONGHOLD_ALTAR = registerEnderscape("stronghold/chest/altar");
    public static final ResourceKey<LootTable> ENDERSCAPE_STRONGHOLD_LIBRARY = registerEnderscape("stronghold/chest/library");
    public static final ResourceKey<LootTable> ENDERSCAPE_STRONGHOLD_SECRET = registerEnderscape("stronghold/chest/secret");
    public static final ResourceKey<LootTable> ENDERSCAPE_STRONGHOLD_GARDEN = registerEnderscape("stronghold/chest/garden");
    public static final ResourceKey<LootTable> ENDERSCAPE_STRONGHOLD_BEDROOM = registerEnderscape("stronghold/chest/bedroom");
    public static final ResourceKey<LootTable> ENDERSCAPE_STRONGHOLD_MANSION = registerEnderscape("stronghold/chest/mansion");

    public static boolean enderscapeStrongholdCommon(ResourceKey<LootTable> id) {
        return id == ENDERSCAPE_STRONGHOLD_BEDROOM || id == ENDERSCAPE_STRONGHOLD_MANSION;
    }
    public static boolean enderscapeStrongholdRare(ResourceKey<LootTable> id) {
        return id == ENDERSCAPE_STRONGHOLD_SECRET || id == ENDERSCAPE_STRONGHOLD_GARDEN;
    }

    public static class Books {
		public static final ResourceKey<LootTable> AS_ABOVE = register("books/as_above");
		public static final ResourceKey<LootTable> ASCENT = register("books/ascent");
		public static final ResourceKey<LootTable> CONSEQUENCES = register("books/consequences");
		public static final ResourceKey<LootTable> DERELICT = register("books/derelict");
		public static final ResourceKey<LootTable> DISTANT_MEMORY = register("books/distant_memory");
		public static final ResourceKey<LootTable> FISHERMANS_TALE = register("books/fishermans_tale");
		public static final ResourceKey<LootTable> FORGOTTEN_TALE = register("books/forgotten_tale");
		public static final ResourceKey<LootTable> KEY = register("books/key");
		public static final ResourceKey<LootTable> KNOWLEDGE = register("books/knowledge");
		public static final ResourceKey<LootTable> LEGACIES = register("books/legacies");
		public static final ResourceKey<LootTable> ONLY_THE_BEGINNING = register("books/only_the_beginning");
		public static final ResourceKey<LootTable> POEM = register("books/poem");
		public static final ResourceKey<LootTable> REMAINS = register("books/remains");
		public static final ResourceKey<LootTable> RESPITE = register("books/respite");
		public static final ResourceKey<LootTable> RUINATION = register("books/ruination");
		public static final ResourceKey<LootTable> THE_END = register("books/the_end");
		public static final ResourceKey<LootTable> THE_FORTRESS = register("books/the_fortress");
		public static final ResourceKey<LootTable> THE_LIBRARY = register("books/the_library");
		public static final ResourceKey<LootTable> THE_PORTAL = register("books/the_portal");
        public static final ResourceKey<LootTable> THE_STRONGHOLD = register("books/the_stronghold");
		public static final ResourceKey<LootTable> THE_WARDEN = register("books/the_warden");
		public static final ResourceKey<LootTable> TREASURE_TALE = register("books/treasure_tale");
		public static final ResourceKey<LootTable> WARD = register("books/ward");
    }

	public static int uncommonWeight = 3;
	public static int rareWeight = 2;
	public static int epicWeight = 1;

	public static void init() {
		UnifiedEvents.LootTables.modify((table, id, registries) -> {
			LootPool.Builder pool;
			HolderLookup.RegistryLookup<Biome> biomeLookup = registries.lookup(Registries.BIOME).get();

			boolean isEnderscapeLoaded = UnifiedPlatform.isModLoaded("enderscape") && LaLConfig.get().integrations.enderscape;

			// IMPROVED LOOT

			if (LaLConfig.get().loot.improved_loot) {
				if (LaLConfig.get().integrations.enderscape) {
					if (enderscapeStrongholdRare(id)) {
						pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
								.add(EmptyLootItem.emptyItem().setWeight(2))
								.add(LootItem.lootTableItem(Items.MUSIC_DISC_MALL).setWeight(1));
						table.addPool(pool);
					}
				}
				if (BuiltInLootTables.BURIED_TREASURE.equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(2))
							.add(LootItem.lootTableItem(Items.MUSIC_DISC_BLOCKS).setWeight(1));
					table.addPool(pool);
				}
				if (BuiltInLootTables.JUNGLE_TEMPLE.equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(2))
							.add(LootItem.lootTableItem(Items.MUSIC_DISC_STRAD).setWeight(1));
					table.addPool(pool);
				}
				if (BuiltInLootTables.NETHER_BRIDGE.equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(11))
							.add(LootItem.lootTableItem(Items.MUSIC_DISC_MELLOHI).setWeight(1));
					table.addPool(pool);
				}
				if (BuiltInLootTables.PILLAGER_OUTPOST.equals(id)) {
					table.editPool(item -> item == Items.CROSSBOW, LootEntry.remove());
					pool = LootPool.lootPool().setRolls(UniformGenerator.between(0F, 1F))
							.add(LootItem.lootTableItem(Items.CROSSBOW).setWeight(4))
							.add(LootItem.lootTableItem(Items.CROSSBOW).apply(EnchantWithLevelsFunction.enchantWithLevels(registries, UniformGenerator.between(10F, 30F))).setWeight(1))
							.add(LootItem.lootTableItem(Items.SHIELD).setWeight(1));
					table.addPool(pool);
				}
				if (BuiltInLootTables.SHIPWRECK_MAP.equals(id)) {

					table.editPool(item -> List.of(Items.COPPER_NAUTILUS_ARMOR, Items.IRON_NAUTILUS_ARMOR, Items.GOLDEN_NAUTILUS_ARMOR, Items.DIAMOND_NAUTILUS_ARMOR).contains(item), LootEntry.remove());
					table.editPool(item -> item == Items.COMPASS || item == Items.MAP || item == Items.CLOCK, LootEntry.remove());
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1F))
							.add(LootItem.lootTableItem(Items.MAP)
									.apply(ExplorationMapFunction.makeExplorationMap().setDestination(StructureTags.ON_TREASURE_MAPS).setMapDecoration(MapDecorationTypes.RED_X).setZoom((byte)1).setSkipKnownStructures(false))
									.apply(SetNameFunction.setName(Component.translatable("filled_map.buried_treasure"), SetNameFunction.Target.ITEM_NAME)));
					table.addPool(pool);
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1F))
							.add(LootItem.lootTableItem(Items.COMPASS))
							.add(LootItem.lootTableItem(Items.MAP))
							.add(LootItem.lootTableItem(Items.CLOCK));
					table.addPool(pool);
				}
				if (BuiltInLootTables.SIMPLE_DUNGEON.equals(id)) {
					table.editPool(item -> item == Items.BOOK, LootEntry.insert(LootItem.lootTableItem(Items.BOOK).apply(EnchantRandomlyFunction.randomEnchantment()).setWeight(5)));
					table.editPool(item -> item == Items.MUSIC_DISC_13, LootEntry.replace(LootItem.lootTableItem(Items.MUSIC_DISC_13).setWeight(10)));
					table.editPool(item -> item == Items.MUSIC_DISC_CAT, LootEntry.replace(LootItem.lootTableItem(Items.MUSIC_DISC_CAT).setWeight(10)));
					table.editPool(item -> item == Items.MUSIC_DISC_OTHERSIDE, LootEntry.insert(LootItem.lootTableItem(Items.MUSIC_DISC_STAL).setWeight(10)));
				}
				if (BuiltInLootTables.STRONGHOLD_CROSSING.equals(id) && !isEnderscapeLoaded) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(2))
							.add(LootItem.lootTableItem(Items.MUSIC_DISC_MALL).setWeight(1));
					table.addPool(pool);
				}
				if (BuiltInLootTables.UNDERWATER_RUIN_BIG.equals(id) || BuiltInLootTables.UNDERWATER_RUIN_SMALL.equals(id)) {
					int emptyWeight = 11;
					if (BuiltInLootTables.UNDERWATER_RUIN_SMALL.equals(id)) emptyWeight = 17;
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(emptyWeight))
							.add(LootItem.lootTableItem(Items.MUSIC_DISC_WAIT).setWeight(1));
					table.addPool(pool);
					table.editPool(item -> item == Items.FISHING_ROD, LootEntry.insert(LootItem.lootTableItem(Items.STONE_SWORD).apply(EnchantWithLevelsFunction.enchantWithLevels(registries, UniformGenerator.between(3F, 24F))).setWeight(1)));
					table.editPool(item -> item == Items.FISHING_ROD, LootEntry.insert(LootItem.lootTableItem(Items.STONE_SPEAR).apply(EnchantWithLevelsFunction.enchantWithLevels(registries, UniformGenerator.between(3F, 24F))).setWeight(1)));
					table.editPool(item -> item == Items.FISHING_ROD, LootEntry.insert(LootItem.lootTableItem(Items.STONE_PICKAXE).apply(EnchantWithLevelsFunction.enchantWithLevels(registries, UniformGenerator.between(3F, 24F))).setWeight(1)));
					table.editPool(item -> item == Items.FISHING_ROD, LootEntry.insert(LootItem.lootTableItem(Items.STONE_AXE).apply(EnchantWithLevelsFunction.enchantWithLevels(registries, UniformGenerator.between(3F, 24F))).setWeight(1)));
					table.editPool(item -> item == Items.FISHING_ROD, LootEntry.insert(LootItem.lootTableItem(Items.STONE_SHOVEL).apply(EnchantWithLevelsFunction.enchantWithLevels(registries, UniformGenerator.between(3F, 24F))).setWeight(1)));
					table.editPool(item -> item == Items.FISHING_ROD, LootEntry.insert(LootItem.lootTableItem(Items.STONE_HOE).apply(EnchantWithLevelsFunction.enchantWithLevels(registries, UniformGenerator.between(3F, 24F))).setWeight(1)));
				}
				if (BuiltInLootTables.WOODLAND_MANSION.equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(5))
							.add(LootItem.lootTableItem(Items.MUSIC_DISC_CHIRP).setWeight(1));
					table.addPool(pool);
				}
				if (DEEP_RUINS.equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(2))
							.add(LootItem.lootTableItem(Items.MUSIC_DISC_WARD).setWeight(1));
					table.addPool(pool);
				}
				if (SCULK_RUINS.equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(2))
							.add(LootItem.lootTableItem(Items.MUSIC_DISC_11).setWeight(1));
					table.addPool(pool);
				}
				if (BIRCH_RUINS.equals(id) || CHERRY_RUINS.equals(id) || MAPLE_RUINS.equals(id) || GOLDEN_BIRCH_RUINS.equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(2))
							.add(LootItem.lootTableItem(Items.MUSIC_DISC_FAR).setWeight(1));
					table.addPool(pool);
				}
			}

			// MISC LOOT

			if (EntityType.ELDER_GUARDIAN.getDefaultLootTable().get().equals(id) && LaLConfig.get().loot.trident_shard) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(LootItem.lootTableItem(LaLItems.TRIDENT_SHARD).setWeight(1));
				table.addPool(pool);
			}
			if (LaLConfig.get().loot.glow_stick) {
				if (BuiltInLootTables.ABANDONED_MINESHAFT.equals(id)) {
					table.editPool(item -> item == Items.TORCH, LootEntry.insert(LootItem.lootTableItem(LaLItems.GLOW_STICK).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 12.0F)))));
				}
				if (BuiltInLootTables.SIMPLE_DUNGEON.equals(id) || DUNGEON_CHEST.equals(id)) {
					table.editPool(item -> item == Items.COAL, LootEntry.insert(LootItem.lootTableItem(LaLItems.GLOW_STICK).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F)))));
				}
				if (UNDERGROUND_CABIN.equals(id) || DEEP_CABIN.equals(id)) {
					pool = LootPool.lootPool().setRolls(UniformGenerator.between(0F, 1F))
							.add(LootItem.lootTableItem(LaLItems.GLOW_STICK).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F))));
					table.addPool(pool);
				}
			}

			// BOOKS

			if (LaLConfig.get().loot.lore_books) {
				if (LaLConfig.get().integrations.enderscape) {
					if (ENDERSCAPE_STRONGHOLD_ALTAR.equals(id)) {
						pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
								.add(EmptyLootItem.emptyItem().setWeight(11))
								.add(NestedLootTable.lootTableReference(Books.THE_END).setWeight(1));
						table.addPool(pool);
					}
					if (ENDERSCAPE_STRONGHOLD_BEDROOM.equals(id)) {
						pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
								.add(EmptyLootItem.emptyItem().setWeight(14))
								.add(NestedLootTable.lootTableReference(Books.THE_END).setWeight(1));
						table.addPool(pool);
					}
					if (ENDERSCAPE_STRONGHOLD_LIBRARY.equals(id)) {
						pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
								.add(EmptyLootItem.emptyItem().setWeight(3))
								.add(NestedLootTable.lootTableReference(Books.THE_STRONGHOLD).setWeight(1))
								.add(NestedLootTable.lootTableReference(Books.THE_PORTAL).setWeight(1))
								.add(NestedLootTable.lootTableReference(Books.THE_LIBRARY).setWeight(1));
						table.addPool(pool);
					}
					if (END_CITY_CHEST.equals(id) && !isEnderscapeLoaded) {
						pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
								.add(EmptyLootItem.emptyItem().setWeight(20))
								.add(NestedLootTable.lootTableReference(Books.POEM).setWeight(1));
						table.addPool(pool);
					}
				}
				if (BuiltInLootTables.FISHING_TREASURE.equals(id)) {
					table.editPool(item -> true, LootEntry.insert(NestedLootTable.lootTableReference(Books.FISHERMANS_TALE).setWeight(1).when(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiomes(biomeLookup.getOrThrow(BiomeTags.IS_DEEP_OCEAN))))));
					table.editPool(item -> true, LootEntry.insert(NestedLootTable.lootTableReference(Books.FORGOTTEN_TALE).setWeight(1).when(LocationCheck.checkLocation(LocationPredicate.Builder.inBiome(biomeLookup.getOrThrow(Biomes.DEEP_DARK))))));
					table.editPool(item -> true, LootEntry.insert(NestedLootTable.lootTableReference(Books.TREASURE_TALE).setWeight(1).when(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiomes(biomeLookup.getOrThrow(TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", "is_shallow_ocean"))))))));
					table.editPool(item -> true, LootEntry.insert(NestedLootTable.lootTableReference(Books.TREASURE_TALE).setWeight(1).when(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiomes(biomeLookup.getOrThrow(BiomeTags.IS_BEACH))))));
				}
				if (BuiltInLootTables.ANCIENT_CITY.equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(69))
							.add(NestedLootTable.lootTableReference(Books.THE_WARDEN).setWeight(4))
							.add(NestedLootTable.lootTableReference(Books.WARD).setWeight(2));
					table.addPool(pool);
				}
				if (BuiltInLootTables.ANCIENT_CITY_ICE_BOX.equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(5))
							.add(NestedLootTable.lootTableReference(Books.KEY).setWeight(1));
					table.addPool(pool);
				}
				if (BuiltInLootTables.BASTION_OTHER.equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(17))
							.add(NestedLootTable.lootTableReference(Books.REMAINS).setWeight(1));
					table.addPool(pool);
				}
				if (BuiltInLootTables.END_CITY_TREASURE.equals(id) && !isEnderscapeLoaded) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(17))
							.add(NestedLootTable.lootTableReference(Books.POEM).setWeight(1));
					table.addPool(pool);
				}
				if (BuiltInLootTables.NETHER_BRIDGE.equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(17))
							.add(NestedLootTable.lootTableReference(Books.THE_FORTRESS).setWeight(1));
					table.addPool(pool);
				}
				if (BuiltInLootTables.STRONGHOLD_CORRIDOR.equals(id) && !isEnderscapeLoaded) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(8))
							.add(NestedLootTable.lootTableReference(Books.THE_END).setWeight(1));
					table.addPool(pool);
				}
				if (BuiltInLootTables.STRONGHOLD_LIBRARY.equals(id) && !isEnderscapeLoaded) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(3))
							.add(NestedLootTable.lootTableReference(Books.THE_STRONGHOLD).setWeight(4))
							.add(NestedLootTable.lootTableReference(Books.THE_PORTAL).setWeight(4))
							.add(NestedLootTable.lootTableReference(Books.THE_LIBRARY).setWeight(4));
					table.addPool(pool);
				}
				if (BuiltInLootTables.WOODLAND_MANSION.equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(8))
							.add(NestedLootTable.lootTableReference(Books.KNOWLEDGE).setWeight(1));
					table.addPool(pool);
				}
				if (UNDERGROUND_CABIN.equals(id) || DEEP_CABIN.equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(8))
							.add(NestedLootTable.lootTableReference(Books.RESPITE).setWeight(1));
					table.addPool(pool);
				}
				if (DEEP_RUINS.equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(6))
							.add(NestedLootTable.lootTableReference(Books.DERELICT).setWeight(2))
							.add(NestedLootTable.lootTableReference(Books.WARD).setWeight(1));
					table.addPool(pool);
				}
				if (SCULK_RUINS.equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(12))
							.add(NestedLootTable.lootTableReference(Books.RUINATION).setWeight(5))
							.add(NestedLootTable.lootTableReference(Books.WARD).setWeight(1));
					table.addPool(pool);
				}
				if (END_RUINS.equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(11))
							.add(NestedLootTable.lootTableReference(Books.ONLY_THE_BEGINNING).setWeight(1));
					table.addPool(pool);
				}
				if (RUINED_AETHER_PORTAL.equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(2))
							.add(NestedLootTable.lootTableReference(Books.AS_ABOVE).setWeight(1));
					table.addPool(pool);
				}
				if (RUINED_LIBRARY.equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(1))
							.add(NestedLootTable.lootTableReference(Books.DISTANT_MEMORY).setWeight(1))
							.add(NestedLootTable.lootTableReference(Books.LEGACIES).setWeight(1));
					table.addPool(pool);
				}
				if (SPIRE.equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(17))
							.add(NestedLootTable.lootTableReference(Books.ASCENT).setWeight(1));
					table.addPool(pool);
				}
			}

			// ACCESSORIES - Pools

			if (LaLLootTables.OVERWORLD_GENERAL_ACCESSORIES.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(LootItem.lootTableItem(LaLItems.NECKLACE_OF_PROTECTION).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.necklace_of_protection) * uncommonWeight))
						.add(LootItem.lootTableItem(LaLItems.NECKLACE_OF_REGENERATION).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.necklace_of_regeneration) * epicWeight))
						.add(LootItem.lootTableItem(LaLItems.NECKLACE_OF_LEAPING).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.necklace_of_leaping) * uncommonWeight))
						.add(LootItem.lootTableItem(LaLItems.RING_OF_STRIKING).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.ring_of_striking) * uncommonWeight))
						.add(LootItem.lootTableItem(LaLItems.RING_OF_EVASION).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.ring_of_evasion) * rareWeight))
						.add(LootItem.lootTableItem(LaLItems.RING_OF_ARCHERY).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.ring_of_archery) * uncommonWeight))
						.add(LootItem.lootTableItem(LaLItems.RING_OF_RESTORATION).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.ring_of_restoration) * epicWeight))
						.add(LootItem.lootTableItem(LaLItems.AMULET_OF_ABSORPTION).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.amulet_of_absorption) * rareWeight));
				table.addPool(pool);
			}
			if (LaLLootTables.OVERWORLD_ARCHAEOLOGY_ACCESSORIES.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(LootItem.lootTableItem(LaLItems.RING_OF_CONSTRUCTION).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.ring_of_construction) * rareWeight));
				table.addPool(pool);
			}
			if (LaLLootTables.OVERWORLD_SWAMP_HUT_ACCESSORIES.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(LootItem.lootTableItem(LaLItems.NECKLACE_OF_PURITY).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.necklace_of_purity) * rareWeight));
				table.addPool(pool);
			}

			if (LaLLootTables.UNDERGROUND_GENERAL_ACCESSORIES.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(LootItem.lootTableItem(LaLItems.NECKLACE_OF_PROTECTION).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.necklace_of_protection) * uncommonWeight))
						.add(LootItem.lootTableItem(LaLItems.NECKLACE_OF_REGENERATION).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.necklace_of_regeneration) * epicWeight))
						.add(LootItem.lootTableItem(LaLItems.NECKLACE_OF_RESILIENCE).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.necklace_of_resilience) * epicWeight))
						.add(LootItem.lootTableItem(LaLItems.RING_OF_STRIKING).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.ring_of_striking) * uncommonWeight))
						.add(LootItem.lootTableItem(LaLItems.RING_OF_EVASION).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.ring_of_evasion) * rareWeight))
						.add(LootItem.lootTableItem(LaLItems.RING_OF_RESTORATION).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.ring_of_restoration) * epicWeight))
						.add(LootItem.lootTableItem(LaLItems.AMULET_OF_ABSORPTION).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.amulet_of_absorption) * rareWeight));
				table.addPool(pool);
			}
			if (LaLLootTables.UNDERGROUND_MINESHAFT_ACCESSORIES.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(LootItem.lootTableItem(LaLItems.RING_OF_EXCAVATION).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.ring_of_excavation) * rareWeight));
				table.addPool(pool);
			}
			if (LaLLootTables.UNDERGROUND_DEEP_ACCESSORIES.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(LootItem.lootTableItem(LaLItems.TOTEM_OF_RESURRECTION).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.totem_of_resurrection) * epicWeight));
				table.addPool(pool);
			}

			if (LaLLootTables.NETHER_GENERAL_ACCESSORIES.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(LootItem.lootTableItem(LaLItems.NECKLACE_OF_PROTECTION).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.necklace_of_protection) * uncommonWeight))
						.add(LootItem.lootTableItem(LaLItems.RING_OF_STRIKING).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.ring_of_striking) * uncommonWeight))
						.add(LootItem.lootTableItem(LaLItems.RING_OF_HUNTING).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.ring_of_hunting) * rareWeight));
				table.addPool(pool);
			}
			if (LaLLootTables.NETHER_FORTRESS_ACCESSORIES.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(LootItem.lootTableItem(LaLItems.AMULET_OF_OBSIDIAN).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.amulet_of_obsidian) * epicWeight));
				table.addPool(pool);
			}
			if (LaLLootTables.NETHER_PIGLIN_ACCESSORIES.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(LootItem.lootTableItem(LaLItems.NECKLACE_OF_BARTERING).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.necklace_of_bartering) * uncommonWeight));
				table.addPool(pool);
			}

			if (LaLLootTables.END_GENERAL_ACCESSORIES.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(LootItem.lootTableItem(LaLItems.RING_OF_EVASION).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.ring_of_evasion) * rareWeight))
						.add(LootItem.lootTableItem(LaLItems.TOTEM_OF_TELEPORTATION).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.totem_of_teleportation) * rareWeight));
				table.addPool(pool);
			}
			if (LaLLootTables.END_CITY_ACCESSORIES.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(LootItem.lootTableItem(LaLItems.AMULET_OF_DEFLECTION).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.ring_of_evasion) * rareWeight));
				table.addPool(pool);
			}
			if (LaLLootTables.END_RUINS_ACCESSORIES.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(LootItem.lootTableItem(LaLItems.NECKLACE_OF_ISOLATION).setWeight(BooleanUtils.toInteger(LaLConfig.get().accessories.necklace_of_isolation) * rareWeight));
				table.addPool(pool);
			}

			// ACCESSORIES - Injects

			if (LaLLootTables.UNDERGROUND_CABIN.equals(id) || LaLLootTables.DEEP_CABIN.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(2))
						.add(NestedLootTable.lootTableReference(LaLLootTables.UNDERGROUND_GENERAL_ACCESSORIES).setWeight(3))
						.add(NestedLootTable.lootTableReference(LaLLootTables.UNDERGROUND_MINESHAFT_ACCESSORIES).setWeight(1));
				table.addPool(pool);
			}
			if (BuiltInLootTables.ABANDONED_MINESHAFT.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(19))
						.add(NestedLootTable.lootTableReference(LaLLootTables.UNDERGROUND_GENERAL_ACCESSORIES).setWeight(3))
						.add(NestedLootTable.lootTableReference(LaLLootTables.UNDERGROUND_MINESHAFT_ACCESSORIES).setWeight(2));
				table.addPool(pool);
			}

			if (LaLLootTables.DEEP_RUINS.equals(id) || LaLLootTables.SCULK_RUINS.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(3))
						.add(NestedLootTable.lootTableReference(LaLLootTables.UNDERGROUND_GENERAL_ACCESSORIES).setWeight(2))
						.add(NestedLootTable.lootTableReference(LaLLootTables.UNDERGROUND_DEEP_ACCESSORIES).setWeight(1));
				table.addPool(pool);
			}
			if (LaLLootTables.DUNGEON_CHEST_DEEP.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(11))
						.add(NestedLootTable.lootTableReference(LaLLootTables.UNDERGROUND_DEEP_ACCESSORIES).setWeight(1));
				table.addPool(pool);
			}
			if (BuiltInLootTables.ANCIENT_CITY.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(20))
						.add(NestedLootTable.lootTableReference(LaLLootTables.UNDERGROUND_DEEP_ACCESSORIES).setWeight(1));
				table.addPool(pool);
			}

			if (LaLLootTables.BIRCH_RUINS.equals(id) || LaLLootTables.CHERRY_RUINS.equals(id) || LaLLootTables.GOLDEN_BIRCH_RUINS.equals(id) || LaLLootTables.MAPLE_RUINS.equals(id) || LaLLootTables.RUINS.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(2))
						.add(NestedLootTable.lootTableReference(LaLLootTables.OVERWORLD_GENERAL_ACCESSORIES).setWeight(1));
				table.addPool(pool);
			}
			if (LaLLootTables.RUINED_LIBRARY.equals(id) || BuiltInLootTables.IGLOO_CHEST.equals(id) || BuiltInLootTables.JUNGLE_TEMPLE.equals(id) || BuiltInLootTables.PILLAGER_OUTPOST.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(NestedLootTable.lootTableReference(LaLLootTables.OVERWORLD_GENERAL_ACCESSORIES).setWeight(1));
				table.addPool(pool);
			}
			if (BuiltInLootTables.DESERT_PYRAMID.equals(id) || BuiltInLootTables.WOODLAND_MANSION.equals(id) || BuiltInLootTables.SHIPWRECK_TREASURE.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(8))
						.add(NestedLootTable.lootTableReference(LaLLootTables.OVERWORLD_GENERAL_ACCESSORIES).setWeight(1));
				table.addPool(pool);
			}
			if (BuiltInLootTables.UNDERWATER_RUIN_BIG.equals(id) || BuiltInLootTables.UNDERWATER_RUIN_SMALL.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(17))
						.add(NestedLootTable.lootTableReference(LaLLootTables.OVERWORLD_GENERAL_ACCESSORIES).setWeight(1));
				table.addPool(pool);
			}

			if (LaLLootTables.END_RUINS.equals(id) || LaLLootTables.END_REMAINS.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(4))
						.add(NestedLootTable.lootTableReference(LaLLootTables.END_GENERAL_ACCESSORIES).setWeight(1))
						.add(NestedLootTable.lootTableReference(LaLLootTables.END_RUINS_ACCESSORIES).setWeight(1));
				table.addPool(pool);
			}
			if (BuiltInLootTables.END_CITY_TREASURE.equals(id) && !isEnderscapeLoaded) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(28))
						.add(NestedLootTable.lootTableReference(LaLLootTables.END_GENERAL_ACCESSORIES).setWeight(1))
						.add(NestedLootTable.lootTableReference(LaLLootTables.END_CITY_ACCESSORIES).setWeight(1));;
				table.addPool(pool);
			}
			if ((LaLLootTables.END_CITY_VAULT.equals(id) || LaLLootTables.END_CITY_ELYTRA_VAULT.equals(id)) && LaLConfig.get().integrations.enderscape) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(13))
						.add(NestedLootTable.lootTableReference(LaLLootTables.END_GENERAL_ACCESSORIES).setWeight(1))
						.add(NestedLootTable.lootTableReference(LaLLootTables.END_CITY_ACCESSORIES).setWeight(1));;
				table.addPool(pool);
			}

			if (LaLLootTables.SPIRE.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(10))
						.add(NestedLootTable.lootTableReference(LaLLootTables.NETHER_GENERAL_ACCESSORIES).setWeight(1))
						.add(NestedLootTable.lootTableReference(LaLLootTables.NETHER_FORTRESS_ACCESSORIES).setWeight(1));
				table.addPool(pool);
			}
			if (BuiltInLootTables.NETHER_BRIDGE.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(22))
						.add(NestedLootTable.lootTableReference(LaLLootTables.NETHER_GENERAL_ACCESSORIES).setWeight(1))
						.add(NestedLootTable.lootTableReference(LaLLootTables.NETHER_FORTRESS_ACCESSORIES).setWeight(1));
				table.addPool(pool);
			}

			if (BuiltInLootTables.BASTION_OTHER.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(15))
						.add(NestedLootTable.lootTableReference(LaLLootTables.NETHER_GENERAL_ACCESSORIES).setWeight(1))
						.add(NestedLootTable.lootTableReference(LaLLootTables.NETHER_PIGLIN_ACCESSORIES).setWeight(2));
				table.addPool(pool);
			}

			if (LaLLootTables.SWAMP_HUT.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(3))
						.add(NestedLootTable.lootTableReference(LaLLootTables.OVERWORLD_GENERAL_ACCESSORIES).setWeight(1))
						.add(NestedLootTable.lootTableReference(LaLLootTables.OVERWORLD_SWAMP_HUT_ACCESSORIES).setWeight(2));
				table.addPool(pool);
			}

			if (BuiltInLootTables.STRONGHOLD_CORRIDOR.equals(id) || BuiltInLootTables.STRONGHOLD_CROSSING.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(14))
						.add(NestedLootTable.lootTableReference(LaLLootTables.UNDERGROUND_GENERAL_ACCESSORIES).setWeight(1));
				table.addPool(pool);
			}
			if (enderscapeStrongholdRare(id) && LaLConfig.get().integrations.enderscape) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(11))
						.add(NestedLootTable.lootTableReference(LaLLootTables.UNDERGROUND_GENERAL_ACCESSORIES).setWeight(1));
				table.addPool(pool);
			}
			if (enderscapeStrongholdCommon(id) && LaLConfig.get().integrations.enderscape) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(17))
						.add(NestedLootTable.lootTableReference(LaLLootTables.UNDERGROUND_GENERAL_ACCESSORIES).setWeight(1));
				table.addPool(pool);
			}
			if (BuiltInLootTables.TRIAL_CHAMBERS_REWARD.equals(id) || BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS.equals(id)) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(29))
						.add(NestedLootTable.lootTableReference(LaLLootTables.UNDERGROUND_GENERAL_ACCESSORIES).setWeight(1));
				table.addPool(pool);
			}

			// ARTIFACTS - Armor

			if (LaLLootTables.DEEP_RUINS.equals(id) && LaLConfig.get().artifacts.reinforced_chestplate) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(8))
						.add(LootItem.lootTableItem(LaLItems.REINFORCED_CHESTPLATE).setWeight(1));
				table.addPool(pool);
			}
			if (LaLLootTables.SCULK_RUINS.equals(id) && LaLConfig.get().artifacts.reinforced_chestplate) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(LaLItems.REINFORCED_CHESTPLATE).setWeight(1));
				table.addPool(pool);
			}
			if (LaLLootTables.DUNGEON_CHEST_DEEP.equals(id) && LaLConfig.get().artifacts.reinforced_chestplate) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(20))
						.add(LootItem.lootTableItem(LaLItems.REINFORCED_CHESTPLATE).setWeight(1));
				table.addPool(pool);
			}
			if (BuiltInLootTables.ANCIENT_CITY.equals(id) && LaLConfig.get().artifacts.reinforced_chestplate) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(20))
						.add(LootItem.lootTableItem(LaLItems.REINFORCED_CHESTPLATE).setWeight(1));
				table.addPool(pool);
			}

			if (LaLLootTables.DUNGEON_CHEST_ARID.equals(id) && LaLConfig.get().artifacts.travelling_strides) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(11))
						.add(LootItem.lootTableItem(LaLItems.TRAVELLING_STRIDES).setWeight(1));
				table.addPool(pool);
			}
			// Travelling Strides Crafting Recipe

			if (LaLLootTables.DUNGEON_CHEST_SIMPLE.equals(id) && LaLConfig.get().artifacts.wanderer_boots) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(14))
						.add(LootItem.lootTableItem(LaLItems.WANDERER_BOOTS).setWeight(1));
				table.addPool(pool);
			}
			if (BuiltInLootTables.SIMPLE_DUNGEON.equals(id) && LaLConfig.get().artifacts.wanderer_boots) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(8))
						.add(LootItem.lootTableItem(LaLItems.WANDERER_BOOTS).setWeight(1));
				table.addPool(pool);
			}

			// ARTIFACTS - Tools

			if (LaLLootTables.DUNGEON_CHEST_VERDANT.equals(id) && LaLConfig.get().artifacts.verdant_sword) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(11))
						.add(LootItem.lootTableItem(LaLItems.VERDANT_SWORD).setWeight(1));
				table.addPool(pool);
			}
			if (BuiltInLootTables.JUNGLE_TEMPLE.equals(id) && LaLConfig.get().artifacts.verdant_sword) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(LaLItems.VERDANT_SWORD).setWeight(1));
				table.addPool(pool);
			}

			if (LaLLootTables.DUNGEON_CHEST.equals(id) && LaLConfig.get().artifacts.cleaving_battleaxe) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(17))
						.add(LootItem.lootTableItem(LaLItems.CLEAVING_BATTLEAXE).setWeight(1));
				table.addPool(pool);
			}
			if (BuiltInLootTables.SIMPLE_DUNGEON.equals(id) && LaLConfig.get().artifacts.cleaving_battleaxe) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(8))
						.add(LootItem.lootTableItem(LaLItems.CLEAVING_BATTLEAXE).setWeight(1));
				table.addPool(pool);
			}

			if (BuiltInLootTables.NETHER_BRIDGE.equals(id) && LaLConfig.get().artifacts.molten_pickaxe) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(8))
						.add(LootItem.lootTableItem(LaLItems.MOLTEN_PICKAXE).setWeight(1));
				table.addPool(pool);
			}
			if (LaLLootTables.SPIRE.equals(id) && LaLConfig.get().artifacts.molten_pickaxe) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(11))
						.add(LootItem.lootTableItem(LaLItems.MOLTEN_PICKAXE).setWeight(1));
				table.addPool(pool);
			}

			if (BuiltInLootTables.BURIED_TREASURE.equals(id) && LaLConfig.get().artifacts.prospector_shovel) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(2))
						.add(LootItem.lootTableItem(LaLItems.PROSPECTOR_SHOVEL).setWeight(1));
				table.addPool(pool);
			}

			if (EntityType.WITHER_SKELETON.getDefaultLootTable().get().equals(id) && LaLConfig.get().artifacts.withered_hoe) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(LootItem.lootTableItem(LaLItems.WITHERED_HOE).setWeight(1).when(LootItemRandomChanceWithEnchantedBonusCondition.randomChanceAndLootingBoost(registries, 0.0125F, 0.0025F)));
				table.addPool(pool);
			}

			if (BuiltInLootTables.IGLOO_CHEST.equals(id) && LaLConfig.get().artifacts.frosted_spear && LaLConfig.get().structures.dungeon_overhaul) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(8))
						.add(LootItem.lootTableItem(LaLItems.FROSTED_SPEAR).setWeight(1));
				table.addPool(pool);
			}
			else if (BuiltInLootTables.IGLOO_CHEST.equals(id) && LaLConfig.get().artifacts.frosted_spear && !LaLConfig.get().structures.dungeon_overhaul) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(2))
						.add(LootItem.lootTableItem(LaLItems.FROSTED_SPEAR).setWeight(1));
				table.addPool(pool);
			}
			if (LaLLootTables.DUNGEON_CHEST_FROZEN.equals(id) && LaLConfig.get().artifacts.frosted_spear) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(11))
						.add(LootItem.lootTableItem(LaLItems.FROSTED_SPEAR).setWeight(1));
				table.addPool(pool);
			}
			if (BuiltInLootTables.SIMPLE_DUNGEON.equals(id) && LaLConfig.get().artifacts.frosted_spear) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(17))
						.add(LootItem.lootTableItem(LaLItems.FROSTED_SPEAR).setWeight(1));
				table.addPool(pool);
			}

			// ARTIFACTS - Totems

			if (LaLLootTables.DUNGEON_CHEST_DEEP.equals(id) && LaLConfig.get().accessories.totem_of_resurrection) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(14))
						.add(LootItem.lootTableItem(LaLItems.TOTEM_OF_RESURRECTION).setWeight(1));
				table.addPool(pool);
			}

			if (BuiltInLootTables.END_CITY_TREASURE.equals(id) && !isEnderscapeLoaded && LaLConfig.get().accessories.totem_of_teleportation) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(20))
						.add(LootItem.lootTableItem(LaLItems.TOTEM_OF_TELEPORTATION).setWeight(1));
				table.addPool(pool);
			}

			// ARTIFACTS - Tablets

			if (BuiltInLootTables.WOODLAND_MANSION.equals(id) && LaLConfig.get().artifacts.tablet_of_channeling) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_CHANNELING).setWeight(1));
				table.addPool(pool);
			}

			if (BuiltInLootTables.ANCIENT_CITY.equals(id) && LaLConfig.get().artifacts.tablet_of_deafening) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(11))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_DEAFENING).setWeight(1));
				table.addPool(pool);
			}

			if (BuiltInLootTables.ABANDONED_MINESHAFT.equals(id) && LaLConfig.get().artifacts.tablet_of_haste) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_HASTE).setWeight(1));
				table.addPool(pool);
			}

			if (BuiltInLootTables.ABANDONED_MINESHAFT.equals(id) && LaLConfig.get().artifacts.tablet_of_revealing) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_REVEALING).setWeight(1));
				table.addPool(pool);
			}

			if (LaLLootTables.RUINED_AETHER_PORTAL.equals(id) && LaLConfig.get().artifacts.tablet_of_recall) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_RECALL).setWeight(1));
				table.addPool(pool);
			}
			if (LaLLootTables.DEEP_RUINS.equals(id) && LaLConfig.get().artifacts.tablet_of_recall) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(11))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_RECALL).setWeight(1));
				table.addPool(pool);
			}
			if (LaLLootTables.SCULK_RUINS.equals(id) && LaLConfig.get().artifacts.tablet_of_recall) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(11))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_RECALL).setWeight(1));
				table.addPool(pool);
			}
			if (LaLLootTables.BIRCH_RUINS.equals(id) || LaLLootTables.CHERRY_RUINS.equals(id) || LaLLootTables.GOLDEN_BIRCH_RUINS.equals(id) || LaLLootTables.MAPLE_RUINS.equals(id) && LaLConfig.get().artifacts.tablet_of_recall) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_RECALL).setWeight(1));
				table.addPool(pool);
			}
			if (LaLLootTables.DUNGEON_CHEST_ARID.equals(id) && LaLConfig.get().artifacts.tablet_of_recall) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_RECALL).setWeight(1));
				table.addPool(pool);
			}
			if (LaLLootTables.DUNGEON_CHEST_FROZEN.equals(id) && LaLConfig.get().artifacts.tablet_of_recall) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_RECALL).setWeight(1));
				table.addPool(pool);
			}
			if (LaLLootTables.DUNGEON_CHEST_SIMPLE.equals(id) && LaLConfig.get().artifacts.tablet_of_recall) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_RECALL).setWeight(1));
				table.addPool(pool);
			}
			if (LaLLootTables.DUNGEON_CHEST_VERDANT.equals(id) && LaLConfig.get().artifacts.tablet_of_recall) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_RECALL).setWeight(1));
				table.addPool(pool);
			}
			if (LaLLootTables.DUNGEON_CHEST_INFERNAL.equals(id) && LaLConfig.get().artifacts.tablet_of_recall) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(11))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_RECALL).setWeight(1));
				table.addPool(pool);
			}
			if (LaLLootTables.DUNGEON_CHEST_DEEP.equals(id) && LaLConfig.get().artifacts.tablet_of_recall) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(11))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_RECALL).setWeight(1));
				table.addPool(pool);
			}
			if (BuiltInLootTables.ABANDONED_MINESHAFT.equals(id) && LaLConfig.get().artifacts.tablet_of_recall) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(8))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_RECALL).setWeight(1));
				table.addPool(pool);
			}
			if (BuiltInLootTables.STRONGHOLD_CORRIDOR.equals(id) && LaLConfig.get().artifacts.tablet_of_recall) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_RECALL).setWeight(1));
				table.addPool(pool);
			}
			if (enderscapeStrongholdRare(id) && LaLConfig.get().artifacts.tablet_of_recall && LaLConfig.get().integrations.enderscape) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_RECALL).setWeight(1));
				table.addPool(pool);
			}
			if (ENDERSCAPE_STRONGHOLD_ALTAR.equals(id) && LaLConfig.get().artifacts.tablet_of_recall && LaLConfig.get().integrations.enderscape) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(8))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_RECALL).setWeight(1));
				table.addPool(pool);
			}
			if (enderscapeStrongholdCommon(id) && LaLConfig.get().artifacts.tablet_of_recall && LaLConfig.get().integrations.enderscape) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(14))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_RECALL).setWeight(1));
				table.addPool(pool);
			}
			if (BuiltInLootTables.RUINED_PORTAL.equals(id) && LaLConfig.get().artifacts.tablet_of_recall) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(11))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_RECALL).setWeight(1));
				table.addPool(pool);
			}
			if (BuiltInLootTables.END_CITY_TREASURE.equals(id) && !isEnderscapeLoaded && LaLConfig.get().artifacts.tablet_of_recall) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(29))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_RECALL).setWeight(1));
				table.addPool(pool);
			}
			if (LaLConfig.get().integrations.enderscape) {
				if (LaLLootTables.END_CITY_VAULT.equals(id) && LaLConfig.get().artifacts.tablet_of_recall) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(14))
							.add(LootItem.lootTableItem(LaLItems.TABLET_OF_RECALL).setWeight(1));
					table.addPool(pool);
				}
				if (LaLLootTables.END_CITY_ELYTRA_VAULT.equals(id) && LaLConfig.get().artifacts.tablet_of_recall) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(14))
							.add(LootItem.lootTableItem(LaLItems.TABLET_OF_RECALL).setWeight(1));
					table.addPool(pool);
				}
			}
			if (BuiltInLootTables.SIMPLE_DUNGEON.equals(id) && LaLConfig.get().artifacts.tablet_of_recall) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_RECALL).setWeight(1));
				table.addPool(pool);
			}

			if (LaLLootTables.DEEP_RUINS.equals(id) && LaLConfig.get().artifacts.tablet_of_instability) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_INSTABILITY).setWeight(1));
				table.addPool(pool);
			}
			if (LaLLootTables.SCULK_RUINS.equals(id) && LaLConfig.get().artifacts.tablet_of_instability) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_INSTABILITY).setWeight(1));
				table.addPool(pool);
			}
			if (LaLLootTables.DUNGEON_CHEST_DEEP.equals(id) && LaLConfig.get().artifacts.tablet_of_instability) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(11))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_INSTABILITY).setWeight(1));
				table.addPool(pool);
			}

			if (BuiltInLootTables.END_CITY_TREASURE.equals(id) && !isEnderscapeLoaded && LaLConfig.get().artifacts.tablet_of_warping) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(14))
						.add(LootItem.lootTableItem(LaLItems.TABLET_OF_WARPING).setWeight(1));
				table.addPool(pool);
			}
			if (LaLConfig.get().integrations.enderscape) {
				if (LaLLootTables.END_CITY_VAULT.equals(id) && LaLConfig.get().artifacts.tablet_of_warping) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(8))
							.add(LootItem.lootTableItem(LaLItems.TABLET_OF_WARPING).setWeight(1));
					table.addPool(pool);
				}
				if (LaLLootTables.END_CITY_ELYTRA_VAULT.equals(id) && LaLConfig.get().artifacts.tablet_of_warping) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(8))
							.add(LootItem.lootTableItem(LaLItems.TABLET_OF_WARPING).setWeight(1));
					table.addPool(pool);
				}
			}

			// LOOT - General

			if (LaLConfig.get().loot.enchanted_beetroot) {
				if (LaLConfig.get().integrations.enderscape) {
					if (LaLLootTables.END_CITY_CHEST.equals(id) && LaLConfig.get().loot.enchanted_beetroot) {
						pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
								.add(EmptyLootItem.emptyItem().setWeight(17))
								.add(LootItem.lootTableItem(LaLItems.ENCHANTED_BEETROOT).setWeight(1));
						table.addPool(pool);
					}
					if (LaLLootTables.ENDERSCAPE_STRONGHOLD_ALTAR.equals(id) && LaLConfig.get().loot.enchanted_beetroot) {
						pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
								.add(EmptyLootItem.emptyItem().setWeight(11))
								.add(LootItem.lootTableItem(LaLItems.ENCHANTED_BEETROOT).setWeight(1));
						table.addPool(pool);
					}
					if (enderscapeStrongholdRare(id) && LaLConfig.get().loot.enchanted_beetroot) {
						pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
								.add(EmptyLootItem.emptyItem().setWeight(2))
								.add(LootItem.lootTableItem(LaLItems.ENCHANTED_BEETROOT).setWeight(1));
						table.addPool(pool);
					}
				}
			}

			if (LaLLootTables.DUNGEON_CHEST.equals(id) && LaLConfig.get().loot.metal_chunk) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(LaLItems.METAL_CHUNK).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))));
				table.addPool(pool);
			}

			if (LaLLootTables.END_REMAINS.equals(id) && LaLConfig.get().loot.lore_books) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(95))
						.add(NestedLootTable.lootTableReference(Books.CONSEQUENCES).setWeight(5));
				table.addPool(pool);
			}

			// LOOT - Weapons

			if (BuiltInLootTables.SIMPLE_DUNGEON.equals(id) && LaLConfig.get().loot.boomerang) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(8))
						.add(LootItem.lootTableItem(LaLItems.BOOMERANG).setWeight(1));
				table.addPool(pool);
			}
			if (LaLLootTables.DUNGEON_CHEST.equals(id) && LaLConfig.get().loot.boomerang) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(11))
						.add(LootItem.lootTableItem(LaLItems.BOOMERANG).setWeight(1));
				table.addPool(pool);
			}

			// LOOT - Music Discs

			if (BuiltInLootTables.ABANDONED_MINESHAFT.equals(id) && LaLConfig.get().loot.new_music_discs) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(LaLItems.MUSIC_DISC_SVALL).setWeight(1));
				table.addPool(pool);
			}
			if (BuiltInLootTables.ABANDONED_MINESHAFT.equals(id) && LaLConfig.get().loot.new_music_discs) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(LaLItems.MUSIC_DISC_SVALL).setWeight(1));
				table.addPool(pool);
			}

			if ((LaLLootTables.UNDERGROUND_CABIN.equals(id) || LaLLootTables.DEEP_CABIN.equals(id)) && LaLConfig.get().loot.new_music_discs) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(2))
						.add(LootItem.lootTableItem(LaLItems.MUSIC_DISC_GRAVEL).setWeight(1));
				table.addPool(pool);
			}

			if (LaLLootTables.DEEP_RUINS.equals(id) && LaLConfig.get().loot.new_music_discs) {
				pool = LootPool.lootPool().setRolls(UniformGenerator.between(0.0F, 1.0F))
						.add(LootItem.lootTableItem(LaLItems.MUSIC_DISC_TASWELL).setWeight(1));
				table.addPool(pool);
			}

			if (BuiltInLootTables.END_CITY_TREASURE.equals(id) && !isEnderscapeLoaded && LaLConfig.get().loot.new_music_discs) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(20))
						.add(LootItem.lootTableItem(LaLItems.MUSIC_DISC_SHULKER).setWeight(1));
				table.addPool(pool);
			}
			if (LaLConfig.get().integrations.enderscape) {
				if (LaLLootTables.END_CITY_CHEST.equals(id) && LaLConfig.get().loot.new_music_discs) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(11))
							.add(LootItem.lootTableItem(LaLItems.MUSIC_DISC_SHULKER).setWeight(1));
					table.addPool(pool);
				}
			}

			if (LaLLootTables.PALE_CABIN_SECRET.equals(id) && LaLConfig.get().loot.new_music_discs) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(LootItem.lootTableItem(LaLItems.MUSIC_DISC_INFINITE_SPOOKY_AMETHYST).setWeight(1));
				table.addPool(pool);
			}
			else if (LaLLootTables.PALE_CABIN_SECRET.equals(id) && !LaLConfig.get().loot.new_music_discs) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(LootItem.lootTableItem(Items.DIAMOND).setWeight(1));
				table.addPool(pool);
			}

			if (LaLLootTables.END_RUINS.equals(id) && LaLConfig.get().loot.new_music_discs) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(2))
						.add(LootItem.lootTableItem(LaLItems.MUSIC_DISC_113).setWeight(1));
				table.addPool(pool);
			}

			if (BuiltInLootTables.IGLOO_CHEST.equals(id) && LaLConfig.get().loot.new_music_discs && !LaLConfig.get().structures.dungeon_overhaul) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(2))
						.add(LootItem.lootTableItem(LaLItems.MUSIC_DISC_TUNDRA).setWeight(1));
				table.addPool(pool);
			}
			else if (BuiltInLootTables.IGLOO_CHEST.equals(id) && LaLConfig.get().loot.new_music_discs && LaLConfig.get().structures.dungeon_overhaul) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(LaLItems.MUSIC_DISC_TUNDRA).setWeight(1));
				table.addPool(pool);
			}
			if (LaLLootTables.DUNGEON_CHEST_FROZEN.equals(id) && LaLConfig.get().loot.new_music_discs) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(8))
						.add(LootItem.lootTableItem(LaLItems.MUSIC_DISC_TUNDRA).setWeight(1));
				table.addPool(pool);
			}

			if (BuiltInLootTables.WOODLAND_MANSION.equals(id) && LaLConfig.get().loot.new_music_discs) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(20))
						.add(LootItem.lootTableItem(LaLItems.DISC_FRAGMENT_FAR_LANDS).setWeight(1));
				table.addPool(pool);
			}
			if (BuiltInLootTables.STRONGHOLD_CROSSING.equals(id) && LaLConfig.get().loot.new_music_discs) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(8))
						.add(LootItem.lootTableItem(LaLItems.DISC_FRAGMENT_FAR_LANDS).setWeight(1));
				table.addPool(pool);
			}
			if (ENDERSCAPE_STRONGHOLD_ALTAR.equals(id) && LaLConfig.get().loot.new_music_discs && LaLConfig.get().integrations.enderscape) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(8))
						.add(LootItem.lootTableItem(LaLItems.DISC_FRAGMENT_FAR_LANDS).setWeight(1));
				table.addPool(pool);
			}
			if (BuiltInLootTables.JUNGLE_TEMPLE.equals(id) && LaLConfig.get().loot.new_music_discs) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(LaLItems.DISC_FRAGMENT_FAR_LANDS).setWeight(1));
				table.addPool(pool);
			}
			if (BuiltInLootTables.ABANDONED_MINESHAFT.equals(id) && LaLConfig.get().loot.new_music_discs) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(20))
						.add(LootItem.lootTableItem(LaLItems.DISC_FRAGMENT_FAR_LANDS).setWeight(1));
				table.addPool(pool);
			}
			if (LaLLootTables.RUINS.equals(id) && LaLConfig.get().loot.new_music_discs) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(2))
						.add(LootItem.lootTableItem(LaLItems.DISC_FRAGMENT_FAR_LANDS).setWeight(1));
				table.addPool(pool);
			}
			if (LaLLootTables.RUINED_LIBRARY.equals(id) && LaLConfig.get().loot.new_music_discs) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(2))
						.add(LootItem.lootTableItem(LaLItems.DISC_FRAGMENT_FAR_LANDS).setWeight(1));
				table.addPool(pool);
			}
			// ENCHANTMENTS

			if (BuiltInLootTables.JUNGLE_TEMPLE.equals(id) && LaLConfig.get().enchantments.tangled) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(1)).apply((new SetEnchantmentsFunction.Builder()).withEnchantment(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(LaLEnchantments.TANGLED), UniformGenerator.between(1.0F, 1.0F)));
				table.addPool(pool);
			}
			if (LaLLootTables.RUINS.equals(id) && LaLConfig.get().enchantments.tangled) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(1)).apply((new SetEnchantmentsFunction.Builder()).withEnchantment(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(LaLEnchantments.TANGLED), UniformGenerator.between(1.0F, 1.0F)));
				table.addPool(pool);
			}

			if (BuiltInLootTables.SIMPLE_DUNGEON.equals(id) && LaLConfig.get().enchantments.rejuvenate) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(11))
						.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(1)).apply((new SetEnchantmentsFunction.Builder()).withEnchantment(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(LaLEnchantments.REJUVENATE), UniformGenerator.between(1.0F, 1.0F)));
				table.addPool(pool);
			}
			if (LaLLootTables.DUNGEON_CHEST_SIMPLE.equals(id) && LaLConfig.get().enchantments.rejuvenate) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(11))
						.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(1)).apply((new SetEnchantmentsFunction.Builder()).withEnchantment(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(LaLEnchantments.REJUVENATE), UniformGenerator.between(1.0F, 1.0F)));
				table.addPool(pool);
			}
			if (LaLLootTables.DUNGEON_LIBRARY_SIMPLE.equals(id) && LaLConfig.get().enchantments.rejuvenate) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(1)).apply((new SetEnchantmentsFunction.Builder()).withEnchantment(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(LaLEnchantments.REJUVENATE), UniformGenerator.between(1.0F, 1.0F)));
				table.addPool(pool);
			}

			if (BuiltInLootTables.SIMPLE_DUNGEON.equals(id) && LaLConfig.get().enchantments.featherweight) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(11))
						.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(1)).apply((new SetEnchantmentsFunction.Builder()).withEnchantment(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(LaLEnchantments.FEATHERWEIGHT), UniformGenerator.between(1.0F, 3.0F)));
				table.addPool(pool);
			}
			if (LaLLootTables.DUNGEON_CHEST_ARID.equals(id) && LaLConfig.get().enchantments.featherweight) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(1)).apply((new SetEnchantmentsFunction.Builder()).withEnchantment(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(LaLEnchantments.FEATHERWEIGHT), UniformGenerator.between(1.0F, 3.0F)));
				table.addPool(pool);
			}
			if (LaLLootTables.DUNGEON_LIBRARY_ARID.equals(id) && LaLConfig.get().enchantments.featherweight) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(2))
						.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(1)).apply((new SetEnchantmentsFunction.Builder()).withEnchantment(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(LaLEnchantments.FEATHERWEIGHT), UniformGenerator.between(1.0F, 3.0F)));
				table.addPool(pool);
			}

			if (BuiltInLootTables.SIMPLE_DUNGEON.equals(id) && LaLConfig.get().enchantments.freeze) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(11))
						.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(1)).apply((new SetEnchantmentsFunction.Builder()).withEnchantment(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(LaLEnchantments.FREEZE), UniformGenerator.between(1.0F, 1.0F)));
				table.addPool(pool);
			}
			if (LaLLootTables.DUNGEON_CHEST_FROZEN.equals(id) && LaLConfig.get().enchantments.freeze) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(1)).apply((new SetEnchantmentsFunction.Builder()).withEnchantment(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(LaLEnchantments.FREEZE), UniformGenerator.between(1.0F, 1.0F)));
				table.addPool(pool);
			}
			if (LaLLootTables.DUNGEON_LIBRARY_FROZEN.equals(id) && LaLConfig.get().enchantments.freeze) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(2))
						.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(1)).apply((new SetEnchantmentsFunction.Builder()).withEnchantment(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(LaLEnchantments.FREEZE), UniformGenerator.between(1.0F, 1.0F)));
				table.addPool(pool);
			}

			if (BuiltInLootTables.SIMPLE_DUNGEON.equals(id) && LaLConfig.get().enchantments.decay) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(11))
						.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(1)).apply((new SetEnchantmentsFunction.Builder()).withEnchantment(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(LaLEnchantments.DECAY), UniformGenerator.between(1.0F, 1.0F)));
				table.addPool(pool);
			}
			if (LaLLootTables.DUNGEON_CHEST_VERDANT.equals(id) && LaLConfig.get().enchantments.decay) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(1)).apply((new SetEnchantmentsFunction.Builder()).withEnchantment(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(LaLEnchantments.DECAY), UniformGenerator.between(1.0F, 1.0F)));
				table.addPool(pool);
			}
			if (LaLLootTables.DUNGEON_LIBRARY_VERDANT.equals(id) && LaLConfig.get().enchantments.decay) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(2))
						.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(1)).apply((new SetEnchantmentsFunction.Builder()).withEnchantment(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(LaLEnchantments.DECAY), UniformGenerator.between(1.0F, 1.0F)));
				table.addPool(pool);
			}

			if (BuiltInLootTables.SIMPLE_DUNGEON.equals(id) && LaLConfig.get().enchantments.shadowstep) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(11))
						.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(1)).apply((new SetEnchantmentsFunction.Builder()).withEnchantment(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(LaLEnchantments.REJUVENATE), UniformGenerator.between(1.0F, 1.0F)));
				table.addPool(pool);
			}
			if (LaLLootTables.DUNGEON_CHEST_DEEP.equals(id) && LaLConfig.get().enchantments.shadowstep) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(11))
						.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(1)).apply((new SetEnchantmentsFunction.Builder()).withEnchantment(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(LaLEnchantments.SHADOWSTEP), UniformGenerator.between(1.0F, 1.0F)));
				table.addPool(pool);
			}
			if (LaLLootTables.DUNGEON_LIBRARY_DEEP.equals(id) && LaLConfig.get().enchantments.shadowstep) {
				pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
						.add(EmptyLootItem.emptyItem().setWeight(5))
						.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(1)).apply((new SetEnchantmentsFunction.Builder()).withEnchantment(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(LaLEnchantments.SHADOWSTEP), UniformGenerator.between(1.0F, 1.0F)));
				table.addPool(pool);
			}

			if (LaLLootTables.END_RUINS.equals(id)) {
				table.editPool(item -> item == Items.BEETROOT, LootEntry.replace(LootItem.lootTableItem(LaLItems.ENCHANTED_BEETROOT).setWeight(6)));
			}

			if (!isEnderscapeLoaded) {
				if (BuiltInLootTables.END_CITY_TREASURE.equals(id)) {
					table.editPool(item -> item == Items.BEETROOT_SEEDS, LootEntry.insert(LootItem.lootTableItem(LaLItems.ENCHANTED_BEETROOT).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 1.0F))).setWeight(1)));
				}
				if (BuiltInLootTables.STRONGHOLD_CORRIDOR.equals(id)) {
					table.editPool(item -> item == Items.GOLDEN_APPLE, LootEntry.insert(LootItem.lootTableItem(LaLItems.ENCHANTED_BEETROOT).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 1.0F))).setWeight(3)));
				}
				if (BuiltInLootTables.STRONGHOLD_CROSSING.equals(id)) {
					table.editPool(item -> item == Items.APPLE, LootEntry.insert(LootItem.lootTableItem(LaLItems.ENCHANTED_BEETROOT).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 1.0F))).setWeight(3)));
				}
			}
			if (LaLLootTables.END_REMAINS.equals(id)) {
				table.editPool(item -> true, LootEntry.insert(LootItem.lootTableItem(LaLItems.ENCHANTED_BEETROOT).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 1.0F))).setWeight(3)));
			}

			if (BuiltInLootTables.FISHING_JUNK.equals(id)) {
				if (LaLConfig.get().loot.hook) {
					table.editPool(item -> item == Items.LILY_PAD, LootEntry.insert(LootItem.lootTableItem(LaLItems.HOOK).apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.0F, 0.9F))).setWeight(2)));
				}
				if (LaLConfig.get().loot.metal_chunk) {
					table.editPool(item -> item == Items.LILY_PAD, LootEntry.insert(LootItem.lootTableItem(LaLItems.METAL_CHUNK).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))).setWeight(10)));
				}
				if (LaLConfig.get().loot.wooden_buckets) {
					table.editPool(item -> item == Items.LILY_PAD, LootEntry.insert(LootItem.lootTableItem(LaLItems.WOODEN_BUCKET).setWeight(10)));
				}
			}

			if (LaLConfig.get().loot.wooden_buckets) {
				if (BuiltInLootTables.SHIPWRECK_SUPPLY.equals(id)) {
					table.editPool(item -> item == Items.PAPER, LootEntry.insert(LootItem.lootTableItem(LaLItems.WOODEN_BUCKET).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 1.0F))).setWeight(3)));
				}
				if (RUINS_ARCHAEOLOGY.equals(id)) {
					table.editPool(item -> item == Items.BUCKET, LootEntry.replace(LootItem.lootTableItem(LaLItems.WOODEN_BUCKET)));
				}
			}

			if (LaLConfig.get().loot.hook) {
				if (BuiltInLootTables.FISHING_TREASURE.equals(id)) {
					table.editPool(item -> item == Items.FISHING_ROD, LootEntry.insert(LootItem.lootTableItem(LaLItems.HOOK).apply(EnchantRandomlyFunction.randomApplicableEnchantment(registries)).setWeight(1)));
				}
			}

			if (BuiltInLootTables.UNDERWATER_RUIN_BIG.equals(id)) {
				table.editPool(item -> item == Items.FISHING_ROD, LootEntry.insert(LootItem.lootTableItem(LaLItems.HOOK).apply(EnchantRandomlyFunction.randomApplicableEnchantment(registries)).setWeight(3)));
			}
			if (BuiltInLootTables.UNDERWATER_RUIN_SMALL.equals(id)) {
				table.editPool(item -> item == Items.FISHING_ROD, LootEntry.insert(LootItem.lootTableItem(LaLItems.HOOK).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 1.0F))).setWeight(3)));
			}

			if (LaLConfig.get().loot.new_music_discs) {
				if (BuiltInLootTables.SIMPLE_DUNGEON.equals(id)) {
					table.editPool(item -> item == Items.GOLDEN_APPLE, LootEntry.insert(LootItem.lootTableItem(LaLItems.DISC_FRAGMENT_FAR_LANDS).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 1.0F))).setWeight(10)));
				}
				if (LaLLootTables.DUNGEON_CHEST_ARID.equals(id)) {
					table.editPool(item -> item == Items.GOLDEN_APPLE, LootEntry.insert(LootItem.lootTableItem(LaLItems.DISC_FRAGMENT_FAR_LANDS).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 1.0F))).setWeight(10)));
				}
				if (LaLLootTables.DUNGEON_CHEST_FROZEN.equals(id)) {
					table.editPool(item -> item == Items.GOLDEN_APPLE, LootEntry.insert(LootItem.lootTableItem(LaLItems.DISC_FRAGMENT_FAR_LANDS).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 1.0F))).setWeight(10)));
				}
				if (LaLLootTables.DUNGEON_CHEST_SIMPLE.equals(id) || LaLLootTables.DUNGEON_CHEST_VERDANT.equals(id)) {
					table.editPool(item -> item == Items.MUSIC_DISC_13, LootEntry.replace(LootItem.lootTableItem(LaLItems.MUSIC_DISC_CASTLES).setWeight(10)));
					table.editPool(item -> item == Items.GOLDEN_APPLE, LootEntry.insert(LootItem.lootTableItem(LaLItems.DISC_FRAGMENT_FAR_LANDS).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 1.0F))).setWeight(10)));
				}
				if (LaLLootTables.DUNGEON_CHEST_SIMPLE.equals(id)) {
					table.editPool(item -> item == Items.MUSIC_DISC_13, LootEntry.insert(LootItem.lootTableItem(LaLItems.DISC_FRAGMENT_FAR_LANDS).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 1.0F))).setWeight(10)));
				}
			}

			if (BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_RARE.equals(id) || LaLLootTables.OBELISK_ARCHAEOLOGY.equals(id)) {
				if (LaLConfig.get().accessories.ring_of_construction) {
					table.editPool(item -> true, LootEntry.insert(LootItem.lootTableItem(LaLItems.RING_OF_CONSTRUCTION).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 1.0F))).setWeight(1)));
				}
				if (LaLConfig.get().loot.knife) {
					table.editPool(item -> true, LootEntry.insert(LootItem.lootTableItem(LaLItems.KNIFE).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 1.0F))).setWeight(1)));
				}
			}

			// MODIFIED VANILLA LOOT

			if (LaLConfig.get().loot.improved_loot && BuiltInLootTables.FISHING_JUNK.equals(id)) {
				table.editPool(item -> true, LootEntry.insert(LootItem.lootTableItem(Items.BAMBOO).setWeight(10).when(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiomes(biomeLookup.getOrThrow(BiomeTags.IS_JUNGLE))))));
				table.editPool(item -> true, LootEntry.insert(LootItem.lootTableItem(Items.COCOA_BEANS).setWeight(10).when(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiomes(biomeLookup.getOrThrow(BiomeTags.IS_JUNGLE))))));
				table.editPool(item -> true, LootEntry.insert(LootItem.lootTableItem(Items.KELP).setWeight(10).when(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiomes(biomeLookup.getOrThrow(BiomeTags.IS_OCEAN))))));
				table.editPool(item -> true, LootEntry.insert(LootItem.lootTableItem(Items.RED_MUSHROOM).setWeight(10).when(LocationCheck.checkLocation(LocationPredicate.Builder.inBiome(biomeLookup.getOrThrow(Biomes.MUSHROOM_FIELDS))))));
				table.editPool(item -> true, LootEntry.insert(LootItem.lootTableItem(Items.BROWN_MUSHROOM).setWeight(10).when(LocationCheck.checkLocation(LocationPredicate.Builder.inBiome(biomeLookup.getOrThrow(Biomes.MUSHROOM_FIELDS))))));
				table.editPool(item -> true, LootEntry.insert(LootItem.lootTableItem(Items.IRON_NUGGET).setWeight(10).when(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiomes(biomeLookup.getOrThrow(BiomeTags.IS_MOUNTAIN))))));
				table.editPool(item -> true, LootEntry.insert(LootItem.lootTableItem(Items.PINK_PETALS).setWeight(10).when(LocationCheck.checkLocation(LocationPredicate.Builder.inBiome(biomeLookup.getOrThrow(Biomes.CHERRY_GROVE))))));
				table.editPool(item -> true, LootEntry.insert(LootItem.lootTableItem(Items.SWEET_BERRIES).setWeight(10).when(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiomes(biomeLookup.getOrThrow(BiomeTags.IS_TAIGA))))));
				table.editPool(item -> true, LootEntry.insert(LootItem.lootTableItem(Items.DEAD_BUSH).setWeight(10).when(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiomes(biomeLookup.getOrThrow(TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", "is_desert"))))))));
				table.editPool(item -> true, LootEntry.insert(LootItem.lootTableItem(Items.DEAD_BUSH).setWeight(10).when(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiomes(biomeLookup.getOrThrow(BiomeTags.IS_BADLANDS))))));
				table.editPool(item -> true, LootEntry.insert(LootItem.lootTableItem(Items.SCULK_VEIN).setWeight(10).when(LocationCheck.checkLocation(LocationPredicate.Builder.inBiome(biomeLookup.getOrThrow(Biomes.DEEP_DARK))))));
				table.editPool(item -> true, LootEntry.insert(LootItem.lootTableItem(Items.POINTED_DRIPSTONE).setWeight(10).when(LocationCheck.checkLocation(LocationPredicate.Builder.inBiome(biomeLookup.getOrThrow(Biomes.DRIPSTONE_CAVES))))));
				table.editPool(item -> true, LootEntry.insert(LootItem.lootTableItem(Items.GLOW_BERRIES).setWeight(10).when(LocationCheck.checkLocation(LocationPredicate.Builder.inBiome(biomeLookup.getOrThrow(Biomes.LUSH_CAVES))))));
				table.editPool(item -> true, LootEntry.insert(LootItem.lootTableItem(Items.PUMPKIN_SEEDS).setWeight(10).when(LocationCheck.checkLocation(LocationPredicate.Builder.inBiome(biomeLookup.getOrThrow(Biomes.DARK_FOREST))))));
				table.editPool(item -> true, LootEntry.insert(LootItem.lootTableItem(Items.PALE_HANGING_MOSS).setWeight(10).when(LocationCheck.checkLocation(LocationPredicate.Builder.inBiome(biomeLookup.getOrThrow(Biomes.PALE_GARDEN))))));
			}

			// GEMS
			if (LaLConfig.get().magic.jeweling) {
				if (EntityType.SLIME.getDefaultLootTable().get().equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(LootItem.lootTableItem(LaLItems.SLIME_GEM).setWeight(1).when(LootItemRandomChanceWithEnchantedBonusCondition.randomChanceAndLootingBoost(registries, 0.0035F, 0.0005F)));
					table.addPool(pool);
				}
				if (EntityType.BREEZE.getDefaultLootTable().get().equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(LootItem.lootTableItem(LaLItems.BREEZE_GEM).setWeight(1).when(LootItemRandomChanceWithEnchantedBonusCondition.randomChanceAndLootingBoost(registries, 0.0125F, 0.0025F)));
					table.addPool(pool);
				}
				if (EntityType.GUARDIAN.getDefaultLootTable().get().equals(id) || EntityType.ELDER_GUARDIAN.getDefaultLootTable().get().equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(LootItem.lootTableItem(LaLItems.PRISMARINE_GEM).setWeight(1).when(LootItemRandomChanceWithEnchantedBonusCondition.randomChanceAndLootingBoost(registries, 0.0125F, 0.0025F)));
					table.addPool(pool);
				}
				if (BuiltInLootTables.FISHING_TREASURE.equals(id)) {
					table.editPool(item -> true, LootEntry.replace(LootItem.lootTableItem(LaLItems.PRISMARINE_GEM).setWeight(1)));
				}
				if (UNDERGROUND_CABIN.equals(id) || DEEP_CABIN.equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(2))
							.add(LootItem.lootTableItem(LaLItems.RUBY_GEM).setWeight(1));
					table.addPool(pool);
				}
				if (DUNGEON_CHEST_FROZEN.equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(14))
							.add(LootItem.lootTableItem(LaLItems.ICE_GEM).setWeight(1));
					table.addPool(pool);
				}
				if (BuiltInLootTables.IGLOO_CHEST.equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(5))
							.add(LootItem.lootTableItem(LaLItems.ICE_GEM).setWeight(1));
					table.addPool(pool);
				}
				if (SPIRE.equals(id) || BuiltInLootTables.NETHER_BRIDGE.equals(id)) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(14))
							.add(LootItem.lootTableItem(LaLItems.OBSIDIAN_GEM).setWeight(1));
					table.addPool(pool);
				}
				if (BuiltInLootTables.END_CITY_TREASURE.equals(id) && !isEnderscapeLoaded) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(17))
							.add(LootItem.lootTableItem(LaLItems.TIMELOST_GEM).setWeight(1));
					table.addPool(pool);
				}
				if ((END_CITY_VAULT.equals(id) || END_CITY_ELYTRA_VAULT.equals(id)) && isEnderscapeLoaded) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(8))
							.add(LootItem.lootTableItem(LaLItems.TIMELOST_GEM).setWeight(1));
					table.addPool(pool);
				}
				if (END_RUINS.equals(id) && !isEnderscapeLoaded) {
					pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
							.add(EmptyLootItem.emptyItem().setWeight(5))
							.add(LootItem.lootTableItem(LaLItems.NEBULITE_GEM).setWeight(1));
					table.addPool(pool);
				}
			}
		});
	}

	public static LootItemCondition.Builder randomChanceAndFortuneBoost(HolderLookup.Provider registries, float chance, float perEnchantmentLevel) {
		HolderLookup.RegistryLookup<Enchantment> enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);
		return () -> new LootItemRandomChanceWithEnchantedBonusCondition(chance, new LevelBasedValue.Linear(chance + perEnchantmentLevel, perEnchantmentLevel), enchantments.getOrThrow(Enchantments.FORTUNE));
	}

	private static @NotNull ResourceKey<LootTable> register(String path) {
		return register(LaLConstants.MOD_ID, path);
	}
	private static @NotNull ResourceKey<LootTable> register(String namespace, String path) {
		return ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(namespace, path));
	}

	private static @NotNull ResourceKey<LootTable> registerEnderscape(String path) {
		return register("enderscape", path);
	}
}
