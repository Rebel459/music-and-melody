package net.rebel459.music_and_melody;

import net.minecraft.resources.Identifier;
import net.rebel459.music_and_melody.network.ServerPresenceHandler;
import net.rebel459.music_and_melody.network.StructureMusicHandler;
import net.rebel459.music_and_melody.sound.MaMSounds;

public class MusicAndMelody {

	public static void initRegistries() {
        MaMSounds.init();
        ServerPresenceHandler.init();
        StructureMusicHandler.init();
	}

    public static void init() {}

    public static final String MOD_ID = "music_and_melody";
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
