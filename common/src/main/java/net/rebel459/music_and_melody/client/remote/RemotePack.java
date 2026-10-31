package net.rebel459.music_and_melody.client.remote;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Base64;
import java.util.List;
import java.util.Locale;

public record RemotePack(
        Identifier id,
        Component name,
        Component description,
        String repository,
        String version,
        String url,
        long downloadSize,
        long installSize,
        String icon,
        String showFromVersion,
        String showBelowVersion,
        List<Tag> tags,
        List<String> dependencies,
        Provenance provenance
) {
    private static final long MINIMUM_TOLERANCE = 4L * 1024L * 1024L;
    private static final long UNSPECIFIED_DOWNLOAD_LIMIT = 256L * 1024L * 1024L;
    private static final long UNSPECIFIED_INSTALL_LIMIT = 512L * 1024L * 1024L;

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

    public long downloadLimit() {
        long limit = limitFor(this.downloadSize);
        return this.installSize > 0L ? limit : Math.min(limit, UNSPECIFIED_DOWNLOAD_LIMIT);
    }

    public long installLimit() {
        return this.installSize > 0L ? limitFor(this.installSize) : UNSPECIFIED_INSTALL_LIMIT;
    }

    public long downloadMinimum() {
        return minimumFor(this.downloadSize);
    }

    public long installMinimum() {
        return this.installSize > 0L ? minimumFor(this.installSize) : 0L;
    }

    private static long toleranceFor(long expected) {
        return Math.max(MINIMUM_TOLERANCE, expected / 5L);
    }

    private static long limitFor(long expected) {
        return expected + toleranceFor(expected);
    }

    private static long minimumFor(long expected) {
        return Math.max(0L, expected - toleranceFor(expected));
    }

    public String expectedFileName() {
        String path = URI.create(this.url).getPath();
        if (path == null) return "";
        return path.substring(path.lastIndexOf('/') + 1);
    }

    public String importFileName() {
        String expected = expectedFileName();
        return expected.isEmpty() ? fileName() : expected;
    }

    public String fileName() {
        String path = this.id.getPath().replace('/', '-');
        return this.id.getNamespace() + "-" + path + ".zip";
    }

    public String tempFileName() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(this.id.toString().getBytes(StandardCharsets.UTF_8)) + ".zip";
    }
}
