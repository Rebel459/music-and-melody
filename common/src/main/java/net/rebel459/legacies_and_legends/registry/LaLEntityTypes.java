package net.rebel459.legacies_and_legends.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.rebel459.legacies_and_legends.LaLConstants;
import net.rebel459.legacies_and_legends.entity.BoomerangProjectile;
import net.rebel459.legacies_and_legends.entity.GlowStickProjectile;
import net.rebel459.unified.platform.UnifiedRegistries;
import net.rebel459.unified.util.registry.Supplied;
import org.jetbrains.annotations.NotNull;

public final class LaLEntityTypes {

    public static UnifiedRegistries.EntityTypes ENTITIES = UnifiedRegistries.EntityTypes.create(LaLConstants.MOD_ID);

    public static final @NotNull Supplied<EntityType<BoomerangProjectile>> BOOMERANG = ENTITIES.register(
            "boomerang",
            EntityType.Builder.<BoomerangProjectile>of(BoomerangProjectile::new, MobCategory.MISC)
                    .sized(0.5F, 0.25F)
                    .clientTrackingRange(64)
                    .updateInterval(10)
    );

    public static final @NotNull Supplied<EntityType<GlowStickProjectile>> GLOW_STICK = ENTITIES.register(
            "glow_stick",
            EntityType.Builder.<GlowStickProjectile>of(GlowStickProjectile::new, MobCategory.MISC)
                    .noLootTable().sized(0.35F, 0.35F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
    );

    public static void init() {}
}