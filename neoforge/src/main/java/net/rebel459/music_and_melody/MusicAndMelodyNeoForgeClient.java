package net.rebel459.music_and_melody;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.rebel459.music_and_melody.client.AlbumListener;
import net.rebel459.music_and_melody.client.EventListener;
import net.rebel459.music_and_melody.client.PlaylistListener;
import net.rebel459.music_and_melody.config.MaMConfigScreen;

@Mod(value = MusicAndMelody.MOD_ID, dist = Dist.CLIENT)
public class MusicAndMelodyNeoForgeClient {

    public MusicAndMelodyNeoForgeClient(IEventBus modEventBus) {
        MusicAndMelodyClient.initRegistries();
        ModLoadingContext.get().registerExtensionPoint(
                IConfigScreenFactory.class,
                () -> (modContainer, parent) ->
                        new MaMConfigScreen(parent)
        );
        modEventBus.addListener(MusicAndMelodyNeoForgeClient::addClientReloadListeners);
        modEventBus.addListener(MusicAndMelodyNeoForgeClient::commonSetup);
    }

    private static void commonSetup(final FMLCommonSetupEvent event) {
        MusicAndMelodyClient.init();
    }

    private static void addClientReloadListeners(final AddClientReloadListenersEvent event) {
        event.addListener(AlbumListener.ID, new AlbumListener());
        event.addListener(PlaylistListener.ID, new PlaylistListener());
        event.addListener(EventListener.ID, new EventListener());
    }
}
