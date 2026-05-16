package net.rebel459.legacies_and_legends.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.rebel459.legacies_and_legends.LaLConstants;
import net.rebel459.legacies_and_legends.menu.JewelingMenu;
import net.rebel459.unified.platform.UnifiedRegistries;
import net.rebel459.unified.util.registry.Supplied;

import java.util.function.Supplier;

public final class LaLMenus {

    public static UnifiedRegistries.DeferredRegistry<MenuType<?>> MENUS = UnifiedRegistries.DeferredRegistry.create(LaLConstants.MOD_ID, BuiltInRegistries.MENU);

    public static final Supplied<MenuType<JewelingMenu>> JEWLING = MENUS.register(
            "jeweling",
            () -> new MenuType<>(JewelingMenu::new, FeatureFlags.VANILLA_SET)
    );

    public static void init() {}
}
