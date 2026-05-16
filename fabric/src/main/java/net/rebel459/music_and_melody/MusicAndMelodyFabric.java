package net.rebel459.music_and_melody;

import net.fabricmc.api.ModInitializer;

public class MusicAndMelodyFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        MusicAndMelody.initRegistries();
        MusicAndMelody.init();
    }
}
