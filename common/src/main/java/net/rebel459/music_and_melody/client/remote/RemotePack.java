package net.rebel459.music_and_melody.client.remote;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public record RemotePack(
        ResourceLocation id,
        Component name,
        Component description,
        String repository,
        String version,
        String url,
        String sha256,
        long size,
        ResourceLocation icon,
        String atLeastVersion,
        String belowVersion
) {
    public String fileName() {
        String path = this.id.getPath().replace('/', '-');
        return this.id.getNamespace() + "-" + path + ".zip";
    }
}
