package net.rebel459.music_and_melody.client;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.rebel459.music_and_melody.client.util.SafeIdentifier;
import net.rebel459.music_and_melody.client.util.CustomAlbums;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.util.*;

public class Album {

    public static Set<Album> ALBUMS = new HashSet<>();
    public static Set<Identifier> LOADED_ALBUMS = new HashSet<>();

    public Identifier album;
    public Component name;
    public Identifier icon;
    public Set<String> tracks;
    public Set<StoredDisc> discs;
    private final Set<String> resolvedTrackIds;

    public Album(Identifier album, Component name, Identifier icon, Set<String> tracks, Set<StoredDisc> discs) {
        this.album = album;
        this.name = name;
        this.icon = icon;
        this.tracks = Collections.unmodifiableSet(new LinkedHashSet<>(tracks));
        this.resolvedTrackIds = this.tracks.stream()
                .map(this::trackId)
                .map(SafeIdentifier::toString)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        this.discs = discs;
        ALBUMS.add(this);
        refreshLoadedAlbums();
    }

    public boolean isEnabled() {
        if (CustomAlbums.isConfigAlbum(this)) return true;
        return this.tracks.isEmpty() || this.tracks.stream().anyMatch(this::isTrackEnabled);
    }

    public boolean isFavourite() {
        return MaMDataConfig.get().albums.favourites.contains(this.album.toString());
    }

    public void setFavourite(boolean favourite) {
        String id = this.album.toString();
        MaMDataConfig config = MaMDataConfig.get();

        if (favourite) {
            if (!config.albums.favourites.contains(id)) {
                config.albums.favourites.add(id);
            }
        } else {
            config.albums.favourites.remove(id);
        }

        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
    }

    public void setEnabled(boolean enabled) {
        if (CustomAlbums.isConfigAlbum(this)) return;
        MaMDataConfig config = MaMDataConfig.get();

        if (enabled) {
            enableTrackIds(trackIds(), config);
        } else if (!this.tracks.isEmpty()) {
            if (!config.albums.disabled_albums.contains(this.album.toString())) {
                config.albums.disabled_albums.add(this.album.toString());
            }
            removeTrackEntriesCoveredByDisabledAlbums(config);
        }

        refreshLoadedAlbums();
        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
    }

    public SafeIdentifier trackId(String song) {
        return song.contains(":") ? SafeIdentifier.parse(song) : SafeIdentifier.fromNamespaceAndPath(this.album.getNamespace(), song);
    }

    public boolean isTrackEnabled(String song) {
        String id = trackId(song).toString();
        return !isTrackDisabled(id, MaMDataConfig.get());
    }

    public void setTrackEnabled(String song, boolean enabled) {
        if (CustomAlbums.isConfigAlbum(this)) return;
        String id = trackId(song).toString();
        MaMDataConfig config = MaMDataConfig.get();

        if (enabled) {
            enableTrackIds(Set.of(id), config);
        } else if (!config.albums.disabled_tracks.contains(id)) {
            config.albums.disabled_tracks.add(id);
            compactFullyDisabledAlbums(config);
        }

        refreshLoadedAlbums();
        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
    }

    private Set<String> trackIds() {
        return this.resolvedTrackIds;
    }

    private static boolean isTrackDisabled(String id, MaMDataConfig config) {
        if (config.albums.disabled_tracks.contains(id)) return true;

        for (Album album : ALBUMS) {
            if (config.albums.disabled_albums.contains(album.album.toString()) && album.trackIds().contains(id)) {
                return true;
            }
        }

        return false;
    }

