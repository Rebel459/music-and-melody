package net.rebel459.music_and_melody;

import net.fabricmc.api.ModInitializer;
import net.rebel459.music_and_melody.platform.MaMFabricPlatform;

public class MusicAndMelodyFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        MaMFabricPlatform.init();
        MusicAndMelody.initRegistries();
        MusicAndMelody.init();
    }
}
