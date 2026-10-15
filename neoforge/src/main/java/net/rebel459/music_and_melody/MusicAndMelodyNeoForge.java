package net.rebel459.music_and_melody;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.rebel459.unified.neoforge.NeoForgeUnifiedBus;

@Mod(MusicAndMelody.MOD_ID)
public class MusicAndMelodyNeoForge {

    public MusicAndMelodyNeoForge(IEventBus modEventBus) {
        NeoForgeUnifiedBus.register(MusicAndMelody.MOD_ID, modEventBus);
        MusicAndMelody.initRegistries();
        modEventBus.addListener(MusicAndMelodyNeoForge::commonSetup);
    }

    private static void commonSetup(final FMLCommonSetupEvent event) {
        MusicAndMelody.init();
    }
}