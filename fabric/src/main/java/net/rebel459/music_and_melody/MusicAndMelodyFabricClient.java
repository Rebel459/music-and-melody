package net.rebel459.music_and_melody;

import net.fabricmc.api.ClientModInitializer;

public class MusicAndMelodyFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MusicAndMelodyClient.initRegistries();
        MusicAndMelodyClient.init();
    }
}
