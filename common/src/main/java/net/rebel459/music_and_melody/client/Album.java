package net.rebel459.music_and_melody.client;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.rebel459.music_and_melody.config.MaMDataConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Album {

    public static Set<Album> ALBUMS = new HashSet<>();
    public static Set<Album> DISABLED_ALBUMS = new HashSet<>();

    public ResourceLocation album;
    public Component name;
    public ResourceLocation icon;
    public List<String> tracks;
    public List<String> discs;
    public Set<String> forcedEnabledTracks;
    public Set<String> forcedUnlockedDiscs;

    public Album(ResourceLocation album, Component name, ResourceLocation icon, List<String> tracks, List<String> discs) {
        this(album, name, icon, tracks, Set.of(), discs, Set.of());
    }

    public Album(ResourceLocation album, Component name, ResourceLocation icon, List<String> tracks, Set<String> forcedEnabledTracks, List<String> discs, Set<String> forcedUnlockedDiscs) {
        this.album = album;
        this.name = name;
        this.icon = icon;
        this.tracks = tracks;
        this.discs = discs;
        this.forcedEnabledTracks = Set.copyOf(forcedEnabledTracks);
        this.forcedUnlockedDiscs = Set.copyOf(forcedUnlockedDiscs);
        ALBUMS.add(this);
        if (!isEnabled()) DISABLED_ALBUMS.add(this);
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
        } else {
            if (!config.albums.disabled_albums.contains(id)) {
                config.albums.disabled_albums.add(id);
            }
            DISABLED_ALBUMS.add(this);
        }

        AutoConfig.getConfigHolder(MaMDataConfig.class).save();
    }

    public ResourceLocation trackId(String song) {
        return ResourceLocation.fromNamespaceAndPath(this.album.getNamespace(), song);
    }

    public boolean isTrackEnabled(String song) {
        return isTrackForcedEnabled(song) || !MaMDataConfig.get().albums.disabled_tracks.contains(trackId(song).toString());
    }

    public boolean isTrackForcedEnabled(String song) {
        return this.forcedEnabledTracks.contains(song);
    }

    public boolean isDiscForcedUnlocked(String disc) {
        return this.forcedUnlockedDiscs.contains(disc);
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

    public record Record(Component name, ResourceLocation icon, List<Track> tracks, List<Disc> discs) {
        public static final Codec<Record> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ComponentSerialization.CODEC.fieldOf("name").forGetter(Record::name),
                ResourceLocation.CODEC.optionalFieldOf("icon", ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png")).forGetter(Record::icon),
                Track.CODEC.listOf().optionalFieldOf("tracks", List.of()).forGetter(Record::tracks),
                Disc.CODEC.listOf().optionalFieldOf("discs", List.of()).forGetter(Record::discs)
        ).apply(instance, Record::new));
    }

    public record Track(String track, boolean enabled) {
        private static final Codec<Track> OBJECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.NON_EMPTY_STRING.fieldOf("track").forGetter(Track::track),
                Codec.BOOL.optionalFieldOf("enabled", false).forGetter(Track::enabled)
        ).apply(instance, Track::new));

        public static final Codec<Track> CODEC = Codec.either(ExtraCodecs.NON_EMPTY_STRING, OBJECT_CODEC).xmap(
                either -> either.map(track -> new Track(track, false), track -> track),
                track -> track.enabled() ? Either.right(track) : Either.left(track.track())
        );
    }

    public record Disc(String disc, boolean unlocked) {
        private static final Codec<Disc> OBJECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.NON_EMPTY_STRING.fieldOf("disc").forGetter(Disc::disc),
                Codec.BOOL.optionalFieldOf("unlocked", false).forGetter(Disc::unlocked)
        ).apply(instance, Disc::new));

        public static final Codec<Disc> CODEC = Codec.either(ExtraCodecs.NON_EMPTY_STRING, OBJECT_CODEC).xmap(
                either -> either.map(track -> new Disc(track, false), disc -> disc),
                disc -> disc.unlocked() ? Either.right(disc) : Either.left(disc.disc())
        );
    }
}
