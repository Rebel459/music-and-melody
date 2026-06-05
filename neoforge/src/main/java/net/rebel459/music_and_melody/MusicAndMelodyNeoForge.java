package net.rebel459.music_and_melody;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.rebel459.music_and_melody.platform.MaMNeoForgePlatform;

import java.util.HashSet;
import java.util.Set;

@Mod(MusicAndMelody.MOD_ID)
public class MusicAndMelodyNeoForge {

    private static final Set<String> registeredBuses = new HashSet<>(Set.of(MusicAndMelody.MOD_ID, "minecraft"));

    public MusicAndMelodyNeoForge(IEventBus modEventBus) {
        MaMNeoForgePlatform.init(modEventBus);
        MusicAndMelody.initRegistries();
        modEventBus.addListener(MusicAndMelodyNeoForge::commonSetup);
    }

    private static void commonSetup(final FMLCommonSetupEvent event) {
        MusicAndMelody.init();
    }
}