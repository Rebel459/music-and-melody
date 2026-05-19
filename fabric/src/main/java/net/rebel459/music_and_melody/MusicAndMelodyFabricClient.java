package net.rebel459.music_and_melody;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;
import net.rebel459.music_and_melody.client.AlbumListener;
import net.rebel459.music_and_melody.client.PlaylistListener;

public class MusicAndMelodyFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MusicAndMelodyClient.initRegistries();
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(AlbumListener.ID, new AlbumListener());
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(PlaylistListener.ID, new PlaylistListener());
        MusicAndMelodyClient.init();
    }
}
