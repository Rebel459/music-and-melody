package net.rebel459.music_and_melody.client.util;

import net.minecraft.client.resources.sounds.Sound;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class EventWeightHelper {
    private static final Set<Sound> REFERENCES = Collections.newSetFromMap(new WeakHashMap<>());

    private EventWeightHelper() {}

    public static void add(Sound sound) {
        REFERENCES.add(sound);
    }

    public static boolean contains(Sound sound) {
        return REFERENCES.contains(sound);
    }
}