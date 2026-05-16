package net.rebel459.legacies_and_legends.registry;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ClearAllStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.TeleportRandomlyConsumeEffect;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;
import net.rebel459.legacies_and_legends.LaLConstants;
import net.rebel459.legacies_and_legends.item.*;
import net.rebel459.legacies_and_legends.sound.LaLJukeboxSongs;
import net.rebel459.legacies_and_legends.tag.LaLBlockTags;
import net.rebel459.legacies_and_legends.tag.LaLItemTags;
import net.rebel459.legacies_and_legends.util.Gem;
import net.rebel459.unified.platform.UnifiedRegistries;
import net.rebel459.unified.registry.UnifiedDataComponents;
import net.rebel459.unified.util.registry.SuppliedItem;

import java.util.List;

public final class LaLItems {

    public static int AMULET_DURABILITY = 100;

    public static final Identifier ARMOR_CHESTPLATE_ID = LaLConstants.id("armor_chestplate");
    public static final Identifier ARMOR_LEGGINGS_ID = LaLConstants.id("armor_leggings");
    public static final Identifier ARMOR_BOOTS_ID = LaLConstants.id("armor_boots");

    public static final Identifier KNOCKBACK_RESISTANCE_CHESTPLATE_ID = LaLConstants.id("knockback_resistance_chestplate");
    public static final Identifier MOVEMENT_SPEED_LEGGINGS_ID = LaLConstants.id("movement_speed_leggings");
    public static final Identifier STEP_HEIGHT_BOOTS_ID = LaLConstants.id("step_height_boots");

