package net.rebel459.music_and_melody.client.remote;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
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
        String showFromVersion,
        String showBelowVersion,
        List<Tag> tags,
        List<String> dependencies,
        Provenance provenance
) {
    public RemotePack {
        tags = List.copyOf(tags);
        dependencies = List.copyOf(dependencies);
    }

    public record Key(Identifier id) {
        public static Key of(RemotePack pack) {
            return new Key(pack.id());
        }
    }

    public Key key() {
        return new Key(this.id);
    }

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
