package net.rebel459.legacies_and_legends.sound;

import net.minecraft.world.level.block.SoundType;

public final class LaLBlockSounds {

    public static final SoundType SAPPHIRE_BLOCK = new SoundType(1F, 1F,
            LaLSounds.SAPPHIRE_BLOCK_BREAK.get(),
            LaLSounds.SAPPHIRE_BLOCK_STEP.get(),
            LaLSounds.SAPPHIRE_BLOCK_PLACE.get(),
            LaLSounds.SAPPHIRE_BLOCK_HIT.get(),
            LaLSounds.SAPPHIRE_BLOCK_FALL.get()
    );
    public static final SoundType WAND_PLATFORM = new SoundType(1F, 1F,
            LaLSounds.WAND_PLATFORM_BREAK.get(),
            LaLSounds.WAND_PLATFORM_STEP.get(),
            LaLSounds.WAND_PLATFORM_PLACE.get(),
            LaLSounds.WAND_PLATFORM_HIT.get(),
            LaLSounds.WAND_PLATFORM_FALL.get()
    );

    public static final SoundType METEORITE = new SoundType(1F, 1F,
            LaLSounds.METEORITE_BREAK.get(),
            LaLSounds.METEORITE_STEP.get(),
            LaLSounds.METEORITE_PLACE.get(),
            LaLSounds.METEORITE_HIT.get(),
            LaLSounds.METEORITE_FALL.get()
    );

    public static void init() {
    }
}