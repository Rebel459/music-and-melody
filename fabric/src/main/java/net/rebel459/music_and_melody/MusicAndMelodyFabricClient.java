package net.rebel459.music_and_melody;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;
import net.rebel459.music_and_melody.client.AlbumListener;
import net.rebel459.music_and_melody.client.EventListener;
import net.rebel459.music_and_melody.client.PlaylistListener;
import net.rebel459.music_and_melody.platform.client.MaMFabricClientPlatform;

public class MusicAndMelodyFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MaMFabricClientPlatform.init();
        MusicAndMelodyClient.initRegistries();
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(AlbumListener.ID, new AlbumListener());
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(PlaylistListener.ID, new PlaylistListener());
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(EventListener.ID, new EventListener());
        MusicAndMelodyClient.init();
    }
}
