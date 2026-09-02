package net.rebel459.music_and_melody.client.util;

import net.minecraft.client.resources.sounds.SoundInstance;

public interface SoundEngineStopper {
    boolean stopEverythingExceptPlaylist(SoundInstance preserved);
}
