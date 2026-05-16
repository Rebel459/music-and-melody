package net.rebel459.legacies_and_legends.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.rebel459.legacies_and_legends.registry.LaLMenus;

public final class LaLMenuScreens {

    private LaLMenuScreens() {}

    public static void init() {
        MenuScreens.register(LaLMenus.JEWLING.get(), JewelingScreen::new);
    }
}
