package net.rebel459.music_and_melody.client.remote;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public record RemotePack(
        Identifier id,
        Component name,
        Component description,
        String repository,
        String version,
        String url,
        String sha256,
        long size,
        String icon,
        String atLeastVersion,
        String belowVersion,
        Tag tag,
        Provenance provenance
) {
    public enum Tag {
        ALBUM,
        PLAYLIST,
        EVENT,
        THEME;

        public static Tag fromSerialized(String value) {
            try {
                return value == null ? null : valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }

    public enum Provenance {
        OFFICIAL("screen.music_and_melody.repository.official"),
        VERIFIED("screen.music_and_melody.repository.verified"),
        UNVERIFIED("screen.music_and_melody.repository.unverified");

        private final String translationKey;

        Provenance(String translationKey) {
            this.translationKey = translationKey;
        }

        public Component label() {
            return Component.translatable(this.translationKey);
        }
    }

    public String fileName() {
        String path = this.id.getPath().replace('/', '-');
        return this.id.getNamespace() + "-" + path + ".zip";
    }
}
