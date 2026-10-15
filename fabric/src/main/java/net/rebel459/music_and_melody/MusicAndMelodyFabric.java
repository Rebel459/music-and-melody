package net.rebel459.music_and_melody;

import net.fabricmc.api.ModInitializer;
import net.rebel459.unified.fabric.FabricUnifiedInitializer;

public class MusicAndMelodyFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        MusicAndMelody.initRegistries();
        FabricUnifiedInitializer.register(MusicAndMelody::init);
    }
}
