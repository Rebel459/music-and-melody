package net.rebel459.music_and_melody;

import net.minecraft.resources.Identifier;
import net.rebel459.music_and_melody.network.MaMNetwork;
import net.rebel459.music_and_melody.sound.MaMSounds;
import net.rebel459.unified.util.helper.impl.StructureMusicImpl;

public class MusicAndMelody {

	public static void initRegistries() {
        MaMSounds.init();
        MaMNetwork.init();
        StructureMusicImpl.enableAutoSync();
	}

    public static void init() {}

    public static final String MOD_ID = "music_and_melody";
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
