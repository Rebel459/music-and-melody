package net.rebel459.legacies_and_legends;

import net.fabricmc.api.ClientModInitializer;

public class LegaciesAndLegendsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        LegaciesAndLegendsClient.initRegistries();
        LegaciesAndLegendsClient.init();
    }
}
