package net.rebel459.music_and_melody;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.rebel459.music_and_melody.client.AlbumListener;
import net.rebel459.music_and_melody.client.EventListener;
import net.rebel459.music_and_melody.client.PlaylistListener;
import net.rebel459.music_and_melody.client.util.JukeboxSongCache;
import net.rebel459.music_and_melody.config.MaMConfigScreen;
import net.rebel459.music_and_melody.platform.client.MaMNeoForgeClientPlatform;

@Mod(value = MusicAndMelody.MOD_ID, dist = Dist.CLIENT)
public class MusicAndMelodyNeoForgeClient {

    public MusicAndMelodyNeoForgeClient(IEventBus modEventBus) {
        MaMNeoForgeClientPlatform.init(modEventBus);
        MusicAndMelodyClient.initRegistries();
        JukeboxSongCache.clear();
        ModList.get().getMods().forEach(mod ->
                JukeboxSongCache.loadFromRoot(mod.getOwningFile().getFile().getFilePath())
        );
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

    private static void addClientReloadListeners(final RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new AlbumListener());
        event.registerReloadListener(new PlaylistListener());
        event.registerReloadListener(new EventListener());
    }
}
