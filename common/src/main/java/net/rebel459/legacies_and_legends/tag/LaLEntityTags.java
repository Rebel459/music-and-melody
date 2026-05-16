package net.rebel459.legacies_and_legends.tag;

import net.rebel459.legacies_and_legends.LaLConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public final class LaLEntityTags {
	public static final TagKey<EntityType<?>> DAMAGELESS_PROJECTILES = bind("damageless_projectiles");

	@NotNull
	private static TagKey<EntityType<?>> bind(@NotNull String path) {
		return TagKey.create(Registries.ENTITY_TYPE, LaLConstants.id(path));
	}
}