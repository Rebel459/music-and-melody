package net.rebel459.music_and_melody.client;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.rebel459.music_and_melody.client.Album.Disc;
import net.rebel459.music_and_melody.client.util.SafeLocation;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Album {

    public static Set<Album> ALBUMS = new HashSet<>();
    public static Set<Album> DISABLED_ALBUMS = new HashSet<>();

    public static Set<ResourceLocation> LOADED_ALBUMS = new HashSet<>();

    public ResourceLocation album;
    public Component name;
    public ResourceLocation icon;
    public Set<String> tracks;
    public Set<StoredDisc> discs;
    public Set<String> forcedEnabledTracks;

    public Album(ResourceLocation album, Component name, ResourceLocation icon, Set<String> tracks, Set<StoredDisc> discs) {
        this(album, name, icon, tracks, Set.of(), discs);
    }

    public Album(ResourceLocation album, Component name, ResourceLocation icon, Set<String> tracks, Set<String> forcedEnabledTracks, Set<StoredDisc> discs) {
        this.album = album;
        this.name = name;
        this.icon = icon;
        this.tracks = tracks;
        this.discs = discs;
        this.forcedEnabledTracks = Set.copyOf(forcedEnabledTracks);
        ALBUMS.add(this);
        if (!isEnabled()) DISABLED_ALBUMS.add(this);
        else LOADED_ALBUMS.add(album);
    }

    public boolean isEnabled() {
        return !MaMDataConfig.get().albums.disabled_albums.contains(this.album.toString());
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
        String id = this.album.toString();
        MaMDataConfig config = MaMDataConfig.get();

        if (enabled) {
            config.albums.disabled_albums.remove(id);
            DISABLED_ALBUMS.remove(this);
            LOADED_ALBUMS.add(this.album);
        } else {
            if (!config.albums.disabled_albums.contains(id)) {
                config.albums.disabled_albums.add(id);
            }
            DISABLED_ALBUMS.add(this);
            LOADED_ALBUMS.remove(this.album);
        }

        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
    }

    public SafeLocation trackId(String song) {
        return song.contains(":") ? SafeLocation.parse(song) : SafeLocation.fromNamespaceAndPath(this.album.getNamespace(), song);
    }

    public boolean isTrackEnabled(String song) {
        String id = trackId(song).toString();
        return this.forcedEnabledTracks.contains(id) || !MaMDataConfig.get().albums.disabled_tracks.contains(id);
    }

    public boolean isTrackForcedEnabled(String song) {
        return this.forcedEnabledTracks.contains(trackId(song).toString());
    }

    public void setTrackEnabled(String song, boolean enabled) {
        if (isTrackForcedEnabled(song)) return;
        String id = trackId(song).toString();
        MaMDataConfig config = MaMDataConfig.get();

        if (enabled) {
            config.albums.disabled_tracks.remove(id);
        } else if (!config.albums.disabled_tracks.contains(id)) {
            config.albums.disabled_tracks.add(id);
        }

        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
    }

    public record StoredDisc(String path, Optional<String> soundEvent, Optional<Component> description) {}

    public record Record(Component name, ResourceLocation icon, List<Track> tracks, List<Disc> discs) {
        public static final Codec<Record> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ComponentSerialization.CODEC.fieldOf("name").forGetter(Record::name),
                ResourceLocation.CODEC.optionalFieldOf("icon", ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png")).forGetter(Record::icon),
                Track.CODEC.listOf().optionalFieldOf("tracks", List.of()).forGetter(Record::tracks),
                Disc.CODEC.listOf().optionalFieldOf("discs", List.of()).forGetter(Record::discs)
        ).apply(instance, Record::new));
    }

    public record Track(String path, boolean enabled, boolean folder) {
        private static final Codec<Track> OBJECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.NON_EMPTY_STRING.fieldOf("path").forGetter(Track::path),
                Codec.BOOL.optionalFieldOf("enabled", false).forGetter(Track::enabled),
                Codec.BOOL.optionalFieldOf("folder", false).forGetter(Track::folder)
        ).apply(instance, Track::new));

        public static final Codec<Track> CODEC = Codec.either(ExtraCodecs.NON_EMPTY_STRING, OBJECT_CODEC).xmap(
                either -> either.map(track -> new Track(track, false, false), track -> track),
                track -> track.enabled() || track.folder() ? Either.right(track) : Either.left(track.path())
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
