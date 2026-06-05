package net.rebel459.music_and_melody;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.rebel459.music_and_melody.client.util.SafeIdentifier;
import net.rebel459.music_and_melody.config.MaMServerConfig;
import net.rebel459.unified.platform.NeoForgeUnifiedRegistries;

import java.util.HashSet;
import java.util.Set;

@Mod(MusicAndMelody.MOD_ID)
public class MusicAndMelodyNeoForge {

    private static final Set<String> registeredBuses = new HashSet<>(Set.of(MusicAndMelody.MOD_ID, "minecraft"));

    public MusicAndMelodyNeoForge(IEventBus modEventBus) {
        NeoForgeUnifiedRegistries.registerBus(MusicAndMelody.MOD_ID, modEventBus);
        NeoForgeUnifiedRegistries.registerBus("minecraft", modEventBus);
        MaMServerConfig.get().sound_events.forEach(soundEvent -> {
            String namespace = SafeIdentifier.parse(soundEvent).getNamespace();
            if (!registeredBuses.contains(namespace)) {
                NeoForgeUnifiedRegistries.registerBus(namespace, modEventBus);
                registeredBuses.add(namespace);
            }
        });
        MusicAndMelody.initRegistries();
        modEventBus.addListener(MusicAndMelodyNeoForge::commonSetup);
    }

    private static void commonSetup(final FMLCommonSetupEvent event) {
        MusicAndMelody.init();
    }
}