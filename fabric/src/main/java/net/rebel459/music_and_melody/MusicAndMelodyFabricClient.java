package net.rebel459.music_and_melody;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.rebel459.music_and_melody.client.AlbumListener;
import net.rebel459.music_and_melody.client.EventListener;
import net.rebel459.music_and_melody.client.PlaylistListener;
import net.rebel459.music_and_melody.platform.client.MaMFabricClientPlatform;
import net.rebel459.music_and_melody.client.util.JukeboxSongCache;
import net.fabricmc.loader.api.FabricLoader;

public class MusicAndMelodyFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MaMFabricClientPlatform.init();
        MusicAndMelodyClient.initRegistries();
        JukeboxSongCache.clear();
        FabricLoader.getInstance().getAllMods().forEach(mod ->
                mod.getRootPaths().forEach(JukeboxSongCache::loadFromRoot)
        );
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new FabricAlbumListener());
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new FabricPlaylistListener());
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new FabricEventListener());
        MusicAndMelodyClient.init();
    }

    public static class FabricAlbumListener extends AlbumListener implements IdentifiableResourceReloadListener {
        @Override
        public ResourceLocation getFabricId() {
            return AlbumListener.ID;
        }
    }

    public static class FabricPlaylistListener extends PlaylistListener implements IdentifiableResourceReloadListener {
        @Override
        public ResourceLocation getFabricId() {
            return PlaylistListener.ID;
        }
    }

    public static class FabricEventListener extends EventListener implements IdentifiableResourceReloadListener {
        @Override
        public ResourceLocation getFabricId() {
            return EventListener.ID;
        }
    }
}
