package net.rebel459.legacies_and_legends;

import net.fabricmc.api.ModInitializer;

public class LegaciesAndLegendsFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        LegaciesAndLegends.initRegistries();
        LegaciesAndLegends.init();
    }
}
