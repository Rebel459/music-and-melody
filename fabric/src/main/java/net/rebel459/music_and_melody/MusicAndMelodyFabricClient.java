package net.rebel459.music_and_melody;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;
import net.rebel459.music_and_melody.client.AlbumListener;
import net.rebel459.music_and_melody.client.EventListener;
import net.rebel459.music_and_melody.client.PlaylistListener;
import net.rebel459.music_and_melody.client.util.JukeboxSongCache;
import net.fabricmc.loader.api.FabricLoader;

public class MusicAndMelodyFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MusicAndMelodyClient.initRegistries();
        JukeboxSongCache.clear();
        FabricLoader.getInstance().getAllMods().forEach(mod ->
                mod.getRootPaths().forEach(JukeboxSongCache::loadFromRoot)
        );
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(AlbumListener.ID, new AlbumListener());
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(PlaylistListener.ID, new PlaylistListener());
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(EventListener.ID, new EventListener());
        MusicAndMelodyClient.init();
    }
}
