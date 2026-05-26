package net.rebel459.music_and_melody.client.remote;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public record RemotePack(
        Identifier id,
        Component name,
        Component description,
        String repository,
        String version,
        String url,
        String sha256,
        long size,
        Identifier icon
) {
    public String fileName() {
        String path = this.id.getPath().replace('/', '-');
        return this.id.getNamespace() + "-" + path + ".zip";
    }
}
