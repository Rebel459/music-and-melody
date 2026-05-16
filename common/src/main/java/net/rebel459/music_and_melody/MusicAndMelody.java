package net.rebel459.music_and_melody;

import net.minecraft.resources.Identifier;
import net.rebel459.music_and_melody.sound.MaMBiomeMusic;
import net.rebel459.music_and_melody.sound.MaMSounds;
import net.rebel459.music_and_melody.sound.MaMStructureMusic;

public class MusicAndMelody {

	public static void initRegistries() {
        MaMSounds.init();
        MaMBiomeMusic.init();
	}

    public static void init() {
        MaMStructureMusic.init();
    }

    public static final String MOD_ID = "music_and_melody";
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