    private static void enableTrackIds(Set<String> enabledIds, MaMDataConfig config) {
        for (Album album : ALBUMS) {
            String albumId = album.album.toString();
            if (!config.albums.disabled_albums.contains(albumId)) continue;

            Set<String> albumTracks = album.trackIds();
            if (Collections.disjoint(albumTracks, enabledIds)) continue;

            config.albums.disabled_albums.remove(albumId);
            for (String id : albumTracks) {
                if (!enabledIds.contains(id) && !config.albums.disabled_tracks.contains(id)) {
                    config.albums.disabled_tracks.add(id);
                }
            }
        }

        config.albums.disabled_tracks.removeIf(enabledIds::contains);
        removeTrackEntriesCoveredByDisabledAlbums(config);
    }

    private static void compactFullyDisabledAlbums(MaMDataConfig config) {
        boolean changed;
        do {
            changed = false;
            for (Album album : ALBUMS) {
                if (CustomAlbums.isConfigAlbum(album) || album.tracks.isEmpty()) continue;
                String albumId = album.album.toString();
                if (config.albums.disabled_albums.contains(albumId)) continue;
                if (!album.trackIds().stream().allMatch(id -> isTrackDisabled(id, config))) continue;

                config.albums.disabled_albums.add(albumId);
                changed = true;
            }
        } while (changed);

        removeTrackEntriesCoveredByDisabledAlbums(config);
    }

    private static void removeTrackEntriesCoveredByDisabledAlbums(MaMDataConfig config) {
        Set<String> coveredTracks = ALBUMS.stream()
                .filter(album -> config.albums.disabled_albums.contains(album.album.toString()))
                .flatMap(album -> album.trackIds().stream())
                .collect(java.util.stream.Collectors.toSet());
        config.albums.disabled_tracks.removeIf(coveredTracks::contains);
    }

    private static void refreshLoadedAlbums() {
        LOADED_ALBUMS.clear();
        ALBUMS.stream()
                .filter(Album::isEnabled)
                .map(album -> album.album)
                .forEach(LOADED_ALBUMS::add);
    }

    public record StoredDisc(String path, Optional<String> soundEvent, Optional<Component> description) {}

    public record Record(Component name, Identifier icon, List<Track> tracks, List<Disc> discs) {
        public static final Codec<Record> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ComponentSerialization.CODEC.fieldOf("name").forGetter(Record::name),
                Identifier.CODEC.optionalFieldOf("icon", Identifier.withDefaultNamespace("textures/misc/unknown_pack.png")).forGetter(Record::icon),
                Track.CODEC.listOf().optionalFieldOf("tracks", List.of()).forGetter(Record::tracks),
                Disc.CODEC.listOf().optionalFieldOf("discs", List.of()).forGetter(Record::discs)
        ).apply(instance, Record::new));
    }

    public record Track(String path, boolean folder) {
        private static final Codec<Track> OBJECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.NON_EMPTY_STRING.fieldOf("path").forGetter(Track::path),
                Codec.BOOL.optionalFieldOf("folder", false).forGetter(Track::folder)
        ).apply(instance, Track::new));

        public static final Codec<Track> CODEC = Codec.either(ExtraCodecs.NON_EMPTY_STRING, OBJECT_CODEC).xmap(
                either -> either.map(track -> new Track(track, false), track -> track),
                track -> track.folder() ? Either.right(track) : Either.left(track.path())
        );
    }

    public record Disc(String path, Optional<String> soundEvent, Optional<Component> description) {
        private static final Codec<Disc> OBJECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.NON_EMPTY_STRING.fieldOf("path").forGetter(Disc::path),
                ExtraCodecs.NON_EMPTY_STRING.optionalFieldOf("sound_event").forGetter(Disc::soundEvent),
                ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(Disc::description)
        ).apply(instance, Disc::new));

        public static final Codec<Disc> CODEC = Codec.either(ExtraCodecs.NON_EMPTY_STRING, OBJECT_CODEC).xmap(
                either -> either.map(track -> new Disc(track, Optional.empty(), Optional.empty()), disc -> disc),
                disc -> disc.soundEvent().isPresent() || disc.description().isPresent() ? Either.right(disc) : Either.left(disc.path())
        );
    }
}