    private static final ItemAttributeModifiers createReinforcedChestplateAttributes = ItemAttributeModifiers.builder()
            .add(Attributes.ARMOR, new AttributeModifier(ARMOR_CHESTPLATE_ID, 7, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.CHEST)
            .add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(KNOCKBACK_RESISTANCE_CHESTPLATE_ID, 0.5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.CHEST)
            .build();

    private static final ItemAttributeModifiers createTravellingStridesAttributes = ItemAttributeModifiers.builder()
            .add(Attributes.ARMOR, new AttributeModifier(ARMOR_LEGGINGS_ID, 3, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.LEGS)
            .add(Attributes.MOVEMENT_SPEED, new AttributeModifier(MOVEMENT_SPEED_LEGGINGS_ID, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.LEGS)
            .build();

    private static final ItemAttributeModifiers createWandererBootsAttributes = ItemAttributeModifiers.builder()
            .add(Attributes.ARMOR, new AttributeModifier(ARMOR_BOOTS_ID, 2, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.FEET)
            .add(Attributes.STEP_HEIGHT, new AttributeModifier(STEP_HEIGHT_BOOTS_ID, 1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.FEET)
            .build();

    public static UnifiedRegistries.Items ITEMS = UnifiedRegistries.Items.create(LaLConstants.MOD_ID);

    // Boomerang
    public static final SuppliedItem BOOMERANG = ITEMS.register("boomerang",
            BoomerangItem::new,
            () -> new Properties()
                    .component(DataComponents.TOOL, BoomerangItem.createToolProperties())
                    .component(DataComponents.ATTRIBUTE_MODIFIERS, BoomerangItem.createAttributes())
                    .repairable(LaLItemTags.BOOMERANG_REPAIR_MATERIALS)
                    .durability(386)
                    .enchantable(15)
                    .rarity(Rarity.RARE)
                    .component(DataComponents.WEAPON, new Weapon(1))
    );

    // Wand
    public static final SuppliedItem WAND = ITEMS.register("wand",
            WandItem::new,
            () -> new Properties()
                    .repairable(LaLItemTags.WAND_REPAIR_MATERIALS)
                    .durability(512)
                    .enchantable(20)
                    .rarity(Rarity.RARE)
                    .component(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(Gem.Slots.DEFAULT.primary().getSerializedName(), "charged"), List.of()))
                    .component(LaLDataComponents.WAND_SLOTS.get(), Gem.Slots.DEFAULT)
                    .useCooldown(1)
    );

    // Misc Items
    public static final SuppliedItem DISC_FRAGMENT_FAR_LANDS = ITEMS.register("disc_fragment_far_lands",
            DiscFragmentItem::new,
            () -> new Properties()
                    .stacksTo(64)
                    .rarity(Rarity.UNCOMMON)
    );
    public static final SuppliedItem METAL_CHUNK = ITEMS.register("metal_chunk",
            Item::new,
            () -> new Properties()
                    .stacksTo(64)
    );
    public static final SuppliedItem WOODEN_BUCKET = ITEMS.register("wooden_bucket",
            Item::new,
            () -> new Properties()
                    .stacksTo(16)
                    .component(UnifiedDataComponents.FURNACE_FUEL.get(), 300)
    );
    public static final SuppliedItem COAL_BUCKET = ITEMS.register("coal_bucket",
            Item::new,
            () -> new Properties()
                    .stacksTo(16)
                    .component(UnifiedDataComponents.FURNACE_FUEL.get(), 20000)
    );
    public static final SuppliedItem CHARCOAL_BUCKET = ITEMS.register("charcoal_bucket",
            Item::new,
            () -> new Properties()
                    .stacksTo(16)
                    .component(UnifiedDataComponents.FURNACE_FUEL.get(), 20000)
    );
    public static final SuppliedItem TRIDENT_SHARD = ITEMS.register("trident_shard",
            Item::new,
            () -> new Properties()
                    .stacksTo(64)
    );
    public static final SuppliedItem SAPPHIRE = ITEMS.register("sapphire",
            Item::new,
            () -> new Properties()
                    .stacksTo(64)
                    .trimMaterial(LaLTrimMaterials.SAPPHIRE)
    );
    public static final SuppliedItem GLOW_STICK = ITEMS.register("glow_stick",
            properties -> new GlowStickItem(LaLBlocks.GLOW_STICK.get(), properties),
            Properties::new
    );
    public static final SuppliedItem METEORITE_BRICK = ITEMS.register("meteorite_brick",
            Item::new,
            () -> new Properties()
                    .fireResistant()
                    .rarity(Rarity.UNCOMMON)
    );

    // Gems
    public static final SuppliedItem SAPPHIRE_GEM = ITEMS.register("sapphire_gem",
            Item::new,
            () -> new Properties()
                    .rarity(Rarity.UNCOMMON)
                    .component(LaLDataComponents.GEM.get(), Gem.SAPPHIRE)
    );
    public static final SuppliedItem SLIME_GEM = ITEMS.register("slime_gem",
            Item::new,
            () -> new Properties()
                    .rarity(Rarity.UNCOMMON)
                    .component(LaLDataComponents.GEM.get(), Gem.SLIME)
    );
    public static final SuppliedItem METEORITE_GEM = ITEMS.register("meteorite_gem",
            Item::new,
            () -> new Properties()
                    .rarity(Rarity.RARE)
                    .fireResistant()
                    .delayedComponent(DataComponents.DAMAGE_RESISTANT, provider -> new DamageResistant(provider.getOrThrow(DamageTypeTags.IS_EXPLOSION)))
                    .component(LaLDataComponents.GEM.get(), Gem.METEORITE)
    );
    public static final SuppliedItem ICE_GEM = ITEMS.register("ice_gem",
            Item::new,
            () -> new Properties()
                    .rarity(Rarity.UNCOMMON)
                    .component(LaLDataComponents.GEM.get(), Gem.ICE)
    );
    public static final SuppliedItem BREEZE_GEM = ITEMS.register("breeze_gem",
            Item::new,
            () -> new Properties()
                    .rarity(Rarity.RARE)
                    .component(LaLDataComponents.GEM.get(), Gem.BREEZE)
    );
    public static final SuppliedItem OBSIDIAN_GEM = ITEMS.register("obsidian_gem",
            Item::new,
            () -> new Properties()
                    .rarity(Rarity.RARE)
                    .fireResistant()
                    .component(LaLDataComponents.GEM.get(), Gem.OBSIDIAN)
    );
    public static final SuppliedItem PRISMARINE_GEM = ITEMS.register("prismarine_gem",
            Item::new,
            () -> new Properties()
                    .rarity(Rarity.UNCOMMON)
                    .component(LaLDataComponents.GEM.get(), Gem.PRISMARINE)
    );
    public static final SuppliedItem TIMELOST_GEM = ITEMS.register("timelost_gem",
            Item::new,
            () -> new Properties()
                    .rarity(Rarity.EPIC)
                    .component(LaLDataComponents.GEM.get(), Gem.TIMELOST)
    );
    public static final SuppliedItem NEBULITE_GEM = ITEMS.register("nebulite_gem",
            Item::new,
            () -> new Properties()
                    .rarity(Rarity.RARE)
                    .component(LaLDataComponents.GEM.get(), Gem.NEBULITE)
    );
    public static final SuppliedItem RUBY_GEM = ITEMS.register("ruby_gem",
            Item::new,
            () -> new Properties()
                    .rarity(Rarity.RARE)
                    .component(LaLDataComponents.GEM.get(), Gem.RUBY)
    );

    public static final List<SuppliedItem> GEMS = List.of(SAPPHIRE_GEM, SLIME_GEM, METEORITE_GEM, ICE_GEM, BREEZE_GEM, OBSIDIAN_GEM, PRISMARINE_GEM, TIMELOST_GEM, NEBULITE_GEM, RUBY_GEM);

    // Discs
    public static final SuppliedItem MUSIC_DISC_SVALL = ITEMS.register("music_disc_svall",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)
                    .jukeboxPlayable(LaLJukeboxSongs.SVALL)
    );
    public static final SuppliedItem MUSIC_DISC_CASTLES = ITEMS.register("music_disc_castles",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)
                    .jukeboxPlayable(LaLJukeboxSongs.CASTLES)
    );
    public static final SuppliedItem MUSIC_DISC_TASWELL = ITEMS.register("music_disc_taswell",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)
                    .jukeboxPlayable(LaLJukeboxSongs.TASWELL)
    );
    public static final SuppliedItem MUSIC_DISC_SHULKER = ITEMS.register("music_disc_shulker",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)
                    .jukeboxPlayable(LaLJukeboxSongs.SHULKER)
    );
    public static final SuppliedItem MUSIC_DISC_TUNDRA = ITEMS.register("music_disc_tundra",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)
                    .jukeboxPlayable(LaLJukeboxSongs.TUNDRA)
    );
    public static final SuppliedItem MUSIC_DISC_FAR_LANDS = ITEMS.register("music_disc_far_lands",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .rarity(Rarity.RARE)
                    .jukeboxPlayable(LaLJukeboxSongs.FAR_LANDS)
    );
    public static final SuppliedItem MUSIC_DISC_INFINITE_SPOOKY_AMETHYST = ITEMS.register("music_disc_infinite_spooky_amethyst",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .rarity(Rarity.RARE)
                    .jukeboxPlayable(LaLJukeboxSongs.INFINITE_SPOOKY_AMETHYST)
    );
    public static final SuppliedItem MUSIC_DISC_113 = ITEMS.register("music_disc_113",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)
                    .jukeboxPlayable(LaLJukeboxSongs.MUSIC_DISC_113)
    );
    public static final SuppliedItem MUSIC_DISC_GRAVEL = ITEMS.register("music_disc_gravel",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)
                    .jukeboxPlayable(LaLJukeboxSongs.GRAVEL)
    );

    // Sherds
    public static final SuppliedItem DUSK_POTTERY_SHERD = ITEMS.register("dusk_pottery_sherd",
            Item::new,
            () -> new Properties()
                    .rarity(Rarity.UNCOMMON)
    );
    public static final SuppliedItem HARVEST_POTTERY_SHERD = ITEMS.register("harvest_pottery_sherd",
            Item::new,
            () -> new Properties()
                    .rarity(Rarity.UNCOMMON)
    );
    public static final SuppliedItem VERDANT_POTTERY_SHERD = ITEMS.register("verdant_pottery_sherd",
            Item::new,
            () -> new Properties()
                    .rarity(Rarity.UNCOMMON)
    );
    public static final SuppliedItem FORAGER_POTTERY_SHERD = ITEMS.register("forager_pottery_sherd",
            Item::new,
            () -> new Properties()
                    .rarity(Rarity.UNCOMMON)
    );

    // Food
    public static final SuppliedItem ENCHANTED_BEETROOT = ITEMS.register("enchanted_beetroot",
            Item::new,
            () -> new Properties()
                    .stacksTo(64)
                    .rarity(Rarity.UNCOMMON)
                    .food(LaLFoods.ENCHANTED_BEETROOT, LaLConsumables.ENCHANTED_BEETROOT)
                    .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
    );
    public static final SuppliedItem ENCHANTED_BEETROOT_SOUP = ITEMS.register("enchanted_beetroot_soup",
            Item::new,
            () -> new Properties()
                    .stacksTo(16)
                    .rarity(Rarity.RARE)
                    .food(LaLFoods.ENCHANTED_BEETROOT_SOUP, LaLConsumables.ENCHANTED_BEETROOT_SOUP)
                    .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                    .usingConvertsTo(Items.BOWL)
    );

    // Equipment
    public static final SuppliedItem HOOK = ITEMS.register("hook",
            (properties) -> new HookItem(LaLToolMaterial.HOOK, 3F, -3.2F, properties), (
                    () -> new Properties()
                            .durability(750)
                            .enchantable(15)
                            .rarity(Rarity.UNCOMMON)
            ));
    public static final SuppliedItem KNIFE = ITEMS.register("knife",
            KnifeItem::new,
            () -> new Properties()
                    .durability(3048)
                    .attributes(KnifeItem.createAttributes())
                    .component(
                            DataComponents.TOOL,
                            new Tool(
                                    List.of(
                                            Tool.Rule.deniesDrops(BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK).getOrThrow(LaLToolMaterial.KNIFE.incorrectBlocksForDrops())),
                                            Tool.Rule.minesAndDrops(BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK).getOrThrow(LaLBlockTags.MINEABLE_WITH_KNIFE), LaLToolMaterial.KNIFE.speed())
                                    ),
                                    1.0F,
                                    2,
                                    false
                            )
                    )
                    .enchantable(15)
                    .rarity(Rarity.RARE)
    );

    // Artifacts
    public static final SuppliedItem TOTEM_OF_RESURRECTION = ITEMS.register("totem_of_resurrection",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)
    );
    public static final SuppliedItem TOTEM_OF_TELEPORTATION = ITEMS.register("totem_of_teleportation",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .rarity(Rarity.RARE)
                    .component(DataComponents.DEATH_PROTECTION, new DeathProtection(
                            List.of(
                                    new TeleportRandomlyConsumeEffect(),
                                    new ClearAllStatusEffectsConsumeEffect(),
                                    new ApplyStatusEffectsConsumeEffect(
                                            List.of(
                                                    new MobEffectInstance(MobEffects.REGENERATION, 300, 1),
                                                    new MobEffectInstance(MobEffects.SPEED, 300, 0),
                                                    new MobEffectInstance(MobEffects.INVISIBILITY, 600, 0)
                                            )
                                    )
                            )
                    ))
    );

    public static final SuppliedItem TABLET = ITEMS.register("tablet",
            Item::new,
            () -> new Properties()
                    .stacksTo(64)
    );
    public static final SuppliedItem TABLET_OF_RECALL = ITEMS.register("tablet_of_recall",
            RecallTabletItem::new,
            () -> new Properties()
                    .stacksTo(16)
                    .component(DataComponents.CONSUMABLE, LaLConsumables.TABLET_OF_RECALL)
                    .useCooldown(300F)
                    .rarity(Rarity.RARE)
    );
    public static final SuppliedItem TABLET_OF_HASTE = ITEMS.register("tablet_of_haste",
            Item::new,
            () -> new Properties()
                    .stacksTo(16)
                    .component(DataComponents.CONSUMABLE, LaLConsumables.TABLET_OF_HASTE)
                    .useCooldown(180F)
                    .rarity(Rarity.UNCOMMON)
    );
    public static final SuppliedItem TABLET_OF_INSTABILITY = ITEMS.register("tablet_of_instability",
            Item::new,
            () -> new Properties()
                    .stacksTo(16)
                    .component(DataComponents.CONSUMABLE, LaLConsumables.TABLET_OF_INSTABILITY)
                    .useCooldown(180F)
                    .rarity(Rarity.UNCOMMON)
    );
    public static final SuppliedItem TABLET_OF_WARPING = ITEMS.register("tablet_of_warping",
            Item::new,
            () -> new Properties()
                    .stacksTo(16)
                    .component(DataComponents.CONSUMABLE, LaLConsumables.TABLET_OF_WARPING)
                    .useCooldown(180F)
                    .rarity(Rarity.RARE)
    );
    public static final SuppliedItem TABLET_OF_CHANNELING = ITEMS.register("tablet_of_channeling",
            Item::new,
            () -> new Properties()
                    .stacksTo(16)
                    .component(DataComponents.CONSUMABLE, LaLConsumables.TABLET_OF_CHANNELING)
                    .useCooldown(300F)
                    .rarity(Rarity.RARE)
    );
    public static final SuppliedItem TABLET_OF_DEAFENING = ITEMS.register("tablet_of_deafening",
            Item::new,
            () -> new Properties()
                    .stacksTo(16)
                    .component(DataComponents.CONSUMABLE, LaLConsumables.TABLET_OF_DEAFENING)
                    .useCooldown(60F)
                    .rarity(Rarity.RARE)
    );
    public static final SuppliedItem TABLET_OF_REVEALING = ITEMS.register("tablet_of_revealing",
            Item::new,
            () -> new Properties()
                    .stacksTo(16)
                    .component(DataComponents.CONSUMABLE, LaLConsumables.TABLET_OF_REVEALING)
                    .useCooldown(60F)
                    .rarity(Rarity.UNCOMMON)
    );

    public static final SuppliedItem REINFORCED_CHESTPLATE = ITEMS.register("reinforced_chestplate",
            Item::new,
            () -> new Properties()
                    .durability(731)
                    .repairable(LaLItemTags.REPAIRS_REINFORCED_ARMOR)
                    .component(DataComponents.EQUIPPABLE, Equippable.builder(ArmorType.CHESTPLATE.getSlot()).setEquipSound(SoundEvents.ARMOR_EQUIP_DIAMOND).setAsset(LaLEquipmentAssets.REINFORCED).build())
                    .enchantable(9)
                    .attributes(createReinforcedChestplateAttributes)
                    .rarity(Rarity.RARE)
    );
    public static final SuppliedItem TRAVELLING_STRIDES = ITEMS.register("travelling_strides",
            Item::new,
            () -> new Properties()
                    .durability(165)
                    .repairable(LaLItemTags.REPAIRS_TRAVELLING_ARMOR)
                    .component(DataComponents.EQUIPPABLE, Equippable.builder(ArmorType.LEGGINGS.getSlot()).setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER).setAsset(LaLEquipmentAssets.TRAVELLING).build())
                    .enchantable(15)
                    .attributes(createTravellingStridesAttributes)
    );
    public static final SuppliedItem WANDERER_BOOTS = ITEMS.register("wanderer_boots",
            Item::new,
            () -> new Properties()
                    .durability(386)
                    .repairable(LaLItemTags.REPAIRS_WANDERER_ARMOR)
                    .component(DataComponents.EQUIPPABLE, Equippable.builder(ArmorType.BOOTS.getSlot()).setEquipSound(SoundEvents.ARMOR_EQUIP_IRON).setAsset(LaLEquipmentAssets.WANDERER).build())
                    .enchantable(12)
                    .attributes(createWandererBootsAttributes)
                    .rarity(Rarity.UNCOMMON)
                    .useItemDescriptionPrefix()
    );
    public static final SuppliedItem VERDANT_SWORD = ITEMS.register("verdant_sword",
            VerdantSwordItem::new,
            () -> new Properties()
                    .rarity(Rarity.UNCOMMON)
                    .sword(LaLToolMaterial.VERDANT, 3F, -2.4F)
    );
    public static final SuppliedItem MOLTEN_PICKAXE = ITEMS.register("molten_pickaxe",
            Item::new,
            () -> new Properties()
                    .rarity(Rarity.RARE)
                    .pickaxe(LaLToolMaterial.MOLTEN, 1F, -2.8F)
    );
    public static final SuppliedItem CLEAVING_BATTLEAXE = ITEMS.register("cleaving_battleaxe",
            CleavingBattleAxeItem::new,
            () -> new Properties()
                    .rarity(Rarity.UNCOMMON)
    );
    public static final SuppliedItem PROSPECTOR_SHOVEL = ITEMS.register("prospector_shovel",
            (properties) -> new ShovelItem(LaLToolMaterial.PROSPECTOR, 1.5F, -3F, properties), (
                    () -> new Properties()
                            .rarity(Rarity.UNCOMMON)
            )
    );
    public static final SuppliedItem WITHERED_HOE = ITEMS.register("withered_hoe",
            (properties) -> new WitheredHoeItem(LaLToolMaterial.WITHERED, -2F, -1F, properties), (
                    () -> new Properties()
                            .rarity(Rarity.UNCOMMON)
            )
    );
    public static final SuppliedItem FROSTED_SPEAR = ITEMS.register("frosted_spear",
            Item::new,
            () -> new Properties()
                    .rarity(Rarity.RARE)
                    .spear(LaLToolMaterial.FROSTED, 0.75F, 0.82F, 0.7F, 4.5F, 10.0F, 9.0F, 5.1F, 13.75F, 4.6F)
    );

    // Accessories
    public static final SuppliedItem RING_OF_HUNTING = ITEMS.register("ring_of_hunting",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .component(LaLDataComponents.VARIABLE_DURABILITY.get(), 195)
                    .repairable(LaLItemTags.HUNTING_RING_MATERIALS)
                    .rarity(Rarity.RARE)
    );
    public static final SuppliedItem RING_OF_EVASION = ITEMS.register("ring_of_evasion",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .component(LaLDataComponents.VARIABLE_DURABILITY.get(), 426)
                    .repairable(LaLItemTags.EVASION_RING_MATERIALS)
                    .rarity(Rarity.RARE)
    );
    public static final SuppliedItem RING_OF_CONSTRUCTION = ITEMS.register("ring_of_construction",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .component(LaLDataComponents.VARIABLE_DURABILITY.get(), 1024)
                    .repairable(LaLItemTags.CONSTRUCTION_RING_MATERIALS)
                    .rarity(Rarity.UNCOMMON)
    );
    public static final SuppliedItem RING_OF_STRIKING = ITEMS.register("ring_of_striking",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .component(LaLDataComponents.VARIABLE_DURABILITY.get(), 352)
                    .repairable(LaLItemTags.STRIKING_RING_MATERIALS)
                    .rarity(Rarity.UNCOMMON)
    );
    public static final SuppliedItem RING_OF_ARCHERY = ITEMS.register("ring_of_archery",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .component(LaLDataComponents.VARIABLE_DURABILITY.get(), 449)
                    .repairable(LaLItemTags.ARCHERY_RING_MATERIALS)
                    .rarity(Rarity.UNCOMMON)
    );
    public static final SuppliedItem RING_OF_EXCAVATION = ITEMS.register("ring_of_excavation",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .component(LaLDataComponents.VARIABLE_DURABILITY.get(), 768)
                    .repairable(LaLItemTags.EXCAVATION_RING_MATERIALS)
                    .rarity(Rarity.RARE)
    );
    public static final SuppliedItem RING_OF_RESTORATION = ITEMS.register("ring_of_restoration",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .component(LaLDataComponents.VARIABLE_DURABILITY.get(), 330)
                    .repairable(LaLItemTags.RESTORATION_RING_MATERIALS)
                    .rarity(Rarity.EPIC)
    );

    public static final SuppliedItem NECKLACE_OF_ISOLATION = ITEMS.register("necklace_of_isolation",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .component(LaLDataComponents.VARIABLE_DURABILITY.get(), 408)
                    .repairable(LaLItemTags.ISOLATION_NECKLACE_MATERIALS)
                    .rarity(Rarity.RARE)
    );
    public static final SuppliedItem NECKLACE_OF_PURITY = ITEMS.register("necklace_of_purity",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .component(LaLDataComponents.VARIABLE_DURABILITY.get(), 173)
                    .repairable(LaLItemTags.PURITY_NECKLACE_MATERIALS)
                    .rarity(Rarity.RARE)
    );
    public static final SuppliedItem NECKLACE_OF_LEAPING = ITEMS.register("necklace_of_leaping",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .component(LaLDataComponents.VARIABLE_DURABILITY.get(), 212)
                    .repairable(LaLItemTags.LEAPING_NECKLACE_MATERIALS)
                    .rarity(Rarity.UNCOMMON)
    );
    public static final SuppliedItem NECKLACE_OF_PROTECTION = ITEMS.register("necklace_of_protection",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .component(LaLDataComponents.VARIABLE_DURABILITY.get(), 237)
                    .repairable(LaLItemTags.PROTECTION_NECKLACE_MATERIALS)
                    .rarity(Rarity.UNCOMMON)
    );
    public static final SuppliedItem NECKLACE_OF_RESILIENCE = ITEMS.register("necklace_of_resilience",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .component(LaLDataComponents.VARIABLE_DURABILITY.get(), 155)
                    .repairable(LaLItemTags.RESILIENCE_NECKLACE_MATERIALS)
                    .rarity(Rarity.EPIC)
    );
    public static final SuppliedItem NECKLACE_OF_REGENERATION = ITEMS.register("necklace_of_regeneration",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .component(LaLDataComponents.VARIABLE_DURABILITY.get(), 284)
                    .repairable(LaLItemTags.REGENERATION_NECKLACE_MATERIALS)
                    .rarity(Rarity.RARE)
    );
    public static final SuppliedItem NECKLACE_OF_BARTERING = ITEMS.register("necklace_of_bartering",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .component(LaLDataComponents.VARIABLE_DURABILITY.get(), 351)
                    .repairable(LaLItemTags.BARTERING_NECKLACE_MATERIALS)
                    .rarity(Rarity.UNCOMMON)
    );

    public static final SuppliedItem AMULET_OF_OBSIDIAN = ITEMS.register("amulet_of_obsidian",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .durability(AMULET_DURABILITY)
                    .rarity(Rarity.EPIC)
    );
    public static final SuppliedItem AMULET_OF_ABSORPTION = ITEMS.register("amulet_of_absorption",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .durability(AMULET_DURABILITY)
                    .rarity(Rarity.RARE)
    );
    public static final SuppliedItem AMULET_OF_DEFLECTION = ITEMS.register("amulet_of_deflection",
            Item::new,
            () -> new Properties()
                    .stacksTo(1)
                    .durability(AMULET_DURABILITY)
                    .rarity(Rarity.RARE)
    );

    public static void init() {}
}
