package net.rebel459.legacies_and_legends.registry;

import net.rebel459.legacies_and_legends.LaLConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.NotNull;

public class LaLEnchantments {
	public static final ResourceKey<Enchantment> DECAY = key("decay");
	public static final ResourceKey<Enchantment> FEATHERWEIGHT = key("featherweight");
	public static final ResourceKey<Enchantment> FREEZE = key("freeze");
	public static final ResourceKey<Enchantment> REBOUND = key("rebound");
	public static final ResourceKey<Enchantment> REJUVENATE = key("rejuvenate");
	public static final ResourceKey<Enchantment> SHADOWSTEP = key("shadowstep");
	public static final ResourceKey<Enchantment> SHATTER = key("shatter");
	public static final ResourceKey<Enchantment> PULL = key("pull");
	public static final ResourceKey<Enchantment> STRIKING = key("striking");
	public static final ResourceKey<Enchantment> TANGLED = key("tangled");

	public static void init() {
	}

	private static @NotNull ResourceKey<Enchantment> key(String path) {
		return ResourceKey.create(Registries.ENCHANTMENT, LaLConstants.id(path));
	}
}
