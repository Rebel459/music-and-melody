package net.rebel459.music_and_melody.platform.registry;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

public interface SoundEventRegistry {
        Supplier<SoundEvent> register(String path);
        Supplier<SoundEvent> register(String path, float fixedRange);

        Holder<SoundEvent> registerForHolder(String path);
        Holder<SoundEvent> registerForHolder(String path, float fixedRange);

        Holder<SoundEvent> registerVanilla(String path);
    }